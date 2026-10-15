package GameObjects;
import CLib.*;
import GameScreen.*;
import Main.GameCanvas;
import Thread_More.LoadMap;
import InterfaceComponents.*;

public class FarmSupportTest {
    static int checks,profile,confirmed;
    static void check(boolean value,String label){checks++;if(!value)throw new RuntimeException(label);}
    static void setup(){
        GameScreen.player=new Player();GameScreen.player.name="profile"+(++profile);
        GameCanvas.currentScreen=GameCanvas.game;GameCanvas.currentDialog=null;GameCanvas.menu2.isShowMenu=false;
        MenuUP.isAutoAttack=true;MenuUP.isGoBackActive=false;GameScreen.isMapLang=false;
        LoadMap.Area=0;GameCanvas.loadmap.idMap=7;Item.VecInvetoryPlayer.removeAllElements();
        AutoFarmSupport.load();Session_ME.count=0;confirmed=0;mSystem.now+=1000;
    }
    static void tick(long ms){mSystem.now+=ms;AutoFarmSupport.update();}
    static mVector options(String a,String b){mVector v=new mVector();
        v.addElement(new iCommand(a,new IAction(){public void perform(){confirmed++;}}));
        v.addElement(new iCommand(b,new IAction(){public void perform(){confirmed++;}}));return v;}
    static Item item(int id,String name){Item i=new Item();i.Id=id;i.ItemCatagory=7;i.itemName=name;i.numPotion=3;Item.VecInvetoryPlayer.addElement(i);return i;}
    static void zone(){
        setup();AutoFarmSupport.zoneEnabled=true;AutoFarmSupport.fallback=3;
        check(AutoFarmSupport.goBackArea(3)==3,"GoBack must retain saved area even with zone manager enabled");
        tick(1);
        check(Session_ME.command==51&&Session_ME.value==1,"zone 2 uses zero-based area 1");
        check(!AutoFarmSupport.dialog("Mua vật phẩm bằng ngọc?",options("Ok","Hủy")),"unrelated purchase untouched");
        check(!AutoFarmSupport.dialog("Vào khu 20 bằng ngọc?",options("Ok","Hủy")),"zone 20 cannot be mistaken for zone 2");
        mVector choices=options("Dùng 10 ngọc","Dùng vé Tyche");
        check(AutoFarmSupport.dynamic(-56,4,"Vào khu 2",choices),"server zone menu captured");tick(1);
        check(Session_ME.command==-30&&Session_ME.value==-56&&Session_ME.index==1,"ticket-first preserves server menu identity and index");
        check(AutoFarmSupport.dialog("Không đủ vé vào khu 2",null),"ticket refusal recognized");tick(1);
        check(Session_ME.command==51,"request gem alternative after explicit ticket refusal");
        AutoFarmSupport.dynamic(-56,4,"Vào khu 2",choices);tick(1);
        check(Session_ME.command==-30&&Session_ME.index==0,"gem fallback selected");
        check(AutoFarmSupport.dialog("Vào khu 2 bằng 10 ngọc?",options("Ok","Hủy")),"one matching confirmation allowed");tick(1);
        check(confirmed==1,"payment confirmation executed once");tick(10);
        check(confirmed==1,"confirmation not repeated");
        LoadMap.Area=1;tick(1);int packets=Session_ME.count;tick(60000);
        check(Session_ME.count==packets,"no charge while already inside zone 2");

        setup();AutoFarmSupport.zoneEnabled=true;AutoFarmSupport.fallback=3;tick(1);
        check(AutoFarmSupport.dialog("Đã hết 6 giờ UP trong ngày",null),"daily limit recognized without zone caption");tick(1);
        check(Session_ME.command==51&&Session_ME.value==3,"daily refusal switches configured fallback");
        LoadMap.Area=3;tick(1);packets=Session_ME.count;tick(3600000);
        check(Session_ME.count==packets,"no retry storm during blocked day");
        tick(86400000);check(Session_ME.value==1,"next day may request zone again");

        setup();AutoFarmSupport.payment=1;
        check(AutoFarmSupport.paymentChoice("",options("10 ngọc","Hủy"))==-1,"ticket-only never spends gems");
        AutoFarmSupport.payment=0;
        check(AutoFarmSupport.paymentChoice("",choices)==0,"gem-only selects gems");
        AutoFarmSupport.zoneEnabled=true;tick(1);AutoFarmSupport.dynamic(1,2,"Khu 2",choices);tick(1);
        packets=Session_ME.count;tick(16000);tick(700000);
        check(Session_ME.count==packets,"ambiguous paid timeout never repays or switches area");
        setup();AutoFarmSupport.zoneEnabled=true;tick(1);AutoFarmSupport.dynamic(1,2,"Khu 2",choices);tick(1);
        GameCanvas.currentScreen=new Object();tick(1);LoadMap.Area=1;GameCanvas.currentScreen=GameCanvas.game;tick(1);
        packets=Session_ME.count;tick(30000);check(Session_ME.count==packets,"loading during entry preserves pending result");
    }
    static void consumables(){
        setup();AutoFarmSupport.fireEnabled=true;AutoFarmSupport.oilEnabled=true;AutoFarmSupport.bookEnabled=true;
        Item fire=item(301,"Lửa thiêng"),oil=item(302,"Bình dầu"),book=item(303,"Sách tri thức");
        tick(1);check(Session_ME.command==-107&&Session_ME.value==301,"auto-detect holy fire and native material packet");
        tick(5000);check(Session_ME.count==1,"wait for consumption acknowledgement");
        fire.numPotion--;tick(1);tick(1);
        check(Session_ME.value==302,"oil follows confirmed fire");
        oil.numPotion--;tick(1);tick(1);check(Session_ME.value==303,"book has independent auto slot");
        book.numPotion--;tick(1);int packets=Session_ME.count;tick(60000);
        check(Session_ME.count==packets,"do not waste consumables before interval");
        setup();AutoFarmSupport.oilEnabled=true;item(302,"Bình dầu");tick(1);
        check(Session_ME.count==0,"oil not used without fire");
        setup();AutoFarmSupport.fireEnabled=true;item(301,"Lửa thiêng");tick(1);tick(16000);tick(3600000);
        check(Session_ME.count==1&&!AutoFarmSupport.fireEnabled,"unacknowledged item use disables feature instead of spamming");
        setup();AutoFarmSupport.fireEnabled=true;item(301,"Lửa thiêng");MenuUP.isGoBackActive=true;tick(1);
        check(Session_ME.count==0,"no fire while travelling");
    }
    static void profiles(){
        setup();String name=GameScreen.player.name;
        AutoFarmSupport.zoneEnabled=true;AutoFarmSupport.bookEnabled=true;AutoFarmSupport.payment=1;AutoFarmSupport.fallback=4;AutoFarmSupport.bookMinutes=15;AutoFarmSupport.save();
        GameScreen.player.name="other";AutoFarmSupport.load();
        check(!AutoFarmSupport.zoneEnabled&&!AutoFarmSupport.bookEnabled,"new profile starts disabled");
        GameScreen.player.name=name;AutoFarmSupport.load();
        check(AutoFarmSupport.zoneEnabled&&AutoFarmSupport.bookEnabled&&AutoFarmSupport.payment==1&&AutoFarmSupport.fallback==4&&AutoFarmSupport.bookMinutes==15,"settings restored per character");
    }
    static void savedArea(){
        setup();AutoFarmSupport.zoneEnabled=true;tick(1);
        check(Session_ME.command==51,"fixture should have a pending automatic zone request");
        AutoFarmSupport.goBackArea(6);
        check(!AutoFarmSupport.dynamic(-56,4,"Vao khu 2",options("Dung ngoc","Dung ve Tyche")),"old zone request intercepted return to saved zone");
        Session_ME.count=0;
        MenuUP.farmMapId=7;MenuUP.farmArea=6;LoadMap.Area=6;tick(1);
        check(Session_ME.count==0,"zone manager moved away from saved area 7 after return");
        check(AutoFarmSupport.goBackArea(6)==6,"saved area lost while zone auto enabled");
        MenuUP.farmMapId=-1;MenuUP.farmArea=-1;
    }
    public static void main(String[] args){zone();consumables();profiles();savedArea();System.out.println("PASS: "+checks+" farm support checks");}
}
