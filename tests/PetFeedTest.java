package GameObjects;

import CLib.*;
import GameScreen.GameScreen;
import Main.GameCanvas;
import netcommand.global.GlobalService;

public class PetFeedTest {
    private static void check(boolean ok,String message){if(!ok)throw new RuntimeException(message);}
    private static MainItem gear(int id){MainItem item=new MainItem();item.Id=id;item.ItemCatagory=3;item.itemName="Sword";item.LvItem=10;Item.VecInvetoryPlayer.addElement(item);return item;}
    public static void main(String[] args){
        AutoPetFeed.load();
        PetItem pet=new PetItem();pet.Id=100;pet.ItemCatagory=9;pet.itemName="Pet";Item.VecEquipPlayer.put("14",pet);
        AutoPetFeed.petId=100;AutoPetFeed.petName="Pet";
        MainItem item=gear(1);
        AutoPetFeed.update();check(GlobalService.packets==0,"default off");
        AutoPetFeed.equipmentEnabled=true;
        check(AutoPetFeed.reserves(item),"reserve food from autosell");
        item.isLock=1;check(!AutoPetFeed.eligibleEquipment(item),"locked");item.isLock=0;
        item.tier=1;check(!AutoPetFeed.eligibleEquipment(item),"upgraded");item.tier=0;
        item.colorNameItem=5;check(!AutoPetFeed.eligibleEquipment(item),"unselected color");item.colorNameItem=0;
        item.LvItem=50;check(!AutoPetFeed.eligibleEquipment(item),"level cap");item.LvItem=10;
        item.type_Only_Item=14;check(!AutoPetFeed.eligibleEquipment(item),"egg");item.type_Only_Item=0;
        GameCanvas.currentDialog=new Object();AutoPetFeed.update();check(GlobalService.packets==0,"dialog pause");GameCanvas.currentDialog=null;
        Session_ME.connected=false;AutoPetFeed.update();check(GlobalService.packets==0,"disconnected");Session_ME.connected=true;
        AutoPetFeed.update();check(GlobalService.packets==1 && GlobalService.pet==100 && GlobalService.id==1 && GlobalService.cat==3 && GlobalService.location==1,"equipped pet protocol");
        mSystem.now+=6000;AutoPetFeed.update();check(GlobalService.packets==1,"wait for acknowledgement");
        Item.VecInvetoryPlayer.removeElement(item);AutoPetFeed.update();
        item=gear(2);AutoPetFeed.update();check(GlobalService.packets==2,"next after acknowledgement");
        mSystem.now+=16000;AutoPetFeed.update();check(!AutoPetFeed.equipmentEnabled,"timeout disables");AutoPetFeed.update();check(GlobalService.packets==2,"no retry after timeout");
        Item.VecEquipPlayer.clear();Item.VecInvetoryPlayer.addElement(pet);
        Item sweet=new Item();sweet.Id=50;sweet.ItemCatagory=7;sweet.numPotion=3;sweet.itemName="Candy";Item.VecInvetoryPlayer.addElement(sweet);
        AutoPetFeed.candyId=50;AutoPetFeed.candyCat=7;AutoPetFeed.candyName="Candy";AutoPetFeed.candyEnabled=true;
        AutoPetFeed.update();check(GlobalService.packets==3 && GlobalService.location==0 && GlobalService.cat==7,"inventory pet candy protocol");
        sweet.numPotion--;AutoPetFeed.update();AutoPetFeed.update();check(GlobalService.packets==3,"candy interval");
        mSystem.now+=60000;AutoPetFeed.update();check(GlobalService.packets==4,"candy repeat");
        sweet.numPotion--;AutoPetFeed.update();AutoPetFeed.save();
        GameScreen.player.name="other";AutoPetFeed.load();check(!AutoPetFeed.candyEnabled && AutoPetFeed.petId==-1,"character isolation");
        GameScreen.player.name="test";AutoPetFeed.load();check(AutoPetFeed.candyEnabled && AutoPetFeed.candyId==50 && AutoPetFeed.petId==100,"restore settings");
        Item.VecInvetoryPlayer.removeElement(pet);AutoPetFeed.update();check(GlobalService.packets==4,"missing chosen pet");
        AutoPetFeed.equipmentEnabled=true;AutoPetFeed.candyEnabled=true;AutoPetFeed.stopAll();
        check(!AutoPetFeed.equipmentEnabled && !AutoPetFeed.candyEnabled,"stop all disables feeding");
        Item.VecInvetoryPlayer.addElement(pet);mSystem.now+=60000;AutoPetFeed.update();
        check(GlobalService.packets==4,"stop all prevents further packets");
        System.out.println("PetFeedTest passed");
    }
}
