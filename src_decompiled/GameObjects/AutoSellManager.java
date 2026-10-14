package GameObjects;

import CLib.mSystem;
import CLib.mVector;
import GameScreen.GameScreen;
import Main.GameCanvas;
import InterfaceComponents.iCommand;
import Model.CRes;
import netcommand.global.GlobalService;

public class AutoSellManager {
    public static boolean isAutoSellEnabled = true;
    public static boolean isSellWhite = true;      // Phẩm chất 0: Đồ trắng
    public static boolean isSellGreen = true;      // Phẩm chất 1: Đồ xanh lá
    public static boolean isSellBlue = false;      // Phẩm chất 2: Đồ xanh dương
    public static boolean isSellYellow = false;    // Phẩm chất 3: Đồ vàng
    public static boolean isSellPurple = false;    // Phẩm chất 4: Đồ tím
    public static boolean isSellOrange = false;    // Phẩm chất 5: Đồ cam
    public static boolean isSellLocked = false;    // Bán đồ đã khóa (mặc định TẮT)
    public static boolean isSellUpgraded = false;  // Bán đồ đã cường hóa / cộng (mặc định TẮT)
    public static long delaySellMs = 1000L;        // Delay bán mỗi món (mặc định 1000ms = 1s, tối thiểu 1s)
    public static int minEmptySlots = 2;           // Số ô trống hành trang để tự động về làng bán đồ (0 = TẮT, 1, 2, 3, 4, 5, 7, 10)
    public static int returnMethod = 0;            // 0: Tự thoát vào lại (Relog) - MẶC ĐỊNH; 1: Đi bộ GoBack
    public static boolean isReturningToSell = false; // Cờ đang trong tiến trình về làng bán đồ
    private static long lastDeleteSentTime = 0L;   // Thời điểm gửi lệnh delete_Item gần nhất

    public static final long[] DELAY_STEPS = new long[]{1000L, 1200L, 1500L, 2000L, 2500L, 3000L};
    public static final int[] MIN_SLOT_STEPS = new int[]{0, 1, 2, 3, 4, 5, 7, 10};

    private static mVector queueSell = new mVector();
    private static long lastSellTime = 0L;
    private static int totalSoldSession = 0;
    private static long lastAutoScanTime = 0L;
    private static long lastClineTime = 0L;
    private static boolean isConfigLoaded = false;

    public static void logInfo(String msg) {
        if (msg == null || msg.length() == 0) return;
        long now = mSystem.currentTimeMillis();
        if (now - lastClineTime >= 3000L) {
            lastClineTime = now;
            GameCanvas.addInfoCharCline(msg);
        }
    }

    public static void loadConfig() {
        if (isConfigLoaded) {
            return;
        }
        isConfigLoaded = true;
        try {
            String sEn = CRes.loadRMSString("hso_up_autosell");
            if (sEn != null && sEn.trim().length() > 0) {
                isAutoSellEnabled = sEn.trim().equals("1");
            }
            String sW = CRes.loadRMSString("hso_up_sell_white");
            if (sW != null && sW.trim().length() > 0) {
                isSellWhite = sW.trim().equals("1");
            }
            String sG = CRes.loadRMSString("hso_up_sell_green");
            if (sG != null && sG.trim().length() > 0) {
                isSellGreen = sG.trim().equals("1");
            }
            String sB = CRes.loadRMSString("hso_up_sell_blue");
            if (sB != null && sB.trim().length() > 0) {
                isSellBlue = sB.trim().equals("1");
            }
            String sY = CRes.loadRMSString("hso_up_sell_yellow");
            if (sY != null && sY.trim().length() > 0) {
                isSellYellow = sY.trim().equals("1");
            }
            String sP = CRes.loadRMSString("hso_up_sell_purple");
            if (sP != null && sP.trim().length() > 0) {
                isSellPurple = sP.trim().equals("1");
            }
            String sO = CRes.loadRMSString("hso_up_sell_orange");
            if (sO != null && sO.trim().length() > 0) {
                isSellOrange = sO.trim().equals("1");
            }
            String sLk = CRes.loadRMSString("hso_up_sell_locked");
            if (sLk != null && sLk.trim().length() > 0) {
                isSellLocked = sLk.trim().equals("1");
            }
            String sUp = CRes.loadRMSString("hso_up_sell_upgraded");
            if (sUp != null && sUp.trim().length() > 0) {
                isSellUpgraded = sUp.trim().equals("1");
            }
            String sDel = CRes.loadRMSString("hso_up_sell_delay");
            if (sDel != null && sDel.trim().length() > 0) {
                try {
                    delaySellMs = Long.parseLong(sDel.trim());
                } catch (Exception ex) {
                    // empty catch block
                }
            }
            if (delaySellMs < 1000L) {
                delaySellMs = 1000L; // Mặc định tối thiểu 1s
            }
            String sSlots = CRes.loadRMSString("hso_up_sell_min_slots");
            if (sSlots != null && sSlots.trim().length() > 0) {
                try {
                    minEmptySlots = Integer.parseInt(sSlots.trim());
                } catch (Exception ex) {
                    // empty catch block
                }
            }
            String sRet = CRes.loadRMSString("hso_up_sell_ret_method");
            if (sRet != null && sRet.trim().length() > 0) {
                try {
                    returnMethod = Integer.parseInt(sRet.trim());
                    if (returnMethod < 0 || returnMethod > 1) {
                        returnMethod = 0;
                    }
                } catch (Exception ex) {
                    // empty catch block
                }
            }
        } catch (Exception ex) {
            // empty catch block
        }
    }

