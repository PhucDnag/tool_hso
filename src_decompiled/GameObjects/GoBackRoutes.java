package GameObjects;

import java.util.Vector;
import java.io.*;

/** Directed live exits plus documented world-atlas hints for unobserved maps. */
public final class GoBackRoutes {
    public static final class Node {
        public String key;
        public int mapId = -1, minLevel = 0, maxLevel = 0;
        public boolean scanned, village;
        public Vector exits = new Vector();
        Node(String value) { key = value; }
    }
    public static final class Exit {
        public int from, to, x, y, npc = -1;
        public boolean confirmed, blocked;
        // Walking distance is local/transient, never used as an invented remote cost.
        public int localCost;
        public long retryAt;
        public int timeouts;
        Exit(int a, int b, int px, int py, int id) {
            from = a; to = b; x = px; y = py; npc = id;
        }
    }
    public static final class Plan {
        public Exit first;
        public int hops;
        Plan(Exit e, int count) { first=e; hops=count; }
    }
    public Vector nodes = new Vector();
    public int revision;

    public Node get(int n) { return (Node)nodes.elementAt(n); }
    public int byId(int id) {
        if (id < 0) return -1;
        for (int i=0;i<nodes.size();i++) if (get(i).mapId==id) return i;
        return -1;
    }
    public int byName(String key) {
        int found=-1;
        for (int i=0;i<nodes.size();i++) if (get(i).key.equals(key)) {
            if (found!=-1) return -1; // Ambiguous names must not silently pick a map.
            found=i;
        }
        return found;
    }
    public int node(String key, int mapId) {
        if (key==null || key.length()==0) return -1;
        int n=byId(mapId);
        if(n>=0) return n;
        n=byName(key);
        if(n<0 && mapId<0) for(int i=0;i<nodes.size();i++)
            if(get(i).key.equals(key)) return -1;
        if(n>=0 && (mapId<0 || get(n).mapId<0)) {
            if(mapId>=0) { get(n).mapId=mapId; revision++; }
            return n;
        }
        if(n>=0 && mapId<0) return n;
        Node entry=new Node(key); entry.mapId=mapId;
        nodes.addElement(entry); revision++;
        return nodes.size()-1;
    }
    // Atlas locations with the same caption are separate maps (four "Chuan bi"
    // areas and two arenas). Never join their exits through a shared name node.
    public int distinctNode(String key,int mapId) {
        int existing=byId(mapId);
        if(existing>=0) return existing;
        Node n=new Node(key); n.mapId=mapId;
        nodes.addElement(n); revision++;
        return nodes.size()-1;
    }
    public Exit exit(int from,int to,int x,int y,int npc) {
        Node n=get(from);
        for(int i=0;i<n.exits.size();i++) {
            Exit e=(Exit)n.exits.elementAt(i);
            if(e.npc==npc && (npc==-1 ? e.x==x && e.y==y : e.to==to)) {
                e.x=x; e.y=y;
                if(!e.confirmed && e.to!=to) { e.to=to; revision++; }
                return e;
            }
        }
        Exit e=new Exit(from,to,x,y,npc); n.exits.addElement(e); revision++; return e;
    }
    public void scan(int from,String[] names,int[] xs,int[] ys) {
        Node n=get(from);
        Vector keep=new Vector();
        for(int i=0;i<names.length;i++) {
            // A label may disappear, but a previously verified physical portal
            // remains usable at the same coordinates. Never guess from direction.
            Exit verified=null;
            for(int j=0;j<n.exits.size();j++) {
                Exit e=(Exit)n.exits.elementAt(j);
                if(e.npc==-1 && e.confirmed && e.x==xs[i] && e.y==ys[i]) { verified=e; break; }
            }
            if(verified!=null) { keep.addElement(verified); continue; }
            String label=names[i];
            if(label==null || label.length()==0) label="@portal:"+from+":"+xs[i]+":"+ys[i];
            int to=node(label,-1);
            if(to<0) {
                // A repeated caption is resolvable only when this source already
                // has exactly one documented/observed neighbor with that caption.
                for(int j=0;j<n.exits.size();j++) {
                    Exit candidate=(Exit)n.exits.elementAt(j);
                    if(!get(candidate.to).key.equals(label)) continue;
                    if(to>=0 && to!=candidate.to) { to=-1; break; }
                    to=candidate.to;
                }
            }
            if(to==from || to<0 || label.startsWith("@portal")) {
                if(names.length==1) {
                    for(int j=0;j<n.exits.size();j++) {
                        Exit candidate=(Exit)n.exits.elementAt(j);
                        if(candidate.npc==-1 && candidate.x<0 && candidate.to!=from) { to=candidate.to; break; }
                    }
                }
                if(to==from || to<0) to=node("@portal:"+from+":"+xs[i]+":"+ys[i],-1);
            }
            if(to==from) continue;
            Exit e=exit(from,to,xs[i],ys[i],-1);
            keep.addElement(e);
        }
        for(int i=n.exits.size()-1;i>=0;i--) {
            Exit e=(Exit)n.exits.elementAt(i);
            if(e.npc==-1 && !keep.contains(e)) { n.exits.removeElementAt(i); revision++; }
        }
        if(!n.scanned) { n.scanned=true; revision++; }
    }
    public void confirm(Exit e,int actual) {
        if(e.to!=actual || !e.confirmed) { e.to=actual; e.confirmed=true; revision++; }
    }
    public void block(Exit e) { if(!e.blocked) { e.blocked=true; revision++; } }
    public void defer(Exit e,long now) {
        block(e);
        e.timeouts++;
        e.retryAt=e.timeouts<3?now+30000L:0L;
    }
    public long retryTimeouts(long now) {
        long next=0L;
        for(int i=0;i<nodes.size();i++) for(int j=0;j<get(i).exits.size();j++) {
            Exit e=(Exit)get(i).exits.elementAt(j);
            if(e.retryAt<=0L) continue;
            if(now>=e.retryAt) { e.blocked=false; e.retryAt=0L; revision++; }
            else if(next==0L || e.retryAt<next) next=e.retryAt;
        }
        return next;
    }
    public void resetFailures() {
        for(int i=0;i<nodes.size();i++) for(int j=0;j<get(i).exits.size();j++) {
            Exit e=(Exit)get(i).exits.elementAt(j);
            e.blocked=false; e.retryAt=0L; e.timeouts=0;
        }
        revision++;
    }

