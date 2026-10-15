"""Run production terrain dispatch and boat boarding logic without a server."""
import os
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
screen = (ROOT / 'src_decompiled/GameScreen/GameScreen.java').read_text(encoding='utf-8')
dispatch = screen[screen.index('            if (RenderSettings.gridMap) RenderSettings.paintGrid'):screen.index('            if (this.isFullScreen)', screen.index('            if (RenderSettings.gridMap) RenderSettings.paintGrid'))]
info = (ROOT / 'src_decompiled/GameScreen/PaintInfoGameScreen.java').read_text(encoding='utf-8')
boarding = info[info.index('    public void paintShip('):info.index('    public static boolean isMarket(')]
source = '''public class ShipGridTest {
    static class mGraphics {}
    static class EffectAuto {
        int x,y;
        EffectAuto(int a,int x,int y,int b,int c,int d,int e){this.x=x;this.y=y;}
        void update(){} void paint(mGraphics g){}
    }
    static class Player {
        int dyWater,Action,Direction,x,y;
        void updateActionPerson(){} void paintPlayer(mGraphics g,int a){}
    }
    static class Terrain {int idMap,paints; void paint(mGraphics g){paints++;}}
    static class GameCanvas {static Terrain loadmap=new Terrain();}
    static class GameScreen {static Player player=new Player();}
    static class RenderSettings {
        static boolean gridMap; static int grids;
        static void paintGrid(mGraphics g){grids++;}
    }
    static class ShipScr {
        static ShipScr instance=new ShipScr(); int typeMap,shows;
        static ShipScr gI(){return instance;} void Show(){shows++;}
    }
    static class Info {
        EffectAuto eff; boolean isShipMove;
''' + boarding + '''
    }
    static Info infoGame;
    static void frame(mGraphics mGraphics2) {
''' + dispatch + '''
    }
    static void check(boolean value,String message){if(!value)throw new RuntimeException(message);}
    public static void main(String[] args) {
        for(int grid=0;grid<2;grid++) for(int route=0;route<2;route++) {
            RenderSettings.gridMap=grid==1; RenderSettings.grids=0;
            GameCanvas.loadmap=new Terrain();GameCanvas.loadmap.idMap=route==0?19:67;
            ShipScr.instance=new ShipScr(); infoGame=new Info();
            mGraphics graphics=new mGraphics();
            frame(graphics);
            check(infoGame.eff!=null,"Boat must initialize in both terrain modes");
            int start=infoGame.eff.x;
            frame(graphics);
            check(infoGame.eff.x==start && ShipScr.gI().shows==0,"Idle boat must not depart");
            infoGame.isShipMove=true;
            for(int tick=0;tick<100 && ShipScr.gI().shows==0;tick++)frame(graphics);
            check(ShipScr.gI().shows==1,"Boarding must reach sailing screen");
            check(ShipScr.gI().typeMap==(route==0?1:3),"Wrong sailing direction");
            check(GameScreen.player.x==infoGame.eff.x+30,"Player must follow boat");
            check(grid==1?GameCanvas.loadmap.paints==0 && RenderSettings.grids>0:
                GameCanvas.loadmap.paints>0 && RenderSettings.grids==0,"Terrain mode changed");
        }
        System.out.println("PASS: idle and boarding, both directions, grid on/off");
    }
}
'''
with tempfile.TemporaryDirectory(prefix='ship-grid-test-') as directory:
    path = Path(directory) / 'ShipGridTest.java'
    path.write_text(source, encoding='utf-8')
    java = os.environ.get('JAVA', 'java')
    subprocess.run([java, '-jar', str(ROOT/'ecj.jar'), '-1.4', '-encoding', 'UTF-8',
                    '-nowarn', '-d', directory, str(path)], check=True)
    subprocess.run([java, '-cp', directory, 'ShipGridTest'], check=True)
