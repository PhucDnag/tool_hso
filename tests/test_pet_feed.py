"""Deterministic pet feed tests with fake inventory, time, and protocol."""
import os
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
# Verify our regression fixtures against the game's actual rendering palette.
import re
renderer = (ROOT / 'src_decompiled/InterfaceComponents/MainTabNew.java').read_text(encoding='utf-8')
renderer = renderer[renderer.index('public static mFont setTextColorName'):]
for code, color in enumerate(('white', 'blue', 'yellow', 'violet', 'orange', 'green')):
    assert re.search(r'case ' + str(code) + r':\s*\{\s*return mFont.tahoma_7b_' + color + r';', renderer)
source = (ROOT / 'tests/test_autoupgrade.py').read_text(encoding='utf-8')
exec(source[:source.index('with tempfile.TemporaryDirectory')])
stubs['CLib/Session_ME.java'] = 'package CLib;public class Session_ME {public static boolean connected=true;static Session_ME s=new Session_ME();public static Session_ME gI(){return s;}public boolean isConnected(){return connected;}}'
stubs['CLib/mHashtable.java'] = 'package CLib;public class mHashtable extends java.util.Hashtable {}'
stubs['GameObjects/MenuUP.java'] = 'package GameObjects;public class MenuUP {public static void showMenu(){}}'
stubs['GameObjects/AutoUpgrade.java'] = 'package GameObjects;public class AutoUpgrade {public static boolean active;public static int itemId=-1;public static boolean protects(MainItem item){return active && item.Id==itemId;}public static void stop(String s){active=false;}}'
stubs['GameObjects/PetItem.java'] = 'package GameObjects;public class PetItem extends Item {}'
stubs['GameObjects/AutoSellManager.java'] = 'package GameObjects;public class AutoSellManager {public static boolean isReturningToSell;public static int queued;public static int getQueueCount(){return queued;}}'
stubs['GameObjects/Item.java'] = stubs['GameObjects/Item.java'].replace('public int Id,', 'public static CLib.mHashtable VecEquipPlayer=new CLib.mHashtable();public int isLock;public int Id,')
stubs['GameScreen/LoadMapScreen.java'] = 'package GameScreen;public class LoadMapScreen {public static boolean isNextMap=true;}'
stubs['Main/GameCanvas.java'] = stubs['Main/GameCanvas.java'].replace('public static Object currentScreen=shopNpc,currentDialog;', 'public static Object game=new Object(),AllInfo=new Object(),foodPet=new Object(),worldmap=new Object(),buy_sell=new Object(),info_other_player=new Object(),mevent=new Object(),clan=new Object(),TabEvent=new Object(),currentScreen=game,currentDialog;public static void end_Dialog(){currentDialog=null;}')
stubs['Main/GameCanvas.java'] = stubs['Main/GameCanvas.java'].replace('public static int IndexServer;', 'public static int IndexServer;public static InterfaceComponents.MsgChat msgchat=new InterfaceComponents.MsgChat();')
stubs['InterfaceComponents/MsgChat.java'] = 'package InterfaceComponents;public class MsgChat {public int messages;public String last;public void addNewChat(String tab,String prefix,String text,byte type,boolean focus){messages++;last=text;}}'
stubs['GameObjects/Player.java'] = stubs['GameObjects/Player.java'].replace('public int hp=', 'public int chats;public String lastChat;public void addChat(String text,boolean timed){chats++;lastChat=text;}public int hp=')
stubs['Main/GameCanvas.java'] = stubs['Main/GameCanvas.java'].replace('public static int IndexServer;', 'public static void start_Ok_Dialog(String text){lastInfo=text;currentDialog=new Object();}public static int IndexServer;')
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
