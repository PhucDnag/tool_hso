package GameObjects;

import CLib.mSystem;
import CLib.mVector;
import GameScreen.GameScreen;
import GameScreen.LoadMapScreen;
import GameScreen.LoginScreen;
import InterfaceComponents.iCommand;
import Main.GameCanvas;
import Model.CRes;
import Model.Point;
import Thread_More.LoadMap;
import netcommand.global.GlobalService;

/** Runtime adapter: observe -> plan -> walk/interact -> await server -> verify. */
public final class GoBackNavigator {
    public static final int STATE_IDLE = 0;
    public static final int STATE_AWAIT_REVIVE = 1;
    public static final int STATE_WAIT_MAP_LOAD = 2;
    public static final int STATE_PLAN_ROUTE = 3;
    public static final int STATE_WALK_TO_PORTAL = 4;
    public static final int STATE_TRIGGER_PORTAL = 5;
    public static final int STATE_CHECK_ARRIVAL = 6;
    public static int fsmState = STATE_IDLE;

    public static GoBackRoutes graph = new GoBackRoutes();
    public static String status = "";
    private static String store = "";
    private static int current=-1, seenMap=-1, seenArea=-1, savedRevision=-1;
    private static int[] seenTiles;
    private static boolean readyLastTick;
    private static boolean returnRequired;
    public static boolean needsReturn() { return returnRequired; }
    private static long lastSave, lastMetadata, nextPlan, waitUntil, lastProgress;
    private static GoBackRoutes.Exit selected, armed;
    private static GoBackRoutes.Plan plan;
    private static int armedFrom=-1, progressEpoch, transitions;
    private static java.util.Hashtable traversed = new java.util.Hashtable();
    private static java.util.Hashtable visitedMaps = new java.util.Hashtable();
    private static int[] road;
    private static int roadIndex, roadWidth, endX, endY, lastX, lastY;
    private static boolean waypointIssued;
    private static int waypointX, waypointY;
    private static boolean[] blockedWalkCells;
    private static long nextObstaclePlan;
    private static int areaAttempts;
    private static long nextAreaAttempt;
    private static boolean halted;
    private static boolean retreating;
    private static int portalAttempts, walkRetries;
    private static GoBackRoutes.Exit walkExit;
    private static long armedAt;
    private static int totalHops;
    private static long seenMapTime;
    private static int probeNpc=-1, menuNpc=-1, menuId, menuMap=-1;
    private static mVector menuOptions;
    private static long menuTime, requestedMenu;
    private static int menuRequests;
    private static MainObject requestedStone;
    private static int openedCategory;
    public static boolean prioritizeStone = true;
    private static java.util.Hashtable probed = new java.util.Hashtable();

