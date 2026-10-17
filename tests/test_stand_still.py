"""Run actual stationary targeting and cancellation methods with JVM doubles."""
import os, subprocess, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
s = (ROOT/'src_decompiled/GameObjects/Player.java').read_text(encoding='utf-8')
methods = s[s.index('    public void cancelStandingChase()'):s.index('    public void setAutoFire()')]
body = """
class MainObject {
 int typeObject=1,x,y,hOne;
 static int getDistance(int x,int y,int a,int b){return (int)Math.sqrt((x-a)*(x-a)+(y-b)*(y-b));}
}
class MenuUP {static boolean enabled=true;static boolean shouldStandStill(){return enabled;}}
class Skill {int typeSkill;}
class MainListSkill {static int getRange(int id){return id==1?100:50;}static Skill getSkillFormId(int id){return new Skill();}}
class HotKey {static int SKILL=1;int type=1,id=1;}
public class StandingTest extends MainObject {
 int KillFire=1,IDAttack=1,countAutoMove=2,xStopMove=90,yStopMove=90,toX=90,toY=90,vx=5,vy=5,Action=1,levelTab;
 Object vecObjFocusSkill=new Object(),posTransRoad=new Object();
 HotKey[][] mhotkey={{new HotKey()}};
 METHODS
 static void check(boolean b){if(!b)throw new RuntimeException("stationary regression");}
 public static void main(String[] args){
  StandingTest p=new StandingTest();MainObject target=new MainObject();
  target.x=50;check(p.canAttackStanding(0,target));
  target.x=51;check(!p.canAttackStanding(0,target));check(p.canReachStanding(target));
  target.x=101;check(!p.canReachStanding(target));
  target.x=0;target.y=60;target.hOne=40;check(p.canAttackStanding(0,target));
  target.x=500;MenuUP.enabled=false;check(p.canReachStanding(target));
  MenuUP.enabled=true;target.typeObject=0;check(p.canAttackStanding(0,target));
  p.cancelStandingChase();check(p.posTransRoad==null && p.KillFire==-1 && p.IDAttack==-1 && p.vecObjFocusSkill==null && p.vx==0 && p.vy==0 && p.toX==p.x && p.toY==p.y && p.Action==0);
  p.Action=2;p.cancelStandingChase();check(p.Action==2);
  System.out.println("StandingTest passed");
 }
}
""".replace('METHODS',methods)
with tempfile.TemporaryDirectory(prefix='standing-test-') as d:
 p=Path(d)/'StandingTest.java';p.write_text(body,encoding='utf-8')
 java=os.environ.get('JAVA','java')
 subprocess.run([java,'-jar',str(ROOT/'ecj.jar'),'-1.4','-encoding','UTF-8','-nowarn','-d',d,str(p)],check=True)
 subprocess.run([java,'-cp',d,'StandingTest'],check=True)
