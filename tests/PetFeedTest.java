package GameObjects;

import CLib.*;
import GameScreen.GameScreen;
import GameScreen.LoginScreen;
import Main.GameCanvas;
import netcommand.global.GlobalService;

public class PetFeedTest {
    private static void check(boolean ok,String message){if(!ok)throw new RuntimeException(message);}
    private static MainItem gear(int id){MainItem item=new MainItem();item.Id=id;item.ItemCatagory=3;item.itemName="Sword";item.LvItem=10;item.colorNameItem=1;Item.VecInvetoryPlayer.addElement(item);return item;}
    public static void main(String[] args) throws Exception {
        AutoPetFeed.load();
        PetItem pet=new PetItem();pet.Id=100;pet.ItemCatagory=9;pet.itemName="Pet";Item.VecEquipPlayer.put("14",pet);
        AutoPetFeed.petId=100;AutoPetFeed.petName="Pet";
        MainItem item=gear(1);
        AutoPetFeed.update();check(GlobalService.packets==0,"default off");
        AutoPetFeed.equipmentEnabled=true;AutoPetFeed.colors=2;
        check(AutoPetFeed.reserves(item),"reserve food from autosell");
        item.isLock=1;check(AutoPetFeed.eligibleEquipment(item),"locked unupgraded food allowed");item.isLock=0;
        item.tier=1;check(!AutoPetFeed.eligibleEquipment(item),"upgraded");item.tier=0;
        item.tier=-1;check(AutoPetFeed.eligibleEquipment(item),"non-upgraded negative tier");item.tier=0;
        check(AutoPetFeed.diagnose().indexOf("cho ăn được: 1")>=0,"diagnosis counts eligible food");
        item.isLock=1;check(AutoPetFeed.diagnose().indexOf("cho ăn được: 1")>=0,"diagnosis includes locked food");item.isLock=0;
        item.colorNameItem=5;check(!AutoPetFeed.eligibleEquipment(item),"unselected color");item.colorNameItem=1;
        item.LvItem=50;check(AutoPetFeed.eligibleEquipment(item),"no level cap");item.LvItem=10;
        AutoPetFeed.colors=63;
        for(int color=0;color<6;color++){
            item.colorNameItem=color;
            check(AutoPetFeed.eligibleEquipment(item)==(color>=1 && color<=4),"allowed colors only");
            item.itemName="Mề đay sức mạnh";
            check(!AutoPetFeed.eligibleEquipment(item),"medal blocked for every color");
            item.itemName="Sword";
        }
        int[] bases={0,20,30,40};
        for(int b=0;b<bases.length;b++)for(int color=0;color<10;color++){
            item.colorNameItem=bases[b]+color;
            check(AutoPetFeed.eligibleEquipment(item)==(color>=1 && color<=4),"renderer color variants");
        }
        for(int selected=1;selected<=4;selected++){
            AutoPetFeed.colors=1<<selected;
            for(int color=1;color<=4;color++){
                item.colorNameItem=color;
                check(AutoPetFeed.eligibleEquipment(item)==(color==selected),"each selection matches rendered color");
            }
        }
        AutoPetFeed.colors=30;item.colorNameItem=1;
        for(int type=8;type<=11;type++){
            item.type_Only_Item=type;check(AutoPetFeed.eligibleEquipment(item),"weapon food allowed");
            item.itemName="Mề đay";check(!AutoPetFeed.eligibleEquipment(item),"medal name always blocked");item.itemName="Sword";
        }
        item.type_Only_Item=12;check(!AutoPetFeed.eligibleEquipment(item),"pet equipment excluded");
        item.type_Only_Item=7;check(!AutoPetFeed.eligibleEquipment(item),"special equipment blocked");
        item.type_Only_Item=14;check(!AutoPetFeed.eligibleEquipment(item),"egg");item.type_Only_Item=0;
        Object openDialog=new Object();GameCanvas.currentDialog=openDialog;
        GameCanvas.menu2.isShowMenu=true;AutoSellManager.isReturningToSell=true;
        AutoUpgrade.active=true;AutoUpgrade.itemId=item.Id;
        check(!AutoPetFeed.eligibleEquipment(item),"active upgrade target protected");
        AutoUpgrade.itemId=999;item.isLock=1;
        GameCanvas.currentScreen=new Object();AutoPetFeed.update();check(GlobalService.packets==0,"login screen excluded");
        GameCanvas.currentScreen=GameCanvas.AllInfo;
        AutoSellManager.queued=3;
        Session_ME.connected=false;AutoPetFeed.update();check(GlobalService.packets==0,"disconnected");Session_ME.connected=true;
        AutoPetFeed.update();check(GlobalService.packets==1 && GlobalService.pet==100 && GlobalService.id==1 && GlobalService.cat==3 && GlobalService.location==1,"equipped pet protocol");
        check(GameCanvas.currentDialog==openDialog && GameCanvas.menu2.isShowMenu,"background feeding preserves UI");
        GameCanvas.currentDialog=null;GameCanvas.menu2.isShowMenu=false;AutoSellManager.isReturningToSell=false;AutoUpgrade.active=false;
        GameCanvas.currentScreen=GameCanvas.shopNpc;
        check(AutoPetFeed.reserves(item),"queued sales cannot take pending pet food");AutoSellManager.queued=0;
        mSystem.now+=1000;AutoPetFeed.update();check(GlobalService.packets==1,"wait for acknowledgement");
        check(!AutoPetFeed.handleFeedNotice("Không đủ điểm cho ăn"),"error dialog preserved");
        check(!AutoPetFeed.handleFeedNotice("Bạn có muốn cho pet ăn?"),"confirmation preserved");
        check(!AutoPetFeed.handleFeedNotice(null),"null notice");
        check(AutoPetFeed.handleFeedNotice("+20 điểm cho ăn, +7"),"feed reward dialog intercepted");
        check("Đã cho pet ăn .. +20 điểm cho ăn, +7".equals(GameScreen.player.lastChat),"server reward in overhead chat");
        AutoPetFeed.update();check(GlobalService.packets==1,"notice still waits for inventory acknowledgement");
        Item.VecInvetoryPlayer.removeElement(item);AutoPetFeed.update();
        check(GameScreen.player.chats==1 && GameCanvas.msgchat.messages==0,"no duplicate generic chat on inventory update");
        check(GameCanvas.notifications==0 && AutoPetFeed.status.length()==0,"no success status or popup");
        item=gear(2);mSystem.now+=999;AutoPetFeed.update();check(GlobalService.packets==1,"wait full two seconds");
        mSystem.now++;AutoPetFeed.update();check(GlobalService.packets==2,"next while shop open");GameCanvas.currentScreen=GameCanvas.game;
        mSystem.now+=16000;AutoPetFeed.update();check(!AutoPetFeed.equipmentEnabled,"timeout disables");AutoPetFeed.update();check(GlobalService.packets==2,"no retry after timeout");
        Item.VecEquipPlayer.clear();Item.VecInvetoryPlayer.addElement(pet);
        Item sweet=new Item();sweet.Id=50;sweet.ItemCatagory=7;sweet.numPotion=3;sweet.itemName="Kẹo bạc hà";Item.VecInvetoryPlayer.addElement(sweet);
        AutoPetFeed.candyId=50;AutoPetFeed.candyCat=7;AutoPetFeed.candyName="Kẹo bạc hà";AutoPetFeed.candyEnabled=true;
        String[] rejected={"Thuốc hồi máu","Nguyên liệu mề đay","Kẹo khác"};
        for(int i=0;i<rejected.length;i++){
            sweet.itemName=rejected[i];AutoPetFeed.candyName=rejected[i];
            AutoPetFeed.update();check(GlobalService.packets==2,"non candy blocked");mSystem.now+=2000;
        }
        sweet.itemName="Kẹo bạc hà";AutoPetFeed.candyName=sweet.itemName;
        AutoPetFeed.update();check(GlobalService.packets==3 && GlobalService.location==0 && GlobalService.cat==7,"inventory pet candy protocol");
        sweet.numPotion--;AutoPetFeed.update();AutoPetFeed.update();check(GlobalService.packets==3,"candy interval");
        mSystem.now+=1999;AutoPetFeed.update();check(GlobalService.packets==3,"candy waits two seconds");
        mSystem.now++;AutoPetFeed.update();check(GlobalService.packets==4,"candy repeat");
        sweet.numPotion--;AutoPetFeed.update();AutoPetFeed.save();
        GameScreen.player.name="other";AutoPetFeed.load();check(!AutoPetFeed.candyEnabled && AutoPetFeed.petId==-1,"character isolation");
        GameScreen.player.name="test";AutoPetFeed.load();check(AutoPetFeed.candyEnabled && AutoPetFeed.candyId==50 && AutoPetFeed.petId==100,"restore settings");
        Item.VecInvetoryPlayer.removeElement(pet);AutoPetFeed.update();check(GlobalService.packets==4,"missing chosen pet");
        AutoPetFeed.equipmentEnabled=true;AutoPetFeed.candyEnabled=true;AutoPetFeed.stopAll();
        check(!AutoPetFeed.equipmentEnabled && !AutoPetFeed.candyEnabled,"stop all disables feeding");
        Item.VecInvetoryPlayer.addElement(pet);mSystem.now+=60000;AutoPetFeed.update();
        check(GlobalService.packets==4,"stop all prevents further packets");
        check(AutoPetFeed.status.length()==0,"no disabled status");
        String[] allowed={"Kẹo bạc hà","Kẹo the","Kẹo sữa","Kẹo hương cam"};
        AutoPetFeed.candyEnabled=true;
        for(int i=0;i<allowed.length;i++){
            sweet.itemName=allowed[i];sweet.numPotion=10;AutoPetFeed.candyName=allowed[i];
            mSystem.now+=2000;AutoPetFeed.update();check(GlobalService.packets==5+i,"each allowed candy");
            sweet.numPotion--;AutoPetFeed.update();
        }
        AutoPetFeed.equipmentEnabled=true;AutoPetFeed.colors=2;
        item.type_Only_Item=8;
        mSystem.now+=2000;AutoPetFeed.update();
        check(GlobalService.packets==9 && GlobalService.id==item.Id && GlobalService.cat==3,"equipment gets turn while candy remains");
        Item.VecInvetoryPlayer.removeElement(item);AutoPetFeed.update();
        MainItem next=gear(3);
        mSystem.now+=1999;AutoPetFeed.update();check(GlobalService.packets==9,"shared interval");
        mSystem.now++;AutoPetFeed.update();check(GlobalService.packets==10 && GlobalService.cat==7,"candy gets next turn");
        sweet.numPotion--;AutoPetFeed.update();
        mSystem.now+=2000;AutoPetFeed.update();check(GlobalService.packets==11 && GlobalService.id==3,"equipment not starved");
        Item.VecInvetoryPlayer.removeElement(next);AutoPetFeed.update();
        sweet.numPotion=0;mSystem.now+=2000;AutoPetFeed.update();
        check(AutoPetFeed.status.length()==0,"no waiting status when food absent");
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream out=new java.io.DataOutputStream(bytes);
        String owner=LoginScreen.ip+":"+GameCanvas.IndexServer+":legacy";
        out.writeInt(1);out.writeUTF(owner);out.writeInt(100);out.writeUTF("Pet");
        out.writeInt(50);out.writeInt(7);out.writeUTF("Thuốc hồi máu");
        out.writeInt(63);out.writeInt(30);out.writeInt(5);out.writeInt(60);
        out.writeBoolean(true);out.writeBoolean(true);out.flush();
        Model.CRes.saveRMS("hso_pet_"+Integer.toHexString(owner.hashCode()),bytes.toByteArray());
        GameScreen.player.name="legacy";AutoPetFeed.load();
        check(AutoPetFeed.colors==30 && AutoPetFeed.equipmentEnabled,"legacy colors sanitized");
        check(!AutoPetFeed.candyEnabled && AutoPetFeed.candyId==-1,"legacy invalid candy cleared");
        AutoPetFeed.save();GameScreen.player.name="other";AutoPetFeed.load();
        GameScreen.player.name="legacy";AutoPetFeed.load();
        check(AutoPetFeed.colors==30 && AutoPetFeed.petId==100,"migrated settings restored");
        bytes=new java.io.ByteArrayOutputStream();out=new java.io.DataOutputStream(bytes);
        owner=LoginScreen.ip+":"+GameCanvas.IndexServer+":legacyBlue";
        out.writeInt(2);out.writeUTF(owner);out.writeInt(100);out.writeUTF("Pet");
        out.writeInt(-1);out.writeInt(7);out.writeUTF("Chưa chọn");out.writeInt(4);
        out.writeBoolean(true);out.writeBoolean(false);out.flush();
        Model.CRes.saveRMS("hso_pet_"+Integer.toHexString(owner.hashCode()),bytes.toByteArray());
        GameScreen.player.name="legacyBlue";AutoPetFeed.load();
        check(AutoPetFeed.colors==2,"v2 blue label migrates to real blue code");
        item=gear(4);item.isLock=1;
        check(AutoPetFeed.eligibleEquipment(item),"locked blue food after migration");
        item.colorNameItem=2;check(!AutoPetFeed.eligibleEquipment(item),"migration does not select yellow");
        System.out.println("PetFeedTest passed");
    }
}
