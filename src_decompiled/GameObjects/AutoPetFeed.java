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
    public static int petId=-1, candyId=-1, candyCat=7, colors=1, maxLevel=30;
    public static int equipmentSeconds=5, candySeconds=60;
    public static String status="Chưa bật", petName="Chưa chọn", candyName="Chưa chọn";
    private static String owner="";
    private static int pendingId=-1, pendingCat, pendingCount, pendingKind;
    private static long deadline, nextEquipment, nextCandy;
    private static String key(){return "hso_pet_"+Integer.toHexString(owner.hashCode());}

    public static void load(){
        if(GameScreen.player==null || GameCanvas.currentScreen!=GameCanvas.game)return;
        String who=LoginScreen.ip+":"+GameCanvas.IndexServer+":"+GameScreen.player.name;
        if(who.equals(owner))return;
        owner=who;equipmentEnabled=candyEnabled=false;petId=candyId=-1;candyCat=7;
        colors=1;maxLevel=30;equipmentSeconds=5;candySeconds=60;
        petName=candyName="Chưa chọn";pendingId=-1;nextEquipment=nextCandy=0;status="Chưa bật";
        try {
            byte[] data=CRes.loadRMS(key());if(data==null)return;
            DataInputStream in=new DataInputStream(new ByteArrayInputStream(data));
            if(in.readInt()!=1 || !owner.equals(in.readUTF()))return;
            petId=in.readInt();petName=in.readUTF();candyId=in.readInt();candyCat=in.readInt();candyName=in.readUTF();
            colors=in.readInt()&63;maxLevel=in.readInt();equipmentSeconds=in.readInt();candySeconds=in.readInt();
            equipmentEnabled=in.readBoolean();candyEnabled=in.readBoolean();
            if(maxLevel<1 || maxLevel>1000 || equipmentSeconds<2 || equipmentSeconds>86400 || candySeconds<2 || candySeconds>86400)
                throw new IOException();
        }catch(Exception e){equipmentEnabled=candyEnabled=false;}
    }
    public static void save(){
        if(owner.length()==0)return;
        try{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            out.writeInt(1);out.writeUTF(owner);out.writeInt(petId);out.writeUTF(petName);
            out.writeInt(candyId);out.writeInt(candyCat);out.writeUTF(candyName);out.writeInt(colors);out.writeInt(maxLevel);
            out.writeInt(equipmentSeconds);out.writeInt(candySeconds);out.writeBoolean(equipmentEnabled);out.writeBoolean(candyEnabled);
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
        return item!=null && item.Id>=0 && item.ItemCatagory==3 && item.type_Only_Item>=0 && item.type_Only_Item<=6
            && item.isLock==0 && item.tier==0 && item.LvItem<=maxLevel
            && item.colorNameItem>=0 && item.colorNameItem<6 && (colors&(1<<item.colorNameItem))!=0
            && !Item.VecEquipPlayer.contains(item)
            && !(item instanceof MainItem && AutoUpgrade.protects((MainItem)item));
    }
    public static boolean reserves(Item item){
        if(item==null)return false;
        if(pendingId==item.Id && pendingCat==item.ItemCatagory)return true;
        return equipmentEnabled && findPet()!=null && eligibleEquipment(item);
    }
    private static boolean candy(Item item){
        return item!=null && (item.ItemCatagory==4 || item.ItemCatagory==7) && item.numPotion>0;
    }
    public static void update(){
        load();
        if(GameScreen.player==null || GameCanvas.currentScreen!=GameCanvas.game || !Session_ME.gI().isConnected()
            || !LoadMapScreen.isNextMap || GameScreen.player.hp<=0 || GameScreen.player.Action==4)return;
        long now=mSystem.currentTimeMillis();
        if(pendingId>=0){
            Item item=Item.getItemInventory(pendingCat,(short)pendingId);
            if(item==null || (pendingCat!=3 && item.numPotion<pendingCount)){
                pendingId=-1;status="Đã cho pet ăn";
            }else if(now>=deadline){
                if(pendingKind==0)equipmentEnabled=false;else candyEnabled=false;
                pendingId=-1;status="Chưa xác nhận cho ăn; đã tắt mục đó";save();
            }
            return;
        }
        if(GameCanvas.currentDialog!=null || GameCanvas.menu2.isShowMenu || AutoUpgrade.active
            || AutoSellManager.isReturningToSell || AutoSellManager.getQueueCount()>0)return;
        if(!equipmentEnabled && !candyEnabled)return;
        PetItem pet=findPet();if(pet==null){status="Chưa có pet đã chọn";return;}
        Item food=null;int kind=1;
        if(candyEnabled && now>=nextCandy){
            Item candidate=Item.getItemInventory(candyCat,(short)candyId);
            if(candy(candidate) && candidate.itemName.equals(candyName))food=candidate;
        }
        if(food==null && equipmentEnabled && now>=nextEquipment){
            kind=0;
            for(int i=0;i<Item.VecInvetoryPlayer.size();i++){
                Item candidate=(Item)Item.VecInvetoryPlayer.elementAt(i);
                if(eligibleEquipment(candidate)){food=candidate;break;}
            }
        }
        if(food==null){status="Chờ đồ/kẹo hoặc chu kỳ";return;}
        pendingId=food.Id;pendingCat=food.ItemCatagory;pendingCount=food.numPotion;pendingKind=kind;deadline=now+15000L;
        if(kind==0)nextEquipment=now+equipmentSeconds*1000L;else nextCandy=now+candySeconds*1000L;
        GlobalService.gI().Pet_Eat((short)pet.Id,(short)food.Id,(byte)food.ItemCatagory,(byte)(Item.VecEquipPlayer.contains(pet)?1:0));
        status="Chờ pet ăn "+food.itemName;
    }
    public static void stopAll(){
        load();
        equipmentEnabled=candyEnabled=false;
        status="Đã tắt";
        // Keep any in-flight acknowledgement; never send another feed here.
        save();
    }
    private static String on(boolean value){return value?"BẬT":"TẮT";}
    public static void show(){
        load();mVector menu=new mVector();
        menu.addElement(new iCommand("Pet: "+petName+" >",new IAction(){public void perform(){choosePet();}}));
        menu.addElement(new iCommand("Ăn đồ: "+on(equipmentEnabled),new IAction(){public void perform(){equipmentEnabled=!equipmentEnabled;save();show();}}));
        menu.addElement(new iCommand("Màu đồ cho ăn >",new IAction(){public void perform(){chooseColors();}}));
        menu.addElement(new iCommand("Cấp đồ tối đa: "+maxLevel,new IAction(){public void perform(){input(0);}}));
        menu.addElement(new iCommand("Chu kỳ ăn đồ: "+equipmentSeconds+" giây",new IAction(){public void perform(){input(1);}}));
        menu.addElement(new iCommand("Ăn kẹo: "+on(candyEnabled),new IAction(){public void perform(){candyEnabled=!candyEnabled;save();show();}}));
        menu.addElement(new iCommand("Kẹo: "+candyName+" >",new IAction(){public void perform(){chooseCandy();}}));
        menu.addElement(new iCommand("Chu kỳ ăn kẹo: "+candySeconds+" giây",new IAction(){public void perform(){input(2);}}));
        menu.addElement(new iCommand(status,new IAction(){public void perform(){show();}}));
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
        String[] names={"Trắng","Xanh lá","Xanh dương","Vàng","Tím","Cam"};mVector menu=new mVector();
        for(int i=0;i<names.length;i++){final int bit=1<<i;
            menu.addElement(new iCommand(names[i]+": "+on((colors&bit)!=0),new IAction(){public void perform(){colors^=bit;save();chooseColors();}}));}
        back(menu,"MÀU ĐỒ CHO PET ĂN");
    }
    private static void input(final int field){
        final InputDialog dialog=new InputDialog();
        dialog.setinfo(field==0?"Cấp đồ tối đa (1-1000)":"Chu kỳ giây (2-86400)",new iCommand("Lưu",new IAction(){public void perform(){
            try{int value=Integer.parseInt(dialog.tfInput.getText().trim());
                if(value<(field==0?1:2) || value>(field==0?1000:86400))throw new NumberFormatException();
                if(field==0)maxLevel=value;else if(field==1)equipmentSeconds=value;else candySeconds=value;
                save();GameCanvas.end_Dialog();show();
            }catch(Exception e){GameCanvas.addInfoCharCline("Giá trị ngoài phạm vi cho phép");}
        }}),true,"");GameCanvas.currentDialog=dialog;
    }
}
