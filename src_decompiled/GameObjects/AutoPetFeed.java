package GameObjects;

import CLib.*;
import GameScreen.*;
import Main.GameCanvas;
import InterfaceComponents.*;
import Model.CRes;
import netcommand.global.GlobalService;
import java.io.*;
import java.util.Enumeration;

/** Feed only explicitly configured inventory items, one acknowledged request at a time. */
public final class AutoPetFeed {
    public static boolean equipmentEnabled, candyEnabled;
    public static int petId=-1, candyId=-1, candyCat=7, colors=0;
    private static final int ALLOWED_COLORS=30;
    private static final long FEED_INTERVAL=2000L;
    private static final String[] CANDY_NAMES={"Kẹo bạc hà","Kẹo the","Kẹo sữa","Kẹo hương cam"};
    public static String status="Chưa bật", petName="Chưa chọn", candyName="Chưa chọn";
    private static String owner="";
    private static int pendingId=-1, pendingCat, pendingCount, pendingKind;
    private static int lastFeedKind;
    private static long deadline, nextEquipment, nextCandy;
    private static String key(){return "hso_pet_"+Integer.toHexString(owner.hashCode());}
    private static boolean inWorld(){
        if(GameCanvas.currentScreen==null)return false;
        return GameCanvas.currentScreen==GameCanvas.game || GameCanvas.currentScreen==GameCanvas.AllInfo
            || GameCanvas.currentScreen==GameCanvas.shopNpc || GameCanvas.currentScreen==GameCanvas.foodPet
            || GameCanvas.currentScreen==GameCanvas.worldmap || GameCanvas.currentScreen==GameCanvas.buy_sell
            || GameCanvas.currentScreen==GameCanvas.info_other_player || GameCanvas.currentScreen==GameCanvas.mevent
            || GameCanvas.currentScreen==GameCanvas.clan || GameCanvas.currentScreen==GameCanvas.TabEvent;
    }

