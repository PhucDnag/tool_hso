"""Test Teleport Stone two-tier menu optimization and cross-island routing."""
import os
import re
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
source = (ROOT / 'src_decompiled/GameObjects/MenuUP.java').read_text(encoding='utf-8')

def method(name):
    start = re.search(r'    (?:public|private) static \S+ ' + name + r'\(', source).start()
    end = source.index('{', start) + 1
    depth = 1
    while depth:
        depth += (source[end] == '{') - (source[end] == '}')
        end += 1
    return source[start:end]

stubs = {
'CLib/mVector.java': 'package CLib; public class mVector extends java.util.Vector {}',
'CLib/mSystem.java': 'package CLib; public class mSystem { public static long now=10000; public static long currentTimeMillis(){return now;} }',
'GameObjects/AutoFarmSupport.java': 'package GameObjects; public class AutoFarmSupport { public static int goBackArea(int a){return a;} }',
'GameScreen/LoginScreen.java': 'package GameScreen; public class LoginScreen { public static String ip="test"; }',
'GameScreen/LoadMapScreen.java': 'package GameScreen; public class LoadMapScreen { public static boolean isNextMap=true; }',
'GameScreen/GameScreen.java': """package GameScreen; import GameObjects.*; import CLib.*;
public class GameScreen { public static Player player=new Player(); public static MainObject ObjFocus;
public static boolean isMapLang; public static mVector Vecplayers=new mVector(); }""",
'GameObjects/Player.java': """package GameObjects; public class Player extends MainObject {
public static byte isAutoFire=1; public static boolean isCurAutoFire=true;
public static int xBeginAutoFire,yBeginAutofire; public static short maxInven=42;
public int toX,toY,xStopMove,yStopMove,countAutoMove,vx,vy,vMax=4; public short[] posTransRoad;
public int getVmount(){return 0;}
public void dofire_(){isAutoFire=1;isCurAutoFire=true;} public void nextMonster(){} public void setAutoFire(){} }""",
'GameObjects/MainObject.java': """package GameObjects; import GameScreen.GameScreen;
public class MainObject { public int x=36,y=36,hp=100,Action,ID,typeObject,Lv,protocolId=9999; public String name="", nameGiaotiep="";
public int getIDnpc(){return protocolId==9999?ID:protocolId;}
public void GiaoTiep(){}
public static int getDistance(int x,int y,int a,int b){return (int)Math.sqrt((x-a)*(x-a)+(y-b)*(y-b));}
public static MainObject get_Object(int id,byte type){for(int i=0;i<GameScreen.Vecplayers.size();i++){
MainObject o=(MainObject)GameScreen.Vecplayers.elementAt(i);if(o.getIDnpc()==id&&o.typeObject==type)return o;}return null;} }""",
'GameObjects/NPC.java': """package GameObjects;
public class NPC extends MainObject { public String nameGiaotiep=""; }""",
'GameObjects/NPCserver.java': """package GameObjects;
public class NPCserver extends MainObject { public String nameGiaotiep=""; }""",
'Thread_More/LoadMap.java': """package Thread_More; import CLib.*;
public class LoadMap { public static int wTile=24; public static byte Area;
public static mVector vecPointChange=new mVector(); public int idMap,mapW=10,mapH=10;
public String nameMap=""; public int[] mapType=new int[100]; }""",
'Main/GameCanvas.java': """package Main; import Thread_More.*;
public class GameCanvas { public static Object game=new Object(),currentScreen=game;
public static LoadMap loadmap=new LoadMap(); public static int IndexServer=0;
public static Model.Menu2 menu2=new Model.Menu2(); public static void addInfoCharCline(String s){} public static void end_Dialog(){} }""",
'Model/Menu2.java': 'package Model; public class Menu2 {public boolean isShowMenu;}',
'GameObjects/Item.java': 'package GameObjects; public class Item { public int Id; public String itemName; public static CLib.mVector VecInvetoryPlayer=new CLib.mVector(); }',
'Model/Point.java': 'package Model; public class Point {public int x,y,dis;public String name;}',
'Model/T.java': 'package Model; public class T {public static String[] mapName=new String[]{"Small Village","Wolf Village","Mines","Edge","Fire","Forest","Canyon"};}',
'Model/CRes.java': """package Model; public class CRes {
static java.util.Hashtable rms=new java.util.Hashtable();
public static byte[] loadRMS(String k){return (byte[])rms.get(k);} public static void saveRMS(String k,byte[] v){rms.put(k,v);}
public static String loadRMSString(String k){return (String)rms.get(k);} public static void saveRMSString_(String k,String v){rms.put(k,v);} }""",
'InterfaceComponents/iCommand.java': 'package InterfaceComponents; public class iCommand { public String caption; public iCommand(String s){caption=s;} }',
'netcommand/global/GlobalService.java': """package netcommand.global; import GameObjects.*; import CLib.*; import InterfaceComponents.*;
public class GlobalService {
static GlobalService s=new GlobalService();
public static int changes, requests, moves;
public static int lastNpcId=-1, lastMenuId=-1, lastIndex=-1;
public static int currentMenuTier = 1; // 1 = Categories, 2 = Maps
public static int selectedCategory = -1;
public static String targetMapName = "";
public static boolean delayed, rootMenu, silent;
public static int expectedRequest=-56, responseNpc=-56, selections, pendingId;
public static mVector pending;
public static void deliver(int menu,mVector options){
    if(delayed){pendingId=menu;pending=options;}
    else GoBackNavigator.onMenu(responseNpc,menu,options);
}
public static void pump(){
    if(pending!=null){mVector opts=pending;pending=null;GoBackNavigator.onMenu(responseNpc,pendingId,opts);}
}
public static GlobalService gI(){return s;}
public void gohome(byte mode){}
public void Change_Area(byte a){changes++;Thread_More.LoadMap.Area=a;}
public void player_move(short x,short y){moves++;}
public void getlist_from_npc(byte id){
    requests++;
    if(id!=expectedRequest) throw new RuntimeException("wrong native request ID: "+id);
    if(silent) return;
    lastNpcId = id;
    currentMenuTier = 1;
    mVector opts = new mVector();
    // Real server Tier 1: Category list
    opts.addElement(new iCommand("C\\u1ed5ng g\\u1ea7n nh\\u1ea5t"));
    opts.addElement(new iCommand("Map Lv 1 - 39"));
    opts.addElement(new iCommand("Map Lv 40 - 69"));
    opts.addElement(new iCommand("Map Lv 70 - 89"));
    opts.addElement(new iCommand("Map Lv 90 - 109"));
    opts.addElement(new iCommand("Map Lv 110+"));
    opts.addElement(new iCommand("Chi\\u1ebfn Tr\\u01b0\\u1eddng & Kh\\u00e1c"));
    Main.GameCanvas.menu2.isShowMenu=true;
    if(rootMenu){mVector root=new mVector();root.addElement(new iCommand("Teleport"));deliver(5,root);}
    else deliver(10,opts);
}
public void Dynamic_Menu(short id, byte menu, byte index){
    if(id!=responseNpc) throw new RuntimeException("response NPC ID was not preserved: "+id);
    selections++;
    lastNpcId = id; lastMenuId = menu; lastIndex = index;
    if(menu==5){
        mVector opts=new mVector();
        // Reversed order catches the old 'lv 1' / 'lv 110' substring bug.
        opts.addElement(new iCommand("Map Lv 110+"));
        opts.addElement(new iCommand("Map Lv 1 - 39"));
        opts.addElement(new iCommand("Map Lv 40 - 69"));
        opts.addElement(new iCommand("Map Lv 70 - 89"));
        deliver(10,opts);
    } else if (menu == 10) {
        // Player selected category index (1=Cat1, 2=Cat2, 3=Cat3, etc.)
        selectedCategory = index;
        currentMenuTier = 2;
        mVector mapOpts = new mVector();
        if (index == 1) { // Lv 1 - 39
            mapOpts.addElement(new iCommand("Ng\\u00f4i L\\u00e0ng Nh\\u1ecf"));
            mapOpts.addElement(new iCommand("L\\u00e0ng S\\u00f3i Tr\\u1eafng"));
            mapOpts.addElement(new iCommand("B\\u00eca R\\u1eebng"));
            mapOpts.addElement(new iCommand("Hang L\\u1eeda"));
            mapOpts.addElement(new iCommand("R\\u1eb7ng \\u0110\\u00e1 Ng\\u1ea7m (500 vang)"));
            mapOpts.addElement(new iCommand("V\\u1ef1c \\u0110\\u00e1"));
        } else if (index == 2) { // Lv 40 - 69
            mapOpts.addElement(new iCommand("Th\\u00e0nh Ph\\u1ed1 Kho B\\u00e1u"));
            mapOpts.addElement(new iCommand("R\\u1eebng Ch\\u1ebft"));
            mapOpts.addElement(new iCommand("Sa M\\u1ea1c"));
        } else if (index == 3) { // Lv 70 - 89
            mapOpts.addElement(new iCommand("Th\\u00e0nh Ph\\u1ed1 C\\u1ea3ng"));
            mapOpts.addElement(new iCommand("R\\u1eebng cao nguy\\u00ean"));
        } else if (index == 4) { // Lv 90 - 109
            mapOpts.addElement(new iCommand("R\\u1eebng Medusa"));
            mapOpts.addElement(new iCommand("V\\u1ecbnh Caribe"));
        }
        Main.GameCanvas.menu2.isShowMenu=true;
        deliver(20,mapOpts);
    } else if (menu == 20) {
        // Player selected destination map index! Server teleports player!
    }
}
}""",
}

