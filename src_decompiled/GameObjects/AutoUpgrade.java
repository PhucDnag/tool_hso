package GameObjects;

import CLib.mSystem;
import GameScreen.GameScreen;
import GameScreen.LoginScreen;
import InterfaceComponents.DataRebuildItem;
import InterfaceComponents.TabRebuildItem;
import Main.GameCanvas;
import Model.CRes;
import net.Message;
import CLib.Session_ME;
import java.io.*;

/** Continuous automated equipment enhancement with customizable delay and clover policies. */
public final class AutoUpgrade {
    public static final int NONE = 0, THREE = 1, FOUR = 2, COLOR = 3;
    public static final int MAX_LEVEL = 15;
    public static int targetLevel = 8, currency = 0, delayMs = 4000;
    // Index is the level being attempted, e.g. policy[8] is +7 -> +8.
    public static byte[] policy = new byte[MAX_LEVEL + 1];
    public static boolean active;
    public static String status = "Chưa bật";
    public static int attempts;
    public static int sentTier, runTarget, runCurrency;
    public static final int MIN_ATTEMPT_INTERVAL = 0;

    public static void applyStandardPolicy() {
        for (int i = 1; i <= MAX_LEVEL; i++) policy[i] = (byte)NONE;
        // +4 lên +5 sài 3 lá
        policy[5] = (byte)THREE;
        // +5 lên +6 sài 4 lá
        policy[6] = (byte)FOUR;
        // +8 lên +9 sài 4 lá
        policy[9] = (byte)FOUR;
        // +9 lên +10 sài 4 lá
        policy[10] = (byte)FOUR;
        // +10 lên +11 sài 4 lá
        policy[11] = (byte)FOUR;
        // +12 lên +13 là 3 lá
        policy[13] = (byte)THREE;
        // +13 +14 sài đá 3 màu (+13 -> +14 và +14 -> +15)
        policy[14] = (byte)COLOR;
        policy[15] = (byte)COLOR;
    }
    private static final int READY = 0, SELECT = 1, REMOVE = 2, ADD = 3, RESULT = 4;
    private static int state, itemId = -1, cloverId = -1, result = -1;
    private static byte[] runPolicy;
    private static long deadline, nextAction, lastAttemptAt, resultAt;
    private static Object player, screen;
    private static TabRebuildItem tab;
    private static String owner = "";

    private static String identity() {
        return GameScreen.player == null ? "" : LoginScreen.ip + ":" + GameCanvas.IndexServer + ":" + GameScreen.player.name;
    }

    private static String storage(String id) {
        return "hso_upgrade_v2_" + Integer.toHexString(id.hashCode());
    }

    private static String legacyStorage(String id) {
        return "hso_upgrade_v1_" + Integer.toHexString(id.hashCode());
    }

    public static String formatDelay(int ms) {
        if (ms % 1000 == 0) return (ms / 1000) + "s";
        int sec = ms / 1000;
        int dec = (ms % 1000) / 100;
        return sec + "." + dec + "s";
    }

    public static int parseDelaySeconds(String s) {
        if (s == null) return 4000;
        s = s.trim().replace(',', '.');
        int dot = s.indexOf('.');
        int ms = 0;
        try {
            if (dot < 0) {
                ms = Integer.parseInt(s) * 1000;
            } else {
                int sec = dot == 0 ? 0 : Integer.parseInt(s.substring(0, dot));
                String fracStr = s.substring(dot + 1);
                if (fracStr.length() > 3) fracStr = fracStr.substring(0, 3);
                while (fracStr.length() < 3) fracStr += "0";
                int frac = Integer.parseInt(fracStr);
                ms = sec * 1000 + frac;
            }
        } catch (Exception e) {
            return 4000;
        }
        if (ms < 4000) ms = 4000;
        if (ms > 30000) ms = 30000;
        return ms;
    }

