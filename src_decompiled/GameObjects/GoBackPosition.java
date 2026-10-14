package GameObjects;

import GameScreen.GameScreen;
import Main.GameCanvas;
import Thread_More.LoadMap;

/** Same tile coordinates as PaintInfoGameScreen.paintPos_minimap. */
public final class GoBackPosition {
    public static int tile(int pixel) { return pixel<0 || LoadMap.wTile<=0?-1:pixel/LoadMap.wTile; }
    public static int pixel(int tile) { return tile<0?-1:tile*LoadMap.wTile+LoadMap.wTile/2; }
    public static int snap(int pixel) { return pixel(tile(pixel)); }
    public static String label(int x,int y) { return tile(x)+":"+tile(y); }
    public static boolean valid() {
        if(!GoBackNavigator.ready() || GameScreen.player.hp<=0 || GameScreen.player.Action==4) return false;
        int x=tile(GameScreen.player.x),y=tile(GameScreen.player.y),w=GameCanvas.loadmap.mapW;
        if(x<0 || y<0 || x>=w || y>=GameCanvas.loadmap.mapH) return false;
        int cell=GameCanvas.loadmap.mapType[y*w+x];
        return cell!=1 && cell!=-1;
    }
}