methods = '\n'.join(method(n) for n in ('stripAccents','isVillage','getMapName','getMapIdByName','farmAreaLabel',
    'isHorseId','isMountKeyword','findMountItem',
    'shouldAutoRevive','updateAutoRevive',
    'rememberFarmLocation','beginGoBack','updateGoBack'))
stubs['GameObjects/MenuUP.java'] = """package GameObjects;
import CLib.*; import GameScreen.*; import Main.*; import Model.*; import Thread_More.*; import netcommand.global.GlobalService;
public class MenuUP {
public static boolean isGoBackEnabled=true,isGoBackActive,isGoBackPaused,isAutoAttack=true;
public static int farmMapId=-1,farmX,farmY,farmArea,saves; public static String farmMapName="",goBackStepInfo="";
public static int autoMountCustomId=-1;
public static int autoReviveMode,reviveState,reviveDelaySec;
public static boolean isReviveCancelled,isCheckingGoldRevive;
public static long timeTargetRevive,timeSentRevive;
public static boolean hasReviveItem(){return false;}public static String getReviveItemName(){return "";}
public static void tickRevive(){if(shouldAutoRevive())updateAutoRevive();}
private static long lastFarmSaveTime;private static boolean farmLocationDirty,resumeNativeAutoFire;
public static void saveConfig(){saves++;}
public static void tick(){GoBackNavigator.observe();rememberFarmLocation();updateGoBack();}
""" + methods + '\n}'

