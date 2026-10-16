"""Build Android/J2ME Loader multi-session suites from the current single JAR.

All game classes (including anonymous classes) are relocated per slot. Resources
are stored once. Hooks are compiled in a separate temporary directory, leaving
the normal single-session source/build unchanged.
"""
import hashlib
import json
import os
import re
import struct
import subprocess
import tempfile
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / 'KnightOnline_v403.jar'
OUTPUT = ROOT / 'multi_build'


def change(source, old, new):
    if old not in source:
        raise ValueError('Hook no longer matches: ' + old[:90])
    return source.replace(old, new)


def hooked_sources():
    read = lambda name: (ROOT / 'src_decompiled' / name).read_text(encoding='utf-8')
    midlet = read('com/silverknight/TemMidlet.java')
    midlet = change(midlet, 'byArray[i] = ~byArray[i];', 'byArray[i] = (byte)~byArray[i];')
    midlet = change(midlet, 'extends MIDlet {', 'implements multi.Slot {\n    public static int slot;')
    midlet = change(midlet, '        temCanvas = new TemCanvas();\n        instance = this;',
                    '        slot=multi.MultiMidlet.constructingSlot;\n        instance=this;\n        temCanvas = new TemCanvas();')
    midlet = change(midlet, 'Display.getDisplay((MIDlet)this).setCurrent((Displayable)temCanvas);',
                    'multi.MultiMidlet.display(slot,temCanvas);')
    midlet = change(midlet, 'instance.notifyDestroyed();', 'multi.MultiMidlet.showMenu();')
    # Keep exact original keys after a slot prefix. Loader accepts these names;
    # no truncated/hash-only keys that could collide or lose cleanup semantics.
    midlet = change(midlet, 'RecordStore.openRecordStore((String)string,',
                    'RecordStore.openRecordStore("s"+slot+"_"+string,')
    midlet = change(midlet, 'if (!CRes.CheckDelRMS(stringArray[i])) continue;',
                    'String prefix="s"+slot+"_";\n                    if(!stringArray[i].startsWith(prefix) || !CRes.CheckDelRMS(stringArray[i].substring(prefix.length())))continue;')
    marker = '    public static byte[] encoding'
    extra = '''    public boolean platformRequest(String url)throws ConnectionNotFoundException{
        return multi.MultiMidlet.instance.platformRequest(url);
    }
    public void clearInput(){Main.GameCanvas.clearAll();}
    public String status(){
        if(CLib.Session_ME.gI().isConnected()){
            if(GameScreen.GameScreen.player!=null && GameScreen.GameScreen.player.name!=null)
                return GameScreen.GameScreen.player.name;
            return "Đã kết nối";
        }
        return "Chưa đăng nhập";
    }
    public void shutdown(){
        if(temCanvas!=null)temCanvas.running=false;
        GameObjects.MenuUP.isAutoReconnect=false;
        GameObjects.MenuUP.isWaitingReconnect=false;
        Session_ME.isCancel=true;
        Session_ME.gI().close();
    }

'''
    midlet = change(midlet, marker, extra + marker)

    canvas = read('com/silverknight/TemCanvas.java')
    canvas = change(canvas, 'implements Runnable {',
                    'implements Runnable, multi.BackgroundCanvas {\n    public volatile boolean running=true;\n    private boolean nickTouch;\n    private long lastBackgroundPaint;')
    canvas = change(canvas, '        gamecanvas.paint(tem);',
                    '        gamecanvas.paint(tem);\n        multi.MultiMidlet.badge(graphics,getWidth(),TemMidlet.slot);')
    canvas = change(canvas, 'protected void paint(Graphics graphics)', 'protected synchronized void paint(Graphics graphics)')
    start = canvas.index('    public void run() {')
    end = canvas.index('    protected void keyPressed', start)
    canvas = canvas[:start] + '''    public synchronized void drawBackground(Graphics graphics){
        TemCanvas.tem.g=graphics;
        gamecanvas.paint(tem);
    }
    public void run() {
        while(running && multi.MultiMidlet.running()){
            long start=mSystem.currentTimeMillis();
            try{
                this.update();
                if(multi.MultiMidlet.visible(TemMidlet.slot)){
                    this.repaint();this.serviceRepaints();
                }else if(start-lastBackgroundPaint>=200L || GameScreen.GameScreen.infoGame.isShipMove){
                    // Some native boarding logic lives in paintShip: preserve it
                    // even for hidden slots. Reuse one scratch image for all slots.
                    multi.MultiMidlet.drawBackground(this,getWidth(),getHeight());
                    lastBackgroundPaint=start;
                }
                long delay=40L-(mSystem.currentTimeMillis()-start);
                Thread.sleep(delay>0L?delay:1L);
            }catch(InterruptedException e){running=false;}
            catch(Throwable e){running=false;multi.MultiMidlet.fault(TemMidlet.slot,e);}
        }
    }

''' + canvas[end:]
    canvas = change(canvas, '        gamecanvas.pointerPressed(n, n2);',
                    '        nickTouch=n>=getWidth()-86 && n2<22;\n        if(nickTouch){multi.MultiMidlet.showMenu();return;}\n        gamecanvas.pointerPressed(n, n2);')
    canvas = change(canvas, '        gamecanvas.pointerDragged(n, n2);',
                    '        if(nickTouch)return;\n        gamecanvas.pointerDragged(n, n2);')
    canvas = change(canvas, '        gamecanvas.pointerReleased(n, n2);',
                    '        if(nickTouch){nickTouch=false;return;}\n        gamecanvas.pointerReleased(n, n2);')
    canvas = change(canvas, '        gamecanvas.keyPressed(n);',
                    '        if(n==35){multi.MultiMidlet.showMenu();return;}\n        gamecanvas.keyPressed(n);')
    canvas = change(canvas, '        gamecanvas.keyReleased(n);',
                    '        if(n==35)return;\n        gamecanvas.keyReleased(n);')

    field = read('CLib/TField.java')
    field = change(field, 'Display.getDisplay((MIDlet)TemMidlet.instance).setCurrent((Displayable)TemMidlet.temCanvas);',
                   'multi.MultiMidlet.display(TemMidlet.slot,TemMidlet.temCanvas);')
    field = change(field, 'Display.getDisplay((MIDlet)TemMidlet.instance).setCurrent((Displayable)textBox);',
                   'multi.MultiMidlet.display(TemMidlet.slot,textBox);')
    loader = read('Thread_More/Load_Data_And_Img.java')
    loader = change(loader, 'while (true) {',
                    'while(multi.MultiMidlet.running() && (com.silverknight.TemMidlet.temCanvas==null || com.silverknight.TemMidlet.temCanvas.running)){')
    # Let startup retain the normal single-game behaviour before the host assigns
    # a current screen. Resource paths remain rooted in the common JAR.
    return {'com/silverknight/TemMidlet.java': midlet,
            'com/silverknight/TemCanvas.java': canvas, 'CLib/TField.java': field,
            'Thread_More/Load_Data_And_Img.java': loader}