    public static void saveConfig() {
        try {
            CRes.saveRMSString_("hso_up_autosell", isAutoSellEnabled ? "1" : "0");
            CRes.saveRMSString_("hso_up_sell_white", isSellWhite ? "1" : "0");
            CRes.saveRMSString_("hso_up_sell_green", isSellGreen ? "1" : "0");
            CRes.saveRMSString_("hso_up_sell_blue", isSellBlue ? "1" : "0");
            CRes.saveRMSString_("hso_up_sell_yellow", isSellYellow ? "1" : "0");
            CRes.saveRMSString_("hso_up_sell_purple", isSellPurple ? "1" : "0");
            CRes.saveRMSString_("hso_up_sell_orange", isSellOrange ? "1" : "0");
            CRes.saveRMSString_("hso_up_sell_locked", isSellLocked ? "1" : "0");
            CRes.saveRMSString_("hso_up_sell_upgraded", isSellUpgraded ? "1" : "0");
            CRes.saveRMSString_("hso_up_sell_delay", String.valueOf(delaySellMs));
            CRes.saveRMSString_("hso_up_sell_min_slots", String.valueOf(minEmptySlots));
            CRes.saveRMSString_("hso_up_sell_ret_method", String.valueOf(returnMethod));
        } catch (Exception ex) {
            // empty catch block
        }
    }

    /**
     * Safe Filter Matrix: Kiểm tra xem một vật phẩm có phải là đồ rác được phép bán không
     */
    public static boolean isTrashItem(MainItem item) {
        if (AutoUpgrade.protects(item)) return false;
        if (item == null || item.setNameNull()) {
            return false;
        }
        if (item.itemName == null || item.itemName.length() == 0) {
            return false;
        }
        if (item.Id < 0) {
            return false;
        }
        // Chỉ bán trang bị (ItemCatagory == 3)
        if (item.ItemCatagory != 3) {
            return false;
        }
        if (AutoPetFeed.reserves(item)) {
            return false;
        }
        // Kiểm tra đồ đã khóa
        if (item.isLock == 1 && !isSellLocked) {
            return false;
        }
        // Kiểm tra đồ đã cường hóa / cộng (tier > 0)
        if (item.tier > 0 && !isSellUpgraded) {
            return false;
        }
        // Tuyệt đối không bán đồ đang trang bị trên người
        if (Item.VecEquipPlayer != null && Item.VecEquipPlayer.contains(item)) {
            return false;
        }
        // Kiểm tra phẩm chất màu (colorNameItem)
        if (item.colorNameItem == 0 && isSellWhite) {
            return true;
        }
        if (item.colorNameItem == 1 && isSellGreen) {
            return true;
        }
        if (item.colorNameItem == 2 && isSellBlue) {
            return true;
        }
        if (item.colorNameItem == 3 && isSellYellow) {
            return true;
        }
        if (item.colorNameItem == 4 && isSellPurple) {
            return true;
        }
        if (item.colorNameItem == 5 && isSellOrange) {
            return true;
        }
        return false;
    }

