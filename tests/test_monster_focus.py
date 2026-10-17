"""Execute production target filtering and target selection against mixed objects."""
import os, subprocess, tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
s=(ROOT/'src_decompiled/GameObjects/Player.java').read_text(encoding='utf-8')
helper=s[s.index('    public static boolean isAutoAttackTarget('):s.index('    public void cancelStandingChase()')]
menu=(ROOT/'src_decompiled/GameObjects/MenuUP.java').read_text(encoding='utf-8')
strip=menu[menu.index('    public static String stripAccents('):menu.index('    public static String farmAreaLabel(')]
selection=s[s.index('    public void nextMonster()'):s.index('    public void updateKey()')]
body="""
class MainObject {
 String name="";int typeObject=1,typeBoss,Action,typeMonster,x,y;boolean isDie,isRemove,isSend,lua,npc,server,cart,house,box,focus=true;
 boolean isLuaThieng(){return lua;}boolean isNPC(){return npc;}boolean isNPC_server(){return server;}
 boolean isMonstervantieu(){return cart;}boolean isMonsterHouse(){return house;}boolean isItemBox(){return box;}boolean canFocus(){return focus;}
 static int getDistance(int x,int y,int a,int b){return Math.abs(x-a)+Math.abs(y-b);}
}
class MenuUP {static boolean only=true;static boolean isMonsterFocusOnly(){return only;}static boolean shouldStandStill(){return false;} STRIP}
class GameScreen {static MainObject ObjFocus;static java.util.Vector Vecplayers=new java.util.Vector();}
class PaintInfoGameScreen {static boolean isPaintInfoFocus;}
public class FocusTest extends MainObject {
 static boolean isCurAutoFire=true;static byte isAutoFire=1;static short demUnFire;int wFocus=140;
 boolean canReachStanding(MainObject obj){return true;}
 METHODS
 static void check(boolean b){if(!b)throw new RuntimeException("focus regression");}
 public static void main(String[] args){
  FocusTest p=new FocusTest();check(!isAutoAttackTarget(null));
  for(int type=0;type<11;type++){MainObject obj=new MainObject();obj.typeObject=type;check(isAutoAttackTarget(obj)==(type==1));}
  for(int boss=1;boss<=5;boss++){MainObject obj=new MainObject();obj.typeBoss=boss;check(!isAutoAttackTarget(obj));GameScreen.Vecplayers.addElement(obj);}
  String[] names={"Bò Cạp Chúa","Quỷ Một Mắt","Xà Nữ","Dê Vàng","Quỷ Đầu Bò","Dê Bạc","Kỵ Sĩ Địa Ngục","Nhện Chúa","Giant Skeleton"};
  MainObject normal=new MainObject();normal.name="Bò cạp";normal.x=40;
  for(int i=0;i<names.length;i++){
   MainObject boss=new MainObject();boss.name=names[i];boss.x=1;
   check(boss.typeBoss==0 && !isAutoAttackTarget(boss));
   boss.name="[BOSS] " + names[i].toUpperCase() + " (Lv 140)";check(!isAutoAttackTarget(boss));
   boss.name=MenuUP.stripAccents(names[i]);check(!isAutoAttackTarget(boss));
   GameScreen.Vecplayers.removeAllElements();GameScreen.Vecplayers.addElement(boss);GameScreen.Vecplayers.addElement(normal);
   GameScreen.ObjFocus=boss;p.nextMonster();check(GameScreen.ObjFocus==normal);
   GameScreen.Vecplayers.removeElement(normal);p.nextMonster();check(GameScreen.ObjFocus==null);
  }
  check(isAutoAttackTarget(normal));normal.name="Nhện";check(isAutoAttackTarget(normal));normal.name=null;check(isAutoAttackTarget(normal));
  normal.name="Bò Cạp Chúa";check(!isAutoAttackTarget(normal));
  GameScreen.Vecplayers.removeAllElements();
  MainObject flaggedBoss=new MainObject();flaggedBoss.typeBoss=2;GameScreen.Vecplayers.addElement(flaggedBoss);
  MainObject npc=new MainObject();npc.typeObject=2;GameScreen.Vecplayers.addElement(npc);
  MainObject player=new MainObject();player.typeObject=0;GameScreen.Vecplayers.addElement(player);
  MainObject obj=new MainObject();obj.npc=true;check(!isAutoAttackTarget(obj));obj.npc=false;obj.server=true;check(!isAutoAttackTarget(obj));obj.server=false;
  obj.lua=true;check(!isAutoAttackTarget(obj));obj.lua=false;obj.cart=true;check(!isAutoAttackTarget(obj));obj.cart=false;
  obj.house=true;check(!isAutoAttackTarget(obj));obj.house=false;obj.box=true;check(!isAutoAttackTarget(obj));obj.box=false;
  obj.typeMonster=7;check(!isAutoAttackTarget(obj));obj.typeMonster=16;check(!isAutoAttackTarget(obj));obj.typeMonster=0;
  obj.isDie=true;check(!isAutoAttackTarget(obj));obj.isDie=false;obj.isRemove=true;check(!isAutoAttackTarget(obj));obj.isRemove=false;
  obj.focus=false;check(!isAutoAttackTarget(obj));obj.focus=true;obj.x=40;GameScreen.Vecplayers.addElement(obj);
  GameScreen.ObjFocus=npc;p.nextMonster();check(GameScreen.ObjFocus==obj);
  GameScreen.Vecplayers.removeElement(obj);p.nextMonster();check(GameScreen.ObjFocus==null && !PaintInfoGameScreen.isPaintInfoFocus);
  obj.x=500;GameScreen.Vecplayers.addElement(obj);p.nextMonster();check(GameScreen.ObjFocus==null);
  MenuUP.only=false;p.nextMonster();check(GameScreen.ObjFocus!=null && GameScreen.ObjFocus.typeBoss==2);
  System.out.println("PASS: ordinary monsters only, all boss flags, NPCs, players, special objects, stale focus, range and disabled mode");
 }
}
""".replace('METHODS',helper+selection).replace('STRIP',strip)
with tempfile.TemporaryDirectory(prefix='focus-test-') as d:
 p=Path(d)/'FocusTest.java';p.write_text(body,encoding='utf-8');java=os.environ.get('JAVA','java')
 subprocess.run([java,'-jar',str(ROOT/'ecj.jar'),'-1.4','-encoding','UTF-8','-nowarn','-d',d,str(p)],check=True)
 subprocess.run([java,'-cp',d,'FocusTest'],check=True)
