package GameObjects;

import CLib.mSystem;
import CLib.mVector;
import GameScreen.GameScreen;
import Main.GameCanvas;
import Model.CRes;
import InterfaceComponents.iCommand;
import InterfaceComponents.IAction;
import netcommand.global.GlobalService;

/** Independent equipment colour selection; other drop types keep native rules. */
public final class AutoLoot {
    public static boolean enabled;
    private static boolean loaded;
    public static boolean[] colors={false,true,true,true,true};
    private static final String[] names={"Trắng","Xanh dương","Vàng","Tím","Cam"};
    private static java.util.Hashtable pending=new java.util.Hashtable();
    private static int map=-1;
    private static long nextSend;
    public static void load(){
        if(loaded)return;loaded=true;
        enabled="1".equals(CRes.loadRMSString("hso_loot_enabled"));
        for(int i=0;i<colors.length;i++){
            String value=CRes.loadRMSString("hso_loot_color_"+i);
            if(value!=null)colors[i]="1".equals(value);
        }
    }
    private static void save(){
        CRes.saveRMSString_("hso_loot_enabled",enabled?"1":"0");
        for(int i=0;i<colors.length;i++)CRes.saveRMSString_("hso_loot_color_"+i,colors[i]?"1":"0");
    }
    public static boolean accepts(int color){return color>=0 && color<colors.length && colors[color];}
    public static void stopAll(){load();enabled=false;pending.clear();save();}
    public static void update(){
        load();
        if(!enabled || GameCanvas.currentScreen!=GameCanvas.game || GameScreen.player==null || GameCanvas.loadmap==null)return;
        Player p=GameScreen.player;
        if(p.hp<=0 || p.Action==4 || Player.isFullInven || AutoSellManager.isReturningToSell || MenuUP.isGoBackActive)return;
        if(Item.VecInvetoryPlayer!=null && Player.maxInven>0 && Item.VecInvetoryPlayer.size()>=Player.maxInven)return;
        if(map!=GameCanvas.loadmap.idMap){map=GameCanvas.loadmap.idMap;pending.clear();nextSend=0L;}
        long now=mSystem.currentTimeMillis();if(now<nextSend)return;
        // Retain only live objects, including when another player picks up loot.
        java.util.Enumeration keys=pending.keys();
        while(keys.hasMoreElements()){Object obj=keys.nextElement();if(!GameScreen.Vecplayers.contains(obj))pending.remove(obj);}
        for(int i=0;i<GameScreen.Vecplayers.size();i++){
            MainObject obj=(MainObject)GameScreen.Vecplayers.elementAt(i);
            if(obj==null || obj.isRemove || obj.typeObject!=3 || !accepts(obj.colorName)
                || MainObject.getDistance(p.x,p.y,obj.x,obj.y)>=p.wFocus)continue;
            Long last=(Long)pending.get(obj);if(last!=null && now-last.longValue()<2000L)continue;
            pending.put(obj,new Long(now));nextSend=now+250L;
            GlobalService.gI().Get_Item_Map((short)obj.ID,obj.typeObject);return;
        }
    }
    public static void show(){
        load();mVector menu=new mVector("Nhat do");
        menu.addElement(new iCommand("Tự nhặt đồ: "+(enabled?"BẬT":"TẮT"),new IAction(){public void perform(){enabled=!enabled;pending.clear();save();show();}}));
        for(int i=0;i<colors.length;i++){
            final int index=i;
            menu.addElement(new iCommand(names[i]+": "+(colors[i]?"BẬT":"TẮT"),new IAction(){public void perform(){colors[index]=!colors[index];save();show();}}));
        }
        menu.addElement(new iCommand("Quay lại",new IAction(){public void perform(){MenuUP.showInventoryMenu();}}));
        GameCanvas.menu2.startAt(menu,2,"NHẶT ĐỒ THEO MÀU",false,null);
    }
}