    /**
     * Quét toàn bộ hành trang và đưa các món rác vào hàng đợi bán
     */
    public static void scanAndEnqueueTrash() {
        if (queueSell.size() > 0) {
            return; // Đang trong tiến trình bán dở dang
        }
        if (Item.VecInvetoryPlayer == null || Item.VecInvetoryPlayer.size() == 0) {
            return;
        }
        for (int i = 0; i < Item.VecInvetoryPlayer.size(); i++) {
            Object obj = Item.VecInvetoryPlayer.elementAt(i);
            if (obj instanceof MainItem) {
                MainItem item = (MainItem) obj;
                if (isTrashItem(item)) {
                    if (!queueSell.contains(item)) {
                        queueSell.addElement(item);
                    }
                }
            }
        }
    }

    public static boolean hasAnyTrashItem() {
        if (Item.VecInvetoryPlayer == null) return false;
        for (int i = 0; i < Item.VecInvetoryPlayer.size(); i++) {
            Object obj = Item.VecInvetoryPlayer.elementAt(i);
            if (obj instanceof MainItem) {
                if (isTrashItem((MainItem)obj)) return true;
            }
        }
        return false;
    }

    public static boolean isRecentlySelling() {
        return mSystem.currentTimeMillis() - lastDeleteSentTime < 3000L;
    }

    public static boolean isSellDialogText(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        String unaccented = MenuUP.stripAccents(text).toLowerCase();
        boolean hasSell = unaccented.indexOf("muon ban") >= 0 || lower.indexOf("mu\u1ed1n b\u00e1n") >= 0 
                || unaccented.indexOf("vender") >= 0 || unaccented.indexOf("menjual") >= 0 
                || unaccented.indexOf("sell") >= 0 || (lower.indexOf("b\u00e1n") >= 0 && (lower.indexOf("\u00e1o") >= 0 || lower.indexOf("quan") >= 0 || lower.indexOf("kiem") >= 0 || lower.indexOf("vu khi") >= 0 || lower.indexOf("nhan") >= 0));
        boolean hasPrice = unaccented.indexOf("gia") >= 0 || lower.indexOf("gi\u00e1") >= 0 
                || unaccented.indexOf("price") >= 0 || unaccented.indexOf("vang") >= 0 
                || lower.indexOf("v\u00e0ng") >= 0 || unaccented.indexOf("gold") >= 0 
                || unaccented.indexOf("coin") >= 0 || unaccented.indexOf("xu") >= 0;
        return hasSell && hasPrice;
    }

    /**
     * Tự động phản hồi và xác nhận hộp thoại bán đồ từ Server (Dialog_server)
     */
    public static boolean handleDialogServer(short idDialog, byte typeDialog, String text) {
        if (text == null) return false;
        if (isSellDialogText(text) || (isRecentlySelling() && (text.indexOf("b\u00e1n") >= 0 || text.indexOf("ban") >= 0))) {
            GlobalService.gI().dialog_Server(idDialog, typeDialog, (byte)1);
            GameCanvas.end_Dialog();
            return true;
        }
        return false;
    }

    /**
     * Tự động chọn OK khi hộp thoại Select Dialog hỏi xác nhận bán
     */
    public static boolean handleSelectDialog(String text, mVector options) {
        if (text == null) return false;
        if (isSellDialogText(text) || (isRecentlySelling() && (text.indexOf("b\u00e1n") >= 0 || text.indexOf("ban") >= 0))) {
            if (options != null && options.size() > 0) {
                for (int i = 0; i < options.size(); i++) {
                    iCommand cmd = (iCommand) options.elementAt(i);
                    if (cmd != null && ((cmd.caption != null && (cmd.caption.equalsIgnoreCase("ok") || cmd.caption.equalsIgnoreCase("co"))) || cmd.subIndex == 1)) {
                        cmd.perform();
                        GameCanvas.end_Dialog();
                        return true;
                    }
                }
                iCommand cmd0 = (iCommand) options.elementAt(0);
                if (cmd0 != null) {
                    cmd0.perform();
                    GameCanvas.end_Dialog();
                    return true;
                }
            }
            GameCanvas.end_Dialog();
            return true;
        }
        return false;
    }

