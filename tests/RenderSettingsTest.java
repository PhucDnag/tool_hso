package GameObjects;
import CLib.*;
import Main.GameCanvas;
import GameScreen.*;
import Thread_More.LoadMap;
public class RenderSettingsTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new RuntimeException(message);}
    public static void main(String[] args){
        RenderSettings.load();check(!RenderSettings.hidePlayers&&!RenderSettings.hideSkills&&!RenderSettings.gridMap,"default visuals unchanged");
        MainObject other=new MainObject(),npc=new MainObject();npc.typeObject=2;
        RenderSettings.hidePlayers=true;
        check(RenderSettings.hidden(other),"hide other player");check(!RenderSettings.hidden(GameScreen.player),"keep self");
        check(!RenderSettings.hidden(npc),"keep NPC");check(!RenderSettings.hidden(null),"null safe");
        Pet pet=new Pet();pet.typeObject=9;pet.owner=other;check(RenderSettings.hidden(pet),"hide other's pet");
        pet.owner=GameScreen.player;check(!RenderSettings.hidden(pet),"keep own pet");
        int[] tiles={0,1,2,-1,0,1,0,0,0};
        check(RenderSettings.walkable(tiles,3,3,0,0),"ground");check(!RenderSettings.walkable(tiles,3,3,1,0),"obstacle");
        check(RenderSettings.walkable(tiles,3,3,2,0),"water matches pathfinder");check(!RenderSettings.walkable(tiles,3,3,0,1),"negative blocker");
        check(!RenderSettings.walkable(tiles,3,3,-1,0),"negative position");check(!RenderSettings.walkable(tiles,3,3,3,0),"right edge");
        check(!RenderSettings.walkable(new int[1],3,3,2,2),"partial map");
        GameCanvas.loadmap.mapType=tiles;GameCanvas.loadmap.mapW=GameCanvas.loadmap.mapH=3;
        mGraphics g=new mGraphics();RenderSettings.paintGrid(g);
        check(g.cells==6,"only six passable cells drawn");check(!g.positions.containsKey("25,1"),"blocked square absent");
        check(g.positions.containsKey("1,1"),"walkable square present");check(tiles[1]==1,"no collision mutation");
        GameCanvas.loadmap.mapW=GameCanvas.loadmap.mapH=100;GameCanvas.loadmap.mapType=new int[10000];
        g=new mGraphics();RenderSettings.paintGrid(g);check(g.cells==9,"only viewport tiles, not entire map");
        MainScreen.cameraMain.xCam=24;g=new mGraphics();RenderSettings.paintGrid(g);check(!g.positions.containsKey("1,1"),"camera scrolling");
        mVector objects=new mVector();MainObject a=new MainObject(),b=new MainObject();a.ySort=1;b.ySort=2;objects.addElement(a);objects.addElement(b);
        check(!RenderSettings.needsSort(objects),"already sorted avoids quicksort");a.ySort=3;check(RenderSettings.needsSort(objects),"moving across another object sorts");
        a.ySort=2;check(!RenderSettings.needsSort(objects),"equal depths stable");
        for(int i=0;i<1000;i++)MenuUP.saveCurrentChar();check(MenuUP.saves==1,"1000 frames one RMS save");
        LoadMap.Area=2;MenuUP.saveCurrentChar();check(MenuUP.lastArea==2&&MenuUP.saves==1,"area tracked without disk write");
        GameScreen.player.name="new";MenuUP.saveCurrentChar();check(MenuUP.saves==2,"character switch saved");
        RenderSettings.hideSkills=RenderSettings.gridMap=true;RenderSettings.save();byte[] saved=Model.CRes.loadRMS("hso_render_v1");
        check(saved.length==3&&saved[0]==1&&saved[1]==1&&saved[2]==1,"options persisted");
        System.out.println("PASS: "+checks+" render/performance checks");
    }
}
