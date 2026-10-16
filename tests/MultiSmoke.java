import java.lang.reflect.*;
import java.security.Permission;
import org.microemu.app.Headless;
import org.microemu.MIDletBridge;

/** Offline lifecycle smoke test with actual game classes and emulator graphics. */
public class MultiSmoke {
    public static void main(String[] args)throws Exception {
        System.setSecurityManager(new SecurityManager(){
            public void checkPermission(Permission p){}
            public void checkConnect(String host,int port){throw new SecurityException("Offline smoke test");}
        });
        try{
            Headless.main(new String[]{args[0]});
            Class host=MIDletBridge.getCurrentMIDlet().getClass();
            Field count=host.getDeclaredField("slots");count.setAccessible(true);
            Object[] slots=(Object[])count.get(null);
            Field errorsField=host.getDeclaredField("errors");errorsField.setAccessible(true);
            Field startingField=host.getDeclaredField("starting");startingField.setAccessible(true);
            ClassLoader loader=host.getClassLoader();
            int[] before=new int[slots.length];
            for(int i=0;i<slots.length;i++){
                host.getMethod("open",new Class[]{Integer.TYPE}).invoke(null,new Object[]{new Integer(i)});
                long deadline=System.currentTimeMillis()+15000L;
                while(((boolean[])startingField.get(null))[i] && System.currentTimeMillis()<deadline)Thread.sleep(100L);
                String error=((String[])errorsField.get(null))[i];
                if(error!=null || slots[i]==null)throw new Exception("slot "+i+": "+error);
                Class game=Class.forName("slot"+(i+1<10?"0":"")+(i+1)+".Main.GameCanvas",false,loader);
                before[i]=game.getField("gameTick").getInt(null);
            }
            Thread.sleep(1500L);
            for(int i=0;i<slots.length;i++){
                String error=((String[])errorsField.get(null))[i];
                if(error!=null)throw new Exception("runtime slot "+i+": "+error);
                Class game=Class.forName("slot"+(i+1<10?"0":"")+(i+1)+".Main.GameCanvas",false,loader);
                if(game.getField("gameTick").getInt(null)==before[i])throw new Exception("background slot stopped: "+i);
            }
            javax.microedition.lcdui.Display display=javax.microedition.lcdui.Display.getDisplay(MIDletBridge.getCurrentMIDlet());
            javax.microedition.lcdui.Displayable foreground=display.getCurrent();
            Field screensField=host.getDeclaredField("screens");screensField.setAccessible(true);
            Object[] screens=(Object[])screensField.get(null);Object original=screens[0];
            host.getMethod("display",new Class[]{Integer.TYPE,javax.microedition.lcdui.Displayable.class})
                .invoke(null,new Object[]{new Integer(0),new javax.microedition.lcdui.Form("Hidden input")});
            if(display.getCurrent()!=foreground)throw new Exception("hidden slot stole foreground display");
            screens[0]=original;
            host.getMethod("open",new Class[]{Integer.TYPE}).invoke(null,new Object[]{new Integer(0)});
            Thread.sleep(300L); // MicroEmulator dispatches setCurrent asynchronously.
            if(display.getCurrent()!=original)throw new Exception("slot switch did not restore screen");
            host.getMethod("showMenu",new Class[0]).invoke(null,new Object[0]);
            Method destroy=host.getDeclaredMethod("destroyApp",new Class[]{Boolean.TYPE});destroy.setAccessible(true);
            destroy.invoke(MIDletBridge.getCurrentMIDlet(),new Object[]{Boolean.TRUE});Thread.sleep(100L);
            for(int i=0;i<slots.length;i++){
                Class midlet=slots[i].getClass();Object canvas=midlet.getField("temCanvas").get(null);
                if(canvas.getClass().getField("running").getBoolean(canvas))throw new Exception("slot loop not stopped");
            }
            System.out.println("PASS: "+slots.length+" sessions started; background ticks, screen ownership, switching and shutdown verified offline");
            System.exit(0);
        }catch(Throwable e){e.printStackTrace();System.exit(1);}
    }
}
