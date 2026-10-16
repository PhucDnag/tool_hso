package multi;

import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.*;

/** One Android MIDlet, isolated game namespaces, one foreground display. */
public final class MultiMidlet extends MIDlet implements CommandListener {
    public static MultiMidlet instance;
    public static volatile int constructingSlot;
    private static volatile int active=-1;
    private static volatile boolean closing;
    private static Slot[] slots;
    private static Displayable[] screens;
    private static String[] errors;
    private static boolean[] starting;
    private static final Object launchLock=new Object();
    private static final Object paintLock=new Object();
    private static Image scratch;
    private static List menu;
    private static final Command back=new Command("Tiếp tục",Command.BACK,1);
    private static final Command quit=new Command("Thoát tất cả",Command.EXIT,2);
    private static final Command cancelQuit=new Command("Hủy",Command.CANCEL,1);
    private static final Command confirmQuit=new Command("Thoát",Command.OK,1);

    public MultiMidlet(){
        instance=this;
        int count=2;
        try {count=Integer.parseInt(getAppProperty("Multi-Slots"));}catch(Exception e){}
        if(count!=2 && count!=3 && count!=5 && count!=10)count=2;
        slots=new Slot[count];screens=new Displayable[count];errors=new String[count];starting=new boolean[count];
    }
    protected void startApp(){showMenu();}
    protected void pauseApp(){}
    protected void destroyApp(boolean unconditional){
        closing=true;
        for(int i=0;i<slots.length;i++)if(slots[i]!=null)try{slots[i].shutdown();}catch(Throwable e){}
    }
    public static boolean visible(int slot){return !closing && active==slot && Display.getDisplay(instance).getCurrent()==screens[slot];}
    public static boolean running(){return !closing;}
    public static void display(int slot,Displayable screen){
        screens[slot]=screen;
        if(active==slot && !closing)Display.getDisplay(instance).setCurrent(screen);
    }
    public static void showMenu(){
        if(closing)return;
        if(active>=0 && slots[active]!=null)slots[active].clearInput();
        menu=new List("HONG PHUC HSO x"+slots.length,List.IMPLICIT);
        for(int i=0;i<slots.length;i++){
            String state=starting[i]?"Đang mở":errors[i]!=null?errors[i]:slots[i]==null?"Chưa mở":slots[i].status();
            menu.append("Nick "+(i+1)+" - "+state,null);
        }
        menu.addCommand(back);menu.addCommand(quit);menu.setCommandListener(instance);
        Display.getDisplay(instance).setCurrent(menu);
    }
    public static void open(final int index){
        if(closing || index<0 || index>=slots.length)return;
        if(active>=0 && slots[active]!=null)slots[active].clearInput();
        active=index;
        if(errors[index]!=null){showMenu();return;}
        if(slots[index]!=null){Display.getDisplay(instance).setCurrent(screens[index]);return;}
        if(starting[index] || errors[index]!=null){showMenu();return;}
        starting[index]=true;showMenu();
        new Thread(new Runnable(){public void run(){
            synchronized(launchLock){
                if(closing)return;
                constructingSlot=index;
                try{
                    String number=index+1<10?"0"+(index+1):String.valueOf(index+1);
                    slots[index]=(Slot)Class.forName("slot"+number+".com.silverknight.TemMidlet").newInstance();
                    if(closing)slots[index].shutdown();
                }catch(Throwable e){e.printStackTrace();errors[index]="Lỗi mở: "+e.getClass().getName();}
                starting[index]=false;
                if(errors[index]!=null)showMenu();
            }
        }}).start();
    }
    public static void fault(int index,Throwable error){
        error.printStackTrace();
        errors[index]="Lỗi chạy: "+error.getClass().getName();
        if(slots[index]!=null)slots[index].shutdown();
        if(active==index)showMenu();
    }
    public static void drawBackground(BackgroundCanvas canvas,int width,int height){
        synchronized(paintLock){
            if(scratch==null || scratch.getWidth()!=width || scratch.getHeight()!=height)scratch=Image.createImage(width,height);
            Graphics g=scratch.getGraphics();g.translate(-g.getTranslateX(),-g.getTranslateY());g.setClip(0,0,width,height);
            canvas.drawBackground(g);
        }
    }
    public static void badge(Graphics g,int width,int index){
        g.translate(-g.getTranslateX(),-g.getTranslateY());g.setClip(0,0,width,22);
        g.setColor(0x342718);g.fillRect(width-86,0,86,22);g.setColor(0xffdd88);
        g.drawString("Nick "+(index+1)+"/"+slots.length,width-3,2,Graphics.TOP|Graphics.RIGHT);
    }
    public void commandAction(Command command,Displayable source){
        if(command==quit){
            Form confirm=new Form("Thoát tất cả nick?");confirm.append("Các nick đang mở sẽ ngắt kết nối.");
            confirm.addCommand(confirmQuit);confirm.addCommand(cancelQuit);confirm.setCommandListener(this);
            Display.getDisplay(this).setCurrent(confirm);
        }else if(command==confirmQuit){destroyApp(true);notifyDestroyed();}
        else if(command==cancelQuit)showMenu();
        else if(command==back){if(active>=0 && screens[active]!=null)Display.getDisplay(this).setCurrent(screens[active]);}
        else if(source==menu && command==List.SELECT_COMMAND)open(menu.getSelectedIndex());
    }
}
