"""Exercise the native Player pickup loop and the native-dialog shortcut."""
import os, subprocess, tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
source=(ROOT/'src_decompiled/GameObjects/Player.java').read_text(encoding='utf-8')
methods=source[source.index('    private void autoGetItem()'):source.index('    public static boolean isAutoAttackTarget(')]
stubs={
 'CLib/mVector.java':'package CLib;public class mVector extends java.util.Vector {}',
 'GameObjects/MainObject.java':"""package GameObjects;public class MainObject {
 public int x,y,ID,colorName;public byte typeObject;public boolean isSend;
 public static int getDistance(int x,int y,int a,int b){return Math.abs(x-a)+Math.abs(y-b);}}""",
 'GameObjects/Item.java':'package GameObjects;public class Item {public static CLib.mVector VecInvetoryPlayer=new CLib.mVector();}',
 'GameObjects/MedalFilter.java':'package GameObjects;public class MedalFilter {public static boolean shouldPickMedal(int c){return true;}}',
 'GameScreen/GameScreen.java':'package GameScreen;public class GameScreen {public static GameObjects.MainObject ObjFocus;public static CLib.mVector Vecplayers=new CLib.mVector();}',
 'Main/GameCanvas.java':"""package Main;public class GameCanvas {
 public static int gameTick=1,opened;public static Menu menu2=new Menu();
 public static void addInfoChar(String s){}public static void start_Auto_Item_Dialog(){if(!menu2.closed)throw new RuntimeException("menu overlay");opened++;}
 public static class Menu {public boolean closed;public void doCloseMenu(){closed=true;}}}""",
 'Model/T.java':'package Model;public class T {public static String fullInven="full";}',
 'netcommand/global/GlobalService.java':"""package netcommand.global;public class GlobalService {
 static GlobalService s=new GlobalService();public static int sent,last;
 public static GlobalService gI(){return s;}public void Get_Item_Map(short id,byte t){sent++;last=id;}}""",
 'GameObjects/Player.java':"""package GameObjects;
 import GameScreen.GameScreen;import Main.GameCanvas;import Model.T;import netcommand.global.GlobalService;
 public class Player extends MainObject {
 public static AutoGetItem autoItem;public static boolean isFullInven;public static int maxInven=42;
 public int wFocus=100;void nextFocus(){}
 METHODS
 static void check(boolean b){if(!b)throw new RuntimeException("native pickup regression");}
 public static void main(String[] args){
  Player p=new Player();MainObject drop=new MainObject();drop.typeObject=3;drop.ID=12;drop.x=20;
  GameScreen.Vecplayers.addElement(drop);
  for(int threshold=-1;threshold<=4;threshold++){
   autoItem=new AutoGetItem((byte)threshold,AutoGetItem.POI_KHONG_NHAT,(byte)1);
   for(int color=0;color<=4;color++){
    drop.colorName=color;drop.isSend=false;GlobalService.sent=0;p.autoGetItem();
    check(GlobalService.sent==((threshold>=0 && color>=threshold)?1:0));
   }
  }
  autoItem=new AutoGetItem((byte)0,AutoGetItem.POI_KHONG_NHAT,(byte)1);
  drop.isSend=false;drop.x=200;GlobalService.sent=0;p.autoGetItem();check(GlobalService.sent==0);
  drop.x=20;drop.isSend=false;isFullInven=true;p.autoGetItem();check(GlobalService.sent==0);isFullInven=false;
  drop.typeObject=4;drop.colorName=0;drop.isSend=false;p.autoGetItem();check(GlobalService.sent==0);
  autoItem.isGetPotion=AutoGetItem.POI_NHAT_MAU;drop.isSend=false;p.autoGetItem();check(GlobalService.sent==1);
  AutoLoot.show();check(GameCanvas.opened==1);
  System.out.println("PASS: six native loot modes, range, full bag, potion settings and native dialog");
 }
 }""".replace('METHODS',methods)
}
java=os.environ.get('JAVA','java')
with tempfile.TemporaryDirectory(prefix='native-loot-test-') as directory:
 files=[]
 for name,content in stubs.items():
  p=Path(directory)/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content,encoding='utf-8');files.append(str(p))
 for name in ('AutoLoot.java','AutoGetItem.java'):
  files.append(str(ROOT/'src_decompiled/GameObjects'/name))
 subprocess.run([java,'-jar',str(ROOT/'ecj.jar'),'-1.4','-encoding','UTF-8','-nowarn','-d',directory]+files,check=True)
 subprocess.run([java,'-cp',directory,'GameObjects.Player'],check=True)
