package GameObjects;

import CLib.*;
import GameScreen.GameScreen;
import GameScreen.LoginScreen;
import GameScreen.LoadMapScreen;
import Main.GameCanvas;
import InterfaceComponents.*;
import Model.CRes;
import Thread_More.LoadMap;
import net.Message;
import java.io.*;

/** Farming consumables and paid zone entry, driven by server replies. */
public final class AutoFarmSupport {
    public static boolean zoneEnabled, fireEnabled, oilEnabled, bookEnabled;
    public static int payment = 2, fallback = 0; // 0 gems, 1 tickets, 2 tickets then gems; areas zero-based
    public static int fireMinutes = 30, oilMinutes = 30, bookMinutes = 30;
    public static String status = "Chưa bật";
    private static int[] ids = {-1, -1, -1}, cats = {7, 7, 7};
    private static long[] nextUse = {0L, 0L, 0L};
    private static long fireUntil;
    private static int fireMap=-1, fireArea=-1;
    private static String owner = "";
    private static long blockedDay = -1L, retryAt, deadline, consumableDeadline;
    private static int requestedArea = -1, requestMap = -1, paidKind = -1;
    private static int pendingUse = -1, pendingCount, pendingMap, pendingArea;
    private static boolean gemFallback, awaitingConfirm, manualPause;
    private static iCommand queuedCommand;
    private static int queuedNpc, queuedMenu, queuedIndex = -1;

    private static long now() { return mSystem.currentTimeMillis(); }
    private static long day() { return (now() + 25200000L) / 86400000L; }
    private static String clean(String value) { return value == null ? "" : MenuUP.stripAccents(value).toLowerCase(); }
    private static boolean has(String s, String part) { return s.indexOf(part) >= 0; }
    private static String key() { return "hso_farm_" + Integer.toHexString(owner.hashCode()); }

    public static synchronized void load() {
        if (GameScreen.player == null) return;
        String who = LoginScreen.ip + ":" + GameCanvas.IndexServer + ":" + GameScreen.player.name;
        if (who.equals(owner)) return;
        owner = who;
        zoneEnabled = fireEnabled = oilEnabled = bookEnabled = false;
        payment = 2; fallback = 0; fireMinutes = oilMinutes = bookMinutes = 30;
        ids = new int[]{-1,-1,-1}; cats = new int[]{7,7,7}; nextUse = new long[]{0L,0L,0L};
        fireUntil=0; fireMap=fireArea=-1; blockedDay = -1; retryAt = 0; pendingUse = -1; resetRequest(); manualPause = false;
        try {
            byte[] data = CRes.loadRMS(key()); if (data == null) return;
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            if (in.readInt() != 2 || !owner.equals(in.readUTF())) return;
            zoneEnabled = in.readBoolean(); fireEnabled = in.readBoolean(); oilEnabled = in.readBoolean(); bookEnabled=in.readBoolean();
            payment = in.readInt(); fallback = in.readInt(); fireMinutes = in.readInt(); oilMinutes = in.readInt(); bookMinutes=in.readInt();
            for (int i=0;i<3;i++) { ids[i]=in.readInt(); cats[i]=in.readInt(); nextUse[i]=in.readLong(); }
            blockedDay = in.readLong();
            if (payment<0 || payment>2) payment=2;
            if (fallback<0 || fallback==1 || fallback>126) fallback=0;
            if (fireMinutes<1 || fireMinutes>120) fireMinutes=30;
            if (oilMinutes<1 || oilMinutes>120) oilMinutes=30;
            if (bookMinutes<1 || bookMinutes>120) bookMinutes=30;
        } catch (Exception e) { zoneEnabled=fireEnabled=oilEnabled=bookEnabled=false; }
    }

