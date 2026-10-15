"""Compile production routing/navigation and run deterministic fake-server regressions.
Set JAVA to java.exe if needed. No live account or server is used.
"""
import os
import re
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
loader = (ROOT / 'src_decompiled/GameScreen/LoadMapScreen.java').read_text(encoding='utf-8')
load_condition = re.search(r'if \((isNextMap && .*?)\) \{', loader).group(1)
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
'GameObjects/AutoFarmSupport.java': 'package GameObjects;public class AutoFarmSupport {public static int goBackArea(int configured){return configured;}}',
'CLib/mVector.java': 'package CLib; public class mVector extends java.util.Vector {}',
'CLib/mSystem.java': 'package CLib; public class mSystem { public static long now=10000; public static long currentTimeMillis(){return now;} }',
'GameScreen/LoginScreen.java': 'package GameScreen; public class LoginScreen { public static String ip="test"; }',
'GameScreen/LoadMapScreen.java': '''package GameScreen; import Main.GameCanvas;import GameObjects.MainRMS;import Thread_More.SaveImageRMS;
public class LoadMapScreen {public static boolean isNextMap=true; public boolean isLoadSkill=true;long time;
public boolean canLeave(){return ''' + load_condition + ';}}',
'GameObjects/MainRMS.java': 'package GameObjects;public class MainRMS {public static boolean isLoadBegin=true;}',
'Thread_More/SaveImageRMS.java': 'package Thread_More;public class SaveImageRMS {public static CLib.mVector vecSaveImage=new CLib.mVector();}',
'GameScreen/GameScreen.java': """package GameScreen; import GameObjects.*; import CLib.*;
public class GameScreen { public static Player player=new Player(); public static MainObject ObjFocus;
public static boolean isMapLang; public static mVector Vecplayers=new mVector(); }""",
'GameObjects/Player.java': """package GameObjects; public class Player extends MainObject {
public static byte isAutoFire=1; public static boolean isCurAutoFire=true;
public static int xBeginAutoFire,yBeginAutofire;
public int toX,toY,xStopMove,yStopMove,countAutoMove,vx,vy,vMax=4; public short[] posTransRoad;
public int getVmount(){return 0;}
public void dofire_(){isAutoFire=1;isCurAutoFire=true;} public void nextMonster(){} public void setAutoFire(){} }""",
'GameObjects/MainObject.java': """package GameObjects; import GameScreen.GameScreen;
public class MainObject { public int x=36,y=36,hp=100,Action,ID,typeObject,Lv; public String name="", nameGiaotiep="";
public int getIDnpc(){return ID;}
public void GiaoTiep(){}
public static int getDistance(int x,int y,int a,int b){return (int)Math.sqrt((x-a)*(x-a)+(y-b)*(y-b));}
public static MainObject get_Object(int id,byte type){for(int i=0;i<GameScreen.Vecplayers.size();i++){
MainObject o=(MainObject)GameScreen.Vecplayers.elementAt(i);if(o.ID==id&&o.typeObject==type)return o;}return null;} }""",
'Thread_More/LoadMap.java': """package Thread_More; import CLib.*;
public class LoadMap { public static int wTile=24; public static byte Area;
public static mVector vecPointChange=new mVector(); public int idMap,mapW=10,mapH=10;
public String nameMap=""; public int[] mapType=new int[100]; }""",
'Main/GameCanvas.java': """package Main; import Thread_More.*;
public class GameCanvas { public static Object game=new Object(),currentScreen=game;public static long timeNow;
public static LoadMap loadmap=new LoadMap(); public static int IndexServer=0;
public static Model.Menu2 menu2=new Model.Menu2(); public static void addInfoCharCline(String s){} public static void end_Dialog(){} }""",
'Model/Menu2.java': 'package Model; public class Menu2 {public boolean isShowMenu;}',
'GameObjects/Item.java': 'package GameObjects; public class Item { public int Id; public String itemName; public static CLib.mVector VecInvetoryPlayer=new CLib.mVector(); }',
'Model/Point.java': 'package Model; public class Point {public int x,y,dis;public String name;}',
'Model/T.java': 'package Model; public class T {public static String[] mapName=new String[]{"Small Village","Wolf Village","Mines","Edge","Fire","Forest","Canyon"};}',
'Model/CRes.java': """package Model; public class CRes {
static java.util.Hashtable rms=new java.util.Hashtable();public static int writes;
public static byte[] loadRMS(String k){return (byte[])rms.get(k);} public static void saveRMS(String k,byte[] v){writes++;rms.put(k,v);} }""",
'InterfaceComponents/iCommand.java': 'package InterfaceComponents; public class iCommand { public String caption; public iCommand(String s){caption=s;} }',
'netcommand/global/GlobalService.java': """package netcommand.global; import GameObjects.*; import CLib.*; import InterfaceComponents.*;
public class GlobalService {static GlobalService s=new GlobalService(); public static int changes,lastIndex=-1,requests,moves; public static boolean rejectArea;
public static GlobalService gI(){return s;}
public static int homes,homeMode=-1;public void gohome(byte mode){homes++;homeMode=mode;}
public void Change_Area(byte a){changes++;if(!rejectArea)Thread_More.LoadMap.Area=a;}
public void player_move(short x,short y){moves++;}
public void getlist_from_npc(byte id){requests++;mVector opts=new mVector();
// Reorder the fresh menu: an old index would choose the wrong action.
if(requests%2==0){opts.addElement(new iCommand("Cancel"));opts.addElement(new iCommand(MenuUP.farmMapName));}
else {opts.addElement(new iCommand(MenuUP.farmMapName));opts.addElement(new iCommand("Cancel"));}
Main.GameCanvas.menu2.isShowMenu=true;GoBackNavigator.onMenu(id,7,opts);}
public void Dynamic_Menu(short id,byte menu,byte index){lastIndex=index;}
}""",
}
methods = '\n'.join(method(n) for n in ('stripAccents','isVillage','getMapName','getMapIdByName','farmAreaLabel',
    'isHorseId','isMountKeyword','findMountItem',
    'shouldAutoRevive','updateAutoRevive',
    'rememberFarmLocation','beginGoBack','updateGoBack','saveCurrentFarmLocation'))
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
private static void stopGoBackMovement(){GoBackNavigator.stop();}
public static void tick(){GoBackNavigator.observe();rememberFarmLocation();updateGoBack();}
""" + methods + '\n}'
java = os.environ.get('JAVA','java')
with tempfile.TemporaryDirectory(prefix='goback-test-') as directory:
    folder=Path(directory)
    files=[]
    for name, body in stubs.items():
        path=folder/name; path.parent.mkdir(parents=True,exist_ok=True); path.write_text(body,encoding='utf-8');files.append(str(path))
    for name in ('GoBackPosition','GoBackPathfinder','GoBackRoutes','GoBackAtlas','GoBackNavigator','GoBackProfile'):
        files.append(str(ROOT/'src_decompiled/GameObjects'/(name+'.java')))
    files.append(str(ROOT/'tests/GoBackTest.java'))
    subprocess.run([java,'-jar',str(ROOT/'ecj.jar'),'-1.4','-encoding','UTF-8','-nowarn','-d',directory]+files,check=True)
    subprocess.run([java,'-cp',directory,'GameObjects.GoBackTest'],check=True)
