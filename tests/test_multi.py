"""Validate actual relocated classes, not just the manifest's slot count."""
import json
import os
import subprocess
import tempfile
from pathlib import Path
from zipfile import ZipFile

ROOT=Path(__file__).resolve().parents[1]
JAVA=os.environ.get('JAVA','java')

VERIFY=r'''
import java.util.*;
import java.util.jar.*;
import java.lang.reflect.*;
public class VerifyMulti {
    static int checks;
    static void check(boolean b,String label){checks++;if(!b)throw new RuntimeException(label);}
    public static void main(String[] args)throws Exception{
        JarFile jar=new JarFile(args[0]);int slots=Integer.parseInt(args[1]),loaded=0;
        Enumeration entries=jar.entries();
        while(entries.hasMoreElements()){
            String name=((JarEntry)entries.nextElement()).getName();
            if(!name.endsWith(".class"))continue;
            Class c=Class.forName(name.substring(0,name.length()-6).replace('/','.'),false,VerifyMulti.class.getClassLoader());
            c.getDeclaredMethods();c.getDeclaredFields();c.getDeclaredConstructors();loaded++;
        }
        Object[] sessions=new Object[slots];
        for(int i=0;i<slots;i++){
            String prefix="slot"+(i+1<10?"0":"")+(i+1)+".";
            Class graphClass=Class.forName(prefix+"GameObjects.GoBackRoutes");
            Object graph=graphClass.newInstance();
            graphClass.getMethod("loadBundled",new Class[]{String.class}).invoke(graph,new Object[]{"hso_gb_routes_v5_c2588702"});
            Vector nodes=(Vector)graphClass.getField("nodes").get(graph);
            check(nodes.size()>=90,"missing bundled maps in slot");
            int learned=0;for(int k=0;k<nodes.size();k++)if(nodes.elementAt(k).getClass().getField("minLevel").getInt(nodes.elementAt(k))>0)learned++;
            check(learned>=10,"missing learned levels");
            Class network=Class.forName(prefix+"CLib.Session_ME");
            sessions[i]=network.getMethod("gI",new Class[0]).invoke(null,new Object[0]);
            for(int j=0;j<i;j++)check(sessions[i]!=sessions[j],"shared socket singleton");
            network.getField("connected").setBoolean(sessions[i],i==0);
            Class midlet=Class.forName(prefix+"com.silverknight.TemMidlet");
            midlet.getField("slot").setInt(null,i);
            byte[] account={(byte)(i+1),11,22};
            midlet.getMethod("saveRMS",new Class[]{String.class,byte[].class}).invoke(null,new Object[]{"account",account});
        }
        for(int i=0;i<slots;i++){
            String prefix="slot"+(i+1<10?"0":"")+(i+1)+".";
            Class midlet=Class.forName(prefix+"com.silverknight.TemMidlet");
            byte[] account=(byte[])midlet.getMethod("loadRMS",new Class[]{String.class}).invoke(null,new Object[]{"account"});
            check(account[0]==i+1 && account[1]==11,"cross-slot RMS overwrite");
            check(sessions[i].getClass().getField("connected").getBoolean(sessions[i])==(i==0),"connection state leak");
        }
        System.out.println("PASS x"+slots+": verified "+loaded+" classes; "+checks+" isolation checks");
    }
}
'''
RMS=r'''
package javax.microedition.rms;
public class RecordStore {
    private static java.util.Hashtable stores=new java.util.Hashtable();
    private byte[] data;
    public static synchronized RecordStore openRecordStore(String name,boolean create)throws Exception{
        RecordStore store=(RecordStore)stores.get(name);
        if(store==null){if(!create)throw new Exception("missing");store=new RecordStore();stores.put(name,store);}return store;
    }
    public int getNumRecords(){return data==null?0:1;}
    public void setRecord(int id,byte[] bytes,int off,int length){data=new byte[length];System.arraycopy(bytes,off,data,0,length);}
    public int addRecord(byte[] bytes,int off,int length){setRecord(1,bytes,off,length);return 1;}
    public byte[] getRecord(int id){byte[] result=new byte[data.length];System.arraycopy(data,0,result,0,data.length);return result;}
    public void closeRecordStore(){}
}
'''

with tempfile.TemporaryDirectory(prefix='multi-verify-') as directory:
    temp=Path(directory)
    source=temp/'VerifyMulti.java';source.write_text(VERIFY)
    rms=temp/'javax/microedition/rms/RecordStore.java';rms.parent.mkdir(parents=True);rms.write_text(RMS)
    subprocess.run([JAVA,'-jar',str(ROOT/'ecj.jar'),'-1.4','-nowarn','-d',str(temp),str(source),str(rms)],check=True)
    report=json.loads((ROOT/'multi_build/build_report.json').read_text())
    for build in report['builds']:
        path=ROOT/'multi_build'/build['file'];count=build['slots']
        with ZipFile(path) as archive:
            names=archive.namelist()
            assert len(names)==len(set(names))
            assert archive.testzip() is None
            for i in range(1,count+1):
                assert sum(n.startswith('slot%02d/'%i) and n.endswith('.class') for n in names)==report['classes_per_slot']
            assert all(n.startswith(('slot','multi/')) for n in names if n.endswith('.class'))
        subprocess.run([JAVA,'-Xverify:all','-cp',str(temp)+';'+str(path)+';'+str(ROOT/'microemulator.jar'),
                        'VerifyMulti',str(path),str(count)],check=True)
