"""Exercise visual filtering, viewport grid drawing and per-frame write prevention."""
import os
import subprocess
import tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
source = (ROOT / 'tests/test_autoupgrade.py').read_text(encoding='utf-8')
exec(source[:source.index('with tempfile.TemporaryDirectory')])
stubs['CLib/Session_ME.java'] = 'package CLib;public class Session_ME {}'
menu = (ROOT / 'src_decompiled/GameObjects/MenuUP.java').read_text(encoding='utf-8')
method = menu[menu.index('    public static void saveCurrentChar()'):menu.index('    public static boolean isDisconnectString(')]
stubs['GameObjects/MenuUP.java'] = '''package GameObjects;import GameScreen.GameScreen;import Thread_More.LoadMap;
public class MenuUP {public static String lastCharName="";public static int lastCharId=-1,saves;public static byte lastArea;
public static void showMenu(){}public static void saveConfig(){saves++;}''' + method + '}'
stubs['GameObjects/MainObject.java'] = 'package GameObjects;public class MainObject {public int typeObject,ySort;}'
stubs['GameObjects/Player.java'] = 'package GameObjects;public class Player extends MainObject {public String name="test";public int ID=1;}'
stubs['GameObjects/Pet.java'] = 'package GameObjects;public class Pet extends MainObject {protected MainObject owner;}'
stubs['Thread_More/LoadMap.java'] = 'package Thread_More;public class LoadMap {public static int wTile=24,Area;public int[] mapType;public int mapW,mapH;}'
stubs['GameScreen/MainScreen.java'] = 'package GameScreen;public class MainScreen {public static Camera cameraMain=new Camera();public static class Camera {public int xCam,yCam;}}'
stubs['Main/GameCanvas.java'] = '''package Main;public class GameCanvas {
public static int w=48,h=48;public static Thread_More.LoadMap loadmap=new Thread_More.LoadMap();
public static Model.Menu2 menu2=new Model.Menu2();public static void addInfoCharCline(String s){}}'''
stubs['CLib/mGraphics.java'] = '''package CLib;public class mGraphics {
public int cells;public java.util.Hashtable positions=new java.util.Hashtable();public void setColor(int c){}
public void fillRect(int x,int y,int w,int h,boolean b){if(w==22&&h==22){cells++;positions.put(x+","+y,"yes");}}
}'''
with tempfile.TemporaryDirectory(prefix='render-test-') as directory:
    files=[]
    for name,body in stubs.items():
        path=Path(directory)/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(body,encoding='utf-8');files.append(str(path))
    files += [str(ROOT/'src_decompiled'/name) for name in ('GameObjects/RenderSettings.java','InterfaceComponents/IAction.java','InterfaceComponents/DataRebuildItem.java')]
    files.append(str(ROOT/'tests/RenderSettingsTest.java'))
    java=os.environ.get('JAVA','java')
    subprocess.run([java,'-jar',str(ROOT/'ecj.jar'),'-1.4','-encoding','UTF-8','-nowarn','-d',directory]+files,check=True)
    subprocess.run([java,'-cp',directory,'GameObjects.RenderSettingsTest'],check=True)
