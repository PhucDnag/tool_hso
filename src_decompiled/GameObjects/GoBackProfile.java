package GameObjects;

import java.io.*;
import Model.CRes;
import Main.GameCanvas;
import GameScreen.GameScreen;
import GameScreen.LoginScreen;

/** Farm targets belong to a server and a character, never to the whole client. */
public final class GoBackProfile {
    private static String owner="";
    private static String identity() {
        if(GameScreen.player==null || GameScreen.player.name==null || GameScreen.player.name.length()==0) return "";
        return LoginScreen.ip+":"+GameCanvas.IndexServer+":"+GameScreen.player.name;
    }
    private static String storage(String id) { return "hso_gb_target_v1_"+Integer.toHexString(id.hashCode()); }
    public static void bind() {
        if(!GoBackNavigator.ready()) return;
        String id=identity();
        if(id.length()==0 || id.equals(owner)) return;
        if(owner.length()>0) write(owner);
        GoBackNavigator.stop();
        MenuUP.isGoBackActive=false; MenuUP.isGoBackPaused=false;
        boolean first=owner.length()==0;
        owner=id;
        try {
            byte[] bytes=CRes.loadRMS(storage(id));
            if(bytes!=null) {
                DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));
                int version=in.readInt();
                if((version!=1 && version!=2) || !id.equals(in.readUTF())) throw new IOException("profile identity");
                boolean enabled=in.readBoolean(); int map=in.readInt(); String name=in.readUTF();
                int x=in.readInt(),y=in.readInt(),area=in.readInt();
                if(area!=-1 && (area<1 || area>128)) throw new IOException("profile area");
                MenuUP.isGoBackEnabled=enabled; MenuUP.farmMapId=map; MenuUP.farmMapName=name;
                MenuUP.farmX=version==2?GoBackPosition.pixel(x):GoBackPosition.snap(x); MenuUP.farmY=version==2?GoBackPosition.pixel(y):GoBackPosition.snap(y); MenuUP.farmArea=area<0?-1:area-1;
                return;
            }
            // Claim legacy global settings once. Never copy them to every new character.
            if(first && CRes.loadRMS("hso_gb_legacy_claimed")==null) {
                CRes.saveRMS("hso_gb_legacy_claimed",new byte[]{1});
                write(owner); return;
            }
        } catch(Exception ex) { /* A corrupt profile must not route another character. */ }
        MenuUP.farmMapId=-1; MenuUP.farmMapName="";
        MenuUP.farmX=-1; MenuUP.farmY=-1; MenuUP.farmArea=-1;
    }
    public static void save() {
        if(owner.length()==0 && identity().length()>0) owner=identity();
        if(owner.length()>0 && (identity().length()==0 || owner.equals(identity()))) write(owner);
    }
    private static void write(String id) {
        try {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            DataOutputStream out=new DataOutputStream(bytes);
            out.writeInt(2); out.writeUTF(id); out.writeBoolean(MenuUP.isGoBackEnabled);
            out.writeInt(MenuUP.farmMapId); out.writeUTF(MenuUP.farmMapName==null?"":MenuUP.farmMapName);
            out.writeInt(GoBackPosition.tile(MenuUP.farmX)); out.writeInt(GoBackPosition.tile(MenuUP.farmY));
            // Human-facing area numbers are persisted; packet index is converted on read.
            out.writeInt(MenuUP.farmArea<0?-1:MenuUP.farmArea+1);
            out.flush(); CRes.saveRMS(storage(id),bytes.toByteArray());
        } catch(Exception ex) { /* Retain the in-memory target when RMS is full. */ }
    }
}
