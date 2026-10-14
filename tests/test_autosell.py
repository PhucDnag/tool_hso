import os
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

stubs = {
'GameObjects/AutoPetFeed.java': 'package GameObjects;public class AutoPetFeed {public static boolean reserves(Item item){return false;}}',
'GameObjects/AutoUpgrade.java': 'package GameObjects;public class AutoUpgrade {public static boolean active;public static int protectedId=-1;public static boolean protects(MainItem item){return active&&item!=null&&item.Id==protectedId;}}',
'CLib/mVector.java': 'package CLib; public class mVector extends java.util.Vector {}',
'CLib/mHashtable.java': 'package CLib; public class mHashtable extends java.util.Hashtable { public boolean contains(Object o){return super.contains(o);} }',
'CLib/mSystem.java': 'package CLib; public class mSystem { public static long now=10000; public static long currentTimeMillis(){return now;} }',
'GameObjects/MainObject.java': """package GameObjects;
public class MainObject {
    public int x, y;
    public byte typeObject;
    public String name, nameGiaotiep;
    public static int getDistance(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2, dy = y1 - y2;
        return (int)Math.sqrt(dx * dx + dy * dy);
    }
}""",
'GameScreen/GameScreen.java': """package GameScreen; import GameObjects.*; import CLib.*;
public class GameScreen {
    public static Player player=new Player();
    public static boolean isMapLang = true;
    public static mVector Vecplayers = new mVector();
    public static MainObject ObjFocus;
}""",
'GameObjects/Player.java': """package GameObjects;
public class Player extends MainObject {
    public static short maxInven=42;
    public static boolean isFullInven;
    public static byte isAutoFire;
    public static boolean isCurAutoFire;
    public int Action=0;
    public int hp=100;
    public int toX, toY, xStopMove, yStopMove, vx, vy, countAutoMove;
    public short[] posTransRoad;
    public void move_to_XY(){ x = toX; y = toY; }
}""",
'Main/GameCanvas.java': """package Main;
public class GameCanvas {
    public static Object game=new Object(),currentScreen=game;
    public static void addInfoCharCline(String s){}
    public static void end_Dialog(){}
    public static Loadmap loadmap = new Loadmap();
    public static class Loadmap { public int idMap = 1; }
}""",
'InterfaceComponents/iCommand.java': """package InterfaceComponents;
public class iCommand { public String caption; public byte subIndex; public void perform(){} public iCommand(String s){caption=s;} public iCommand(String s, int n, int n2){caption=s;subIndex=(byte)n2;} }""",
'GameObjects/GoBackRoutes.java': """package GameObjects;
public class GoBackRoutes {
    public static class Plan { public int hops = 1; }
    public int byId(int id) { return id; }
    public Plan plan(int from, int to) { return new Plan(); }
}""",
'GameObjects/GoBackNavigator.java': """package GameObjects;
public class GoBackNavigator {
    public static GoBackRoutes graph = new GoBackRoutes();
    public static void update(int a, int b, int c, int d) {}
    public static String currentName() { return "Lang"; }
}""",
'GameObjects/GoBackPosition.java': """package GameObjects;
public class GoBackPosition {
    public static int snap(int v) { return v; }
}""",
'Thread_More/LoadMap.java': """package Thread_More;
public class LoadMap {
    public static byte Area = 0;
}""",
'GameObjects/MenuUP.java': """package GameObjects;
public class MenuUP {
    public static boolean isGoBackEnabled=true;
    public static int farmMapId=13;
    public static int farmX=100, farmY=100;
    public static byte farmArea=0;
    public static String farmMapName="Map 13";
    public static boolean isAutoAttack=true;
    public static boolean isWaitingReconnect=false;
    public static boolean isReconnectingChar=false;
    public static long timeTargetReconnect=0L;
    public static void triggerRelogToVillage() { isWaitingReconnect=true; }
    public static void resumeGoBackAfterSell(){}
    public static String stripAccents(String s){return s;}
    public static void saveConfig(){}
}""",
'GameObjects/Item.java': """package GameObjects; import CLib.*;
public class Item {
    public int Id; public String itemName; public int ItemCatagory; public byte isLock;
    public byte tier; public int colorNameItem;
    public static mVector VecInvetoryPlayer=new mVector();
    public static mHashtable VecEquipPlayer=new mHashtable();
    public boolean setNameNull() { return itemName == null || itemName.length() == 0; }
}""",
'GameObjects/MainItem.java': 'package GameObjects; public class MainItem extends Item {}',
'Model/CRes.java': """package Model;
public class CRes {
    static java.util.Hashtable rms=new java.util.Hashtable();
    public static String loadRMSString(String k){return (String)rms.get(k);}
    public static void saveRMSString_(String k,String v){rms.put(k,v);}
}""",
'netcommand/global/GlobalService.java': """package netcommand.global;
public class GlobalService {
    static GlobalService s=new GlobalService();
    public static int sellPackets, lastSoldCat, lastSoldId, lastSoldAction, dialogReplies, homes;
    public static GlobalService gI(){return s;}
    public void delete_Item(byte cat, short id, byte action) {
        sellPackets++;
        lastSoldCat = (int)cat;
        lastSoldId = (int)id;
        lastSoldAction = (int)action;
    }
    public void dialog_Server(short id, byte type, byte action){dialogReplies++;}
    public void gohome(byte b){homes++;}
}""",
}

java = os.environ.get('JAVA', 'java')
with tempfile.TemporaryDirectory(prefix='autosell-test-') as directory:
    folder = Path(directory)
    files = []
    for name, body in stubs.items():
        path = folder / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(body, encoding='utf-8')
        files.append(str(path))
    files.append(str(ROOT / 'src_decompiled/GameObjects/AutoSellManager.java'))
    files.append(str(ROOT / 'tests/AutoSellTest.java'))
    subprocess.run([java, '-jar', str(ROOT / 'ecj.jar'), '-1.4', '-encoding', 'UTF-8', '-nowarn', '-d', directory] + files, check=True)
    subprocess.run([java, '-cp', directory, 'GameObjects.AutoSellTest'], check=True)
