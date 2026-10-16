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
        MenuUP.isGoBackEnabled=true;MenuUP.farmMapId=-1;MenuUP.farmArea=-1;Player.isAutoFire=0;
        LoadMap.Area=0;GameCanvas.loadmap.idMap=7;Item.VecInvetoryPlayer.removeAllElements();
        GoBackNavigator.near=true;AutoFarmSupport.load();Session_ME.count=0;Session_ME.command=0;confirmed=0;mSystem.now+=1000;
    }
    static void tick(long ms){mSystem.now+=ms;AutoFarmSupport.update();
        if(Session_ME.command==54 && AutoFarmSupport.areaMenu(areas())) AutoFarmSupport.update();}
    static mVector areas(){mVector v=new mVector();for(int i=0;i<10;i++)v.addElement(new iCommand("Khu "+(i+1),null));return v;}
    static mVector options(String a,String b){mVector v=new mVector();
        v.addElement(new iCommand(a,new IAction(){public void perform(){confirmed++;}}));
        v.addElement(new iCommand(b,new IAction(){public void perform(){confirmed++;}}));return v;}
    static Item item(int id,String name){Item i=new Item();i.Id=id;i.ItemCatagory=7;i.itemName=name;i.numPotion=3;Item.VecInvetoryPlayer.addElement(i);return i;}
    static void zone(){
        setup();AutoFarmSupport.zoneEnabled=true;AutoFarmSupport.fallback=3;
        check(AutoFarmSupport.goBackArea(3)==-1,"zone manager owns area selection while enabled");
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
        book.numPotion--;tick(1);
        oil.numPotion=10;tick(2000);check(Session_ME.value==302,"second oil follows first");
        oil.numPotion--;tick(1);tick(2000);check(Session_ME.value==302,"third oil follows second");
        oil.numPotion--;tick(1);int packets=Session_ME.count;tick(60000);
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
        AutoFarmSupport.zoneEnabled=true;AutoFarmSupport.bookEnabled=true;AutoFarmSupport.payment=1;AutoFarmSupport.fallback=4;AutoFarmSupport.save();
        GameScreen.player.name="other";AutoFarmSupport.load();
        check(!AutoFarmSupport.zoneEnabled&&!AutoFarmSupport.bookEnabled,"new profile starts disabled");
        GameScreen.player.name=name;AutoFarmSupport.load();
        check(AutoFarmSupport.zoneEnabled&&AutoFarmSupport.bookEnabled&&AutoFarmSupport.payment==1&&AutoFarmSupport.fallback==4&&AutoFarmSupport.bookMinutes==120,"settings restored per character");
    }
    static void savedArea(){
        setup();MenuUP.farmMapId=7;MenuUP.farmArea=6;LoadMap.Area=6;
        AutoFarmSupport.show();click(0);tick(1);
        check(!GameCanvas.menu2.isShowMenu,"enabling zone closes menu so entry is not blocked");
        check(Session_ME.count==2&&Session_ME.command==51&&Session_ME.value==1,
            "enabling zone must request zone 2 despite a saved GoBack area");
        check(AutoFarmSupport.goBackArea(6)==-1&&MenuUP.farmArea==6,"GoBack skips area without overwriting saved setting");
        check(AutoFarmSupport.dynamic(-56,4,"Vao khu 2",options("Dung ngoc","Dung ve Tyche")),"GoBack area lookup must preserve payment request");
        tick(1);LoadMap.Area=1;MenuUP.isGoBackActive=true;tick(1);
        check(AutoFarmSupport.goBackArea(6)==-1,"arrival must not send GoBack to the old area");
        MenuUP.isGoBackActive=false;tick(1);int packets=Session_ME.count;tick(60000);
        check(Session_ME.count==packets,"no repeated entry after GoBack finishes on zone 2");
        AutoFarmSupport.show();click(0);close();
        check(AutoFarmSupport.goBackArea(6)==6,"disabling zone restores the saved GoBack area");

        setup();MenuUP.farmMapId=7;MenuUP.farmArea=6;LoadMap.Area=6;
        AutoFarmSupport.zoneEnabled=true;AutoFarmSupport.fallback=3;tick(1);
        AutoFarmSupport.dialog("Đã hết 6 giờ UP trong ngày",null);tick(1);
        check(Session_ME.value==3&&AutoFarmSupport.goBackArea(6)==-1,"fallback also overrides GoBack area after daily refusal");
        MenuUP.farmMapId=-1;MenuUP.farmArea=-1;
    }
    static void click(int row){((iCommand)GameCanvas.menu2.items.elementAt(row)).perform();}
    static void close(){GameCanvas.menu2.doCloseMenu();}
    static void menuRecovery(){
        setup();AutoFarmSupport.stopAll();item(303,"Sách tri thức");
        AutoFarmSupport.show();click(5);close();tick(1);
        check(AutoFarmSupport.bookEnabled&&Session_ME.count==1&&Session_ME.value==303,
            "book enabled through menu must run after Stop All");

        setup();AutoFarmSupport.stopAll();Item fire=item(301,"Lửa thiêng");item(302,"Bình dầu");
        AutoFarmSupport.show();click(3);click(4);close();tick(1);
        check(Session_ME.count==1&&Session_ME.value==301,"fire must run after Stop All");
        fire.numPotion--;tick(1);tick(1);
        check(Session_ME.count==2&&Session_ME.value==302,"oil must run after Stop All and confirmed fire");

        setup();item(303,"Sách tri thức");MenuUP.farmMapId=99;MenuUP.isGoBackEnabled=false;
        AutoFarmSupport.show();click(5);close();tick(1);
        check(Session_ME.count==1,"disabled GoBack must not restrict consumables to stale saved map");
        setup();MenuUP.farmMapId=99;MenuUP.isGoBackEnabled=false;
        AutoFarmSupport.show();click(1);click(2);click(2);click(0);tick(1);
        check(AutoFarmSupport.payment==0&&AutoFarmSupport.fallback==3,"payment and fallback menu callbacks");
        check(Session_ME.count==2&&Session_ME.value==1,"disabled GoBack must not block zone entry");

        setup();item(303,"Sách tri thức");AutoFarmSupport.show();click(5);close();tick(1);tick(16000);
        check(!AutoFarmSupport.bookEnabled,"timeout disables book");
        AutoFarmSupport.show();click(5);close();tick(1);
        check(Session_ME.count==2,"explicit re-enable after failure must not wait old thirty minute interval");

        setup();Item book=item(303,"Sách tri thức");AutoFarmSupport.bookEnabled=true;tick(1);
        long used=mSystem.now;book.numPotion--;tick(1);
        tick(7200000L-2);check(Session_ME.count==1,"book waits full two hours");
        tick(1);check(Session_ME.count==2,"book repeats at two hours");

        setup();item(304,"Sách khác");AutoFarmSupport.bookEnabled=true;tick(1);
        check(Session_ME.count==0,"automatic selection ignores unrelated books");
        book=item(303,"Sách tri thức");tick(1000);
        check(Session_ME.count==1 && Session_ME.value==303,"automatic selection finds correct book without configuration");

        setup();AutoFarmSupport.zoneEnabled=true;tick(1);
        AutoFarmSupport.dynamic(1,2,"Khu 2",options("Dùng ngọc","Dùng vé Tyche"));tick(1);tick(16000);
        int packets=Session_ME.count;item(303,"Sách tri thức");AutoFarmSupport.show();click(5);close();tick(1);
        check(Session_ME.count==packets+1&&Session_ME.command==-107,"ambiguous zone payment must not disable independent book use");
        tick(700000);check(Session_ME.count==packets+1,"book enable must not resume ambiguous zone payment");

        setup();item(303,"Sách tri thức");AutoFarmSupport.bookEnabled=true;MenuUP.isAutoAttack=false;tick(1);
        check(Session_ME.count==0&&AutoFarmSupport.status.indexOf("auto")>=0,"explain inactive auto attack without using items");
    }
    static void nativeAreaFlow(){
        setup();GoBackNavigator.near=false;AutoFarmSupport.zoneEnabled=true;
        AutoFarmSupport.update();
        check(Session_ME.count==0&&AutoFarmSupport.isApproachingArea(),"walk to sign before sending any area request");
        GoBackNavigator.near=true;AutoFarmSupport.update();
        check(Session_ME.count==1&&Session_ME.command==54,"request native area list first");
        AutoFarmSupport.update();check(Session_ME.count==1,"wait for list instead of sending selection immediately");
        check(!AutoFarmSupport.dynamic(1,2,"Khu 2",options("Ngọc","Vé Tyche")),"do not pay before area list and selection");
        check(AutoFarmSupport.areaMenu(areas()),"capture server list while entry is pending");
        AutoFarmSupport.update();
        check(Session_ME.count==2&&Session_ME.command==51&&Session_ME.value==1,"select zone 2 only after native list arrives");
        check(!AutoFarmSupport.areaMenu(areas()),"ignore duplicate lists after selecting");
        setup();AutoFarmSupport.zoneEnabled=true;AutoFarmSupport.update();
        mVector one=new mVector();one.addElement(new iCommand("Khu 1",null));
        check(!AutoFarmSupport.areaMenu(one),"missing zone stops automatic selection");
        AutoFarmSupport.update();check(Session_ME.count==1,"missing zone must not send invalid change command");
    }
    static void nativePayment(){
        setup();AutoFarmSupport.payment=1;
        mVector menu=options("Sử dụng vé","Sử dụng lượt");
        check(!AutoFarmSupport.dynamic(12,3,"MENU",menu),"do not select native menu outside our zone request");
        AutoFarmSupport.zoneEnabled=true;tick(1);
        check(!AutoFarmSupport.dynamic(12,3,"MENU",options("Sử dụng vé","Mua đồ")),"unrelated generic menu remains manual");
        check(AutoFarmSupport.dynamic(12,3,"MENU",menu),"recognize exact server menu without zone title");
        tick(1);check(Session_ME.command==-30&&Session_ME.value==12&&Session_ME.index==0,"select use ticket with server NPC and menu index");
        int packets=Session_ME.count;
        check(!AutoFarmSupport.dynamic(12,3,"MENU",menu),"duplicate payment menu must not pay twice");
        tick(1);check(Session_ME.count==packets,"no duplicate payment packet");
        setup();AutoFarmSupport.payment=1;AutoFarmSupport.zoneEnabled=true;tick(1);
        check(AutoFarmSupport.dynamic(12,3,"MENU",options("Sử dụng lượt","Sử dụng vé")),"recognize reversed menu order");
        tick(1);check(Session_ME.index==1,"select ticket by caption rather than first row");
        setup();AutoFarmSupport.payment=1;AutoFarmSupport.zoneEnabled=true;tick(1);
        check(AutoFarmSupport.dialog("MENU",menu),"same menu works through dialog callback");
        tick(1);check(confirmed==1,"ticket dialog callback executes");
        setup();AutoFarmSupport.payment=0;AutoFarmSupport.zoneEnabled=true;tick(1);packets=Session_ME.count;
        AutoFarmSupport.dynamic(12,3,"MENU",menu);tick(1);
        check(Session_ME.count==packets,"use turn must not be guessed to mean gems");
    }
    static void oilCycle(){
        setup();Item fire=item(301,"Lửa thiêng"),oil=item(302,"Bình dầu");oil.numPotion=20;
        AutoFarmSupport.fireEnabled=AutoFarmSupport.oilEnabled=true;
        tick(1);long start=mSystem.now;fire.numPotion--;tick(1);tick(1);
        check(Session_ME.value==302,"oil begins only after fire acknowledgement");
        check(!AutoFarmSupport.dialog("Không thể mua thêm vật phẩm",null),"unrelated refusal preserved");
        check(AutoFarmSupport.dialog("Không thể châm thêm",null),"oil cap response stops current fire");
        int packets=Session_ME.count;tick(2000);check(Session_ME.count==packets && AutoFarmSupport.oilEnabled,"refusal waits for next fire without disabling oil");
        String name=GameScreen.player.name;AutoFarmSupport.save();GameScreen.player.name="oil-other";AutoFarmSupport.load();
        GameScreen.player.name=name;AutoFarmSupport.load();tick(1);check(Session_ME.count==packets,"refusal quota survives profile reload");
        mSystem.now=start+3599999L;AutoFarmSupport.update();check(Session_ME.count==packets,"fire waits full hour");
        tick(1);check(Session_ME.count==packets+1 && Session_ME.value==301,"new fire at one hour");
        fire.numPotion--;tick(1);LoadMap.Area=2;tick(1);check(Session_ME.count==packets+1,"oil blocked in different area");
        LoadMap.Area=0;tick(1);check(Session_ME.count==packets+2 && Session_ME.value==302,"new confirmed fire resets oil quota");
        oil.numPotion--;tick(1);tick(2000);oil.numPotion--;tick(1);tick(2000);oil.numPotion--;tick(1);
        packets=Session_ME.count;tick(2000);check(Session_ME.count==packets,"never send fourth oil");
        AutoFarmSupport.stopAll();AutoFarmSupport.show();
        for(int i=0;i<GameCanvas.menu2.items.size();i++)check(!((iCommand)GameCanvas.menu2.items.elementAt(i)).caption.equals("Đã tắt"),"no disabled status row");
        close();
    }
    public static void main(String[] args){oilCycle();zone();consumables();profiles();savedArea();menuRecovery();nativeAreaFlow();nativePayment();System.out.println("PASS: "+checks+" farm support checks");}
}
