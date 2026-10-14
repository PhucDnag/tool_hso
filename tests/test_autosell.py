import os
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

stubs = {
'GameObjects/AutoUpgrade.java': 'package GameObjects;public class AutoUpgrade {public static boolean active;public static int protectedId=-1;public static boolean protects(MainItem item){return active&&item!=null&&item.Id==protectedId;}}',
'CLib/mVector.java': 'package CLib; public class mVector extends java.util.Vector {}',
'CLib/mHashtable.java': 'package CLib; public class mHashtable extends java.util.Hashtable { public boolean contains(Object o){return super.contains(o);} }',
'CLib/mSystem.java': 'package CLib; public class mSystem { public static long now=10000; public static long currentTimeMillis(){return now;} }',
'GameScreen/GameScreen.java': """package GameScreen; import GameObjects.*; import CLib.*;
public class GameScreen { public static Player player=new Player(); public static boolean isMapLang = true; }""",
'GameObjects/Player.java': """package GameObjects;
public class Player { public static short maxInven=42; public static boolean isFullInven; public int Action=0; public int hp=100; }""",
'Main/GameCanvas.java': """package Main;
public class GameCanvas { public static Object game=new Object(),currentScreen=game;
public static void addInfoCharCline(String s){} public static void end_Dialog(){} }""",
'InterfaceComponents/iCommand.java': """package InterfaceComponents;
public class iCommand { public String caption; public byte subIndex; public void perform(){} public iCommand(String s){caption=s;} public iCommand(String s, int n, int n2){caption=s;subIndex=(byte)n2;} }""",
'GameObjects/MenuUP.java': """package GameObjects;
public class MenuUP { public static boolean isGoBackEnabled=true; public static int farmMapId=13; public static void resumeGoBackAfterSell(){} public static String stripAccents(String s){return s;} }""",
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