    /**
     * Tự động chọn OK khi hộp thoại Single Dialog hỏi xác nhận bán
     */
    public static boolean handleSingleDialog(String text, iCommand cmd) {
        if (text == null) return false;
        String lower = MenuUP.stripAccents(text).toLowerCase();
        if (lower.indexOf("muon ban") >= 0 && lower.indexOf("gia") >= 0) {
            if (cmd != null) cmd.perform();
            GameCanvas.end_Dialog();
            return true;
        }
        return false;
    }

    /**
     * Tìm NPC bán đồ thích hợp nhất trong map làng hiện tại (Hammer, Lisa, Doubar, hoặc NPC gần nhất)
     */
    public static MainObject findShopNpc() {
        if (GameScreen.Vecplayers == null || GameScreen.player == null) {
            return null;
        }
        MainObject bestNpc = null;
        int bestScore = -1;
        int bestDist = 999999;
        Player p = GameScreen.player;

        for (int i = 0; i < GameScreen.Vecplayers.size(); i++) {
            MainObject obj = (MainObject) GameScreen.Vecplayers.elementAt(i);
            if (obj == null || obj.typeObject != 2) {
                continue; // Chỉ xét NPC (typeObject == 2)
            }
            String name = obj.name != null ? MenuUP.stripAccents(obj.name).toLowerCase() : "";
            String gt = obj.nameGiaotiep != null ? MenuUP.stripAccents(obj.nameGiaotiep).toLowerCase() : "";
            int score = 10;
            // Ưu tiên các NPC thợ rèn, bán đồ, tạp hóa
            if (name.indexOf("hammer") >= 0 || gt.indexOf("hammer") >= 0
                    || name.indexOf("tho ren") >= 0 || gt.indexOf("tho ren") >= 0
                    || name.indexOf("ren") >= 0 || gt.indexOf("ren") >= 0) {
                score = 100;
            } else if (name.indexOf("lisa") >= 0 || gt.indexOf("lisa") >= 0
                    || name.indexOf("tap hoa") >= 0 || gt.indexOf("tap hoa") >= 0
                    || name.indexOf("shop") >= 0 || gt.indexOf("shop") >= 0
                    || name.indexOf("ban do") >= 0 || gt.indexOf("ban do") >= 0) {
                score = 90;
            } else if (name.indexOf("doubar") >= 0 || gt.indexOf("doubar") >= 0
                    || name.indexOf("alisabeth") >= 0 || gt.indexOf("alisabeth") >= 0
                    || name.indexOf("aman") >= 0 || gt.indexOf("aman") >= 0
                    || name.indexOf("zulu") >= 0 || gt.indexOf("zulu") >= 0) {
                score = 80;
            }

            int dist = MainObject.getDistance(p.x, p.y, obj.x, obj.y);
            if (score > bestScore || (score == bestScore && dist < bestDist)) {
                bestScore = score;
                bestDist = dist;
                bestNpc = obj;
            }
        }
        return bestNpc;
    }

    /**
     * Dẫn nhân vật di chuyển tới đứng cạnh NPC (cách khoảng 28px)
     */
    public static void moveToNpc(MainObject npc) {
        if (npc == null || GameScreen.player == null) return;
        Player p = GameScreen.player;
        int targetX = p.x < npc.x ? (npc.x - 28) : (npc.x + 28);
        int targetY = npc.y;

        int distToTarget = MainObject.getDistance(p.x, p.y, targetX, targetY);
        if (distToTarget <= 10) {
            stopPlayerMove();
            return;
        }

        p.toX = targetX;
        p.toY = targetY;
        p.xStopMove = targetX;
        p.yStopMove = targetY;
        p.countAutoMove = 1;
        p.posTransRoad = new short[]{0};
        p.move_to_XY();
    }