    public static void load(){
        if(GameScreen.player==null || !inWorld())return;
        String who=LoginScreen.ip+":"+GameCanvas.IndexServer+":"+GameScreen.player.name;
        if(who.equals(owner))return;
        owner=who;equipmentEnabled=candyEnabled=false;petId=candyId=-1;candyCat=7;
        colors=0;
        petName=candyName="Chưa chọn";pendingId=-1;lastFeedKind=0;nextEquipment=nextCandy=0;status="Chưa bật";
        try {
            byte[] data=CRes.loadRMS(key());if(data==null)return;
            DataInputStream in=new DataInputStream(new ByteArrayInputStream(data));
            int version=in.readInt();
            if((version<1 || version>3) || !owner.equals(in.readUTF()))return;
            petId=in.readInt();petName=in.readUTF();candyId=in.readInt();candyCat=in.readInt();candyName=in.readUTF();
            int savedColors=in.readInt();
            // v1/v2 labels used bits 2..5 for blue/yellow/purple/orange.
            colors=(version<3 ? (savedColors&60)>>1 : savedColors)&ALLOWED_COLORS;
            // Discard the retired level cap and intervals from existing profiles.
            if(version==1){in.readInt();in.readInt();in.readInt();}
            equipmentEnabled=in.readBoolean();candyEnabled=in.readBoolean();
            if(!allowedCandyName(candyName)){candyId=-1;candyName="Chưa chọn";candyEnabled=false;}
        }catch(Exception e){equipmentEnabled=candyEnabled=false;}
    }
    public static void save(){
        if(owner.length()==0)return;
        try{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            out.writeInt(3);out.writeUTF(owner);out.writeInt(petId);out.writeUTF(petName);
            out.writeInt(candyId);out.writeInt(candyCat);out.writeUTF(candyName);out.writeInt(colors&ALLOWED_COLORS);
            out.writeBoolean(equipmentEnabled);out.writeBoolean(candyEnabled);
            out.flush();CRes.saveRMS(key(),bytes.toByteArray());
        }catch(Exception e){}
    }
    private static PetItem findPet(){
        for(Enumeration e=Item.VecEquipPlayer.keys();e.hasMoreElements();){
            Object item=Item.VecEquipPlayer.get((String)e.nextElement());
            if(item instanceof PetItem && ((Item)item).Id==petId)return (PetItem)item;
        }
        Item item=Item.getItemInventory(9,(short)petId);
        return item instanceof PetItem?(PetItem)item:null;
    }
    public static boolean eligibleEquipment(Item item){
        return item!=null && item.Id>=0 && item.ItemCatagory==3
            && ((item.type_Only_Item>=0 && item.type_Only_Item<=6) || (item.type_Only_Item>=8 && item.type_Only_Item<=11))
            && !isMedal(item) && item.tier<=0
            && selectedColor(item.colorNameItem)
            && !Item.VecEquipPlayer.contains(item)
            && !(item instanceof MainItem && AutoUpgrade.protects((MainItem)item));
    }
    public static boolean reserves(Item item){
        if(item==null)return false;
        if(pendingId==item.Id && pendingCat==item.ItemCatagory)return true;
        return equipmentEnabled && findPet()!=null && eligibleEquipment(item);
    }
    private static boolean isMedal(Item item){
        String name=item.itemName==null?"":item.itemName.toLowerCase();
        return name.indexOf("mề đay")>=0 || name.indexOf("medal")>=0;
    }
    private static boolean selectedColor(int raw){
        // Match MainTabNew.setTextColorName, including the game's color variants.
        int color=raw;
        if(raw>=20 && raw<50)color=raw%10;
        return color>=1 && color<=4 && (colors&(1<<color))!=0;
    }
    private static boolean allowedCandyName(String name){
        for(int i=0;i<CANDY_NAMES.length;i++)if(CANDY_NAMES[i].equals(name))return true;
        return false;
    }
    private static boolean candy(Item item){
        return item!=null && !isMedal(item) && allowedCandyName(item.itemName)
            && (item.ItemCatagory==4 || item.ItemCatagory==7) && item.numPotion>0;
    }
    /** Replace only the server's successful feeding reward, including manual feeding. */
    public static boolean handleFeedNotice(String message){
        if(message==null || GameScreen.player==null)return false;
        String text=message.trim();
        String lower=text.toLowerCase();
        int points=lower.indexOf("điểm cho ăn");
        if(points<0)points=lower.indexOf("diem cho an");
        if(points<2 || text.charAt(0)!='+')return false;
        // Do not swallow errors, prompts, or unrelated server dialogs.
        String amount=text.substring(1,points).trim();
        if(amount.length()==0)return false;
        for(int i=0;i<amount.length();i++)if(amount.charAt(i)<'0' || amount.charAt(i)>'9')return false;
        GameScreen.player.addChat("Đã cho pet ăn .. "+text,true);
        return true;
    }
    public static void update(){
        load();
        if(GameScreen.player==null || !inWorld() || !Session_ME.gI().isConnected()
            || !LoadMapScreen.isNextMap || GameScreen.player.hp<=0 || GameScreen.player.Action==4)return;
        long now=mSystem.currentTimeMillis();
        if(pendingId>=0){
            Item item=Item.getItemInventory(pendingCat,(short)pendingId);
            if(item==null || (pendingCat!=3 && item.numPotion<pendingCount)){
                pendingId=-1;status="";
            }else if(now>=deadline){
                if(pendingKind==0)equipmentEnabled=false;else candyEnabled=false;
                pendingId=-1;status="Chưa xác nhận cho ăn; đã tắt mục đó";save();
            }
            return;
        }
        // Feed in the background without closing menus or dialogs. Sales recheck
        // reserves(), and eligibleEquipment excludes the active upgrade target.
        if(!equipmentEnabled && !candyEnabled)return;
        PetItem pet=findPet();if(pet==null){status="Chưa có pet đã chọn";return;}
        Item food=null;int kind=1;
        if(candyEnabled && now>=nextCandy){
            Item candidate=Item.getItemInventory(candyCat,(short)candyId);
            if(candy(candidate) && candidate.itemName.equals(candyName))food=candidate;
        }
        if((food==null || lastFeedKind==1) && equipmentEnabled && now>=nextEquipment){
            for(int i=0;i<Item.VecInvetoryPlayer.size();i++){
                Item candidate=(Item)Item.VecInvetoryPlayer.elementAt(i);
                if(eligibleEquipment(candidate)){food=candidate;kind=0;break;}
            }
        }
        if(food==null){
            // Empty bags need not be scanned every frame; retry once per second.
            if(now>=nextEquipment)nextEquipment=now+1000L;
            if(now>=nextCandy)nextCandy=now+1000L;
            status="";return;
        }
        pendingId=food.Id;pendingCat=food.ItemCatagory;pendingCount=food.numPotion;pendingKind=kind;deadline=now+15000L;
        nextEquipment=nextCandy=now+FEED_INTERVAL;
        lastFeedKind=kind;
        GlobalService.gI().Pet_Eat((short)pet.Id,(short)food.Id,(byte)food.ItemCatagory,(byte)(Item.VecEquipPlayer.contains(pet)?1:0));
        status="Chờ pet ăn "+food.itemName;
    }
    public static void stopAll(){
        load();
        equipmentEnabled=candyEnabled=false;
        status="";
        // Keep any in-flight acknowledgement; never send another feed here.
        save();
    }
    private static String on(boolean value){return value?"BẬT":"TẮT";}
    public static String diagnose(){
        load();
        int total=0,eligible=0,upgraded=0,medals=0,wrongColor=0,other=0;
        for(int i=0;i<Item.VecInvetoryPlayer.size();i++){
            Item item=(Item)Item.VecInvetoryPlayer.elementAt(i);
            if(item==null || item.ItemCatagory!=3)continue;
            total++;
            if(eligibleEquipment(item)){eligible++;continue;}
            if(isMedal(item))medals++;
            else if(item.tier>0)upgraded++;
            else if(!selectedColor(item.colorNameItem))wrongColor++;
            else other++;
        }
        String reason;
        if(!equipmentEnabled)reason="Ăn đồ đang tắt.";
        else if(findPet()==null)reason="Không tìm thấy pet đã chọn. Hãy chọn lại pet.";
        else if(!Session_ME.gI().isConnected())reason="Chưa kết nối server.";
        else if(GameScreen.player==null || GameScreen.player.hp<=0 || GameScreen.player.Action==4)reason="Nhân vật đang chết.";
        else if(!inWorld() || !LoadMapScreen.isNextMap)reason="Chưa vào game hoặc đang chuyển map.";
        else if(pendingId>=0)reason="Đã gửi lệnh; đang đợi server cập nhật hành trang.";
        else if(eligible==0)reason="Không có đồ phù hợp trong hành trang.";
        else reason="Có đồ phù hợp. Pet tự ăn ngầm mỗi 2 giây, kể cả khi mở menu.";
        return reason+"\nTrang bị: "+total+"; cho ăn được: "+eligible
            +"\nBỏ qua: đã cộng "+upgraded+", mề đay "+medals
            +", sai màu "+wrongColor+", loại khác/đang dùng "+other+".";
    }
    public static void show(){
        load();mVector menu=new mVector();
        menu.addElement(new iCommand("Pet: "+petName+" >",new IAction(){public void perform(){choosePet();}}));
        menu.addElement(new iCommand("Ăn đồ: "+on(equipmentEnabled),new IAction(){public void perform(){equipmentEnabled=!equipmentEnabled;save();show();}}));
        menu.addElement(new iCommand("Màu đồ cho ăn >",new IAction(){public void perform(){chooseColors();}}));
        menu.addElement(new iCommand("Ăn kẹo: "+on(candyEnabled),new IAction(){public void perform(){candyEnabled=!candyEnabled;save();show();}}));
        menu.addElement(new iCommand("Kẹo: "+candyName+" >",new IAction(){public void perform(){chooseCandy();}}));
        menu.addElement(new iCommand("Kiểm tra cho ăn >",new IAction(){public void perform(){GameCanvas.start_Ok_Dialog(diagnose());}}));
        if(status.length()>0)menu.addElement(new iCommand(status,new IAction(){public void perform(){show();}}));
        menu.addElement(new iCommand("Quay lại",new IAction(){public void perform(){MenuUP.showMenu();}}));
        GameCanvas.menu2.startAt(menu,2,"TỰ ĐỘNG CHO PET ĂN",false,null);
    }
    private static void back(mVector menu,String title){
        menu.addElement(new iCommand("Quay lại",new IAction(){public void perform(){show();}}));
        GameCanvas.menu2.startAt(menu,2,title,false,null);
    }
    private static void addPet(mVector menu,final Item pet){
        if(!(pet instanceof PetItem))return;
        menu.addElement(new iCommand(pet.itemName+" #"+pet.Id,new IAction(){public void perform(){petId=pet.Id;petName=pet.itemName;save();show();}}));
    }
    private static void choosePet(){
        mVector menu=new mVector();
        for(Enumeration e=Item.VecEquipPlayer.keys();e.hasMoreElements();)addPet(menu,(Item)Item.VecEquipPlayer.get((String)e.nextElement()));
        for(int i=0;i<Item.VecInvetoryPlayer.size();i++)addPet(menu,(Item)Item.VecInvetoryPlayer.elementAt(i));
        back(menu,"CHỌN PET");
    }
    private static void chooseCandy(){
        mVector menu=new mVector();
        for(int i=0;i<Item.VecInvetoryPlayer.size();i++){
            final Item item=(Item)Item.VecInvetoryPlayer.elementAt(i);
            if(!candy(item))continue;
            menu.addElement(new iCommand(item.itemName+" ("+item.numPotion+")",new IAction(){public void perform(){
                candyId=item.Id;candyCat=item.ItemCatagory;candyName=item.itemName;save();show();}}));
        }
        back(menu,"CHỌN VẬT PHẨM KẸO CHO PET");
    }
    private static void chooseColors(){
        String[] names={"Xanh dương","Vàng","Tím","Cam"};mVector menu=new mVector();
        for(int i=0;i<names.length;i++){final int bit=1<<(i+1);
            menu.addElement(new iCommand(names[i]+": "+on((colors&bit)!=0),new IAction(){public void perform(){colors^=bit;save();chooseColors();}}));}
        back(menu,"MÀU ĐỒ CHO PET ĂN");
    }
}