    public static synchronized void save() {
        if (owner.length()==0) return;
        try {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream(); DataOutputStream out=new DataOutputStream(bytes);
            out.writeInt(2); out.writeUTF(owner);
            out.writeBoolean(zoneEnabled); out.writeBoolean(fireEnabled); out.writeBoolean(oilEnabled); out.writeBoolean(bookEnabled);
            out.writeInt(payment); out.writeInt(fallback); out.writeInt(fireMinutes); out.writeInt(oilMinutes); out.writeInt(bookMinutes);
            for(int i=0;i<3;i++) {out.writeInt(ids[i]);out.writeInt(cats[i]);out.writeLong(nextUse[i]);}
            out.writeLong(blockedDay);out.flush(); CRes.saveRMS(key(),bytes.toByteArray());
        } catch(Exception e) { }
    }

    public static synchronized void stopAll() {
        load();
        zoneEnabled=fireEnabled=oilEnabled=bookEnabled=false;
        resetRequest();
        pendingUse=-1;
        manualPause=true;
        status="Đã tắt";
        save();
    }

    private static void resetRequest() {
        requestedArea=-1; paidKind=-1; awaitingConfirm=false;
        queuedCommand=null; queuedIndex=-1;
    }
    public static int goBackArea(int configured) { load(); return zoneEnabled ? -1 : configured; }
    private static boolean farming() {
        return GameScreen.player!=null && GameScreen.player.hp>0 && GameScreen.player.Action!=4
            && GameCanvas.currentScreen==GameCanvas.game && LoadMapScreen.isNextMap
            && GameCanvas.loadmap!=null && !GameScreen.isMapLang && !MenuUP.isGoBackActive
            && !AutoSellManager.isReturningToSell && !AutoUpgrade.active
            && (MenuUP.isAutoAttack || Player.isAutoFire==1)
            && (MenuUP.farmMapId<0 || GameCanvas.loadmap.idMap==MenuUP.farmMapId);
    }
    private static void area(int area) {
        requestedArea=area; requestMap=GameCanvas.loadmap.idMap; deadline=now()+15000L;
        paidKind=-1; awaitingConfirm=false;
        send(51,area,-1,-1);
        status="Chờ vào khu " +(area+1);
    }
    private static void send(int command,int a,int b,int c) {
        Message message=new Message(command);
        try {
            if(command==-30) {message.writer().writeShort(a);message.writer().writeByte(b);message.writer().writeByte(c);}
            else if(command==51) message.writer().writeByte(a);
            else message.writer().writeShort(a);
            Session_ME.gI().sendMessage(message);
        } catch(Exception e) {status="Không gửi được lệnh; đã tạm dừng";manualPause=true;resetRequest();}
        finally {message.cleanup();}
    }

    public static synchronized void update() {
        load();
        // Loading/death/dialog transitions do not prove whether a paid request
        // completed. Keep it pending until arrival, refusal, or timeout.
        if(!farming()) return;
        long time=now();
        if(requestedArea>=0 && requestMap!=GameCanvas.loadmap.idMap) {
            if(paidKind>=0) {manualPause=true;status="Đổi map khi đang trả phí; kiểm tra rồi bật lại";}
            resetRequest();
        }
        if(requestedArea>=0 && LoadMap.Area==requestedArea) {
            status="Đang UP khu "+(LoadMap.Area+1); resetRequest(); gemFallback=false;
        }
        if(queuedCommand!=null || queuedIndex>=0) {
            if(requestedArea<0 || time>deadline) {resetRequest();return;}
            iCommand command=queuedCommand; int index=queuedIndex;
            queuedCommand=null;queuedIndex=-1;
            GameCanvas.end_Dialog();GameCanvas.menu2.doCloseMenu();
            if(command!=null) command.perform(); else send(-30,queuedNpc,queuedMenu,index);
            return;
        }
        if(requestedArea>=0) {
            if(time>=deadline) {
                // A payment could already have succeeded: never blindly pay again.
                boolean paid=paidKind>=0, failedFallback=requestedArea!=1;
                resetRequest();retryAt=time+600000L;manualPause=paid || failedFallback;
                status=paid ? "Chưa rõ kết quả trả phí; kiểm tra rồi bật lại"
                    : failedFallback ? "Khu dự phòng không phản hồi; đã tạm dừng" : "Khu 2 không phản hồi; dùng khu dự phòng";
            } else return;
        }
        if(manualPause || GameCanvas.currentDialog!=null || GameCanvas.menu2.isShowMenu) return;
        if(zoneEnabled) {
            int target=blockedDay==day() || time<retryAt ? fallback : 1;
            if(LoadMap.Area!=target) {
                if(target!=1 && time<retryAt-570000L) return;
                area(target);return;
            }
        }
        useConsumables(time);
    }