    public static void loadConfig() {
        String id = identity();
        if (id.length() == 0 || id.equals(owner)) return;
        owner = id;
        targetLevel = 8;
        currency = 0;
        delayMs = 4000;
        policy = new byte[MAX_LEVEL + 1];
        applyStandardPolicy();
        try {
            byte[] bytes = CRes.loadRMS(storage(id));
            if (bytes == null) bytes = CRes.loadRMS(legacyStorage(id));
            if (bytes == null) return;
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            int ver = in.readInt();
            if ((ver != 1 && ver != 2) || !id.equals(in.readUTF())) return;
            int target = in.readUnsignedByte(), money = in.readUnsignedByte();
            byte[] choices = new byte[MAX_LEVEL + 1];
            for (int i = 1; i <= MAX_LEVEL; i++) {
                choices[i] = in.readByte();
                if (choices[i] < NONE || choices[i] > COLOR) return;
            }
            if (target < 1 || target > MAX_LEVEL || money > 1) return;
            targetLevel = target;
            currency = money;
            policy = choices;
            if (ver >= 2 && in.available() >= 2) {
                int d = in.readShort();
                if (d >= 4000 && d <= 30000) delayMs = d;
                else delayMs = 4000;
            }
        } catch (Exception ex) { /* Defaults remain usable. */ }
    }

