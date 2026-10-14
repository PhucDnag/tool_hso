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
    public static long delaySellMs = 350L;         // Delay bán mỗi món (350ms theo chuẩn toolmau chống server chặn)
    public static int minEmptySlots = 2;           // Số ô trống hành trang để tự động về làng bán đồ (0 = TẮT, 1..5 ô)
    public static boolean isReturningToSell = false; // Cờ đang trong tiến trình về làng bán đồ
    private static long lastDeleteSentTime = 0L;   // Thời điểm gửi lệnh delete_Item gần nhất

    private static mVector queueSell = new mVector();
    private static long lastSellTime = 0L;
    private static int totalSoldSession = 0;
    private static long lastAutoScanTime = 0L;
    private static boolean isConfigLoaded = false;

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
            String sSlots = CRes.loadRMSString("hso_up_sell_min_slots");
            if (sSlots != null && sSlots.trim().length() > 0) {
                try {
                    minEmptySlots = Integer.parseInt(sSlots.trim());
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
        if (queueSell.size() > 0) {
            GameCanvas.addInfoCharCline("Auto Sell: Phat hien " + queueSell.size() + " mon rac, dang ban...");
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
                || unaccented.indexOf("sell") >= 0 || (lower.indexOf("b\u00e1n") >= 0 && (lower.indexOf("áo") >= 0 || lower.indexOf("quan") >= 0 || lower.indexOf("kiem") >= 0 || lower.indexOf("vu khi") >= 0 || lower.indexOf("nhan") >= 0));
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
     * Bán thủ công 1 chạm từ Menu
     */
    public static void manualSellTrashNow() {
        loadConfig();
        scanAndEnqueueTrash();
        if (queueSell.size() == 0) {
            GameCanvas.addInfoCharCline("Auto Sell: Khong co do rac phu hop de ban!");
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

        // 1. Tự động kiểm tra hành trang để về làng bán đồ
        if (isAutoSellEnabled && queueSell.size() == 0 && Item.VecInvetoryPlayer != null) {
            int currentItems = Item.VecInvetoryPlayer.size();
            int maxItems = Player.maxInven > 0 ? (int)Player.maxInven : 42;
            int emptySlots = maxItems - currentItems;
            boolean isFull = Player.isFullInven || emptySlots <= 0 || (minEmptySlots > 0 && emptySlots <= minEmptySlots);

            if (!GameScreen.isMapLang) {
                // Đang ở bãi ngoài làng: nếu đầy rương hoặc <= minEmptySlots -> về làng
                if (isFull && !isReturningToSell) {
                    if (now - lastAutoScanTime >= 2000L) {
                        lastAutoScanTime = now;
                        if (hasAnyTrashItem()) {
                            isReturningToSell = true;
                            scanAndEnqueueTrash();
                            GameCanvas.addInfoCharCline("Hanh trang day (con " + emptySlots + " o trong) -> Ve lang ban do!");
                            GlobalService.gI().gohome((byte)0);
                        }
                    }
                }
            } else {
                // Đang ở trong làng: quét đồ rác bán ngay
                if (isReturningToSell || isFull) {
                    if (hasAnyTrashItem()) {
                        scanAndEnqueueTrash();
                    } else if (isReturningToSell) {
                        isReturningToSell = false;
                        Player.isFullInven = false;
                        if (MenuUP.isGoBackEnabled && MenuUP.farmMapId >= 0) {
                            MenuUP.resumeGoBackAfterSell();
                        }
                    }
                }
            }
        }

        // 2. Xử lý hàng đợi bán tuần tự có độ trễ delaySellMs chống server chặn spam
        // Chỉ gửi lệnh delete_Item khi thực sự đang ở trong làng (tránh lỗi "Mục tiêu ở quá xa")
        if (queueSell.size() > 0 && GameScreen.isMapLang) {
            long delay = delaySellMs > 0L ? delaySellMs : 350L;
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
                    if (queueSell.size() == 0) {
                        GameCanvas.addInfoCharCline("Auto Sell: Da don dep xong (Tong: " + totalSoldSession + " mon)!");
                        Player.isFullInven = false;
                        if (isReturningToSell) {
                            isReturningToSell = false;
                            if (MenuUP.isGoBackEnabled && MenuUP.farmMapId >= 0) {
                                GameCanvas.addInfoCharCline("Tiep tuc GoBack ve bai farm...");
                                MenuUP.resumeGoBackAfterSell();
                            }
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
        GameCanvas.addInfoCharCline("Auto Ban Do: " + (isAutoSellEnabled ? "BAT" : "TAT"));
    }

    public static void toggleSellWhite() {
        loadConfig();
        isSellWhite = !isSellWhite;
        saveConfig();
        GameCanvas.addInfoCharCline("Ban do Trang: " + (isSellWhite ? "BAT" : "TAT"));
    }

    public static void toggleSellGreen() {
        loadConfig();
        isSellGreen = !isSellGreen;
        saveConfig();
        GameCanvas.addInfoCharCline("Ban do Xanh la: " + (isSellGreen ? "BAT" : "TAT"));
    }

    public static void toggleSellBlue() {
        loadConfig();
        isSellBlue = !isSellBlue;
        saveConfig();
        GameCanvas.addInfoCharCline("Ban do Xanh duong: " + (isSellBlue ? "BAT" : "TAT"));
    }

    public static void toggleSellYellow() {
        loadConfig();
        isSellYellow = !isSellYellow;
        saveConfig();
        GameCanvas.addInfoCharCline("Ban do Vang: " + (isSellYellow ? "BAT" : "TAT"));
    }

    public static void toggleSellPurple() {
        loadConfig();
        isSellPurple = !isSellPurple;
        saveConfig();
        GameCanvas.addInfoCharCline("Ban do Tim: " + (isSellPurple ? "BAT" : "TAT"));
    }

    public static void toggleSellOrange() {
        loadConfig();
        isSellOrange = !isSellOrange;
        saveConfig();
        GameCanvas.addInfoCharCline("Ban do Cam: " + (isSellOrange ? "BAT" : "TAT"));
    }

    public static void toggleSellLocked() {
        loadConfig();
        isSellLocked = !isSellLocked;
        saveConfig();
        GameCanvas.addInfoCharCline("Ban do Da Khoa: " + (isSellLocked ? "BAT (Can than)" : "TAT (An toan)"));
    }

    public static void toggleSellUpgraded() {
        loadConfig();
        isSellUpgraded = !isSellUpgraded;
        saveConfig();
        GameCanvas.addInfoCharCline("Ban do Da Cuong Hoa (+): " + (isSellUpgraded ? "BAT (Can than)" : "TAT (An toan)"));
    }

    public static void cycleDelaySell() {
        loadConfig();
        if (delaySellMs == 350L) {
            delaySellMs = 500L;
        } else if (delaySellMs == 500L) {
            delaySellMs = 700L;
        } else if (delaySellMs == 700L) {
            delaySellMs = 1000L;
        } else if (delaySellMs == 1000L) {
            delaySellMs = 1500L;
        } else if (delaySellMs == 1500L) {
            delaySellMs = 2000L;
        } else if (delaySellMs == 2000L) {
            delaySellMs = 350L;
        } else {
            delaySellMs = 700L;
        }
        saveConfig();
        GameCanvas.addInfoCharCline("Delay ban do: " + delaySellMs + "ms");
    }

    public static void cycleMinEmptySlots() {
        loadConfig();
        minEmptySlots = (minEmptySlots + 1) % 6; // 0=TẮT, 1, 2, 3, 4, 5
        saveConfig();
        GameCanvas.addInfoCharCline("Ve lang khi con: " + (minEmptySlots == 0 ? "TAT" : (minEmptySlots + " o trong")));
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
}