    public static boolean ready() {
        return GameCanvas.currentScreen==GameCanvas.game && mapDataReady();
    }
    private static boolean mapDataReady() {
        return GameCanvas.loadmap!=null
            && GameScreen.player!=null && LoadMapScreen.isNextMap
            && GameCanvas.loadmap.mapType!=null && GameCanvas.loadmap.mapW>0
            && GameCanvas.loadmap.mapH>0
            && GameCanvas.loadmap.mapType.length>=GameCanvas.loadmap.mapW*GameCanvas.loadmap.mapH;
    }
    // Image persistence is background work, not a prerequisite for navigation.
    // LoadMapScreen still checks the initial skill/RMS handshake separately.
    public static boolean canResumeMap() {
        return MenuUP.isGoBackEnabled && !MenuUP.isGoBackPaused && MenuUP.farmMapId>=0
            && mapDataReady() && GameScreen.player.hp>0 && GameScreen.player.Action!=4;
    }
    public static String key(String s) { return MenuUP.stripAccents(s); }
    public static String currentName() {
        String name=GameCanvas.loadmap.nameMap;
        return name!=null && name.length()>0 ? name : MenuUP.getMapName(GameCanvas.loadmap.idMap);
    }
    private static String name(int node) {
        GoBackRoutes.Node n=graph.get(node);
        return n.mapId>=0 ? MenuUP.getMapName(n.mapId) : n.key;
    }
    public static void observe() {
        if(!ready()) {
            readyLastTick=false;
            if (MenuUP.isGoBackActive || returnRequired) fsmState = STATE_WAIT_MAP_LOAD;
            return;
        }
        Player p=GameScreen.player;
        if(p.hp<=0 || p.Action==4) {
            if(fsmState!=STATE_AWAIT_REVIVE) start();
            armed=null; returnRequired=true; selected=null; waitUntil=0L;
            halted=false; stopMovement();
            fsmState = STATE_AWAIT_REVIVE;
            return;
        }
        String rms="hso_gb_routes_v5_"+Integer.toHexString((LoginScreen.ip+":"+GameCanvas.IndexServer).hashCode());
        if(!rms.equals(store)) {
            if(savedRevision!=graph.revision) saveGraph();
            store=rms;
            try { graph=GoBackRoutes.load(CRes.loadRMS(store)); }
            catch(Exception ex) { graph=new GoBackRoutes(); }
            GoBackAtlas.seed(graph);
            armed=null; selected=null; plan=null; readyLastTick=false;
            seenMap=-1; current=-1; savedRevision=graph.revision;
            visitedMaps.clear();
        }
        int id=GameCanvas.loadmap.idMap;
        boolean entered=!readyLastTick || seenMap!=id || seenArea!=LoadMap.Area || seenTiles!=GameCanvas.loadmap.mapType;
        readyLastTick=true;
        if(entered) {
            blockedWalkCells=null;
            seenMapTime=mSystem.currentTimeMillis();
            returnRequired=MenuUP.farmMapId>=0;
            int previous=current;
            current=graph.node(key(currentName()),id);
            boolean newMap=!graph.get(current).scanned;
            if(newMap) progressEpoch++;
            if(armed!=null && armedFrom==previous && seenMap!=id
                    && (armed.npc==-1 || armed.to==current)
                    && mSystem.currentTimeMillis()-armedAt<=(armed.npc==-1?10000L:90000L)) {
                graph.confirm(armed,current);
                String visit=armed.from+":"+armed.x+":"+armed.y+":"+armed.npc+":"+progressEpoch;
                Integer count=(Integer)traversed.get(visit);
                int visits=count==null?1:count.intValue()+1;
                traversed.put(visit,new Integer(visits));
                if(visits>=2) graph.block(armed);
                transitions++;
            }
            // A death/recall/login is not evidence of a portal edge.
            armed=null; selected=null; plan=null; probeNpc=-1; menuOptions=null;
            requestedMenu=0L; menuRequests=0; requestedStone=null; halted=false;
            waitUntil=0L; nextPlan=0L;
            portalAttempts=0; retreating=false;
            if(road!=null || MenuUP.isGoBackActive) stopMovement();
            seenMap=id; seenArea=LoadMap.Area; seenTiles=GameCanvas.loadmap.mapType;
            lastMetadata=0L;
            int size=LoadMap.vecPointChange.size();
            String[] names=new String[size]; int[] xs=new int[size],ys=new int[size];
            for(int i=0;i<size;i++) {
                Point exit=(Point)LoadMap.vecPointChange.elementAt(i);
                names[i]=key(exit.name); xs[i]=exit.x;
                // ReadMessenge offsets the TOP arrow by -10 only for drawing.
                ys[i]=exit.y+(exit.dis==2?10:0);
            }
            graph.scan(current,names,xs,ys);
            graph.get(current).village=GameScreen.isMapLang;
        }
        long now=mSystem.currentTimeMillis();
        // Learn manual crossings too, but only from a recent approach to exactly
        // one physical portal. Loading preserves the candidate; death/login clears it.
        if(!MenuUP.isGoBackActive) {
            GoBackRoutes.Exit candidate=null;
            GoBackRoutes.Node here=graph.get(current);
            for(int i=0;i<here.exits.size();i++) {
                GoBackRoutes.Exit e=(GoBackRoutes.Exit)here.exits.elementAt(i);
                if(e.npc!=-1 || e.x<0 || MainObject.getDistance(p.x,p.y,e.x,e.y)>36) continue;
                if(candidate!=null) { candidate=null; break; }
                candidate=e;
            }
            armed=candidate; armedFrom=current; armedAt=now;
        }
        if(now-lastMetadata>2000L) {
            lastMetadata=now;
            GoBackRoutes.Node node=graph.get(current);
            for(int i=0;i<GameScreen.Vecplayers.size();i++) {
                MainObject obj=(MainObject)GameScreen.Vecplayers.elementAt(i);
                if(obj==null || obj.typeObject!=1 || obj.Lv<=0) continue;
                int low=node.minLevel==0?obj.Lv:Math.min(node.minLevel,obj.Lv);
                int high=Math.max(node.maxLevel,obj.Lv);
                if(low!=node.minLevel || high!=node.maxLevel) {
                    node.minLevel=low; node.maxLevel=high; graph.revision++;
                }
            }
        }
        // Do not serialize/write the whole graph on the frame that starts the
        // next leg. stop() persists it at arrival or cancellation.
        if(!MenuUP.isGoBackActive && !(returnRequired && MenuUP.isGoBackEnabled && !MenuUP.isGoBackPaused)
                && now-lastSave>5000L && savedRevision!=graph.revision) saveGraph();
    }
    private static void saveGraph() {
        if(store.length()==0) return;
        try { CRes.saveRMS(store,graph.save()); savedRevision=graph.revision; lastSave=mSystem.currentTimeMillis(); }
        catch(Exception ex) { /* Keep the in-memory graph if RMS is full. */ }
    }
    public static void start() {
        blockedWalkCells=null;
        fsmState = STATE_PLAN_ROUTE;
        graph.resetFailures(); traversed.clear(); probed.clear();
        visitedMaps.clear();
        selected=null; armed=null; plan=null; probeNpc=-1; menuOptions=null;
        requestedMenu=0L; menuRequests=0; requestedStone=null;
        transitions=0; areaAttempts=0; nextAreaAttempt=0L; halted=false; nextPlan=0L; waitUntil=0L;
        portalAttempts=0; retreating=false; totalHops=0;
        stopMovement();
    }
    public static void stop() {
        fsmState = STATE_IDLE;
        returnRequired=false;
        visitedMaps.clear();
        selected=null; armed=null; plan=null; probeNpc=-1; menuOptions=null;
        requestedMenu=0L; menuRequests=0; requestedStone=null;
        waitUntil=0L; nextPlan=0L;
        stopMovement(); saveGraph();
    }
    public static void disconnected() {
        returnRequired=true;
        // A subsequent spawn is not a successful use of the last portal/stone.
        start();
        readyLastTick=false;
    }
    public static void stopMovement() {
        road=null; roadIndex=0;
        waypointIssued=false;
        Player p=GameScreen.player;
        if(p==null) return;
        p.posTransRoad=null; p.countAutoMove=0; p.toX=p.x; p.toY=p.y;
        p.xStopMove=0; p.yStopMove=0; p.vx=0; p.vy=0;
    }
    private static int[] collision(GoBackRoutes.Exit chosen) {
        int[] original=GameCanvas.loadmap.mapType;
        int[] tiles=new int[original.length]; System.arraycopy(original,0,tiles,0,tiles.length);
        if(blockedWalkCells!=null && blockedWalkCells.length==tiles.length) {
            for(int i=0;i<tiles.length;i++) if(blockedWalkCells[i]) tiles[i]=1;
        }
        int w=GameCanvas.loadmap.mapW,t=LoadMap.wTile;
        int start=GameScreen.player.y/t*w+GameScreen.player.x/t;
        for(int i=0;i<LoadMap.vecPointChange.size();i++) {
            Point e=(Point)LoadMap.vecPointChange.elementAt(i);
            int ex=e.x,ey=e.y+(e.dis==2?10:0);
            if(chosen!=null && chosen.npc==-1 && chosen.x==ex && chosen.y==ey) continue;
            // Include unnamed exits and their activation neighborhood. A spawn
            // already inside a portal must be allowed to walk out of that region.
            int spawnDistance=MainObject.getDistance(GameScreen.player.x,GameScreen.player.y,ex,ey);
            for(int yy=Math.max(0,(ey-36)/t);yy<=Math.min(GameCanvas.loadmap.mapH-1,(ey+36)/t);yy++)
                for(int xx=Math.max(0,(ex-36)/t);xx<=Math.min(w-1,(ex+36)/t);xx++) {
                    int cell=yy*w+xx;
                    if(spawnDistance<=48 && MainObject.getDistance(xx*t+t/2,yy*t+t/2,ex,ey)>=spawnDistance) continue;
                    if(cell!=start) tiles[cell]=1;
                }
        }
        return tiles;
    }
    private static int[] path(int x,int y,int radius,GoBackRoutes.Exit exit) {
        int t=LoadMap.wTile;
        if(x<0 || y<0 || t<=0) return null;
        int[] tiles=collision(exit);
        int[] result=GoBackPathfinder.findCells(tiles,GameCanvas.loadmap.mapW,GameCanvas.loadmap.mapH,
                GameScreen.player.x/t,GameScreen.player.y/t,x/t,y/t,radius);
        // Some server portal markers lie on a solid border tile. Approach a
        // reachable neighboring cell, never mark the solid tile as walkable.
        if(result==null && exit!=null && exit.npc==-1) {
            int tx=Math.min(GameCanvas.loadmap.mapW-1,x/t),ty=Math.min(GameCanvas.loadmap.mapH-1,y/t);
            result=GoBackPathfinder.findCells(tiles,GameCanvas.loadmap.mapW,GameCanvas.loadmap.mapH,
                GameScreen.player.x/t,GameScreen.player.y/t,tx,ty,1);
            if(result!=null) {
                int last=result[result.length-1],w=GameCanvas.loadmap.mapW;
                if(MainObject.getDistance(last%w*t+t/2,last/w*t+t/2,x,y)>36) result=null;
            }
        }
        return result;
    }
    private static boolean startWalk(int x,int y,int radius,GoBackRoutes.Exit exit) {
        stopMovement();
        road=path(x,y,radius,exit);
        if(road==null) return false;
        walkExit=exit; walkRetries=0;
        nextObstaclePlan=0L;
        roadWidth=GameCanvas.loadmap.mapW; roadIndex=0;
        int last=road[road.length-1],t=LoadMap.wTile;
        boolean exact=radius==0 && last==y/t*roadWidth+x/t;
        endX=exact?x:last%roadWidth*t+t/2;
        endY=exact?y:last/roadWidth*t+t/2;
        lastX=GameScreen.player.x; lastY=GameScreen.player.y; lastProgress=mSystem.currentTimeMillis();
        return true;
    }
    // Return -1 on stall, 0 while moving, 1 on arrival. No coordinate teleport.
    private static boolean near(int x,int y) {
        Player p=GameScreen.player;
        int tolerance=Math.max(6,p.vMax+p.getVmount());
        return Math.abs(p.x-x)<=tolerance && Math.abs(p.y-y)<=tolerance;
    }
    private static int walk() {
        Player p=GameScreen.player; long now=mSystem.currentTimeMillis();
        if(road==null) return -1;
        if(MainObject.getDistance(p.x,p.y,lastX,lastY)>6) {
            lastX=p.x; lastY=p.y; lastProgress=now; walkRetries=0;
            int occupied=p.y/LoadMap.wTile*GameCanvas.loadmap.mapW+p.x/LoadMap.wTile;
            if(blockedWalkCells!=null && occupied>=0 && occupied<blockedWalkCells.length) blockedWalkCells[occupied]=false;
        }
        if(now-lastProgress>(walkRetries==0?3000L:1000L)) {
            if(++walkRetries>3) { stopMovement(); return -1; }
            lastProgress=now;
            // A short stall is not evidence that the portal/route is wrong.
            // First retry the same waypoint; only sustained stalls re-solve
            // collision toward the same endpoint, never an arbitrary side step.
            if(walkRetries>1) recoverWalk(true);
        }
        // Newly loaded NPC blocks may invalidate a segment after it was planned.
        // Re-solve immediately rather than pushing into a known solid tile.
        if(waypointIssued && !segmentClear(p.x,p.y,waypointX,waypointY)) {
            if(now<nextObstaclePlan || !recoverWalk(false)) {
                if(now>=nextObstaclePlan) nextObstaclePlan=now+500L;
                p.posTransRoad=null; p.vx=0; p.vy=0;
                return 0;
            }
        }
        // Collision/server correction can clear the native road or reset toX/Y.
        // Its disappearance does not mean the waypoint was reached.
        if(waypointIssued && !near(waypointX,waypointY)) {
            if(p.posTransRoad==null || p.toX!=waypointX || p.toY!=waypointY) issueWaypoint(waypointX,waypointY);
            return 0;
        }
        waypointIssued=false;
        if(roadIndex>road.length) {
            if(near(endX,endY)) { stopMovement(); return 1; }
            stopMovement(); return -1;
        }
        int t=LoadMap.wTile;
        // Coalesce only straight runs, at most twelve tiles. Corners remain
        // explicit; the native packed-short road never grows with map size.
        if(roadIndex==0 && road.length>1
                && p.x==road[0]%roadWidth*t+t/2 && p.y==road[0]/roadWidth*t+t/2) roadIndex=1;
        if(roadIndex>0 && roadIndex<road.length-1) {
            int first=roadIndex-1,step=road[roadIndex]-road[first];
            while(roadIndex<road.length-1 && roadIndex-first<12
                    && road[roadIndex+1]-road[roadIndex]==step) roadIndex++;
        }
        int x=roadIndex==road.length?endX:road[roadIndex]%roadWidth*t+t/2;
        int y=roadIndex==road.length?endY:road[roadIndex]/roadWidth*t+t/2;
        roadIndex++;
        // Keep a road active while moving, with its cursor already exhausted. Player
        // clears it on arrival without decoding a signed-byte tile coordinate.
        issueWaypoint(x,y);
        return 0;
    }
    private static void issueWaypoint(int x,int y) {
        Player p=GameScreen.player;
        waypointX=x; waypointY=y; waypointIssued=true;
        p.toX=x; p.toY=y; p.xStopMove=x; p.yStopMove=y; p.countAutoMove=1;
        p.posTransRoad=new short[]{0};
    }
    private static boolean segmentClear(int x,int y,int tx,int ty) {
        int t=LoadMap.wTile,w=GameCanvas.loadmap.mapW,h=GameCanvas.loadmap.mapH;
        int sx=x/t,sy=y/t,ex=tx/t,ey=ty/t;
        int[] tiles=GameCanvas.loadmap.mapType;
        while(sx!=ex || sy!=ey) {
            if(sx!=ex) sx+=sx<ex?1:-1; else sy+=sy<ey?1:-1;
            if(sx<0 || sy<0 || sx>=w || sy>=h) return false;
            int at=sy*w+sx;
            if(tiles[at]==1 || tiles[at]==-1 || blockedWalkCells!=null && blockedWalkCells[at]) return false;
        }
        return true;
    }
    private static boolean recoverWalk(boolean rememberObstacle) {
        Player p=GameScreen.player;
        int t=LoadMap.wTile,w=GameCanvas.loadmap.mapW,h=GameCanvas.loadmap.mapH;
        if(rememberObstacle && waypointIssued) {
            int nx=p.x/t,ny=p.y/t;
            if(Math.abs(waypointX-p.x)>6 && waypointX/t!=nx) nx+=waypointX>p.x?1:-1;
            else if(Math.abs(waypointX-p.x)<=6 && Math.abs(waypointY-p.y)>6 && waypointY/t!=ny) ny+=waypointY>p.y?1:-1;
            if(nx>=0 && ny>=0 && nx<w && ny<h && (nx!=p.x/t || ny!=p.y/t)) {
                if(blockedWalkCells==null) blockedWalkCells=new boolean[GameCanvas.loadmap.mapType.length];
                blockedWalkCells[ny*w+nx]=true;
            }
        }
        int[] tiles=collision(walkExit);
        int[] recovery=GoBackPathfinder.findCells(tiles,w,h,p.x/t,p.y/t,endX/t,endY/t,0);
        if(recovery==null) return false;
        stopMovement(); road=recovery; roadWidth=w; roadIndex=0;
        lastX=p.x; lastY=p.y; lastProgress=mSystem.currentTimeMillis();
        return true;
    }
    public static void recoverOnError(String reason) {
        status="GoBack: "+reason+". Hay kiem tra lo trinh hoac bat lai GoBack.";
        halted=true;
        selected=null; armed=null; plan=null; probeNpc=-1;
        waitUntil=0L; requestedMenu=0L; menuOptions=null; requestedStone=null;
        stopMovement();
    }
    private static void failExit() {
        if(selected!=null) graph.block(selected);
        selected=null; armed=null; plan=null; waitUntil=0L; nextPlan=0L;
        menuOptions=null; requestedMenu=0L; menuRequests=0; requestedStone=null;
        probeNpc=-1;
        stopMovement();
    }
    private static void failStone() {
        if(selected!=null) {
            probed.put(current+":"+selected.npc,Boolean.TRUE);
            GoBackRoutes.Node node=graph.get(current);
            for(int i=0;i<node.exits.size();i++) {
                GoBackRoutes.Exit e=(GoBackRoutes.Exit)node.exits.elementAt(i);
                if(e.npc==selected.npc) graph.defer(e,mSystem.currentTimeMillis());
            }
        }
        failExit();
    }
    public static boolean stone(MainObject npc) {
        if(npc==null) return false;
        String n=key(npc.name);
        String gt=key(npc.nameGiaotiep);
        return n.indexOf("da dich chuyen")>=0 || n.indexOf("teleport")>=0 || n.indexOf("dich chuyen")>=0
            || gt.indexOf("da dich chuyen")>=0 || gt.indexOf("teleport")>=0 || gt.indexOf("dich chuyen")>=0;
    }
    public static MainObject findStoneNpc() {
        if (GameScreen.Vecplayers == null) return null;
        for (int i = 0; i < GameScreen.Vecplayers.size(); i++) {
            MainObject obj = (MainObject) GameScreen.Vecplayers.elementAt(i);
            if (obj!=null && obj.typeObject==2 && stone(obj)) return obj;
        }
        return null;
    }
    // NPC IDs are signed bytes on requests and shorts in dynamic-menu replies.
    private static boolean sameNpc(int a,int b) {
        return a==b || (a>=-128 && a<=255 && b>=-128 && b<=255 && (byte)a==(byte)b);
    }
    private static boolean ownsMenu(MainObject npc,int id) {
        return npc!=null && (sameNpc(npc.getIDnpc(),id) || sameNpc(npc.ID,id));
    }
    private static boolean stoneNear(MainObject npc) {
        return stone(npc) && MainObject.getDistance(GameScreen.player.x,GameScreen.player.y,npc.x,npc.y)<=48;
    }
    private static boolean approachStone(int id) {
        MainObject npc=MainObject.get_Object(id,(byte)2);
        if(!stone(npc)) npc=findStoneNpc();
        if(npc==null) { recoverOnError("Khong thay da dich chuyen tren map"); return false; }
        if(stoneNear(npc)) return true;
        requestedMenu=0L; menuOptions=null;
        if(road==null && !startWalk(npc.x,npc.y,1,selected)) {
            recoverOnError("Khong den gan duoc da dich chuyen");
        }
        status="Dang den gan da dich chuyen";
        return false;
    }
    private static boolean matchesMap(String caption,String map) {
        if(map.length()==0) return false;
        if(caption.equals(map)) return true;
        // Price/level suffixes are permitted; a longer map name is not a match.
        if(!caption.startsWith(map+" ")) return false;
        String suffix=caption.substring(map.length()+1);
        return suffix.length()>0 && (Character.isDigit(suffix.charAt(0))
            || suffix.startsWith("lv ") || suffix.startsWith("level ")
            || suffix.startsWith("phi ") || suffix.startsWith("gia "));
    }
    private static int menuDestination(String label) {
        int exact=graph.byName(label);
        if(exact>=0) return exact;
        int found=-1,length=0;
        for(int i=0;i<graph.nodes.size();i++) {
            String name=graph.get(i).key;
            if(matchesMap(label,name) && name.length()>length) {found=i;length=name.length();}
        }
        return found;
    }
    /** Return true when GoBack owns this response so it need not open a UI menu. */
    public static boolean onMenu(int npcId,int id,mVector options) {
        if(!ready() || current<0 || options==null) return false;
        boolean pending=requestedMenu>0L && ownsMenu(requestedStone,npcId);
        MainObject npc=pending?requestedStone:MainObject.get_Object(npcId,(byte)2);
        if(!stone(npc) || (requestedStone!=null && !pending)) return false;
        menuNpc=npcId; menuId=id; menuOptions=options; menuMap=current; menuTime=mSystem.currentTimeMillis();
        java.util.Hashtable offered=new java.util.Hashtable();
        int categories=0;
        for(int i=0;i<options.size();i++) {
            String label=key(((iCommand)options.elementAt(i)).caption);
            int to=menuDestination(label);
            if(to<0 && label.equals(key(MenuUP.farmMapName))) to=graph.node(label,MenuUP.farmMapId);
            if(to>=0 && to!=current) {
                graph.exit(current,to,npc.x,npc.y,npc.getIDnpc());
                offered.put(new Integer(to),Boolean.TRUE);
            }
            for(int cat=1;cat<=6;cat++) if(GoBackAtlas.matchesCategory(label,cat)) categories|=1<<cat;
        }
        if(pending) {
            GoBackRoutes.Node here=graph.get(current);
            for(int i=0;i<here.exits.size();i++) {
                GoBackRoutes.Exit e=(GoBackRoutes.Exit)here.exits.elementAt(i);
                if(e.npc!=npc.getIDnpc()) continue;
                int cat=GoBackAtlas.getCategoryForMap(graph.get(e.to).mapId);
                if(offered.size()>0 && (openedCategory==0 || cat==openedCategory)
                        && !offered.containsKey(new Integer(e.to))) graph.block(e);
                if(categories!=0 && cat>0 && (categories&(1<<cat))==0) graph.block(e);
            }
        }
        nextPlan=0L;
        return pending && MenuUP.isGoBackActive;
    }
    private static void requestStone(int id) {
        requestedStone=MainObject.get_Object(id,(byte)2);
        if(!stone(requestedStone)) requestedStone=findStoneNpc();
        if(!stoneNear(requestedStone)) return;
        menuRequests++; requestedMenu=mSystem.currentTimeMillis(); menuOptions=null; openedCategory=0;
        // Exactly the same request ID as the native NPC interaction. Calling
        // GiaoTiep as well sends duplicates and can replace a submenu with root.
        GlobalService.gI().player_move((short)GameScreen.player.x,(short)GameScreen.player.y);
        GlobalService.gI().getlist_from_npc((byte)requestedStone.getIDnpc());
        GameCanvas.menu2.isShowMenu=false;
    }
    private static void chooseMenu(int index,long now) {
        if(!stoneNear(requestedStone)) { requestedMenu=0L; menuOptions=null; armed=null; waitUntil=0L; return; }
        int npc=menuNpc,id=menuId;
        requestedMenu=now; menuOptions=null; menuRequests++;
        GlobalService.gI().Dynamic_Menu((short)npc,(byte)id,(byte)index);
        GameCanvas.menu2.isShowMenu=false;
    }
    private static boolean freshMenu() {
        return menuOptions!=null && menuMap==current && menuTime>=requestedMenu;
    }
    private static boolean nearbyLanding(int goalMap,long now) {
        int goal=graph.byId(goalMap),best=-1,hops=2147483647;
        if(goal<0) return false;
        GoBackRoutes.Exit landing=null;
        int cap=GoBackAtlas.getCategoryForMap(goalMap);
        int origin=GoBackAtlas.getCategoryForMap(GameCanvas.loadmap.idMap);
        if(origin>=1 && origin<=5) cap=Math.max(cap,origin);
        for(int i=0;i<menuOptions.size();i++) {
            int to=menuDestination(key(((iCommand)menuOptions.elementAt(i)).caption));
            if(to<0 || to==current || visitedMaps.containsKey(new Integer(to))) continue;
            int cat=GoBackAtlas.getCategoryForMap(graph.get(to).mapId);
            if(cap>=1 && cap<=5 && cat>cap && cat<=5) continue;
            GoBackRoutes.Exit e=graph.exit(current,to,requestedStone.x,requestedStone.y,requestedStone.getIDnpc());
            if(e.blocked) continue;
            GoBackRoutes.Plan rest=graph.planWalking(to,goal,visitedMaps);
            if(to!=goal && rest==null) continue;
            int cost=to==goal?0:rest.hops;
            if(cost<hops) {hops=cost;best=i;landing=e;}
        }
        if(best<0) return false;
        GoBackRoutes.Plan walking=graph.planWalking(current,goal,visitedMaps);
        if(walking!=null && walking.hops<hops+1) return false;
        selected=landing;plan=new GoBackRoutes.Plan(landing,hops+1);
        armed=selected;armedFrom=current;armedAt=now;waitUntil=now+30000L;
        status="Dich chuyen den "+name(landing.to)+", sau do di bo toi diem UP";
        chooseMenu(best,now);
        return true;
    }
    private static boolean inspectStoneRoutes(long now) {
        if(probeNpc==-1) {
            MainObject sn = findStoneNpc();
            if(sn != null) {
                String id=current+":"+sn.getIDnpc();
                if(!probed.containsKey(id)) {
                    probed.put(id,Boolean.TRUE); probeNpc=sn.getIDnpc();
                    if(!startWalk(sn.x,sn.y,1,null)) { probeNpc=-1; return false; }
                    requestedMenu=0L; menuRequests=0; requestedStone=null;
                }
            }
        }
        if(probeNpc==-1) return false;
        status="Dang doc lo trinh da dich chuyen";
        if(requestedMenu==0L) {
            int state=walk();
            if(state<0) { recoverOnError("Bi ket tren duong toi da dich chuyen"); return true; }
            if(state==1 && approachStone(probeNpc)) requestStone(probeNpc);
            return true;
        }
        if(freshMenu()) {
            for(int i=0;i<menuOptions.size();i++) {
                String label=key(((iCommand)menuOptions.elementAt(i)).caption);
                if(label.equals("dich chuyen") || label.equals("teleport")) {
                    if(menuRequests>=6) { probeNpc=-1; return false; }
                    chooseMenu(i,now);
                    return true;
                }
            }
            probeNpc=-1; GameCanvas.menu2.isShowMenu=false; return false;
        }
        if(now-requestedMenu>15000L) { probeNpc=-1; return false; }
        return true;
    }
    public static boolean update(int mapId,int x,int y,int area) {
        if(!ready() || current<0) return false;
        Player p=GameScreen.player; long now=mSystem.currentTimeMillis();
        if(p.hp<=0 || p.Action==4) { armed=null; stopMovement(); return false; }
        if(halted) return false;
        visitedMaps.put(new Integer(current),Boolean.TRUE);
        if(transitions>256) {
            recoverOnError("Vuot qua 256 chang");
            stopMovement();
            return false;
        }
        if(GameCanvas.loadmap.idMap==mapId) {
            fsmState = STATE_CHECK_ARRIVAL;
            selected=null; armed=null;
            if(area>=0 && LoadMap.Area!=area) {
                stopMovement(); status="Dang doi ve khu "+(area+1);
                if(now>=nextAreaAttempt) {
                    if(areaAttempts>=3) {
                        halted=true;
                        status="Chua vao duoc khu da luu "+(area+1)+"; da dung, khong UP sai khu.";
                    } else {
                        areaAttempts++; nextAreaAttempt=now+5000L;
                        GlobalService.gI().Change_Area((byte)area);
                    }
                }
                return false;
            }
            if(x<0 || y<0) { fsmState = STATE_IDLE; return true; } // Map-only selection has no invented farm coordinates.
            if(near(x,y)) { stopMovement(); fsmState = STATE_IDLE; return true; }
            status="Ve toa do UP ("+x+","+y+")";
            if(road==null && !startWalk(x,y,0,null)) { halted=true; status="Toa do UP bi chan; hay luu lai vi tri hop le."; return false; }
            if(walk()<0) { halted=true; status="Bi ket tren duong ve diem UP. Hay kiem tra vat can."; }
            return false;
        }
        if(waitUntil>0L) {
            fsmState = STATE_TRIGGER_PORTAL;
            status="Cho server xac nhan chuyen map";
            if(now>=waitUntil) {
                if(selected!=null && selected.npc==-1 && portalAttempts<1 && retreatFromPortal()) {
                    portalAttempts++; waitUntil=0L; armed=null; retreating=true;
                } else {
                    if(selected!=null && selected.npc!=-1) graph.defer(selected,now);
                    failExit();
                }
            }
            return false;
        }
        if(retreating) {
            fsmState = STATE_TRIGGER_PORTAL;
            status="Lui va thu lai cong";
            int state=walk();
            if(state<0) { retreating=false; recoverOnError("Bi ket khi thu lai cong chuyen map"); }
            else if(state==1) {
                retreating=false;
                if(!startWalk(selected.x,selected.y,0,selected)) failExit();
            }
            return false;
        }
        if(selected==null) {
            fsmState = STATE_PLAN_ROUTE;
            if(now<nextPlan) return false;
            long retryAt=graph.retryTimeouts(now);
            int goal=graph.byId(mapId);
            if(goal<0) goal=graph.node(key(MenuUP.farmMapName),mapId);
            MainObject sn = findStoneNpc();
            if(sn!=null && prioritizeStone) {
                GoBackAtlas.addStoneRoutes(graph,current,sn.x,sn.y,sn.getIDnpc());
            }
            GoBackRoutes.Plan known=graph.planReturn(current,goal,visitedMaps);
            if(known==null && sn==null && GameScreen.isMapLang && now-seenMapTime<1000L) {
                status="Dang doi da dich chuyen...";
                return false;
            }
            if(probeNpc!=-1 || known==null)
                if(inspectStoneRoutes(now)) return false;
            GoBackRoutes.Node node=graph.get(current);
            for(int i=0;i<node.exits.size();i++) {
                GoBackRoutes.Exit e=(GoBackRoutes.Exit)node.exits.elementAt(i);
                if(e.blocked) continue;
                if(e.npc!=-1) {
                    MainObject npc=MainObject.get_Object(e.npc,(byte)2);
                    if(!stone(npc)) {
                        if(sn != null) { e.npc=sn.getIDnpc(); e.x=sn.x; e.y=sn.y; }
                        else { graph.block(e); continue; }
                    } else {
                        e.x=npc.x; e.y=npc.y;
                    }
                }
                // Rank equal-hop exits cheaply; solve collision only for the
                // chosen leg, instead of scanning the whole map for every exit.
                e.localCost=(Math.abs(p.x-e.x)+Math.abs(p.y-e.y))/LoadMap.wTile;
            }
            plan=null;
            for(int attempt=0;attempt<node.exits.size();attempt++) {
                plan=graph.planReturn(current,goal,visitedMaps);
                if(plan==null) break;
                GoBackRoutes.Exit candidate=plan.first;
                if(startWalk(candidate.x,candidate.y,candidate.npc!=-1?1:0,candidate)) break;
                graph.block(candidate);
                if(candidate.npc!=-1) {
                    // One unreachable stone can have dozens of destinations.
                    // All share this same physical approach; do not solve it again.
                    for(int j=0;j<node.exits.size();j++) {
                        GoBackRoutes.Exit other=(GoBackRoutes.Exit)node.exits.elementAt(j);
                        if(other.npc==candidate.npc) graph.block(other);
                    }
                }
                plan=null;
            }
            nextPlan=now+2000L;
            if(plan==null) {
                status=retryAt>now ? "Server cham; thu lai da sau "+((retryAt-now+999L)/1000L)+"s"
                    : "Chua co lo trinh phu hop toi "+MenuUP.farmMapName+"; da dung, khong di vong. Bam Ve Map UP ngay de thu lai.";
                return false;
            }
            selected=plan.first; portalAttempts=0;
            if(!freshMenu() || !ownsMenu(requestedStone,selected.npc)) {
                requestedMenu=0L; menuRequests=0; menuOptions=null; requestedStone=null;
            }
            if(totalHops==0) totalHops=plan.hops;
        }
        fsmState = STATE_WALK_TO_PORTAL;
        status="Chang "+(transitions+1)+"/"+Math.max(totalHops,transitions+plan.hops)+": "+name(selected.to);
        if(selected.npc==-1 && MainObject.getDistance(p.x,p.y,selected.x,selected.y)<=36) {
            armed=selected; armedFrom=current; armedAt=now;
        }
        int state=road==null?1:walk();
        if(state<0) {
            recoverOnError("Di chuyen bi ket; da dung va giu nguyen dich UP. Kiem tra vat can roi bam Ve Map UP ngay");
            return false;
        }
        if(state==0) return false;
        if(selected.npc!=-1) {
            if(!approachStone(selected.npc)) return false;
            if(requestedMenu==0L) { requestStone(selected.npc); return false; }
            if(!freshMenu()) {
                if(now-requestedMenu>15000L) {
                    if (menuRequests < 3) {
                        requestStone(selected.npc);
                    } else {
                        failStone();
                    }
                }
                return false;
            }
            GoBackRoutes.Node targetNode=graph.get(selected.to);
            String targetKey=targetNode.key;
            int targetMapId=GoBackAtlas.mapIdForName(targetKey);
            if(targetMapId<0) targetMapId=targetNode.mapId;
            int targetCat=GoBackAtlas.getCategoryForMap(targetMapId);
            String atlasKey=key(GoBackAtlas.getMapName(targetMapId));
            // Prefer exact labels first, then bounded price/level suffixes.
            for(int pass=0;pass<2;pass++) for(int i=0;i<menuOptions.size();i++) {
                String label=key(((iCommand)menuOptions.elementAt(i)).caption);
                boolean match=pass==0 ? label.equals(targetKey) || (atlasKey.length()>0 && label.equals(atlasKey))
                    : matchesMap(label,targetKey) || matchesMap(label,atlasKey);
                if(match) {
                    armed=selected; armedFrom=current; armedAt=now;
                    waitUntil=now+30000L;
                    chooseMenu(i,now);
                    return false;
                }
            }
            if(menuRequests>=6) { failExit(); return false; }
            for(int i=0;i<menuOptions.size();i++) {
                String label=key(((iCommand)menuOptions.elementAt(i)).caption);
                if(GoBackAtlas.matchesCategory(label,targetCat)) {
                    openedCategory=targetCat;
                    chooseMenu(i,now); return false;
                }
            }
            for(int i=0;i<menuOptions.size();i++) {
                String label=key(((iCommand)menuOptions.elementAt(i)).caption);
                if(label.equals("dich chuyen") || label.equals("teleport")) {
                    chooseMenu(i,now); return false;
                }
            }

            if(nearbyLanding(mapId,now)) return false;
            failExit(); GameCanvas.menu2.isShowMenu=false; return false;
        }
        armed=selected; armedFrom=current; armedAt=now;
        fsmState = STATE_TRIGGER_PORTAL;
        GlobalService.gI().player_move((short)p.x,(short)p.y); waitUntil=now+5000L;
        return false;
    }
    private static boolean retreatFromPortal() {
        int t=LoadMap.wTile,w=GameCanvas.loadmap.mapW,h=GameCanvas.loadmap.mapH;
        Player p=GameScreen.player;
        // Try adjacent inward cells through the same collision solver, never
        // change player coordinates directly or blindly step into a wall.
        int[] dx={-2,2,0,0},dy={0,0,-2,2};
        for(int i=0;i<4;i++) {
            int x=p.x/t+dx[i],y=p.y/t+dy[i];
            if(x<0 || y<0 || x>=w || y>=h) continue;
            int px=x*t+t/2,py=y*t+t/2;
            if(MainObject.getDistance(px,py,selected.x,selected.y)<=36) continue;
            if(startWalk(px,py,0,selected)) return true;
        }
        return false;
    }
}