    public void atlasLink(int from,int to) {
        Node n=get(from);
        if(n.scanned) return;
        for(int i=0;i<n.exits.size();i++) if(((Exit)n.exits.elementAt(i)).to==to) return;
        n.exits.addElement(new Exit(from,to,-1,-1,-1)); revision++;
    }

    /** BFS only when there is a complete route to the requested target.
     * Ties in map hops prefer the shortest walk to a reachable exit on this map.
     */
    public Plan plan(int start,int goal) {
        return plan(start,goal,null,false,true);
    }
    /** A return trip may use a local stone, then a complete walking route.
     * Cached stones on other maps are not evidence of an available shortcut.
     */
    public Plan planReturn(int start,int goal,java.util.Hashtable visited) {
        return plan(start,goal,visited,true,true);
    }
    public Plan planWalking(int start,int goal,java.util.Hashtable visited) {
        return plan(start,goal,visited,true,false);
    }
    private Plan plan(int start,int goal,java.util.Hashtable visited,boolean returning,boolean localStone) {
        if(start<0 || start>=nodes.size() || goal<0 || goal>=nodes.size() || goal==start) return null;
        int ceiling=GoBackAtlas.getCategoryForMap(get(goal).mapId);
        if(ceiling<1 || ceiling>5) ceiling=0;
        int origin=GoBackAtlas.getCategoryForMap(get(start).mapId);
        if(ceiling>0 && origin>=1 && origin<=5) ceiling=Math.max(ceiling,origin);
        int count=nodes.size();
        int[] distance=new int[count], queue=new int[count];
        Exit[] first=new Exit[count];
        for(int i=0;i<count;i++) distance[i]=-1;
        int head=0,tail=0; queue[tail++]=start; distance[start]=0;
        while(head<tail) {
            int u=queue[head++]; Node n=get(u);
            // Process one BFS layer at a time; equal-depth first-hop costs are settled
            // before a node is dequeued, so tie improvements propagate to its children.
            for(int j=0;j<n.exits.size();j++) {
                Exit e=(Exit)n.exits.elementAt(j);
                if(e.blocked || e.to==start) continue;
                if(returning) {
                    if(e.npc!=-1 && (!localStone || u!=start)) continue;
                    if(e.to!=goal && visited!=null && visited.containsKey(new Integer(e.to))) continue;
                    int category=GoBackAtlas.getCategoryForMap(get(e.to).mapId);
                    if(e.to!=goal && ceiling>0 && category>ceiling && category<=5) continue;
                }
                Exit hop=u==start?e:first[u];
                int d=distance[u]+1;
                if(distance[e.to]<0) {
                    distance[e.to]=d; first[e.to]=hop; queue[tail++]=e.to;
                } else if(distance[e.to]==d) {
                    boolean hopIsStone = (hop.npc != -1);
                    boolean curIsStone = (first[e.to].npc != -1);
                    if(hop.localCost<first[e.to].localCost
                            || hop.localCost==first[e.to].localCost && hopIsStone && !curIsStone) {
                        first[e.to]=hop;
                    }
                }
            }
        }
        if(goal>=0 && goal<count && distance[goal]>0) return new Plan(first[goal],distance[goal]);
        return null;
    }

    public byte[] save() throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(2); out.writeInt(nodes.size());
        for(int i=0;i<nodes.size();i++) {
            Node n=get(i); out.writeUTF(n.key); out.writeInt(n.mapId);
            out.writeBoolean(n.scanned); out.writeBoolean(n.village);
            out.writeInt(n.minLevel); out.writeInt(n.maxLevel); out.writeInt(n.exits.size());
            for(int j=0;j<n.exits.size();j++) {
                Exit e=(Exit)n.exits.elementAt(j);
                out.writeInt(e.to); out.writeInt(e.x); out.writeInt(e.y); out.writeInt(e.npc); out.writeBoolean(e.confirmed);
            }
        }
        out.flush(); return bytes.toByteArray();
    }
    public static GoBackRoutes load(byte[] bytes) throws IOException {
        GoBackRoutes graph=new GoBackRoutes();
        if(bytes==null) return graph;
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readInt()!=2) throw new IOException("route version");
        int count=in.readInt();
        if(count<0 || count>4096) throw new IOException("route count");
        for(int i=0;i<count;i++) {
            Node n=new Node(in.readUTF()); n.mapId=in.readInt(); n.scanned=in.readBoolean(); n.village=in.readBoolean();
            n.minLevel=in.readInt(); n.maxLevel=in.readInt(); int size=in.readInt();
            if(size<0 || size>256) throw new IOException("exit count");
            for(int j=0;j<size;j++) {
                int to=in.readInt(),x=in.readInt(),y=in.readInt(),npc=in.readInt();
                if(to<0 || to>=count) throw new IOException("exit target");
                Exit e=new Exit(i,to,x,y,npc); e.confirmed=in.readBoolean(); n.exits.addElement(e);
            }
            graph.nodes.addElement(n);
        }
        return graph;
    }
}