def transform_class(data, mapping):
    """Rewrite UTF8 class names/descriptors; leave bytecode and string data intact.

    Modified UTF8 is kept as bytes so Vietnamese text and NUL constants survive.
    Classfile offsets/constant-pool indexes do not change.
    """
    assert data[:4] == b'\xca\xfe\xba\xbe'
    count = struct.unpack_from('>H', data, 8)[0]
    out = bytearray(data[:10]); pos = 10; index = 1
    sizes = {3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4, 11: 4,
             12: 4, 15: 3, 16: 2, 18: 4, 17: 4, 19: 2, 20: 2}
    dotted = {k.replace(b'/', b'.'): v.replace(b'/', b'.') for k, v in mapping.items()}
    while index < count:
        tag = data[pos]; pos += 1; out.append(tag)
        if tag == 1:
            length = struct.unpack_from('>H', data, pos)[0]; pos += 2
            value = data[pos:pos+length]; pos += length
            if value in mapping:
                value = mapping[value]
            elif value in dotted:
                value = dotted[value]
            else:
                value = re.sub(rb'L([^;]+);', lambda m: b'L'+mapping.get(m[1], m[1])+b';', value)
            out += struct.pack('>H', len(value)) + value
        else:
            length = sizes[tag]; out += data[pos:pos+length]; pos += length
            if tag in (5, 6): index += 1
        index += 1
    out += data[pos:]
    return bytes(out)