    /**
     * Dừng mọi di chuyển của nhân vật
     */
    public static void stopPlayerMove() {
        Player p = GameScreen.player;
        if (p == null) return;
        p.posTransRoad = null;
        p.countAutoMove = 0;
        p.toX = p.x;
        p.toY = p.y;
        p.xStopMove = 0;
        p.yStopMove = 0;
        p.vx = 0;
        p.vy = 0;
    }

    /**
     * Tìm map làng gần nhất từ bản đồ thế giới
     */
    public static int findNearestVillageMapId() {
        int[] villages = new int[]{1, 0, 54, 56, 58, 60, 76};
        if (GameCanvas.loadmap == null) return 1;
        int cur = GameCanvas.loadmap.idMap;
        int bestMap = 1;
        int bestHops = 9999;
        if (GoBackNavigator.graph != null) {
            int fromNode = GoBackNavigator.graph.byId(cur);
            if (fromNode >= 0) {
                for (int i = 0; i < villages.length; i++) {
                    int v = villages[i];
                    int toNode = GoBackNavigator.graph.byId(v);
                    if (toNode >= 0) {
                        GoBackRoutes.Plan plan = GoBackNavigator.graph.plan(fromNode, toNode);
                        if (plan != null && plan.hops < bestHops) {
                            bestHops = plan.hops;
                            bestMap = v;
                        }
                    }
                }
            }
        }
        return bestMap;
    }

    /**
     * Bán thủ công 1 chạm từ Menu (phải ở trong làng và đứng cạnh NPC)
     */
    public static void manualSellTrashNow() {
        loadConfig();
        if (!GameScreen.isMapLang) {
            logInfo("Can ve lang va dung canh NPC de ban do (tranh ban nick)!");
            return;
        }
        MainObject npc = findShopNpc();
        if (npc == null) {
            logInfo("Khong tim thay NPC ban do trong lang!");
            return;
        }
        int dist = MainObject.getDistance(GameScreen.player.x, GameScreen.player.y, npc.x, npc.y);
        if (dist > 45) {
            moveToNpc(npc);
            logInfo("Dang di chuyen den NPC de ban do...");
            return;
        }
        stopPlayerMove();
        GameScreen.ObjFocus = npc;
        scanAndEnqueueTrash();
        if (queueSell.size() == 0) {
            logInfo("Khong co do rac phu hop de ban!");
        }
    }

