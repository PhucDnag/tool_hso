package GameObjects;

import CLib.*;
import GameScreen.*;
import Main.GameCanvas;
import Model.Point;
import Thread_More.LoadMap;
import netcommand.global.GlobalService;

public class GoBackTest {
    static int assertions;
    static boolean nativeMovement;
    static void check(boolean condition,String message) {
        assertions++;
        if(!condition) throw new RuntimeException(message);
    }
    static void paths() {
        int[] tiles=new int[49];
        for(int y=0;y<6;y++) tiles[y*7+3]=1;
        int[] road=GoBackPathfinder.findCells(tiles,7,7,1,1,5,1,0);
        check(road.length==15,"shortest detour");
        for(int i=1;i<road.length;i++) {
            check(tiles[road[i]]!=1,"wall collision");
            check(Math.abs(road[i]%7-road[i-1]%7)+Math.abs(road[i]/7-road[i-1]/7)==1,"diagonal");
        }
        tiles[45]=-1;
        check(GoBackPathfinder.findCells(tiles,7,7,1,1,5,1,0)==null,"sealed wall");
        check(GoBackPathfinder.findCells(tiles,7,7,1,1,3,1,0)==null,"blocked destination");
        check(GoBackPathfinder.findCells(tiles,7,7,3,1,1,1,0)==null,"blocked start");
        check(GoBackPathfinder.findCells(new int[]{0,1,1,0},2,2,0,0,1,1,0)==null,"corner cutting");
        check(GoBackPathfinder.findCells(new int[40000],200,200,0,0,199,199,0).length==399,"large map");
        int[] npcMap=new int[25];npcMap[12]=1;
        check(GoBackPathfinder.findCells(npcMap,5,5,0,0,2,2,1).length==4,"NPC approach");
    }
    static void routes() throws Exception {
        GoBackRoutes g=new GoBackRoutes();
        int a=g.node("village",1), b=g.node("edge",3), target=g.node("reef",134);
        g.scan(a,new String[]{"edge"},new int[]{12},new int[]{36});
        check(g.plan(a,target)==null,"must not explore when the full route is unknown");
        g.scan(b,new String[]{"village","fire"},new int[]{12,108},new int[]{36,36});
        GoBackRoutes.Plan p=g.plan(b,target);
        check(p==null,"must not choose an unscanned frontier");
        int c=g.node("fire",4);
        g.scan(c,new String[]{"edge","reef"},new int[]{12,108},new int[]{36,36});
        check(g.plan(a,target).hops==3,"shortest known route");
        check(g.plan(target,a)==null,"invented reverse edge");
        GoBackRoutes.Exit e=g.plan(b,target).first;
        g.confirm(e,a); // Server contradicts the label.
        g.scan(b,new String[]{"village","fire"},new int[]{12,108},new int[]{36,36});
        check(e.to==a && g.plan(b,target)==null,"confirmed destination overwritten / oscillation");
        GoBackRoutes copy=GoBackRoutes.load(g.save());
        check(copy.get(b).exits.size()==2 && copy.plan(b,target)==null,"RMS roundtrip");
        GoBackRoutes.Exit shortcut=g.exit(a,target,60,60,9);
        check(g.plan(a,target).hops==1,"teleport shortcut");
        g.block(shortcut); check(g.plan(a,target)==null,"blocked exit reused");
        g.resetFailures(); check(g.plan(a,target).first==shortcut,"manual retry");
        g.scan(a,new String[0],new int[0],new int[0]);
        check(g.get(a).exits.size()==1,"stale walking exits retained");
        // No hard map/monster-level cap, no guessed edges from neighbouring IDs.
        check(g.node("new map lv134",501)>=0 && g.plan(g.byId(501),target)==null,"map ID cap / fake edge");
        int dup=g.node("arena",600);g.node("arena",601);
        check(g.byName("arena")==-1 && g.node("arena",-1)==-1,"ambiguous map name");
        GoBackRoutes finite=new GoBackRoutes();
        int x=finite.node("a",1),y=finite.node("b",2),z=finite.node("missing",3);
        finite.scan(x,new String[]{"b"},new int[]{1},new int[]{1});
        finite.scan(y,new String[]{"a"},new int[]{1},new int[]{1});
        check(finite.plan(x,z)==null && finite.plan(y,z)==null,"endless two-map loop");
    }
    static void setMap(int id,String name,String[] names) {
        GameCanvas.loadmap=new LoadMap(); GameCanvas.loadmap.idMap=id;GameCanvas.loadmap.nameMap=name;
        LoadMap.vecPointChange.removeAllElements();
        for(int i=0;i<names.length;i++) {
            Point p=new Point();p.name=names[i];p.x=i==0?12:204;p.y=108;p.dis=0;
            LoadMap.vecPointChange.addElement(p);
        }
        GameScreen.player.x=108;GameScreen.player.y=60;GameScreen.player.posTransRoad=null;
        GameScreen.player.hp=100;GameScreen.player.Action=0;GameScreen.isMapLang=id==1;
        GameScreen.Vecplayers.removeAllElements();
        if(id!=1) { MainObject m=new MainObject();m.typeObject=1;m.Lv=id==134?134:3;GameScreen.Vecplayers.addElement(m); }
        LoadMap.Area=0;LoadMapScreen.isNextMap=true;GameCanvas.currentScreen=GameCanvas.game;
    }
    static void newRun(String server,int goal,String name) {
        LoginScreen.ip=server;MenuUP.isGoBackActive=false;MenuUP.isGoBackPaused=false;MenuUP.isAutoAttack=true;
        MenuUP.farmMapId=goal;MenuUP.farmMapName=name;MenuUP.farmX=84;MenuUP.farmY=84;MenuUP.farmArea=3;
        GlobalService.changes=0;GlobalService.lastIndex=-1;GlobalService.requests=0;GlobalService.rejectArea=false;
        Player.isAutoFire=1;Player.isCurAutoFire=true;
    }
    // Simulate Player's single-waypoint consumer with collision checks on every pixel.
    static void movePlayer() {
        Player p=GameScreen.player;
        if(p.posTransRoad==null) return;
        int tx=p.toX,ty=p.toY;
        if(nativeMovement) {
            // Match Player.update and MainObject.move_to_XY: axis tolerance,
            // fixed speed, and no snapping to the requested coordinate.
            if(Math.abs(p.x-tx)<=6 && Math.abs(p.y-ty)<=6) p.posTransRoad=null;
            if(Math.abs(p.x-tx)>p.vMax) p.x+=p.x<tx?p.vMax:-p.vMax;
            else if(Math.abs(p.y-ty)>p.vMax) p.y+=p.y<ty?p.vMax:-p.vMax;
            int tile=GameCanvas.loadmap.mapType[p.y/24*GameCanvas.loadmap.mapW+p.x/24];
            check(tile!=1 && tile!=-1,"native movement hit obstacle");
            return;
        }
        for(int k=0;k<12;k++) {
            if(p.x!=tx) p.x+=p.x<tx?1:-1;
            else if(p.y!=ty) p.y+=p.y<ty?1:-1;
            int tile=GameCanvas.loadmap.mapType[p.y/24*GameCanvas.loadmap.mapW+p.x/24];
            check(tile!=1 && tile!=-1,"runtime walked through obstacle");
        }
        if(p.x==tx && p.y==ty) p.posTransRoad=null;
    }
    static int exitTouched() {
        for(int i=0;i<LoadMap.vecPointChange.size();i++) {
            Point p=(Point)LoadMap.vecPointChange.elementAt(i);
            if(MainObject.getDistance(GameScreen.player.x,GameScreen.player.y,p.x,p.y)<=3) return i;
        }
        return -1;
    }
    static void runtime() {
        newRun("walk",134,"reef");
        setMap(4,"fire",new String[]{"edge","reef"});GoBackNavigator.observe();
        setMap(3,"edge",new String[]{"village","fire"});GoBackNavigator.observe();
        setMap(1,"village",new String[]{"edge"});
        int transitions=0; boolean done=false;
        for(int tick=0;tick<1200;tick++) {
            mSystem.now+=100;MenuUP.tick();movePlayer();
            int touched=exitTouched(),id=GameCanvas.loadmap.idMap;
            if(id==1 && touched==0) {setMap(3,"edge",new String[]{"village","fire"});transitions++;}
            else if(id==3 && touched==0) throw new RuntimeException("regression: Bia Rung returned to village");
            else if(id==3 && touched==1) {setMap(4,"fire",new String[]{"edge","reef"});transitions++;}
            else if(id==4 && touched==0) throw new RuntimeException("regression: fire returned to edge");
            else if(id==4 && touched==1) {setMap(134,"reef",new String[]{"fire"});transitions++;}
            if(id==134 && !MenuUP.isGoBackActive) {done=true;break;}
        }
        check(done && transitions==3,"did not reach saved map");
        check(LoadMap.Area==3 && GlobalService.changes==1,"saved area");
        check(GameScreen.player.x==84 && GameScreen.player.y==84,"saved coordinates");
        check(Player.isAutoFire==1,"combat not resumed");
        check(GoBackNavigator.graph.get(GoBackNavigator.graph.byId(134)).maxLevel==134,"Lv134 metadata");
        int node=GoBackNavigator.graph.byId(1),goal=GoBackNavigator.graph.byId(134);
        check(GoBackNavigator.graph.plan(node,goal).hops==3,"learned route missing");
        // Reload the graph for the same server after switching to a different one.
        LoginScreen.ip="other";GoBackNavigator.observe();LoginScreen.ip="walk";GoBackNavigator.observe();
        check(GoBackNavigator.graph.plan(GoBackNavigator.graph.byId(1),GoBackNavigator.graph.byId(134)).hops==3,"route not persisted");

        newRun("no-route",134,"reef");setMap(1,"village",new String[]{"edge"});
        transitions=0;
        for(int tick=0;tick<500;tick++) {
            mSystem.now+=100;MenuUP.tick();movePlayer();
            if(GameCanvas.loadmap.idMap==1 && exitTouched()==0) {setMap(3,"edge",new String[]{"village"});transitions++;}
            else if(GameCanvas.loadmap.idMap==3 && exitTouched()==0) throw new RuntimeException("closed component oscillation");
        }
        check(transitions==0 && GoBackNavigator.status.indexOf("Chua co lo trinh")>=0,"unknown target must not cause any map exploration");
    }
    static void atlas() {
        GoBackRoutes g=new GoBackRoutes();GoBackAtlas.seed(g);
        int start=g.byName("lang soi trang"),target=g.byName("rang da ngam");
        check(start>=0 && target>=0,"atlas name legend");
        GoBackRoutes.Plan route=g.plan(start,target);
        check(route!=null && route.hops==4,"world-map shortest route to reef");
        check(!g.get(route.first.to).key.equals("bia rung"),"wrong branch toward forest");
        g.scan(start,new String[]{"bia rung","canh dong soi"},new int[]{12,204},new int[]{108,108});
        route=g.plan(start,target);
        check(g.get(route.first.to).key.equals("canh dong soi"),"actual village exit toward reef");
        int fields=g.byName("canh dong soi");
        check(g.get(g.plan(fields,target).first.to).key.equals("bo bien"),"fields to coast");
        g.scan(fields,new String[]{"lang soi trang"},new int[]{12},new int[]{108});
        check(g.plan(start,target)==null,"live portals must override an incompatible atlas");
        GoBackAtlas.seed(g);
        check(g.plan(start,target)==null,"reseed must not resurrect rejected atlas edges");

        newRun("atlas-cold",13,"Rặng Đá Ngầm");
        setMap(1,"Làng Sói Trắng",new String[]{"Bìa Rừng","Cánh Đồng Sói"});
        boolean done=false;int changes=0;
        for(int tick=0;tick<1600;tick++) {
            mSystem.now+=100;MenuUP.tick();movePlayer();
            int touched=exitTouched(),id=GameCanvas.loadmap.idMap;
            if(touched==0 && id!=13) throw new RuntimeException("cold atlas chose a backward/wrong exit");
            if(touched==1 && id==1) {setMap(7,"Cánh Đồng Sói",new String[]{"Làng Sói Trắng","Bờ Biển"});changes++;}
            else if(touched==1 && id==7) {setMap(11,"Bờ Biển",new String[]{"Cánh Đồng Sói","Vực Đá"});changes++;}
            else if(touched==1 && id==11) {setMap(12,"Vực Đá",new String[]{"Bờ Biển","Rặng Đá Ngầm"});changes++;}
            else if(touched==1 && id==12) {setMap(13,"Rặng Đá Ngầm",new String[]{"Vực Đá"});changes++;}
            if(id==13 && !MenuUP.isGoBackActive) {done=true;break;}
        }
        check(done && changes==4,"cold-start route without exploring maps");
    }
    static void teleport() {
        newRun("teleport",134,"reef");setMap(1,"village",new String[0]);
        MainObject stone=new MainObject();stone.typeObject=2;stone.ID=9;stone.name="Da dich chuyen";stone.x=84;stone.y=84;
        GameScreen.Vecplayers.addElement(stone);GameCanvas.loadmap.mapType[33]=1;
        boolean sent=false;
        for(int tick=0;tick<300;tick++) {
            mSystem.now+=100;MenuUP.tick();movePlayer();
            if(GlobalService.lastIndex>=0) {sent=true;break;}
        }
        check(sent && GlobalService.requests==1 && GlobalService.lastIndex==0,"reuse fresh teleport menu / NPC obstacle");
        setMap(134,"reef",new String[0]);mSystem.now+=100;MenuUP.tick();
        GoBackRoutes.Node village=GoBackNavigator.graph.get(GoBackNavigator.graph.byId(1));
        GoBackRoutes.Exit e=null;
        for(int i=0;i<village.exits.size();i++) {
            GoBackRoutes.Exit candidate=(GoBackRoutes.Exit)village.exits.elementAt(i);
            if(candidate.to==GoBackNavigator.graph.byId(134)) e=candidate;
        }
        check(e!=null && e.confirmed && e.npc==9,"teleport not verified");
        // An area rejection must stop retrying rather than hammering the server.
        GlobalService.rejectArea=true;LoadMap.Area=0;
        for(int i=0;i<5;i++){mSystem.now+=6000;MenuUP.tick();}
        for(int i=0;i<100;i++){mSystem.now+=100;MenuUP.tick();movePlayer();}
        check(GlobalService.changes<=3 && !MenuUP.isGoBackActive && Player.isAutoFire==1,"area fallback did not resume combat");
    }
    static void snapshotsAndTimeouts() {
        newRun("snapshot",-1,"");setMap(134,"new high map",new String[0]);
        LoadMap.Area=4;MenuUP.tick();
        check(MenuUP.farmMapId==134 && MenuUP.farmArea==4,"automatic snapshot of Lv134 map");
        GameScreen.player.x=132;mSystem.now+=100;MenuUP.tick();
        check(MenuUP.farmX==108,"combat movement overwrote saved minimap tile");
        int saves=MenuUP.saves;mSystem.now+=6000;MenuUP.tick();
        check(MenuUP.saves>=saves && saves>0,"fixed snapshot not persisted");
        GameScreen.player.y=85;MenuUP.saveCurrentFarmLocation();
        check(MenuUP.farmX==132 && MenuUP.farmY==84,"manual minimap tile 5:3 must snap to cell center");
        check(GoBackPosition.label(MenuUP.farmX,MenuUP.farmY).equals("5:3"),"display disagrees with minimap");
        GameScreen.player.x=156;GameScreen.player.y=132;mSystem.now+=6000;MenuUP.tick();
        check(MenuUP.farmX==132 && MenuUP.farmY==84,"explicit position overwritten by combat");
        setMap(1,"village",new String[0]);MenuUP.tick();
        check(MenuUP.farmMapId==134 && MenuUP.farmX==132 && MenuUP.farmArea==4,"village overwrote snapshot");
        newRun("loading",134,"reef");setMap(1,"village",new String[]{"reef"});
        LoadMapScreen.isNextMap=false;MenuUP.tick();
        check(!MenuUP.isGoBackActive && GameScreen.player.posTransRoad==null,"movement during loading");
        LoadMapScreen.isNextMap=true;
        for(int i=0;i<300;i++){mSystem.now+=100;MenuUP.tick();movePlayer();}
        check(GoBackNavigator.status.indexOf("Chua co lo trinh")>=0,"portal timeout never blocked");
        int village=GoBackNavigator.graph.byId(1);
        GoBackRoutes.Exit e=(GoBackRoutes.Exit)GoBackNavigator.graph.get(village).exits.elementAt(0);
        check(e.blocked && !e.confirmed,"failed crossing learned as successful");
        GoBackNavigator.disconnected();setMap(200,"unrelated spawn",new String[0]);MenuUP.tick();
        check(!e.confirmed,"reconnect mistaken for portal success");
    }
    static void nativeReturn() {
        nativeMovement=true;
        for(int speed=4;speed<=10;speed+=6) {
            newRun("native-"+speed,134,"reef");setMap(134,"reef",new String[0]);
            GameScreen.player.vMax=speed;GameScreen.player.x=43;GameScreen.player.y=43;
            // Reconnect onto the target map in the wrong area must keep the snapshot.
            GoBackNavigator.disconnected();MenuUP.tick();
            check(MenuUP.farmX==84 && MenuUP.farmY==84 && MenuUP.farmArea==3,"same-map spawn overwrote farm");
            boolean done=false;
            for(int i=0;i<300;i++) {
                mSystem.now+=100;MenuUP.tick();movePlayer();
                if(!MenuUP.isGoBackActive) {done=true;break;}
            }
            check(done && LoadMap.Area==3,"native/mounted return failed");
            check(Math.abs(GameScreen.player.x-84)<=Math.max(6,speed)
                && Math.abs(GameScreen.player.y-84)<=Math.max(6,speed),"native arrival tolerance");
            // Death without a map-loading screen must also protect the saved point.
            GameScreen.player.hp=0;MenuUP.tick();
            GameScreen.player.hp=100;GameScreen.player.x=43;GameScreen.player.y=43;MenuUP.tick();
            check(MenuUP.farmX==84 && MenuUP.farmY==84 && MenuUP.isGoBackActive,"same-map revive lost target");
            GoBackNavigator.stop();
        }
        nativeMovement=false;GameScreen.player.vMax=4;

        newRun("direct-with-stone",13,"Rặng Đá Ngầm");
        GoBackNavigator.prioritizeStone = false;
        setMap(1,"Làng Sói Trắng",new String[]{"Bìa Rừng","Cánh Đồng Sói"});
        MainObject stone=new MainObject();stone.typeObject=2;stone.ID=9;
        stone.name="Da dich chuyen";stone.x=36;stone.y=36;GameScreen.Vecplayers.addElement(stone);
        int touched=-1;
        for(int i=0;i<300 && touched<0;i++) {mSystem.now+=100;MenuUP.tick();movePlayer();touched=exitTouched();}
        check(touched==1 && GlobalService.requests==0,"known route detoured to inspect stone");
        GoBackNavigator.stop();

        newRun("prioritize-stone",13,"Rặng Đá Ngầm");
        GoBackNavigator.prioritizeStone = true;
        setMap(1,"Làng Sói Trắng",new String[]{"Bìa Rừng","Cánh Đồng Sói"});
        MainObject stone2=new MainObject();stone2.typeObject=2;stone2.ID=9;
        stone2.name="Da dich chuyen";stone2.x=36;stone2.y=36;GameScreen.Vecplayers.addElement(stone2);
        boolean stoneUsed=false;
        for(int i=0;i<300;i++) {mSystem.now+=100;MenuUP.tick();movePlayer();if(GlobalService.requests>0){stoneUsed=true;break;}}
        check(stoneUsed,"prioritized teleport stone when multi-hop route exists");
        GoBackNavigator.stop();
    }
    static void strictMounts() {
        Item.VecInvetoryPlayer.removeAllElements();
        Item other=new Item();other.Id=63;other.itemName="Ngua Trang";Item.VecInvetoryPlayer.addElement(other);
        check(MenuUP.findMountItem(1)==null,"missing brown horse substituted another mount");
        check(MenuUP.findMountItem(2)==other,"selected white horse not found");
        for(int i=3;i<=5;i++) check(MenuUP.findMountItem(i)==null,"specific mount fell back");
        MenuUP.autoMountCustomId=999;
        check(MenuUP.findMountItem(7)==null,"missing custom mount substituted");
        check(MenuUP.findMountItem(0)==null && MenuUP.findMountItem(6)==other,"off / explicit any mount");
        Item chosen=new Item();chosen.Id=999;chosen.itemName="Custom";Item.VecInvetoryPlayer.addElement(chosen);
        check(MenuUP.findMountItem(7)==chosen,"custom selection not exact");
    }
    static void profiles() throws Exception {
        newRun("profile-server",134,"reef");setMap(134,"reef",new String[0]);
        GameScreen.player.name="Alpha";MenuUP.farmArea=0;
        GoBackProfile.bind();GoBackProfile.save();
        check(MenuUP.farmAreaLabel().equals("1"),"area 0 displayed instead of area 1");
        String owner="profile-server:0:Alpha";
        byte[] saved=Model.CRes.loadRMS("hso_gb_target_v1_"+Integer.toHexString(owner.hashCode()));
        java.io.DataInputStream in=new java.io.DataInputStream(new java.io.ByteArrayInputStream(saved));
        check(in.readInt()==2,"tile profile version");check(in.readUTF().equals(owner),"profile owner missing");
        in.readBoolean();in.readInt();in.readUTF();
        check(in.readInt()==GoBackPosition.tile(MenuUP.farmX) && in.readInt()==GoBackPosition.tile(MenuUP.farmY),"profile must persist minimap tiles, not pixels");
        check(in.readInt()==1,"persisted human area is not one based");
        GameScreen.player.name="Beta";GoBackProfile.bind();
        check(MenuUP.farmMapId==-1,"farm leaked to other character");
        MenuUP.farmMapId=400;MenuUP.farmArea=5;MenuUP.farmMapName="other";GoBackProfile.save();
        GameScreen.player.name="Alpha";GoBackProfile.bind();
        check(MenuUP.farmMapId==134 && MenuUP.farmArea==0,"profile target/packet area not restored");
        LoginScreen.ip="different-server";GoBackProfile.bind();
        check(MenuUP.farmMapId==-1,"farm leaked to other server");
        GameScreen.player.name="";
    }
    static void portalRegressions() {
        GoBackRoutes g=new GoBackRoutes();GoBackAtlas.seed(g);
        check(g.byName("chuan bi")==-1,"preparation maps merged by name");
        check(g.byId(53)!=g.byId(55) && g.byId(57)!=g.byId(59),"preparation IDs merged");
        check(g.plan(g.byName("lang anh sang"),g.byName("lang lua"))==null,"invented path between elemental villages");
        int light=g.byName("lang anh sang");
        g.scan(light,new String[]{"chuan bi"},new int[]{12},new int[]{108});
        check(g.plan(light,g.byId(53))!=null && g.plan(light,g.byId(55))==null,"duplicate caption lost source-specific atlas destination");
        int a=g.node("blank origin",400),b=g.node("blank target",401);
        g.scan(a,new String[]{""},new int[]{12},new int[]{108});
        check(g.plan(a,b)==null,"unnamed portal guessed a target");
        GoBackRoutes.Exit e=(GoBackRoutes.Exit)g.get(a).exits.elementAt(0);
        g.confirm(e,b);g.scan(a,new String[]{""},new int[]{12},new int[]{108});
        check(g.plan(a,b).first==e && g.plan(b,a)==null,"verified unnamed / one-way portal lost");

        newRun("passive-blank",-1,"");setMap(400,"blank origin",new String[]{""});
        GameScreen.player.x=12;GameScreen.player.y=108;GoBackNavigator.observe();
        mSystem.now+=100;setMap(401,"blank target",new String[0]);GoBackNavigator.observe();
        g=GoBackNavigator.graph;
        check(g.plan(g.byId(400),g.byId(401)).first.confirmed,"manual blank crossing not learned");

        newRun("portal-retry",134,"reef");setMap(1,"village",new String[]{"reef"});
        GlobalService.moves=0;
        int maxDistance=0;boolean done=false;
        for(int i=0;i<600;i++) {
            mSystem.now+=100;MenuUP.tick();movePlayer();
            if(GlobalService.moves==1) maxDistance=Math.max(maxDistance,MainObject.getDistance(GameScreen.player.x,GameScreen.player.y,12,108));
            if(GlobalService.moves==2) {done=true;break;}
        }
        check(done && maxDistance>=48,"portal timeout did not retreat and re-enter exactly once");
        for(int i=0;i<150;i++){mSystem.now+=100;MenuUP.tick();movePlayer();}
        check(GlobalService.moves==2,"unbounded portal retries");

        newRun("solid-portal",134,"reef");setMap(1,"village",new String[]{"reef"});
        GameCanvas.loadmap.mapType[40]=1;GlobalService.moves=0;
        for(int i=0;i<150 && GlobalService.moves==0;i++){mSystem.now+=100;MenuUP.tick();movePlayer();}
        check(GlobalService.moves==1 && MainObject.getDistance(GameScreen.player.x,GameScreen.player.y,12,108)<=36,"solid portal marker treated as unreachable");

        newRun("avoid-blank",134,"reef");setMap(1,"village",new String[]{"","reef"});
        Point blank=(Point)LoadMap.vecPointChange.elementAt(0);blank.x=120;blank.y=60;
        Point target=(Point)LoadMap.vecPointChange.elementAt(1);target.y=60;
        GameScreen.player.x=36;GameScreen.player.y=60;GlobalService.moves=0;
        for(int i=0;i<250 && GlobalService.moves==0;i++) {
            mSystem.now+=100;MenuUP.tick();movePlayer();
            check(MainObject.getDistance(GameScreen.player.x,GameScreen.player.y,120,60)>36,"crossed unnamed wrong portal");
        }
        check(GlobalService.moves==1,"detour around unnamed portal failed");
        GoBackNavigator.stop();
    }
    static void fiveVillages() {
        int[] ids={1,54,56,58,60};
        String[] names={"Làng Sói Trắng","Làng ánh sáng","Làng gió","Làng sét","Làng lửa"};
        for(int village=0;village<ids.length;village++) {
            // Server fixtures explicitly provide each village's outgoing portal;
            // these are regression inputs, not claims about live server topology.
            newRun("five-"+village,134,"reef");
            setMap(400,"passage",new String[]{"reef"});GoBackNavigator.observe();
            setMap(ids[village],names[village],new String[]{"passage"});GameScreen.isMapLang=true;
            boolean done=false;int transitions=0;
            for(int i=0;i<500;i++) {
                mSystem.now+=100;MenuUP.tick();movePlayer();
                if(exitTouched()==0) {
                    if(GameCanvas.loadmap.idMap==ids[village]) {setMap(400,"passage",new String[]{"reef"});transitions++;}
                    else if(GameCanvas.loadmap.idMap==400) {setMap(134,"reef",new String[0]);transitions++;}
                }
                if(GameCanvas.loadmap.idMap==134 && !MenuUP.isGoBackActive) {done=true;break;}
            }
            check(done && transitions==2 && LoadMap.Area==3,"return from village "+names[village]);
        }
    }
    static void largeMapAndRecovery() {
        newRun("large-runtime",134,"reef");setMap(134,"reef",new String[0]);
        GameCanvas.loadmap.mapW=200;GameCanvas.loadmap.mapH=10;GameCanvas.loadmap.mapType=new int[2000];
        MenuUP.farmX=195*24+12;MenuUP.farmY=84;MenuUP.farmArea=0;
        nativeMovement=true;GameScreen.player.vMax=4;
        boolean done=false;
        for(int i=0;i<2000;i++) {
            mSystem.now+=50;MenuUP.tick();
            if(i>75) movePlayer(); // force the three-second watchdog once
            if(GameScreen.player.posTransRoad!=null) {
                check(GameScreen.player.posTransRoad.length<=14,"oversized native road");
                check(MainObject.getDistance(GameScreen.player.x,GameScreen.player.y,GameScreen.player.toX,GameScreen.player.toY)<=300,"waypoint exceeded twelve tiles");
            }
            if(!MenuUP.isGoBackActive) {done=true;break;}
        }
        check(done && Math.abs(GameScreen.player.x-MenuUP.farmX)<=6,"large-map / watchdog recovery failed");
        nativeMovement=false;
    }
    static void deathReturn() {
        newRun("death-return",134,"reef");setMap(134,"reef",new String[0]);
        GameScreen.player.hp=0;MenuUP.autoReviveMode=0;
        MenuUP.reviveState=0;MenuUP.isReviveCancelled=false;GlobalService.homes=0;
        MenuUP.tick();MenuUP.tickRevive();
        check(GoBackNavigator.needsReturn() && GameScreen.player.posTransRoad==null,"death left stale movement");
        mSystem.now+=3001;MenuUP.tickRevive();
        check(GlobalService.homes==1 && GlobalService.homeMode==0,"GoBack did not request village revival");
        for(int i=0;i<20;i++){mSystem.now+=100;MenuUP.tickRevive();}
        check(GlobalService.homes==1,"revival request spam");
        MenuUP.reviveState=0;MenuUP.isReviveCancelled=true;MenuUP.tickRevive();mSystem.now+=4000;MenuUP.tickRevive();
        check(GlobalService.homes==1,"revival ignored cancellation");
        MenuUP.isReviveCancelled=false;MenuUP.isGoBackPaused=true;MenuUP.tickRevive();
        check(MenuUP.reviveState==0,"paused GoBack revived anyway");
    }
    public static void main(String[] args) throws Exception {
        paths();routes();runtime();atlas();teleport();snapshotsAndTimeouts();
        nativeReturn();
        strictMounts();portalRegressions();fiveVillages();largeMapAndRecovery();profiles();
        deathReturn();
        check(MenuUP.stripAccents("  Rặng  Đá Ngầm ").equals("rang da ngam"),"name normalization");
        check(MenuUP.getMapIdByName("Fire extra")==-1,"substring destination matching");
        System.out.println("PASS: "+assertions+" checks; graph, persistence, village regression, obstacles, Lv134, teleport, area retry");
    }
}
