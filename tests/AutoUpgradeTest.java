package GameObjects;
import CLib.mSystem;
import GameScreen.*;
import InterfaceComponents.*;
import Main.GameCanvas;
import netcommand.global.GlobalService;
import net.Message;
import java.io.*;

public class AutoUpgradeTest {
    static int checks;
    static TabRebuildItem tab;
    static void check(boolean b,String s){checks++;if(!b)throw new RuntimeException(s);}
    static MainItem item(int id,int cat,int count,String name){
        MainItem it=new MainItem();it.Id=id;it.ItemCatagory=cat;it.numPotion=count;it.itemName=name;
        Item.VecInvetoryPlayer.addElement(it);return it;
    }
    static void setup(int tier,int target){
        if(AutoUpgrade.active)AutoUpgrade.stop("reset");
        GameScreen.player=new Player();GameCanvas.currentScreen=GameCanvas.shopNpc;
        GameCanvas.currentDialog=null;GameCanvas.menu2.isShowMenu=false;
        GameCanvas.shopNpc.VecTabScreen.removeAllElements();
        tab=new TabRebuildItem();GameCanvas.shopNpc.VecTabScreen.addElement(tab);
        Item.VecInvetoryPlayer.removeAllElements();
        MainItem gear=item(100,3,1,"Sword");gear.tier=tier;
        // The real forge displays a copy, not the inventory object.
        TabRebuildItem.itemRe=new MainItem();TabRebuildItem.itemRe.Id=100;
        TabRebuildItem.itemRe.ItemCatagory=3;TabRebuildItem.itemRe.tier=tier;
        item(20,7,999,"Ore");
        item(31,7,10,"Co 3 la").typeMaterial=11;
        item(41,7,10,"Co 4 la").typeMaterial=11;
        item(51,7,10,"Co 3 mau").typeMaterial=11;
        TabRebuildItem.isLucky=false;TabRebuildItem.itemDaMayMan=null;
        TabRebuildItem.isBeginEff=0;TabRebuildItem.isNextRebuild=0;
        TabRebuildItem.idMaterial=new short[]{20};
        TabRebuildItem.dataRebuild=new DataRebuildItem[15];
        for(int i=0;i<15;i++)TabRebuildItem.dataRebuild[i]=new DataRebuildItem((byte)i,100,5,new byte[]{2});
        AutoUpgrade.loadConfig();AutoUpgrade.targetLevel=target;AutoUpgrade.currency=0;
        AutoUpgrade.delayMs=500;AutoUpgradeMenu.isShowing=false;
        AutoUpgrade.policy=new byte[16];
        GlobalService.packets=0;GlobalService.attempts=0;
        GameCanvas.notifications=0;
        mSystem.now+=1000;
    }
    static void tick(long ms){mSystem.now+=ms;AutoUpgrade.update();}
    static void start(){check(AutoUpgrade.start(tab),"start");tick(1);}
    static void ackClover(int id){
        TabRebuildItem.isLucky=true;AutoUpgrade.onReply(new int[]{0,id,7,-1});tick(400);
    }
    static void inventory(int tier){
        Item old=Item.getItemInventory(3,(short)100);
        Item.VecInvetoryPlayer.removeElement(old);
        MainItem next=item(100,3,1,"Sword");next.tier=tier;
        AutoUpgrade.onInventory();
    }
    static void result(int value){
        TabRebuildItem.isBeginEff=1;AutoUpgrade.onReply(new int[]{2,-1,-1,value});
    }
    static void finishEffect(){
        TabRebuildItem.itemRe=(MainItem)Item.getItemInventory(3,(short)100);
        TabRebuildItem.isBeginEff=0;TabRebuildItem.isLucky=false;TabRebuildItem.itemDaMayMan=null;
        tick(1);tick(4000);
    }
    static void policies(){
        setup(6,9);AutoUpgrade.policy[7]=1;AutoUpgrade.policy[8]=2;AutoUpgrade.policy[9]=3;
        start();check(GlobalService.op==0&&GlobalService.id==31,"+7 needs 3 leaves");
        tick(10000);check(GlobalService.packets==1,"no attempt before clover acknowledgement");
        AutoUpgrade.onReply(new int[]{0,41,7,-1});tick(1);check(GlobalService.packets==1,"wrong clover ack ignored");
        ackClover(31);check(GlobalService.op==2&&GlobalService.cat==0,"coin attempt");
        result(3);
        inventory(7);finishEffect();
        check(GlobalService.op==0&&GlobalService.id==41,"+8 needs 4 leaves");
        ackClover(41);check(GlobalService.attempts==2,"second attempt");
        result(3);
        inventory(8);finishEffect();
        check(GlobalService.op==0&&GlobalService.id==51,"+9 needs 3 colors");
        ackClover(51);check(GlobalService.attempts==3,"third attempt");
        result(3);inventory(9);tick(1);
        check(!AutoUpgrade.active&&GlobalService.attempts==3,"stop immediately at target");
        for(int i=0;i<100;i++)tick(1000);
        check(GlobalService.attempts==3,"never over-upgrade");
    }
    static void failureAndTimeout(){
        setup(7,8);start();check(GlobalService.op==2,"no clover requested");
        result(4);finishEffect();
        check(AutoUpgrade.active&&GlobalService.attempts==2,"retry acknowledged failure at same tier without inventory packet");
        result(4);inventory(6);finishEffect();
        check(GlobalService.attempts==3,"downgraded item can retry with new tier policy");
        tick(30001);check(!AutoUpgrade.active,"timeout stops");
        tick(90000);check(GlobalService.attempts==3,"paid request never resent after timeout");
        setup(7,8);start();AutoUpgrade.onReply(new int[]{3,-1,-1,-1});
        check(!AutoUpgrade.active&&GlobalService.attempts==1,"server error stops");
        setup(7,8);start();GameCanvas.currentScreen=new Object();tick(1);
        check(!AutoUpgrade.active,"leaving forge stops");
        setup(7,8);start();GameScreen.player=new Player();tick(1);
        check(!AutoUpgrade.active,"character change stops");
        setup(7,8);start();TabRebuildItem.itemRe=item(101,3,1,"Other");tick(1);
        check(!AutoUpgrade.active&&GlobalService.attempts==1,"item swap stops");
    }
    static void continuousWithoutReselect(){
        setup(0,3);AutoUpgrade.delayMs=500;start();
        for(int tier=1;tier<=3;tier++){
            check(GlobalService.op==2&&GlobalService.attempts==tier,"next paid attempt without selection acknowledgement");
            result(3);inventory(tier);tick(1);
            if(tier<3){
                check(GameCanvas.notifications==0,"no per-attempt notifications");
                tick(3998);check(GlobalService.attempts==tier,"server pacing respected");
                tick(1);
            }
        }
        check(!AutoUpgrade.active&&GlobalService.packets==3,"three upgrades use only three paid packets and stop at target");
        check(GameCanvas.notifications==1,"one completion notification for entire run");
    }
    static void delayedServer(){
        setup(0,5);start();result(3);tick(1);
        check(Item.getItemInventory(3,(short)100).tier==0,"result cannot invent inventory tier");
        check(TabRebuildItem.itemRe.tier==0,"display cannot invent upgraded tier");
        tick(6000);check(GlobalService.attempts==1,"wait for delayed authoritative item even after pacing interval");
        inventory(1);tick(1);tick(1);
        check(GlobalService.attempts==2,"resume when delayed item arrives");
        // A leftover native animation value must not acknowledge attempt two.
        TabRebuildItem.isNextRebuild=3;tick(5000);
        check(GlobalService.attempts==2,"stale animation result cannot trigger another paid request");
        result(4);tick(1);tick(500);
        check(GlobalService.attempts==3,"confirmed failure resumes");

        setup(0,2);start();inventory(1);tick(5000);
        check(GlobalService.attempts==1,"inventory arriving first still waits for result");
        result(3);tick(1);tick(500);
        check(GlobalService.attempts==2,"inventory before result also resumes");
        inventory(2);tick(1);
        check(!AutoUpgrade.active&&GlobalService.attempts==2,"authoritative target stops without extra paid request");

        setup(0,3);start();result(3);tick(30001);
        check(!AutoUpgrade.active&&GlobalService.attempts==1,"missing authoritative item stops without fake tier or retry");
        check(AutoUpgrade.status.indexOf("vật phẩm")>=0,"timeout identifies missing inventory");

        setup(0,3);AutoUpgrade.delayMs=7000;start();result(4);tick(1);
        tick(6998);check(GlobalService.attempts==1,"long configured delay preserved");
        tick(1);check(GlobalService.attempts==2,"long delay eventually resumes");
    }
    static void wireReplies() throws Exception {
        setup(0,3);start();
        Message out=new Message(67);out.writer().writeByte(2);out.writer().writeByte(3);out.writer().writeUTF("OK");
        Message in=new Message((byte)67,out.getData());
        int[] reply=AutoUpgrade.peekReply(in);
        // Same read order and fields as the shipped native ReadMessenge parser.
        check(in.reader().readByte()==2,"native parser sees unconsumed opcode");
        TabRebuildItem.isBeginEff=1;TabRebuildItem.isNextRebuild=in.reader().readByte();
        TabRebuildItem.contentShow=in.reader().readUTF();
        AutoUpgrade.onReply(reply);inventory(1);tick(1);
        AutoUpgrade.onReply(reply);tick(3999);
        check(GlobalService.attempts==2,"duplicate callback during ready cannot stall next attempt");
        check(Item.getItemInventory(3,(short)100).tier==1,"duplicate result cannot increase item twice");
    }
    static void materials(){
        setup(7,8);AutoUpgrade.policy[8]=2;
        Item.VecInvetoryPlayer.removeElement(Item.getItemInventory(7,(short)41));
        start();check(!AutoUpgrade.active&&GlobalService.packets==0,"missing exact clover cannot substitute");
        setup(7,8);TabRebuildItem.isLucky=true;
        TabRebuildItem.itemDaMayMan=Item.getItemInventory(7,(short)31);
        start();check(GlobalService.op==1&&GlobalService.cat==7,"remove existing clover for NONE");
        tick(1000);check(GlobalService.attempts==0,"wait for remove ack");
        TabRebuildItem.isLucky=false;AutoUpgrade.onReply(new int[]{1,-1,7,-1});tick(400);
        check(GlobalService.attempts==1,"attempt after removal confirmed");
        setup(7,8);GameScreen.player.coin=0;start();
        check(!AutoUpgrade.active&&GlobalService.packets==0,"insufficient coins cannot spend gems");
        setup(7,8);AutoUpgrade.currency=1;start();check(GlobalService.cat==1,"explicit gem choice");
        setup(7,8);Item.getItemInventory(7,(short)20).numPotion=1;start();
        check(!AutoUpgrade.active&&GlobalService.attempts==0,"insufficient ore");
        setup(7,8);TabRebuildItem.dataRebuild[7].mValue[0]=-1;start();
        check(GlobalService.attempts==1,"negative material marker follows native no-cost rule");
        setup(8,8);check(!AutoUpgrade.start(tab)&&GlobalService.packets==0,"already at target");
        setup(7,8);tab.plain=false;check(!AutoUpgrade.start(tab),"not wing/gem/combining screen");
        setup(7,8);check(!AutoUpgrade.start(new TabRebuildItem()),"stale forge tab cannot become selected index -1");
        setup(7,8);start();AutoUpgrade.onReply(new int[]{1,-1,3,-1});
        check(!AutoUpgrade.active,"manual removal stops instead of reinserting gear");
    }
    static void configAndUi(){
        setup(3,10);AutoUpgrade.policy[8]=3;AutoUpgrade.currency=1;AutoUpgrade.delayMs=1500;AutoUpgrade.saveConfig();
        GameScreen.player.name="second";AutoUpgrade.loadConfig();
        check(AutoUpgrade.targetLevel==8&&AutoUpgrade.policy[8]==0&&AutoUpgrade.delayMs==500,"new character defaults");
        GameScreen.player.name="test";AutoUpgrade.loadConfig();
        check(AutoUpgrade.targetLevel==10&&AutoUpgrade.policy[8]==3&&AutoUpgrade.currency==1&&AutoUpgrade.delayMs==1500,"profile config restored");
        AutoUpgradeMenu.show(tab);
        ((iCommand)GameCanvas.menu2.items.elementAt(1)).action.perform();
        ((iCommand)GameCanvas.menu2.items.elementAt(8)).action.perform();
        check(AutoUpgrade.targetLevel==9,"UI target +9");
        ((iCommand)GameCanvas.menu2.items.elementAt(2)).action.perform();
        ((iCommand)GameCanvas.menu2.items.elementAt(1)).action.perform();
        check(AutoUpgrade.delayMs==500,"UI delay 500ms");
        ((iCommand)GameCanvas.menu2.items.elementAt(4)).action.perform();
        ((iCommand)GameCanvas.menu2.items.elementAt(5)).action.perform();
        ((iCommand)GameCanvas.menu2.items.elementAt(7)).action.perform();
        ((iCommand)GameCanvas.menu2.items.elementAt(2)).action.perform();
        check(AutoUpgrade.policy[8]==2,"UI +7 -> +8 clover setting");
    }
    static void parser() throws Exception {
        Message out=new Message(67);out.writer().writeByte(0);out.writer().writeShort(41);
        out.writer().writeByte(7);out.writer().writeUTF("50%");
        Message in=new Message((byte)67,out.getData());int[] parsed=AutoUpgrade.peekReply(in);
        check(parsed[1]==41&&parsed[2]==7,"reply parsed");
        check(in.reader().readByte()==0&&in.reader().readShort()==41,"stream restored for native parser");
        in=new Message((byte)67,new byte[]{2,3});
        check(AutoUpgrade.peekReply(in)==null,"truncated result cannot acknowledge paid operation");
        check(in.reader().readByte()==2,"malformed stream restored");
    }
    static void displayPrivacy(){
        setup(0,3);GameScreen.player.name="RealName";
        check(DisplayPrivacy.text("RealName").equals("PhucHso"),"character label masked");
        check(DisplayPrivacy.text("REALNAME Lv: 26").equals("PhucHso Lv: 26"),"uppercase profile label masked");
        check(DisplayPrivacy.text("RealName: hi RealName").equals("PhucHso: hi PhucHso"),"repeated name masked");
        check(DisplayPrivacy.text("OtherRealName").equals("OtherRealName"),"other character name preserved");
        check(GameScreen.player.name.equals("RealName"),"real identity stays unchanged");
        GameScreen.player.name="test";
    }
    public static void main(String[] args)throws Exception {
        continuousWithoutReselect();delayedServer();wireReplies();policies();failureAndTimeout();materials();configAndUi();parser();displayPrivacy();
        System.out.println("PASS: "+checks+" upgrade checks; per-level clovers, acknowledgements, delayed inventory, target stop, failures, config, menus");
    }
}
