"""Deterministic farm protocol tests, without a live account or spending."""
import os
import subprocess
import tempfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
# Reuse the basic JVM harness, not the upgrade test invocation.
source=(ROOT/'tests/test_autoupgrade.py').read_text(encoding='utf-8')
exec(source[:source.index('with tempfile.TemporaryDirectory')])
stubs['InterfaceComponents/iCommand.java']=stubs['InterfaceComponents/iCommand.java'].replace('public String caption;', 'public void perform(){if(action!=null)action.perform();}public String caption;')
stubs['GameObjects/MenuUP.java']='''package GameObjects;public class MenuUP {
public static boolean isGoBackActive,isAutoAttack=true;public static int farmMapId=-1;
public static void showMenu(){}'''+strip+'}'
stubs['GameObjects/AutoUpgrade.java']='package GameObjects;public class AutoUpgrade {public static boolean active;}'
stubs['GameScreen/LoadMapScreen.java']='package GameScreen;public class LoadMapScreen {public static boolean isNextMap=true;public int idMap=7;}'
stubs['Thread_More/LoadMap.java']='package Thread_More;public class LoadMap {public static int Area=0,MaxArea=10;}'
stubs['GameScreen/GameScreen.java']='package GameScreen;public class GameScreen {public static boolean isMapLang;public static GameObjects.Player player=new GameObjects.Player();}'
stubs['GameObjects/Player.java']=stubs['GameObjects/Player.java'].replace('public int hp','public static int isAutoFire;public int hp')
stubs['Main/GameCanvas.java']=stubs['Main/GameCanvas.java'].replace('public static Object currentScreen=shopNpc,currentDialog;',
    'public static Object game=new Object(),currentScreen=game,currentDialog;public static GameScreen.LoadMapScreen loadmap=new GameScreen.LoadMapScreen();public static void end_Dialog(){currentDialog=null;}')
stubs['CLib/Session_ME.java']='''package CLib;public class Session_ME {
static Session_ME s=new Session_ME(); public static int count,command,value,index;
public static Session_ME gI(){return s;}public void sendMessage(net.Message m){try{
count++;command=m.command;java.io.DataInputStream in=new java.io.DataInputStream(new java.io.ByteArrayInputStream(m.getData()));
value=command==51?in.readByte():in.readShort();if(command==-30){in.readByte();index=in.readByte();}
}catch(Exception e){throw new RuntimeException(e.toString());}}}'''
with tempfile.TemporaryDirectory(prefix='farm-test-') as directory:
    files=[]
    for name,body in stubs.items():
        path=Path(directory)/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(body,encoding='utf-8');files.append(str(path))
    for name in ('GameObjects/AutoFarmSupport.java','InterfaceComponents/DataRebuildItem.java','InterfaceComponents/IAction.java','net/Message.java'):
        files.append(str(ROOT/'src_decompiled'/name))
    files.append(str(ROOT/'tests/FarmSupportTest.java'))
    java=os.environ.get('JAVA','java')
    subprocess.run([java,'-jar',str(ROOT/'ecj.jar'),'-1.4','-encoding','UTF-8','-nowarn','-d',directory]+files,check=True)
    subprocess.run([java,'-cp',directory,'GameObjects.FarmSupportTest'],check=True)