def build():
    OUTPUT.mkdir(exist_ok=True)
    with ZipFile(SOURCE) as archive:
        original = {info.filename: archive.read(info) for info in archive.infolist() if not info.is_dir()}
    with tempfile.TemporaryDirectory(prefix='hso-multi-') as temporary:
        temp=Path(temporary); classes=temp/'classes'; classes.mkdir()
        sources=list((ROOT/'multi_src').rglob('*.java'))
        for name, content in hooked_sources().items():
            path=temp/name; path.parent.mkdir(parents=True,exist_ok=True)
            path.write_text(content,encoding='utf-8');sources.append(path)
        subprocess.run([os.environ.get('JAVA','java'),'-jar',str(ROOT/'ecj.jar'),'-1.4',
                        '-encoding','UTF-8','-nowarn','-cp',str(SOURCE)+';'+str(ROOT/'microemulator.jar'),
                        '-d',str(classes)]+[str(p) for p in sources],check=True)
        compiled={p.relative_to(classes).as_posix():p.read_bytes() for p in classes.rglob('*.class')}
    game={name[:-6]:data for name,data in original.items() if name.endswith('.class')}
    game.update({name[:-6]:data for name,data in compiled.items() if not name.startswith('multi/')})
    shared={name:data for name,data in compiled.items() if name.startswith('multi/')}
    resources={name:data for name,data in original.items()
               if not name.endswith('.class') and not name.upper().startswith('META-INF/')}
    relocated={}
    for index in range(1,11):
        prefix='slot%02d/'%index
        mapping={name.encode(): (prefix+name).encode() for name in game}
        for name,data in game.items():
            relocated[prefix+name+'.class']=transform_class(data,mapping)
    report={'source':SOURCE.name,'source_sha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
            'classes_per_slot':len(game),'builds':[]}
    for count in (2,3,5,10):
        name='KnightOnline_v403_x%d.jar'%count
        manifest=('Manifest-Version: 1.0\r\nMIDlet-Name: HONG PHUC HSO x%d\r\n'
                  'MIDlet-Vendor: HONG PHUC HSO\r\nMIDlet-Version: 1.0.0\r\n'
                  'MIDlet-1: HSO x%d,/icon.png,multi.MultiMidlet\r\n'
                  'MicroEdition-Configuration: CLDC-1.1\r\nMicroEdition-Profile: MIDP-2.0\r\n'
                  'Multi-Slots: %d\r\n\r\n')%(count,count,count)
        path=OUTPUT/name
        with ZipFile(path,'w',ZIP_DEFLATED) as archive:
            archive.writestr('META-INF/MANIFEST.MF',manifest)
            for key,data in resources.items():archive.writestr(key,data)
            for key,data in shared.items():archive.writestr(key,data)
            for key,data in relocated.items():
                if int(key[4:6])<=count:archive.writestr(key,data)
        with ZipFile(path) as archive:assert archive.testzip() is None
        jad=manifest.rstrip()+'\r\nMIDlet-Jar-URL: '+name+'\r\nMIDlet-Jar-Size: '+str(path.stat().st_size)+'\r\n'
        path.with_suffix('.jad').write_text(jad,encoding='utf-8',newline='')
        report['builds'].append({'file':name,'slots':count,'bytes':path.stat().st_size,
                                'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
        print('Built',name,round(path.stat().st_size/1024/1024,2),'MiB')
    (OUTPUT/'build_report.json').write_text(json.dumps(report,indent=2),encoding='utf-8')


if __name__=='__main__':build()