    private static void useConsumables(long time) {
        if(pendingUse>=0) {
            int slot=pendingUse;Item item=Item.getItemInventory(cats[slot],(short)ids[slot]);
            if(item==null || item.numPotion<pendingCount) {
                if(slot==0) {fireUntil=nextUse[0];fireMap=pendingMap;fireArea=pendingArea;}
                pendingUse=-1;save();status="Đã dùng "+itemName(slot);
            } else if(time>=consumableDeadline) {
                if(slot==0) fireEnabled=false; else if(slot==1) oilEnabled=false; else bookEnabled=false;
                pendingUse=-1;save();status="Chưa xác nhận dùng vật phẩm; đã tắt mục đó";
            }
            return;
        }
        for(int i=0;i<3;i++) {
            if(!(i==0?fireEnabled:i==1?oilEnabled:bookEnabled) || time<nextUse[i]) continue;
            if(i==1 && (time>=fireUntil || fireMap!=GameCanvas.loadmap.idMap || fireArea!=LoadMap.Area)) continue;
            Item item=findItem(i);
            if(item==null || item.numPotion<=0) {nextUse[i]=time+1000L;status="Chưa có "+itemName(i);continue;}
            if(item.ItemCatagory!=4 && item.ItemCatagory!=7) continue;
            // Native inventory uses command 32 for potions, -107 for materials.
            pendingUse=i;pendingCount=item.numPotion;consumableDeadline=time+15000L;
            pendingMap=GameCanvas.loadmap.idMap;pendingArea=LoadMap.Area;
            nextUse[i]=time+(long)(i==0?fireMinutes:i==1?oilMinutes:bookMinutes)*60000L;
            save();send(item.ItemCatagory==4?32:-107,item.Id,-1,-1);
            status="Chờ dùng "+itemName(i);return;
        }
    }

    private static String itemName(int slot) {return slot==0?"Lửa thiêng":slot==1?"Bình dầu":"Sách tri thức";}
    private static Item findItem(int slot) {
        Item chosen=Item.getItemInventory(cats[slot],(short)ids[slot]);
        if(chosen!=null && chosen.numPotion>0) return chosen;
        String expected=clean(itemName(slot));
        for(int i=0;i<Item.VecInvetoryPlayer.size();i++) {
            Item item=(Item)Item.VecInvetoryPlayer.elementAt(i);
            if((item.ItemCatagory==4 || item.ItemCatagory==7) && item.numPotion>0 && clean(item.itemName).equals(expected)) {
                ids[slot]=item.Id;cats[slot]=item.ItemCatagory;return item;
            }
        }
        return null;
    }

