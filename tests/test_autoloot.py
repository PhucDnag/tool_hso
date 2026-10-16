import os, subprocess, tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
# Share the small game API fixtures; compile production AutoLoot separately.
namespace={"__file__":str(ROOT/'tests/test_autosell.py')}
source=(ROOT/'tests/test_autosell.py').read_text(encoding='utf-8')
exec(source[:source.index("java = os.environ")],namespace)
stubs=namespace['stubs']
stubs['CLib/mVector.java']='package CLib;public class mVector extends java.util.Vector {public mVector(){}public mVector(String s){}}'
stubs['GameObjects/AutoSellManager.java']='package GameObjects;public class AutoSellManager {public static boolean isReturningToSell;}'
stubs['GameObjects/AutoUpgrade.java']='package GameObjects;public class AutoUpgrade {}'
stubs['GameObjects/MainObject.java']='package GameObjects;public class MainObject {public int x,y,ID,colorName,hp=100,Action;public byte typeObject;public boolean isRemove;public static int getDistance(int x,int y,int a,int b){return Math.abs(x-a)+Math.abs(y-b);}}'
stubs['GameObjects/Player.java']='package GameObjects;public class Player extends MainObject {public int wFocus=100;public static boolean isFullInven;public static short maxInven=42;}'
stubs['GameObjects/MenuUP.java']='package GameObjects;public class MenuUP {public static boolean isGoBackActive;public static void showInventoryMenu(){}}'
stubs['GameObjects/GoBackNavigator.java']='package GameObjects;public class GoBackNavigator {}'
stubs['GameScreen/GameScreen.java']='package GameScreen;import GameObjects.*;import CLib.*;public class GameScreen {public static Player player=new Player();public static mVector Vecplayers=new mVector();}'
stubs['InterfaceComponents/IAction.java']='package InterfaceComponents;public interface IAction {void perform();}'
stubs['InterfaceComponents/iCommand.java']='package InterfaceComponents;public class iCommand {public iCommand(String s,IAction a){}}'
stubs['Main/GameCanvas.java']='package Main;public class GameCanvas {public static Object game=new Object(),currentScreen=game;public static Map loadmap=new Map();public static Menu menu2=new Menu();public static class Map {public int idMap;}public static class Menu {public void startAt(CLib.mVector v,int i,String s,boolean b,Object o){}}}'
stubs['netcommand/global/GlobalService.java']='package netcommand.global;public class GlobalService {static GlobalService s=new GlobalService();public static int sent,last;public static GlobalService gI(){return s;}public void Get_Item_Map(short id,byte t){sent++;last=id;}}'
stubs['GameObjects/LootTest.java']="""package GameObjects;import GameScreen.*;import CLib.*;import netcommand.global.*;
public class LootTest {
static void check(boolean b){if(!b)throw new RuntimeException("loot regression");}
public static void main(String[] args){
AutoLoot.load();AutoLoot.enabled=true;
for(int i=0;i<5;i++)AutoLoot.colors[i]=false;AutoLoot.colors[1]=true;
check(AutoLoot.accepts(1)&&!AutoLoot.accepts(0)&&!AutoLoot.accepts(5)&&!AutoLoot.accepts(-1));
MainObject drop=new MainObject();drop.ID=12;drop.typeObject=3;drop.colorName=1;drop.x=200;
GameScreen.Vecplayers.addElement(drop);mSystem.now=10000;AutoLoot.update();check(GlobalService.sent==0);
drop.x=20;AutoLoot.update();check(GlobalService.sent==1);
mSystem.now+=300;AutoLoot.update();check(GlobalService.sent==1);
mSystem.now+=2000;AutoLoot.update();check(GlobalService.sent==2);
mSystem.now+=2000;Player.isFullInven=true;AutoLoot.update();check(GlobalService.sent==2);Player.isFullInven=false;
AutoSellManager.isReturningToSell=true;AutoLoot.update();check(GlobalService.sent==2);AutoSellManager.isReturningToSell=false;
MenuUP.isGoBackActive=true;AutoLoot.update();check(GlobalService.sent==2);MenuUP.isGoBackActive=false;
drop.colorName=2;AutoLoot.update();check(GlobalService.sent==2);
drop.colorName=1;drop.typeObject=7;AutoLoot.update();check(GlobalService.sent==2);
drop.typeObject=3;drop.isRemove=true;AutoLoot.update();check(GlobalService.sent==2);
System.out.println("PASS: loot colors, range, retry limit, full bag, return route, removed drops and medal isolation");
}}
"""
java=os.environ.get('JAVA','java')
with tempfile.TemporaryDirectory(prefix='loot-test-') as directory:
    files=[]
    for name,content in stubs.items():
        p=Path(directory)/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content,encoding='utf-8');files.append(str(p))
    files.append(str(ROOT/'src_decompiled/GameObjects/AutoLoot.java'))
    subprocess.run([java,'-jar',str(ROOT/'ecj.jar'),'-1.4','-encoding','UTF-8','-nowarn','-d',directory]+files,check=True)
    subprocess.run([java,'-cp',directory,'GameObjects.LootTest'],check=True)