    public static void saveConfig() {
        if (owner.length() == 0) return;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(2);
            out.writeUTF(owner);
            out.writeByte(targetLevel);
            out.writeByte(currency);
            for (int i = 1; i <= MAX_LEVEL; i++) out.writeByte(policy[i]);
            out.writeShort(delayMs);
            out.flush();
            CRes.saveRMS(storage(owner), bytes.toByteArray());
        } catch (Exception ex) { }
    }

    public static int cloverType(Item item) {
        if (item == null || item.ItemCatagory != 7 || item.typeMaterial != 11) return NONE;
        String name = MenuUP.stripAccents(item.itemName).toLowerCase();
        if (name.indexOf("3 mau") >= 0 || name.indexOf("ba mau") >= 0 || name.indexOf("tam sac") >= 0) return COLOR;
        if (name.indexOf("4 la") >= 0 || name.indexOf("bon la") >= 0) return FOUR;
        if (name.indexOf("3 la") >= 0 || name.indexOf("ba la") >= 0) return THREE;
        return NONE;
    }

    public static String cloverName(int type) {
        return type == THREE ? "Cỏ 3 lá" : type == FOUR ? "Cỏ 4 lá" : type == COLOR ? "Đá 3 màu" : "Không dùng cỏ";
    }

    public static Item clover(int type) {
        for (int i = 0; i < Item.VecInvetoryPlayer.size(); i++) {
            Item item = (Item)Item.VecInvetoryPlayer.elementAt(i);
            if (cloverType(item) == type && item.numPotion > 0) return item;
        }
        return null;
    }

    public static boolean protects(Item item) {
        return active && item != null && item.ItemCatagory == 3 && item.Id == itemId;
    }

    public static synchronized boolean start(TabRebuildItem chosen) {
        loadConfig();
        if (active) return false;
        if (chosen == null || !chosen.isEquipmentRebuild() || GameCanvas.currentScreen != GameCanvas.shopNpc
                || !GameCanvas.shopNpc.VecTabScreen.contains(chosen)
                || TabRebuildItem.itemRe == null || TabRebuildItem.itemRe.ItemCatagory != 3
                || TabRebuildItem.isBeginEff != 0) {
            stop("Mở thợ rèn, bỏ trang bị vào ô cường hóa trước.");
            return false;
        }
        if (GameScreen.player == null || GameScreen.player.hp <= 0 || MenuUP.isGoBackActive
                || AutoSellManager.isReturningToSell || AutoSellManager.getQueueCount() > 0) {
            stop("Đang GoBack/bán đồ/hồi sinh; chưa thể cường hóa.");
            return false;
        }
        itemId = TabRebuildItem.itemRe.Id;
        Item item = Item.getItemInventory(3, (short)itemId);
        if (item == null || targetLevel <= item.tier || targetLevel < 1 || targetLevel > MAX_LEVEL) {
            stop("Vật phẩm đã đạt cấp mục tiêu hoặc không còn trong túi.");
            return false;
        }
        tab = chosen;
        player = GameScreen.player;
        screen = GameCanvas.currentScreen;
        GameCanvas.shopNpc.selectTab = GameCanvas.shopNpc.VecTabScreen.indexOf(tab);
        runTarget = targetLevel;
        runCurrency = currency;
        runPolicy = new byte[policy.length];
        System.arraycopy(policy, 0, runPolicy, 0, policy.length);
        active = true;
        state = READY;
        result = -1;
        attempts = 0;
        lastAttemptAt = -MIN_ATTEMPT_INTERVAL;
        nextAction = 0L;
        TabRebuildItem.isDapnhanh = true;
        TabRebuildItem.isBeginEff = 0;
        TabRebuildItem.vecEffRe.removeAllElements();
        status = "Chuẩn bị: " + item.itemName + " -> +" + runTarget;
        return true;
    }

    public static synchronized void stop(String reason) {
        boolean running = active;
        active = false;
        state = READY;
        status = reason;
        result = -1;
        TabRebuildItem.isDapnhanh = false;
        TabRebuildItem.isBeginEff = 0;
        TabRebuildItem.isNextRebuild = 0;
        TabRebuildItem.vecEffRe.removeAllElements();
        if (itemId != -1) {
            Item currentItem = Item.getItemInventory(3, (short)itemId);
            if (currentItem != null) {
                TabRebuildItem.itemRe = MainItem.MainItem_Item(
                    currentItem.Id, currentItem.itemNameExcludeLv, currentItem.imageId,
                    currentItem.tier, currentItem.colorNameItem, currentItem.classcharItem,
                    (byte)3, currentItem.mInfo, currentItem.type_Only_Item, false,
                    currentItem.IdTem, currentItem.priceItem, (short)currentItem.LvItem,
                    currentItem.canSell, currentItem.canTrade, 0, (byte)0, (byte)0
                );
            }
        }
        if (tab != null) {
            tab.setPaintInfo();
            tab.restoreCommands();
        }
        if (running) {
            TabRebuildItem.itemDaMayMan = null;
            TabRebuildItem.isLucky = false;
        }
        GameCanvas.addInfoCharCline("Cường hóa: " + reason);
    }

    private static void send(int next, int op, int id, int category) {
        // GlobalService builds packets in a shared mutable `m` field. The
        // network callback and game loop may both use it. Own this packet so
        // another service call cannot replace an enhancement request.
        Message message = new Message(67);
        try {
            message.writer().writeByte(op);
            message.writer().writeShort(id);
            message.writer().writeByte(category);
            state = next;
            deadline = mSystem.currentTimeMillis() + 30000L;
            Session_ME.gI().sendMessage(message);
        } catch (Exception ex) {
            stop("Không gửi được lệnh cường hóa; kiểm tra kết nối.");
        } finally {
            message.cleanup();
        }
    }

    public static synchronized String progressLabel() {
        long now = mSystem.currentTimeMillis();
        if (state == READY) {
            if (AutoUpgradeMenu.isShowing && GameCanvas.menu2.isShowMenu) return "Đang mở menu";
            if (GameCanvas.currentDialog != null) return "Chờ đóng hộp thoại";
            long seconds = Math.max(0L, nextAction - now + 999L) / 1000L;
            return seconds > 0 ? "Lượt tiếp: " + seconds + "s" : "Chuẩn bị";
        }
        String label = state == RESULT ? (result == 3 ? "Chờ túi đồ" : "Chờ kết quả")
            : state == SELECT ? "Chờ chọn đồ" : state == ADD ? "Chờ thêm cỏ" : "Chờ bỏ cỏ";
        return label + " " + Math.max(0L, (now - (deadline - 30000L)) / 1000L) + "s";
    }

    public static synchronized void update() {
        if (!active) return;
        if (GameScreen.player != player || !owner.equals(identity()) || GameCanvas.currentScreen != screen
                || GameScreen.player.hp <= 0 || GameScreen.player.Action == 4
                || !GameCanvas.shopNpc.VecTabScreen.contains(tab)
                || GameCanvas.shopNpc.selectTab != GameCanvas.shopNpc.VecTabScreen.indexOf(tab)) {
            stop("Đã đóng thợ rèn, đổi nhân vật hoặc mất kết nối.");
            return;
        }
        Item item = Item.getItemInventory(3, (short)itemId);
        if (item == null) {
            stop("Vật phẩm không còn trong túi.");
            return;
        }
        if (item.tier >= runTarget) {
            stop("Đã đạt +" + item.tier + "; hoàn thành mục tiêu!");
            return;
        }
        if (TabRebuildItem.itemRe != null && TabRebuildItem.itemRe.Id != itemId) {
            stop("Đã đổi vật phẩm trong ô cường hóa.");
            return;
        }
        if (TabRebuildItem.itemRe != null && TabRebuildItem.itemRe.tier != item.tier) {
            TabRebuildItem.itemRe.tier = item.tier;
        }

        long now = mSystem.currentTimeMillis();
        if (state != READY) {
            // Never invent a tier locally. Result and inventory are separate
            // packets; success is not ready until the server's item arrives.
            boolean waitingInventory = state == RESULT && result == 3 && item.tier <= sentTier;
            if (state != RESULT || result < 0 || waitingInventory) {
                status = waitingInventory ? "Chờ cập nhật vật phẩm từ server"
                    : state == RESULT ? "Chờ kết quả lần " + attempts
                    : state == ADD ? "Chờ thêm cỏ" : state == REMOVE ? "Chờ bỏ cỏ" : "Chờ chọn vật phẩm";
                if (now >= deadline) {
                    stop(status + " quá 30 giây; chưa gửi thêm lệnh đập.");
                }
                return;
            }

            // Attempt finished: clear animation/effects immediately to prevent freezing
            TabRebuildItem.isBeginEff = 0;
            TabRebuildItem.isDapnhanh = true;
            TabRebuildItem.vecEffRe.removeAllElements();
            TabRebuildItem.isLucky = false;
            TabRebuildItem.itemDaMayMan = null;
            if (tab != null) {
                tab.restoreCommands();
            }

            if (GameCanvas.menu2.isShowMenu && !AutoUpgradeMenu.isShowing) {
                GameCanvas.menu2.doCloseMenu();
            }

            if (item.tier >= runTarget) {
                stop("Đã đạt +" + item.tier + "; hoàn thành mục tiêu!");
                return;
            }

            // The forge keeps the equipment selected after a result, just as
            // for manual enhancement. Selecting it again can leave us waiting
            // for an unnecessary acknowledgement instead of attempting again.
            state = READY;
            nextAction = resultAt + (long)delayMs;
            status = "Chờ lượt tiếp theo: +" + item.tier + " -> +" + (item.tier + 1);
            result = -1;
            TabRebuildItem.isNextRebuild = 0;
            return;
        }

        // State is READY: check if user menu is open or dialogs are up
        if (AutoUpgradeMenu.isShowing && GameCanvas.menu2.isShowMenu) return;
        AutoUpgradeMenu.isShowing = false;

        if (GameCanvas.menu2.isShowMenu) {
            GameCanvas.menu2.doCloseMenu();
        }
        if (GameCanvas.currentDialog != null) return;
        if (now < nextAction) return;

        if (item.tier < 0 || item.tier >= MAX_LEVEL || TabRebuildItem.dataRebuild == null
                || item.tier >= TabRebuildItem.dataRebuild.length) {
            stop("Chưa có dữ liệu cấp cường hóa.");
            return;
        }

        if (TabRebuildItem.itemRe == null) {
            send(SELECT, 0, itemId, 3);
            return;
        }

        int choice = runPolicy[item.tier + 1];
        Item lucky = choice == NONE ? null : clover(choice);
        if (choice != NONE && lucky == null) {
            stop("Không đủ " + cloverName(choice) + " cho cấp +" + (item.tier + 1) + "; đã dừng.");
            return;
        }
        if (TabRebuildItem.isLucky && (lucky == null || TabRebuildItem.itemDaMayMan == null
                || TabRebuildItem.itemDaMayMan.Id != lucky.Id)) {
            int removeId = TabRebuildItem.itemDaMayMan == null ? 0 : TabRebuildItem.itemDaMayMan.Id;
            send(REMOVE, 1, removeId, 7);
            return;
        }
        if (lucky != null && !TabRebuildItem.isLucky) {
            cloverId = lucky.Id;
            TabRebuildItem.itemDaMayMan = lucky;
            send(ADD, 0, lucky.Id, 7);
            return;
        }

        DataRebuildItem data = TabRebuildItem.dataRebuild[item.tier];
        if (data == null || data.mValue == null || TabRebuildItem.idMaterial == null
                || data.mValue.length > TabRebuildItem.idMaterial.length) {
            stop("Thiếu dữ liệu nguyên liệu.");
            return;
        }
        int cost = runCurrency == 0 ? data.priceCoin : data.priceGold;
        long balance = runCurrency == 0 ? GameScreen.player.coin : GameScreen.player.gold;
        if (cost <= 0 || balance < cost) {
            stop("Không đủ tiền hoặc cấp này không hỗ trợ loại tiền đã chọn.");
            return;
        }
        for (int i = 0; i < data.mValue.length; i++) {
            int need = data.mValue[i];
            if (need <= 0) continue;
            Item material = Item.getItemInventory(7, TabRebuildItem.idMaterial[i]);
            if (material == null || material.numPotion < need) {
                stop("Không đủ đá/nguyên liệu cường hóa.");
                return;
            }
        }

        sentTier = item.tier;
        lastAttemptAt = now;
        result = -1;
        attempts++;
        TabRebuildItem.isNextRebuild = 0;
        TabRebuildItem.isDapnhanh = true;
        status = "Lần " + attempts + ": +" + item.tier + " -> +" + (item.tier + 1) + " (Đích +" + runTarget + ")";
        send(RESULT, 2, 0, runCurrency);
    }

    public static synchronized void onInventory() {
        if (active || itemId != -1) {
            Item item = Item.getItemInventory(3, (short)itemId);
            if (item != null) {
                TabRebuildItem.itemRe = MainItem.MainItem_Item(
                    item.Id, item.itemNameExcludeLv, item.imageId,
                    item.tier, item.colorNameItem, item.classcharItem,
                    (byte)3, item.mInfo, item.type_Only_Item, false,
                    item.IdTem, item.priceItem, (short)item.LvItem,
                    item.canSell, item.canTrade, 0, (byte)0, (byte)0
                );
                if (tab != null) {
                    tab.setPaintInfo();
                    tab.restoreCommands();
                }
            }
        }
    }

    public static synchronized void onRebuildAck(byte op, short id, byte cat) {
        if (!active) return;
        if (op == 0) {
            if (cat == 3 && ((short)id == (short)itemId || id == -1 || id == 0)) {
                if (state == SELECT) {
                    state = READY;
                    nextAction = mSystem.currentTimeMillis() + 50L;
                }
            } else if (cat == 7 && ((short)id == (short)cloverId || id == -1 || id == 0)) {
                TabRebuildItem.isLucky = true;
                if (state == ADD) {
                    state = READY;
                    nextAction = mSystem.currentTimeMillis() + 50L;
                }
            }
        } else if (op == 1) {
            if (cat == 7) {
                TabRebuildItem.itemDaMayMan = null;
                TabRebuildItem.isLucky = false;
                if (state == REMOVE) {
                    state = READY;
                    nextAction = mSystem.currentTimeMillis() + 50L;
                }
            } else if (cat == 3) {
                stop("Đã lấy ra hoặc đổi vật phẩm cường hóa.");
            }
        }
    }

    public static synchronized void onRebuildResult(int nextRebuild, String text) {
        if (!active || state != RESULT || result >= 0) {
            TabRebuildItem.isBeginEff = 0;
            TabRebuildItem.isDapnhanh = false;
            TabRebuildItem.vecEffRe.removeAllElements();
            if (tab != null) {
                tab.restoreCommands();
            }
            return;
        }
        if (nextRebuild != 3 && nextRebuild != 4) {
            stop("Kết quả cường hóa không xác định (" + nextRebuild + ").");
            return;
        }
        result = nextRebuild;
        resultAt = mSystem.currentTimeMillis();
        deadline = resultAt + 30000L;
        // Progress stays in the forge HUD. Only stop() reports the final outcome.
        TabRebuildItem.isBeginEff = 0;
        TabRebuildItem.isDapnhanh = true;
        TabRebuildItem.vecEffRe.removeAllElements();
        TabRebuildItem.isLucky = false;
        TabRebuildItem.itemDaMayMan = null;
        if (tab != null) {
            tab.setPaintInfo();
            tab.restoreCommands();
        }
        if (GameCanvas.menu2.isShowMenu && !AutoUpgradeMenu.isShowing) {
            GameCanvas.menu2.doCloseMenu();
        }
    }

    public static synchronized void onRebuildReject(String reason) {
        if (!active) return;
        stop("Server từ chối: " + (reason == null ? "Không xác định" : reason));
    }

    public static synchronized void onRebuildClear() {
        if (!active) return;
        stop("Vật phẩm đã bị xóa khỏi ô cường hóa.");
    }

    // Peek a complete reply, then restore the stream for the unmodified parser.
    public static int[] peekReply(Message message) {
        if (message == null || message.reader() == null) return null;
        DataInputStream in = message.reader();
        in.mark(65536);
        try {
            int op = in.readByte(), id = -1, cat = -1, value = -1;
            if (op == 0) { id = in.readShort(); cat = in.readByte(); in.readUTF(); }
            else if (op == 1) { cat = in.readByte(); in.readUTF(); }
            else if (op == 2 || op == 5) { value = in.readByte(); in.readUTF(); }
            else if (op == 3) in.readUTF();
            else if (op == 6) { }
            else return null;
            return new int[]{op, id, cat, value};
        } catch (Exception ex) {
            return null;
        } finally {
            try { in.reset(); } catch (Exception ex) { }
        }
    }

    public static synchronized void onReply(int[] reply) {
        if (!active || reply == null) return;
        int op = reply[0], id = reply[1], cat = reply[2], val = reply[3];
        if (op == 3) {
            onRebuildReject("Server từ chối cường hóa; xem thông báo của game.");
            return;
        }
        if (op == 6) {
            onRebuildClear();
            return;
        }
        if (op == 1 && cat == 3 || (op == 0 && cat == 3 && (short)id != (short)itemId && id != -1 && id != 0)) {
            stop("Đã lấy ra hoặc đổi vật phẩm cường hóa.");
            return;
        }
        if (op == 0) {
            onRebuildAck((byte)0, (short)id, (byte)cat);
        } else if (op == 1) {
            onRebuildAck((byte)1, (short)id, (byte)cat);
        } else if (op == 2 || op == 5) {
            onRebuildResult(val, "");
        }
    }
}