    private static boolean zoneText(String s) {
        int at=s.indexOf("khu 2"),end=at+5;
        boolean exact=at>=0 && (end==s.length() || s.charAt(end)<'0' || s.charAt(end)>'9');
        return exact || has(s,"khu hai") || has(s,"khu vip");
    }
    private static boolean ticket(String s) {
        return has(s,"tyche") || has(" "+s+" "," ve ");
    }
    private static boolean gem(String s) {return has(s,"ngoc") || has(s,"gem") || has(s,"ruby");}
    private static boolean refusal(String s) {
        return has(s,"khong du") || has(s,"khong co") || has(s,"da het") || has(s,"het thoi gian")
            || has(s,"toi da") || has(s,"gioi han") || has(s,"khong the vao") || has(s,"khu day") || has(s,"bi khoa");
    }
    private static boolean relevant() {
        return zoneEnabled && requestedArea==1 && now()<deadline && farming()
            && requestMap==GameCanvas.loadmap.idMap;
    }
    private static boolean reject(String s) {
        if(!refusal(s)) return false;
        if(has(s,"6 gio") || has(s,"6 tieng") || has(s,"trong ngay") || has(s,"hom nay")) {
            blockedDay=day();save();status="Hết giờ khu 2 hôm nay; chuyển khu "+(fallback+1);
        } else if(payment==2 && paidKind==1 && ticket(s) && (has(s,"khong du") || has(s,"khong co"))) {
            gemFallback=true;resetRequest();retryAt=0;status="Hết vé; chuyển sang ngọc";return true;
        } else {retryAt=now()+600000L;status="Khu 2 bị từ chối; dùng khu "+(fallback+1);}
        resetRequest();return true;
    }
    private static boolean unavailablePayment() {
        retryAt=now()+600000L;resetRequest();
        status="Không có cách trả phí đã chọn; dùng khu "+(fallback+1);
        return true;
    }
    // Never choose a payment by a hard-coded index; use server-provided captions.
    public static int paymentChoice(String text,mVector options) {
        int tickets=-1,gems=-1;
        if(options==null) return -1;
        for(int i=0;i<options.size();i++) {
            String s=clean(((iCommand)options.elementAt(i)).caption);
            if(ticket(s) && !gem(s)) tickets=i;
            if(gem(s) && !ticket(s)) gems=i;
        }
        if(payment==0 || gemFallback) return gems;
        if(payment==1) return tickets;
        return tickets>=0?tickets:gems;
    }
    public static synchronized boolean dialog(String text,mVector options) {
        load();
        if(!relevant()) return false;
        String s=clean(text);
        if(!zoneText(s) && !(refusal(s) && (has(s,"6 gio") || has(s,"6 tieng")
                || paidKind==1 && ticket(s)))) return false;
        if(reject(s)) return true;
        int index=paymentChoice(s,options);
        if(index<0 && options!=null && gem(s)!=ticket(s)) {
            boolean allowed=gem(s)?payment!=1:payment!=0 && !gemFallback;
            if(!allowed) return unavailablePayment();
            if(allowed) for(int i=0;i<options.size();i++) {
                String label=clean(((iCommand)options.elementAt(i)).caption);
                if(label.equals("ok") || label.equals("dong y") || label.equals("co")) {index=i;break;}
            }
        }
        if(index<0 || paidKind>=0 && !awaitingConfirm) {manualPause=true;status="Cần chọn xác nhận khu 2 trong game";return false;}
        String label=clean(((iCommand)options.elementAt(index)).caption);
        int kind=gem(label) || !ticket(label) && gem(s)?0:1;
        if(paidKind>=0 && paidKind!=kind) return false;
        paidKind=kind;awaitingConfirm=false;queuedCommand=(iCommand)options.elementAt(index);
        return true;
    }
    public static synchronized boolean dynamic(int npc,int menu,String title,mVector options) {
        load();
        if(options==null) return false;
        if(!relevant()) return false;
        String s=clean(title);String context=s;
        for(int i=0;i<options.size();i++) context+=" "+clean(((iCommand)options.elementAt(i)).caption);
        if(!zoneText(context)) return false;
        if(reject(s)) return true;
        if(paidKind>=0) return false;
        int index=paymentChoice(s,options);
        if(index<0) {
            for(int i=0;i<options.size();i++) {
                String label=clean(((iCommand)options.elementAt(i)).caption);
                if(gem(label) || ticket(label)) return unavailablePayment();
            }
            manualPause=true;status="Chưa nhận diện cách trả phí khu 2";return false;
        }
        paidKind=gem(clean(((iCommand)options.elementAt(index)).caption))?0:1;
        awaitingConfirm=true;queuedNpc=npc;queuedMenu=menu;queuedIndex=index;return true;
    }