test_java = """package GameObjects;
import CLib.*; import GameScreen.*; import Main.GameCanvas; import Model.Point; import Thread_More.LoadMap; import netcommand.global.GlobalService;

public class TestTwoTierStone {
    static void check(boolean cond, String msg) {
        if (!cond) throw new RuntimeException("FAIL: " + msg);
    }
    static void movePlayer() {
        Player p = GameScreen.player;
        if (p.posTransRoad == null) return;
        p.x = p.toX; p.y = p.toY; p.posTransRoad = null;
    }
    static void setup(String server,int objectId,int protocolId,int replyId,boolean delayed) {
        GoBackNavigator.stop();
        LoginScreen.ip=server; MenuUP.isGoBackActive=false;
        MenuUP.farmMapId=13; MenuUP.farmMapName=GoBackAtlas.getMapName(13);
        MenuUP.farmX=100; MenuUP.farmY=100; MenuUP.farmArea=1;
        GameCanvas.loadmap=new LoadMap(); GameCanvas.loadmap.idMap=1;
        GameCanvas.loadmap.nameMap=GoBackAtlas.getMapName(1);
        LoadMap.vecPointChange.removeAllElements();
        GameScreen.player.x=36; GameScreen.player.y=36; GameScreen.player.hp=100;
        GameScreen.isMapLang=true; GameScreen.Vecplayers.removeAllElements();
        MainObject npc=new MainObject(); npc.typeObject=2; npc.ID=objectId; npc.protocolId=protocolId;
        npc.name="Da dich chuyen"; npc.x=40; npc.y=40; GameScreen.Vecplayers.addElement(npc);
        GlobalService.requests=0; GlobalService.selections=0; GlobalService.lastMenuId=-1;
        GlobalService.expectedRequest=protocolId; GlobalService.responseNpc=replyId;
        GlobalService.delayed=delayed; GlobalService.rootMenu=true; GlobalService.silent=false;
        GlobalService.pending=null;
        GoBackNavigator.observe();
    }
    static void asyncAndTimeouts() {
        setup("signed-async",700,-56,200,true);
        boolean done=false;
        for(int i=0;i<100;i++) {
            mSystem.now+=100; MenuUP.tick(); movePlayer();
            if(GlobalService.pending!=null) {
                mVector unrelated=new mVector(); unrelated.addElement(new InterfaceComponents.iCommand(MenuUP.farmMapName));
                check(!GoBackNavigator.onMenu(77,99,unrelated),"unrelated NPC response consumed");
            }
            if(i%3==0) GlobalService.pump();
            if(GlobalService.lastMenuId==20){done=true;break;}
        }
        check(done && GlobalService.lastIndex==4,"async signed byte / unsigned reply / price suffix");
        check(GlobalService.requests==1 && GlobalService.selections==3,"root/category/map must use one open and three selections");
        setup("zero-id",0,0,0,false);
        for(int i=0;i<50 && GlobalService.lastMenuId!=20;i++) {mSystem.now+=100;MenuUP.tick();movePlayer();}
        check(GlobalService.lastMenuId==20 && GlobalService.lastNpcId==0,"NPC ID zero replaced with fallback");
        check(GlobalService.requests==1,"request counter leaked from preceding run");

        setup("failed-destination",700,-56,-56,false);
        int failedSelections=0; boolean fallback=false;
        for(int i=0;i<400;i++) {
            mSystem.now+=100; MenuUP.tick(); movePlayer();
            if(GlobalService.lastMenuId==20 && GlobalService.lastIndex==4) {
                failedSelections++;
                GlobalService.lastMenuId=-1; // Deliberately no map-load response.
            }
            if(GlobalService.lastMenuId==20 && GlobalService.lastIndex==5) {fallback=true;break;}
        }
        check(fallback && failedSelections==1,"failed direct teleport must choose nearest complete alternative");
        GameCanvas.loadmap=new LoadMap(); GameCanvas.loadmap.idMap=12;
        GameCanvas.loadmap.nameMap=GoBackAtlas.getMapName(12);
        GameScreen.isMapLang=false; GameScreen.Vecplayers.removeAllElements();
        Point exit=new Point(); exit.name=MenuUP.farmMapName; exit.x=204;exit.y=108;
        LoadMap.vecPointChange.addElement(exit);
        boolean arrived=false;
        for(int i=0;i<200;i++) {
            mSystem.now+=100; MenuUP.tick(); movePlayer();
            if(GameCanvas.loadmap.idMap==12 && MainObject.getDistance(GameScreen.player.x,GameScreen.player.y,204,108)<=36) {
                GameCanvas.loadmap=new LoadMap(); GameCanvas.loadmap.idMap=13;
                GameCanvas.loadmap.nameMap=MenuUP.farmMapName; LoadMap.vecPointChange.removeAllElements();
                GameScreen.player.x=36; GameScreen.player.y=36;
            }
            if(GameCanvas.loadmap.idMap==13 && !MenuUP.isGoBackActive){arrived=true;break;}
        }
        check(arrived,"teleport to neighboring map then walk to saved map/area/coordinates");

        setup("exact-map-name",700,-56,-56,false);
        for(int i=0;i<50 && GlobalService.requests==0;i++){mSystem.now+=100;MenuUP.tick();movePlayer();}
        mVector names=new mVector();
        names.addElement(new InterfaceComponents.iCommand(MenuUP.farmMapName+" phia bac"));
        names.addElement(new InterfaceComponents.iCommand(MenuUP.farmMapName+" (500 vang)"));
        GoBackNavigator.onMenu(-56,20,names);
        mSystem.now+=100;MenuUP.tick();
        check(GlobalService.lastMenuId==20 && GlobalService.lastIndex==1,"longer map name matched by substring");

        setup("silent-stone",700,-56,-56,true); GlobalService.silent=true;
        for(int i=0;i<600;i++) {mSystem.now+=100;MenuUP.tick();movePlayer();}
        check(GlobalService.requests==3,"unresponsive stone retried indefinitely");
        check(GlobalService.selections==0,"silent stone selected a stale menu");
        GoBackRoutes g=GoBackNavigator.graph;
        GoBackRoutes.Node origin=g.get(g.byId(1));
        for(int i=0;i<origin.exits.size();i++) {
            GoBackRoutes.Exit e=(GoBackRoutes.Exit)origin.exits.elementAt(i);
            if(e.npc==-56) check(e.blocked,"stone failures cleared during replan");
        }
        GlobalService.silent=false;
        for(int i=0;i<400 && GlobalService.lastMenuId!=20;i++) {
            mSystem.now+=100;MenuUP.tick();movePlayer();GlobalService.pump();
        }
        check(GlobalService.lastMenuId==20,"temporary menu lag must recover without toggling GoBack");
        for(int run=0;run<2;run++) {
            setup("repeat-after-lag",700,-56,-56,false);
            for(int i=0;i<100 && GlobalService.lastMenuId!=20;i++){mSystem.now+=100;MenuUP.tick();movePlayer();}
            check(GlobalService.lastMenuId==20,"repeated run requests teleport");
            int requests=GlobalService.requests;
            mSystem.now+=12000;MenuUP.tick();
            check(GlobalService.requests==requests,"12s map lag must not send another teleport");
            GameCanvas.loadmap=new LoadMap();GameCanvas.loadmap.idMap=13;GameCanvas.loadmap.nameMap=MenuUP.farmMapName;
            GameScreen.isMapLang=false;GameScreen.Vecplayers.removeAllElements();
            for(int i=0;i<100;i++){mSystem.now+=100;MenuUP.tick();movePlayer();}
            check(!MenuUP.isGoBackActive,"first/second delayed teleport completes");
            check(MenuUP.farmX==100 && MenuUP.farmY==100,"delayed arrival changed saved target");
        }
        System.out.println("PASS: async menus, signed/zero/split NPC IDs, unrelated responses, bounded retries");
    }

    public static void main(String[] args) {
        System.out.println("Starting Two-Tier Teleport Stone Tests...");

        // 1. Test Atlas categories
        check(GoBackAtlas.getCategoryForMap(13) == 1, "Rang Da Ngam must be Cat 1");
        check(GoBackAtlas.getCategoryForMap(33) == 2, "Thanh Pho Kho Bau must be Cat 2");
        check(GoBackAtlas.getCategoryForMap(67) == 3, "Thanh Pho Cang must be Cat 3");
        check(GoBackAtlas.getCategoryForMap(73) == 4, "Vinh Caribe must be Cat 4");
        check(GoBackAtlas.getCategoryForMap(98) == 5, "Tram Nui Tuyet must be Cat 5");
        check(GoBackAtlas.getCategoryForMap(82) == 6, "Khu mua ban must be Cat 6");
        System.out.println("PASS: Atlas categories verified");

        // 2. Test Category matching strings
        check(GoBackAtlas.matchesCategory("Map Lv 1 - 39", 1), "Match Cat 1");
        check(GoBackAtlas.matchesCategory("Map Lv 40 - 69", 2), "Match Cat 2");
        check(GoBackAtlas.matchesCategory("Map Lv 70 - 89", 3), "Match Cat 3");
        check(GoBackAtlas.matchesCategory("Map Lv 90 - 109", 4), "Match Cat 4");
        check(GoBackAtlas.matchesCategory("Map Lv 110+", 5), "Match Cat 5");
        check(GoBackAtlas.matchesCategory("Chiến Trường & Khác", 6), "Match Cat 6");
        check(!GoBackAtlas.matchesCategory("Map Lv 110+",1), "Cat 5 is not Cat 1");
        check(GoBackAtlas.matchesCategory("Map Lv 1 39",1), "Normalized level range");
        System.out.println("PASS: Category string matching verified");

        // 3. Test Two-Tier interaction: Map 1 (Village) -> Map 13 (Rang Da Ngam, Cat 1)
        LoginScreen.ip = "two-tier-test";
        MenuUP.isGoBackActive = false;
        MenuUP.farmMapId = 13;
        MenuUP.farmMapName = "R\\u1eb7ng \\u0110\\u00e1 Ng\\u1ea7m";
        MenuUP.farmX = 100; MenuUP.farmY = 100; MenuUP.farmArea = 1;
        GoBackNavigator.prioritizeStone = true;

        GameCanvas.loadmap = new LoadMap();
        GameCanvas.loadmap.idMap = 1;
        GameCanvas.loadmap.nameMap = "L\\u00e0ng S\\u00f3i Tr\\u1eafng";
        LoadMap.vecPointChange.removeAllElements();
        GameScreen.player.x = 36; GameScreen.player.y = 36; GameScreen.player.hp = 100; GameScreen.isMapLang = true;
        GameScreen.Vecplayers.removeAllElements();
        MainObject stone = new MainObject(); stone.typeObject = 2; stone.ID = 9; stone.name = "Da dich chuyen";
        // Real NPC IDs are signed bytes; -56 must still be a teleport edge.
        stone.ID = -56;
        stone.x = 40; stone.y = 40;
        GameScreen.Vecplayers.addElement(stone);
        GoBackNavigator.observe();

        // Tick loop to simulate talking to stone and selecting Tier 1 Cat 1, then Tier 2 Map 13
        GlobalService.requests = 0; GlobalService.lastMenuId = -1; GlobalService.lastIndex = -1;
        boolean selectedMap = false;
        for (int i = 0; i < 50; i++) {
            mSystem.now += 100;
            MenuUP.tick();
            movePlayer();
            if (GlobalService.lastMenuId == 20) {
                // Tier 2 map selected!
                selectedMap = true;
                break;
            }
        }
        check(GlobalService.requests == 1, "Exactly one native menu request");
        check(GlobalService.selectedCategory == 1, "Selected Category 1 (Map Lv 1 - 39)");
        check(selectedMap, "Selected destination map in Tier 2 menu");
        check(GlobalService.lastIndex == 4, "Selected Rang Da Ngam (index 4 in Tier 2)");
        System.out.println("PASS: Two-tier teleport interaction completed successfully!");

        // 4. Test Cross-Island: Map 1 (Village) -> Map 67 (Thanh Pho Cang, Cat 3)
        MenuUP.isGoBackActive = false;
        MenuUP.farmMapId = 67;
        MenuUP.farmMapName = "Th\\u00e0nh ph\\u1ed1 c\\u1ea3ng";
        GoBackNavigator.stop();
        GoBackNavigator.observe();

        GlobalService.requests = 0; GlobalService.lastMenuId = -1; GlobalService.lastIndex = -1;
        GlobalService.selectedCategory = -1;
        selectedMap = false;
        for (int i = 0; i < 50; i++) {
            mSystem.now += 100;
            MenuUP.tick();
            movePlayer();
            if (GlobalService.lastMenuId == 20) {
                selectedMap = true;
                break;
            }
        }
        check(GlobalService.selectedCategory == 3, "Cross-island selected Category 3 (Map Lv 70 - 89)");
        check(selectedMap, "Cross-island selected destination map in Tier 2 menu");
        check(GlobalService.lastIndex == 0, "Selected Thanh Pho Cang (index 0 in Tier 2)");
        System.out.println("PASS: Cross-island teleport to Map 67 completed successfully!");

        asyncAndTimeouts();
        System.out.println("ALL TWO-TIER TELEPORT OPTIMIZATION CHECKS PASSED!");
    }
}
"""

java = os.environ.get('JAVA','java')
with tempfile.TemporaryDirectory(prefix='stone-test-') as directory:
    folder = Path(directory)
    files = []
    for name, body in stubs.items():
        path = folder / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(body, encoding='utf-8')
        files.append(str(path))
    for name in ('GoBackPosition','GoBackPathfinder','GoBackRoutes','GoBackAtlas','GoBackNavigator','GoBackProfile'):
        files.append(str(ROOT/'src_decompiled/GameObjects'/(name+'.java')))
    test_path = folder / 'GameObjects/TestTwoTierStone.java'
    test_path.write_text(test_java, encoding='utf-8')
    files.append(str(test_path))

    subprocess.run([java, '-jar', str(ROOT/'ecj.jar'), '-1.4', '-encoding', 'UTF-8', '-nowarn', '-d', directory] + files, check=True)
    subprocess.run([java, '-cp', directory, 'GameObjects.TestTwoTierStone'], check=True)
