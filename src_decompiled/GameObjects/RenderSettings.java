package GameObjects;

import CLib.*;
import GameScreen.GameScreen;
import GameScreen.MainScreen;
import Main.GameCanvas;
import Model.CRes;
import Thread_More.LoadMap;
import InterfaceComponents.*;

/** Visual switches only; collision, network state and effect lifetimes stay intact. */
public final class RenderSettings {
    public static boolean hidePlayers, hideSkills, gridMap;
    private static boolean loaded;
    public static void load(){
        if(loaded)return;
        loaded=true;
        byte[] data=CRes.loadRMS("hso_render_v1");
        if(data!=null && data.length==3){hidePlayers=data[0]==1;hideSkills=data[1]==1;gridMap=data[2]==1;}
    }
    public static void save(){
        try{CRes.saveRMS("hso_render_v1",new byte[]{(byte)(hidePlayers?1:0),(byte)(hideSkills?1:0),(byte)(gridMap?1:0)});}
        catch(Exception e){GameCanvas.addInfoCharCline("Không lưu được tùy chọn hiển thị");}
    }
    public static boolean hidden(MainObject object){
        if(!hidePlayers || object==null || object==GameScreen.player)return false;
        if(object.typeObject==0)return true;
        if(object instanceof Pet){MainObject owner=((Pet)object).owner;return owner!=null && owner!=GameScreen.player && owner.typeObject==0;}
        return false;
    }
    public static boolean walkable(int[] tiles,int width,int height,int x,int y){
        if(tiles==null || width<=0 || height<=0 || x<0 || y<0 || x>=width || y>=height)return false;
        int index=y*width+x;
        return index>=0 && index<tiles.length && tiles[index]!=1 && tiles[index]!=-1;
    }
    public static void paintGrid(mGraphics g){
        LoadMap map=GameCanvas.loadmap;
        int cx=MainScreen.cameraMain.xCam,cy=MainScreen.cameraMain.yCam,size=LoadMap.wTile;
        g.setColor(0x101419);g.fillRect(cx,cy,GameCanvas.w,GameCanvas.h,false);
        if(map==null || size<=0)return;
        int left=Math.max(0,cx/size),top=Math.max(0,cy/size);
        int right=Math.min(map.mapW,(cx+GameCanvas.w)/size+1),bottom=Math.min(map.mapH,(cy+GameCanvas.h)/size+1);
        g.setColor(0x354550);
        for(int y=top;y<bottom;y++)for(int x=left;x<right;x++)
            if(walkable(map.mapType,map.mapW,map.mapH,x,y))g.fillRect(x*size+1,y*size+1,size-2,size-2,false);
    }
    public static boolean needsSort(mVector objects){
        int previous=Integer.MIN_VALUE;
        for(int i=0;i<objects.size();i++){
            MainObject object=(MainObject)objects.elementAt(i);
            if(object==null)continue;
            if(object.ySort<previous)return true;
            previous=object.ySort;
        }
        return false;
    }
    private static String state(boolean enabled){return enabled?"BẬT":"TẮT";}
    public static void show(){
        load();mVector menu=new mVector();
        menu.addElement(new iCommand("Ẩn người chơi khác: "+state(hidePlayers),new IAction(){public void perform(){hidePlayers=!hidePlayers;save();show();}}));
        menu.addElement(new iCommand("Ẩn chiêu/hiệu ứng: "+state(hideSkills),new IAction(){public void perform(){hideSkills=!hideSkills;save();show();}}));
        menu.addElement(new iCommand("Map ô vuông: "+state(gridMap),new IAction(){public void perform(){gridMap=!gridMap;save();show();}}));
        menu.addElement(new iCommand("Bật cả 3 để giảm tải",new IAction(){public void perform(){hidePlayers=hideSkills=gridMap=true;save();show();}}));
        menu.addElement(new iCommand("Hiển thị đầy đủ",new IAction(){public void perform(){hidePlayers=hideSkills=gridMap=false;save();show();}}));
        menu.addElement(new iCommand("Quay lại",new IAction(){public void perform(){MenuUP.showMenu();}}));
        GameCanvas.menu2.startAt(menu,2,"HIỂN THỊ & GIẢM LAG",false,null);
    }
}