    private static String paymentName() {return payment==0?"Ngọc":payment==1?"Chỉ vé":"Vé trước, ngọc sau";}
    public static void show() {
        load();mVector menu=new mVector("Ho tro UP");
        menu.addElement(new iCommand("UP khu 2: "+(zoneEnabled?"Bật":"Tắt"),new IAction(){public void perform(){
            zoneEnabled=!zoneEnabled;manualPause=false;gemFallback=false;resetRequest();retryAt=0;save();show();}}));
        menu.addElement(new iCommand("Trả phí: "+paymentName(),new IAction(){public void perform(){
            payment=(payment+1)%3;resetRequest();manualPause=false;save();show();}}));
        menu.addElement(new iCommand("Khu dự phòng: "+(fallback+1),new IAction(){public void perform(){showAreas();}}));
        menu.addElement(new iCommand("Đốt lửa: "+(fireEnabled?"Bật":"Tắt"),new IAction(){public void perform(){
            fireEnabled=!fireEnabled;save();show();}}));
        menu.addElement(new iCommand("Vật phẩm lửa / "+fireMinutes+" phút >",new IAction(){public void perform(){showItem(0);}}));
        menu.addElement(new iCommand("Bình dầu: "+(oilEnabled?"Bật":"Tắt"),new IAction(){public void perform(){
            oilEnabled=!oilEnabled;save();show();}}));
        menu.addElement(new iCommand("Vật phẩm dầu / "+oilMinutes+" phút >",new IAction(){public void perform(){showItem(1);}}));
        menu.addElement(new iCommand("Sách tri thức: "+(bookEnabled?"Bật":"Tắt"),new IAction(){public void perform(){
            bookEnabled=!bookEnabled;save();show();}}));
        menu.addElement(new iCommand("Sách / "+bookMinutes+" phút >",new IAction(){public void perform(){showItem(2);}}));
        menu.addElement(new iCommand(status,new IAction(){public void perform(){show();}}));
        menu.addElement(new iCommand("Quay lại",new IAction(){public void perform(){MenuUP.showMenu();}}));
        GameCanvas.menu2.startAt(menu,2,"HỖ TRỢ UP",false,null);
    }
    private static void showAreas() {
        mVector menu=new mVector("Khu du phong");
        int max=LoadMap.MaxArea>0?LoadMap.MaxArea:10;
        for(int i=0;i<max;i++) {if(i==1)continue;final int area=i;
            menu.addElement(new iCommand("Khu "+(i+1),new IAction(){public void perform(){fallback=area;save();show();}}));}
        menu.addElement(new iCommand("Quay lại",new IAction(){public void perform(){show();}}));
        GameCanvas.menu2.startAt(menu,2,"KHU DỰ PHÒNG",false,null);
    }
    private static void showItem(final int slot) {
        mVector menu=new mVector("Vat pham");
        menu.addElement(new iCommand("Chu kỳ sử dụng >",new IAction(){public void perform(){showIntervals(slot);}}));
        for(int i=0;i<Item.VecInvetoryPlayer.size();i++) {
            final Item item=(Item)Item.VecInvetoryPlayer.elementAt(i);
            if((item.ItemCatagory!=4 && item.ItemCatagory!=7) || item.numPotion<=0)continue;
            menu.addElement(new iCommand((ids[slot]==item.Id && cats[slot]==item.ItemCatagory?"[Chọn] ":"")+item.itemName,
                new IAction(){public void perform(){ids[slot]=item.Id;cats[slot]=item.ItemCatagory;save();show();}}));
        }
        menu.addElement(new iCommand("Quay lại",new IAction(){public void perform(){show();}}));
        GameCanvas.menu2.startAt(menu,2,"CHỌN "+itemName(slot).toUpperCase(),false,null);
    }
    private static void showIntervals(final int slot) {
        mVector menu=new mVector("Chu ky");int[] times={1,5,10,15,30,60,120};
        for(int i=0;i<times.length;i++){final int minutes=times[i];
            menu.addElement(new iCommand(minutes+" phút",new IAction(){public void perform(){
                if(slot==0)fireMinutes=minutes;else if(slot==1)oilMinutes=minutes;else bookMinutes=minutes;save();showItem(slot);}}));}
        menu.addElement(new iCommand("Quay lại",new IAction(){public void perform(){showItem(slot);}}));
        GameCanvas.menu2.startAt(menu,2,"CHU KỲ THEO THỜI HẠN VẬT PHẨM",false,null);
    }
}