    /**
     * Gọi định kỳ trong vòng lặp game update()
     */
    public static void update() {
        if (AutoUpgrade.active) return;
        loadConfig();
        if (GameCanvas.currentScreen != GameCanvas.game || GameScreen.player == null) {
            return;
        }

        long now = mSystem.currentTimeMillis();

        // 1. Kiểm tra hành trang để tự động về làng bán đồ
        if (isAutoSellEnabled && Item.VecInvetoryPlayer != null) {
            int currentItems = Item.VecInvetoryPlayer.size();
            int maxItems = Player.maxInven > 0 ? (int)Player.maxInven : 42;
            if (currentItems > maxItems) {
                maxItems = ((currentItems / 42) + 1) * 42;
            }
            int emptySlots = maxItems - currentItems;
            boolean isFull = Player.isFullInven || emptySlots <= 0 || (minEmptySlots > 0 && emptySlots <= minEmptySlots);

            if (!GameScreen.isMapLang) {
                // ĐANG Ở BÃI QUÁI NGOÀI LÀNG: nếu đầy rương hoặc <= minEmptySlots -> VỀ LÀNG
                if (isFull && !isReturningToSell) {
                    if (now - lastAutoScanTime >= 2000L) {
                        lastAutoScanTime = now;
                        if (hasAnyTrashItem()) {
                            isReturningToSell = true;

                            // Tự động ghi nhớ vị trí bãi farm nếu chưa có
                            if (MenuUP.farmMapId < 0 || !MenuUP.isGoBackEnabled) {
                                if (GameCanvas.loadmap != null) {
                                    MenuUP.farmMapId = GameCanvas.loadmap.idMap;
                                    MenuUP.farmX = GoBackPosition.snap(GameScreen.player.x);
                                    MenuUP.farmY = GoBackPosition.snap(GameScreen.player.y);
                                    MenuUP.farmArea = Thread_More.LoadMap.Area;
                                    MenuUP.farmMapName = GoBackNavigator.currentName();
                                    MenuUP.isGoBackEnabled = true;
                                    MenuUP.saveConfig();
                                }
                            }

                            // Tạm dừng auto đánh quái
                            MenuUP.isAutoAttack = false;
                            Player.isAutoFire = (byte)-1;
                            Player.isCurAutoFire = false;
                            GameScreen.ObjFocus = null;

                            logInfo("Hanh trang con " + emptySlots + " o trong -> Ve lang ban do!");

                            if (returnMethod == 0) {
                                // Phương thức 1 (Mặc định): Tự thoát ra vào lại (Relog) để tức thì về Làng
                                MenuUP.triggerRelogToVillage();
                            } else {
                                // Phương thức 2: Dùng GoBack tìm đường đi bộ về Làng
                                int villageMap = findNearestVillageMapId();
                                GoBackNavigator.update(villageMap, -1, -1, -1);
                            }
                        }
                    }
                }
            } else {
                // ĐANG Ở TRONG LÀNG: Phải tìm và đứng cạnh NPC mới được bán đồ
                if (isReturningToSell || isFull || queueSell.size() > 0) {
                    MainObject npc = findShopNpc();
                    if (npc != null) {
                        int dist = MainObject.getDistance(GameScreen.player.x, GameScreen.player.y, npc.x, npc.y);
                        if (dist > 45) {
                            // Chưa đứng cạnh NPC -> Di chuyển lại cạnh NPC, TUYỆT ĐỐI KHÔNG GỬI LỆNH BÁN ĐỒ TỪ XA
                            moveToNpc(npc);
                            return;
                        }

                        // ĐÃ ĐỨNG CẠNH NPC!
                        stopPlayerMove();
                        GameScreen.ObjFocus = npc;

                        if (queueSell.size() == 0 && hasAnyTrashItem()) {
                            scanAndEnqueueTrash();
                        }
                    }
                }
            }
        }

        // 2. Xử lý hàng đợi bán tuần tự: CHỈ BÁN KHI Ở TRONG LÀNG VÀ ĐỨNG CẠNH NPC
        if (queueSell.size() > 0 && GameScreen.isMapLang) {
            MainObject npc = findShopNpc();
            if (npc == null || MainObject.getDistance(GameScreen.player.x, GameScreen.player.y, npc.x, npc.y) > 45) {
                // Chưa đứng cạnh NPC -> không bán
                return;
            }

            long delay = delaySellMs >= 1000L ? delaySellMs : 1000L;
            if (now - lastSellTime >= delay) {
                MainItem itemToSell = null;
                while (queueSell.size() > 0) {
                    MainItem candidate = (MainItem) queueSell.elementAt(0);
                    queueSell.removeElementAt(0);
                    // Kiểm tra món đồ vẫn còn tồn tại trong hành trang và vẫn thỏa mãn điều kiện rác
                    if (candidate != null && Item.VecInvetoryPlayer != null
                            && Item.VecInvetoryPlayer.contains(candidate)
                            && isTrashItem(candidate)) {
                        itemToSell = candidate;
                        break;
                    }
                }

                if (itemToSell != null) {
                    // Gói tin bán: (byte)ItemCatagory (3), (short)Id, (byte)1 = BÁN
                    lastDeleteSentTime = now;
                    GlobalService.gI().delete_Item((byte)itemToSell.ItemCatagory, (short)itemToSell.Id, (byte)1);
                    lastSellTime = now;
                    totalSoldSession++;
                }

                if (queueSell.size() == 0) {
                    Player.isFullInven = false;
                    logInfo("Auto Sell: Da don dep xong (" + totalSoldSession + " mon)!");
                    if (isReturningToSell) {
                        isReturningToSell = false;
                        if (MenuUP.isGoBackEnabled && MenuUP.farmMapId >= 0) {
                            MenuUP.resumeGoBackAfterSell();
                        }
                    }
                }
            }
        }
    }

