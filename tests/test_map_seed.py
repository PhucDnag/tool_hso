import os,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
body="""
import GameObjects.GoBackRoutes;
public class SeedTest {
 static void check(boolean b){if(!b)throw new RuntimeException("seed regression");}
 public static void main(String[] args)throws Exception {
  GoBackRoutes g=new GoBackRoutes();g.loadBundled("hso_gb_routes_v5_c2588702");
  int levels=0,scanned=0,exits=0;
  for(int i=0;i<g.nodes.size();i++){GoBackRoutes.Node n=g.get(i);if(n.minLevel>0)levels++;if(n.scanned)scanned++;exits+=n.exits.size();}
  check(levels>=10 && scanned>=10 && exits>0);
  byte[] first=g.save();g.loadBundled("hso_gb_routes_v5_c2588702");check(java.util.Arrays.equals(first,g.save()));
  GoBackRoutes local=new GoBackRoutes();int a=local.node("canh dong soi",1);local.get(a).minLevel=200;local.get(a).maxLevel=201;
  local.loadBundled("hso_gb_routes_v5_c2588702");check(local.get(a).minLevel==200 && local.get(a).maxLevel==201);
  GoBackRoutes unknown=new GoBackRoutes();unknown.loadBundled("other_server");check(unknown.nodes.size()==0);
  GoBackRoutes legacy=new GoBackRoutes();legacy.loadBundled("hso_gb_routes_v5_c25886fe");check(legacy.nodes.size()>0);
  System.out.println("PASS: seed "+g.nodes.size()+" maps, "+levels+" level ranges, "+scanned+" visited maps, "+exits+" exits; idempotent merge and local/server isolation");
 }
}
"""
with tempfile.TemporaryDirectory(prefix='seed-test-') as d:
 p=Path(d)/'SeedTest.java';p.write_text(body);java=os.environ.get('JAVA','java');cp=str(ROOT/'KnightOnline_v403.jar')+';'+str(ROOT/'microemulator.jar')
 subprocess.run([java,'-jar',str(ROOT/'ecj.jar'),'-1.4','-nowarn','-cp',cp,'-d',d,str(p)],check=True)
 subprocess.run([java,'-cp',d+';'+cp,'SeedTest'],check=True)
