"""Deterministic pet feed tests with fake inventory, time, and protocol."""
import os
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
source = (ROOT / 'tests/test_autoupgrade.py').read_text(encoding='utf-8')
exec(source[:source.index('with tempfile.TemporaryDirectory')])
stubs['CLib/Session_ME.java'] = 'package CLib;public class Session_ME {public static boolean connected=true;static Session_ME s=new Session_ME();public static Session_ME gI(){return s;}public boolean isConnected(){return connected;}}'
stubs['CLib/mHashtable.java'] = 'package CLib;public class mHashtable extends java.util.Hashtable {}'
stubs['GameObjects/MenuUP.java'] = 'package GameObjects;public class MenuUP {public static void showMenu(){}}'
stubs['GameObjects/AutoUpgrade.java'] = 'package GameObjects;public class AutoUpgrade {public static boolean active;public static boolean protects(MainItem item){return false;}}'
stubs['GameObjects/PetItem.java'] = 'package GameObjects;public class PetItem extends Item {}'
stubs['GameObjects/Item.java'] = stubs['GameObjects/Item.java'].replace('public int Id,', 'public static CLib.mHashtable VecEquipPlayer=new CLib.mHashtable();public int type_Only_Item,isLock,LvItem,colorNameItem;public int Id,')
stubs['GameScreen/LoadMapScreen.java'] = 'package GameScreen;public class LoadMapScreen {public static boolean isNextMap=true;}'
stubs['Main/GameCanvas.java'] = stubs['Main/GameCanvas.java'].replace('public static Object currentScreen=shopNpc,currentDialog;', 'public static Object game=new Object(),currentScreen=game,currentDialog;public static void end_Dialog(){currentDialog=null;}')
stubs['netcommand/global/GlobalService.java'] = '''package netcommand.global;public class GlobalService {
static GlobalService s=new GlobalService();public static int packets,pet,id,cat,location;
public static GlobalService gI(){return s;}public void Pet_Eat(short p,short i,byte c,byte l){packets++;pet=p;id=i;cat=c;location=l;}}'''
with tempfile.TemporaryDirectory(prefix='pet-test-') as directory:
    files = []
    for name, body in stubs.items():
        path = Path(directory) / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(body, encoding='utf-8')
        files.append(str(path))
    files += [str(ROOT / 'src_decompiled' / name) for name in ('GameObjects/AutoPetFeed.java', 'InterfaceComponents/IAction.java', 'InterfaceComponents/DataRebuildItem.java')]
    files.append(str(ROOT / 'tests/PetFeedTest.java'))
    java = os.environ.get('JAVA', 'java')
    subprocess.run([java, '-jar', str(ROOT / 'ecj.jar'), '-1.4', '-encoding', 'UTF-8', '-nowarn', '-d', directory] + files, check=True)
    subprocess.run([java, '-cp', directory, 'GameObjects.PetFeedTest'], check=True)