    public static void toggleAutoSell() {
        loadConfig();
        isAutoSellEnabled = !isAutoSellEnabled;
        saveConfig();
        logInfo("Auto Ban Do: " + (isAutoSellEnabled ? "BAT" : "TAT"));
    }

    public static void toggleSellWhite() {
        loadConfig();
        isSellWhite = !isSellWhite;
        saveConfig();
        logInfo("Ban do Trang: " + (isSellWhite ? "BAT" : "TAT"));
    }

    public static void toggleSellGreen() {
        loadConfig();
        isSellGreen = !isSellGreen;
        saveConfig();
        logInfo("Ban do Xanh la: " + (isSellGreen ? "BAT" : "TAT"));
    }

    public static void toggleSellBlue() {
        loadConfig();
        isSellBlue = !isSellBlue;
        saveConfig();
        logInfo("Ban do Xanh duong: " + (isSellBlue ? "BAT" : "TAT"));
    }

    public static void toggleSellYellow() {
        loadConfig();
        isSellYellow = !isSellYellow;
        saveConfig();
        logInfo("Ban do Vang: " + (isSellYellow ? "BAT" : "TAT"));
    }

    public static void toggleSellPurple() {
        loadConfig();
        isSellPurple = !isSellPurple;
        saveConfig();
        logInfo("Ban do Tim: " + (isSellPurple ? "BAT" : "TAT"));
    }

    public static void toggleSellOrange() {
        loadConfig();
        isSellOrange = !isSellOrange;
        saveConfig();
        logInfo("Ban do Cam: " + (isSellOrange ? "BAT" : "TAT"));
    }

    public static void toggleSellLocked() {
        loadConfig();
        isSellLocked = !isSellLocked;
        saveConfig();
        logInfo("Ban do Da Khoa: " + (isSellLocked ? "BAT (Can than)" : "TAT (An toan)"));
    }

    public static void toggleSellUpgraded() {
        loadConfig();
        isSellUpgraded = !isSellUpgraded;
        saveConfig();
        logInfo("Ban do Da Cuong Hoa (+): " + (isSellUpgraded ? "BAT (Can than)" : "TAT (An toan)"));
    }

    public static void cycleDelaySell() {
        loadConfig();
        int nextIdx = 0;
        for (int i = 0; i < DELAY_STEPS.length; i++) {
            if (delaySellMs == DELAY_STEPS[i]) {
                nextIdx = (i + 1) % DELAY_STEPS.length;
                break;
            }
        }
        delaySellMs = DELAY_STEPS[nextIdx];
        saveConfig();
        logInfo("Delay ban do: " + (delaySellMs / 1000) + "." + ((delaySellMs % 1000) / 100) + "s");
    }

    public static void cycleMinEmptySlots() {
        loadConfig();
        int nextIdx = 0;
        for (int i = 0; i < MIN_SLOT_STEPS.length; i++) {
            if (minEmptySlots == MIN_SLOT_STEPS[i]) {
                nextIdx = (i + 1) % MIN_SLOT_STEPS.length;
                break;
            }
        }
        minEmptySlots = MIN_SLOT_STEPS[nextIdx];
        saveConfig();
        logInfo("Ve lang khi con: " + (minEmptySlots == 0 ? "TAT" : (minEmptySlots + " o trong")));
    }

    public static void cycleReturnMethod() {
        loadConfig();
        returnMethod = (returnMethod == 0) ? 1 : 0;
        saveConfig();
        logInfo("Cach ve lang: " + (returnMethod == 0 ? "Thoat vao lai (Relog)" : "Di bo GoBack"));
    }

    public static int getQueueCount() {
        return queueSell.size();
    }

    public static int getTotalSold() {
        return totalSoldSession;
    }

    public static void resetSession() {
        queueSell.removeAllElements();
        totalSoldSession = 0;
        lastSellTime = 0L;
    }

    public static void stopAll() {
        loadConfig();
        isAutoSellEnabled = false;
        isReturningToSell = false;
        queueSell.removeAllElements();
        lastDeleteSentTime = -3000L;
        saveConfig();
    }
}
