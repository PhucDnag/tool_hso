package GameObjects;

import CLib.Session_ME;
import CLib.mFont;
import CLib.mGraphics;
import CLib.mSystem;
import CLib.mVector;
import GameObjects.Item;
import GameObjects.Other_Players;
import GameObjects.Player;
import GameScreen.GameScreen;
import GameScreen.LoginScreen;
import GameScreen.SelectCharScreen;
import InterfaceComponents.IAction;
import InterfaceComponents.InputDialog;
import InterfaceComponents.MsgDialog;
import InterfaceComponents.iCommand;
import Main.GameCanvas;
import Main.GameMidlet;
import GameObjects.MainObject;
import GameScreen.LoadMapScreen;
import Model.CRes;
import Model.Point;
import Model.T;
import Skill.HotKey;
import Thread_More.LoadMap;
import netcommand.global.GlobalService;

public class MenuUP {
    public static final String AUTHOR = "HONG PHUC HSO";
    public static final String VERSION = "v1.6";

    // 1. Auto Reconnect (Tự động đăng nhập lại khi mất kết nối)
    public static boolean isAutoReconnect = true;
    public static boolean isWaitingReconnect = false;
    public static boolean isReconnectingChar = false;
    public static long timeTargetReconnect = 0L;
    public static int reconnectDelaySec = 12;
    public static int reconnectAttempts = 0;
    public static int lastCharId = -1;
    public static String lastCharName = "";
    public static byte lastArea = 0;

    // 2. Auto Revive (Tự động hồi sinh khi chết)
    // 0: TẮT, 1: Về Làng, 2: Tại Chỗ
    public static int autoReviveMode = 0;
    public static int reviveState = 0; // 0: IDLE, 1: COUNTDOWN, 2: WAITING_RESPONSE
    public static long timeTargetRevive = 0L;
    public static long timeSentRevive = 0L;
    public static int reviveDelaySec = 3;
    public static boolean isReviveCancelled = false;
    public static boolean isCheckingGoldRevive = false;

    // 3. Auto Đánh Quái (Tự động đánh và focus vào quái)
    public static boolean isAutoAttack = false;

    // 4. Auto Bơm HP / MP
    public static boolean isAutoHpMp = false;
    public static int autoHpPercent = 50;
    public static int autoMpPercent = 40;
    public static InputDialog inputHp;
    public static InputDialog inputMp;

    // 5. Auto Thú Cưỡi
    // 0: TẮT, 1: Ngựa Nâu (62), 2: Ngựa Trắng (63), 3: Ngựa Giáp (64), 4: Ngựa Đỏ (65), 5: Ngựa Đen (66), 6: Bất Kỳ Trong Túi, 7: Chọn Từ Túi Đồ
    public static int autoMountType = 0;
    public static int autoMountCustomId = -1;
    public static String autoMountCustomName = "";
    public static long lastMountAttemptTime = 0L;

    // 6. GoBack (Tự động tìm đường về Map UP khi bị kết nối về làng hoặc rời map)
    public static boolean isGoBackEnabled = true;
    public static boolean isGoBackActive = false;
    public static boolean isGoBackPaused = false;
    public static int farmMapId = -1;
    public static String farmMapName = "";
    public static int farmX = 0;
    public static int farmY = 0;
    public static int farmArea = 0;
    public static String goBackStepInfo = "";
    public static boolean isVillage(int mapId) {
        if (GameCanvas.loadmap != null && GameCanvas.loadmap.idMap == mapId) return GameScreen.isMapLang;
        int node = GoBackNavigator.graph.byId(mapId);
        return node >= 0 && GoBackNavigator.graph.get(node).village;
    }

    // HUD box coordinates
    public static int hudBoxW = 170;
    public static int hudBoxH = 12;
    public static boolean hudVisible = true;
    private static String[] hudLines = new String[8];
    private static int hudLineCount, hudCancelY = -1, hudCancelAction;
    private static long hudRefresh;

    private static int hudY() { return Math.max(65, GameCanvas.h / 3); }
    private static String enabled(boolean value) { return value ? "Bật" : "Tắt"; }

    private static boolean isConfigLoaded = false;

    public static void loadConfig() {
        if (isConfigLoaded) {
            return;
        }
        isConfigLoaded = true;
        try {
            String sRec = CRes.loadRMSString("hso_up_reconnect");
            if (sRec != null && sRec.trim().length() > 0) {
                isAutoReconnect = sRec.trim().equals("1");
            }
            String sRev = CRes.loadRMSString("hso_up_revive");
            if (sRev != null && sRev.trim().length() > 0) {
                try {
                    autoReviveMode = Integer.parseInt(sRev.trim());
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            String sChar = CRes.loadRMSString("hso_up_lastchar");
            if (sChar != null && sChar.trim().length() > 0) {
                lastCharName = sChar.trim();
            }
            String sCharId = CRes.loadRMSString("hso_up_lastcharid");
            if (sCharId != null && sCharId.trim().length() > 0) {
                try {
                    lastCharId = Integer.parseInt(sCharId.trim());
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }

            // Load Auto Attack
            String sAtt = CRes.loadRMSString("hso_up_autoattack");
            if (sAtt != null && sAtt.trim().length() > 0) {
                isAutoAttack = sAtt.trim().equals("1");
            }

            // Load Auto HP/MP
            String sHpMp = CRes.loadRMSString("hso_up_autohpmp");
            if (sHpMp != null && sHpMp.trim().length() > 0) {
                isAutoHpMp = sHpMp.trim().equals("1");
            }
            String sHpPer = CRes.loadRMSString("hso_up_hp_percent");
            if (sHpPer != null && sHpPer.trim().length() > 0) {
                try {
                    autoHpPercent = Integer.parseInt(sHpPer.trim());
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            String sMpPer = CRes.loadRMSString("hso_up_mp_percent");
            if (sMpPer != null && sMpPer.trim().length() > 0) {
                try {
                    autoMpPercent = Integer.parseInt(sMpPer.trim());
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }

            // Load Auto Mount
            String sMount = CRes.loadRMSString("hso_up_mount_type");
            if (sMount != null && sMount.trim().length() > 0) {
                try {
                    autoMountType = Integer.parseInt(sMount.trim());
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            String sCustomMount = CRes.loadRMSString("hso_up_mount_cid");
            if (sCustomMount != null && sCustomMount.trim().length() > 0) {
                try {
                    autoMountCustomId = Integer.parseInt(sCustomMount.trim());
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            String sCustomName = CRes.loadRMSString("hso_up_mount_cname");
            if (sCustomName != null && sCustomName.trim().length() > 0) {
                autoMountCustomName = sCustomName.trim();
            }

            // Load GoBack
            String sGb = CRes.loadRMSString("hso_up_goback_en");
            if (sGb != null && sGb.trim().length() > 0) {
                isGoBackEnabled = sGb.trim().equals("1");
            }
            String sGmap = CRes.loadRMSString("hso_up_goback_map");
            if (sGmap != null && sGmap.trim().length() > 0) {
                try {
                    farmMapId = Integer.parseInt(sGmap.trim());
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            String sGx = CRes.loadRMSString("hso_up_goback_x");
            if (sGx != null && sGx.trim().length() > 0) {
                try {
                    farmX = Integer.parseInt(sGx.trim());
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            String sGy = CRes.loadRMSString("hso_up_goback_y");
            if (sGy != null && sGy.trim().length() > 0) {
                try {
                    farmY = Integer.parseInt(sGy.trim());
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            String sGarea = CRes.loadRMSString("hso_up_goback_area");
            if (sGarea != null && sGarea.trim().length() > 0) {
                try {
                    farmArea = Integer.parseInt(sGarea.trim());
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            String sGname = CRes.loadRMSString("hso_up_goback_name");
            if (sGname != null && sGname.trim().length() > 0) {
                farmMapName = sGname.trim();
            }
            String sStone = CRes.loadRMSString("hso_up_goback_stone");
            if (sStone != null && sStone.trim().length() > 0) {
                GoBackNavigator.prioritizeStone = sStone.trim().equals("1");
            }
            MedalFilter.loadConfig();
            AutoSellManager.loadConfig();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void saveConfig() {
        try {
            CRes.saveRMSString_("hso_up_reconnect", isAutoReconnect ? "1" : "0");
            CRes.saveRMSString_("hso_up_revive", String.valueOf(autoReviveMode));
            if (lastCharName != null && lastCharName.length() > 0) {
                CRes.saveRMSString_("hso_up_lastchar", lastCharName);
            }
            if (lastCharId >= 0) {
                CRes.saveRMSString_("hso_up_lastcharid", String.valueOf(lastCharId));
            }
            CRes.saveRMSString_("hso_up_autoattack", isAutoAttack ? "1" : "0");
            CRes.saveRMSString_("hso_up_autohpmp", isAutoHpMp ? "1" : "0");
            CRes.saveRMSString_("hso_up_hp_percent", String.valueOf(autoHpPercent));
            CRes.saveRMSString_("hso_up_mp_percent", String.valueOf(autoMpPercent));
            CRes.saveRMSString_("hso_up_mount_type", String.valueOf(autoMountType));
            CRes.saveRMSString_("hso_up_mount_cid", String.valueOf(autoMountCustomId));
            if (autoMountCustomName != null && autoMountCustomName.length() > 0) {
                CRes.saveRMSString_("hso_up_mount_cname", autoMountCustomName);
            }
            CRes.saveRMSString_("hso_up_goback", isGoBackEnabled ? "1" : "0");
            CRes.saveRMSString_("hso_up_goback_map", String.valueOf(farmMapId));
            CRes.saveRMSString_("hso_up_goback_x", String.valueOf(farmX));
            CRes.saveRMSString_("hso_up_goback_y", String.valueOf(farmY));
            CRes.saveRMSString_("hso_up_goback_area", String.valueOf(farmArea));
            if (farmMapName != null && farmMapName.length() > 0) {
                CRes.saveRMSString_("hso_up_goback_name", farmMapName);
            }
            CRes.saveRMSString_("hso_up_goback_stone", GoBackNavigator.prioritizeStone ? "1" : "0");
            GoBackProfile.save();
            AutoSellManager.saveConfig();
            MedalFilter.saveConfig();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void saveCurrentChar() {
        if (GameScreen.player != null && GameScreen.player.name != null && GameScreen.player.name.length() > 0) {
            boolean changed = !GameScreen.player.name.equals(lastCharName) || lastCharId != GameScreen.player.ID;
            lastCharName = GameScreen.player.name;
            lastCharId = GameScreen.player.ID;
            lastArea = (byte)LoadMap.Area;
            if (changed) saveConfig();
        }
    }

    public static boolean isDisconnectString(String string) {
        if (string == null) {
            return false;
        }
        String lower = string.toLowerCase();
        return lower.indexOf("m\u1ea5t k\u1ebft n\u1ed1i") != -1
            || lower.indexOf("mat ket noi") != -1
            || lower.indexOf("disconnect") != -1
            || lower.indexOf("koneksi") != -1
            || lower.indexOf("desconectado") != -1;
    }

    public static boolean handleSelectDialog(String string, mVector mVector2) {
        loadConfig();
        if (AutoFarmSupport.dialog(string, mVector2)) return true;
        if (string == null) {
            return false;
        }
        if (AutoSellManager.handleSelectDialog(string, mVector2)) {
            return true;
        }
        // 1. Kiem tra hop thoai mat ket noi
        if (isAutoReconnect && isDisconnectString(string)) {
            onDisconnect();
            return true;
        }

        // 2. Kiem tra hop thoai hoi sinh khi chet
        boolean isDead = (GameScreen.player != null && (GameScreen.player.hp <= 0 || GameScreen.player.Action == 4));
        if (isDead || isCheckingGoldRevive || reviveState > 0) {
            String lower = string.toLowerCase();

            // YEU CAU NGOC: TUYET DOI KHONG DUNG NGOC -> TU DONG VE LANG!
            if (lower.indexOf("ng\u1ecdc") != -1 || lower.indexOf("ngoc") != -1 || lower.indexOf("gem") != -1 || lower.indexOf("ruby") != -1) {
                isCheckingGoldRevive = false;
                reviveState = 2;
                GameCanvas.end_Dialog();
                GlobalService.gI().gohome((byte)0);
                GameCanvas.addInfoCharCline("Y\u00eau c\u1ea7u NG\u1eccC -> T\u1ef1 \u0111\u1ed9ng V\u1ec0 L\u00c0NG (Kh\u00f4ng d\u00f9ng ng\u1ecdc)!");
                return true;
            }

            // YEU CAU VANG: CHON DUNG VANG!
            if (lower.indexOf("v\u00e0ng") != -1 || lower.indexOf("vang") != -1 || lower.indexOf("gold") != -1 || lower.indexOf("xu") != -1 || lower.indexOf("coin") != -1) {
                isCheckingGoldRevive = false;
                reviveState = 2;
                if (mVector2 != null && mVector2.size() > 0) {
                    iCommand cmdOk = (iCommand)mVector2.elementAt(0);
                    if (cmdOk != null) {
                        cmdOk.perform();
                    }
                }
                GameCanvas.end_Dialog();
                GameCanvas.addInfoCharCline("H\u1ed3i sinh t\u1ea1i ch\u1ed7 b\u1eb1ng V\u00c0NG th\u00e0nh c\u00f4ng!");
                return true;
            }

            // KHONG DU / KHONG CO VAT PHAM: TU DONG VE LANG
            if (lower.indexOf("kh\u00f4ng c\u00f3") != -1 || lower.indexOf("khong co") != -1 || lower.indexOf("kh\u00f4ng \u0111\u1ee7") != -1 || lower.indexOf("khong du") != -1) {
                isCheckingGoldRevive = false;
                reviveState = 2;
                GameCanvas.end_Dialog();
                GlobalService.gI().gohome((byte)0);
                GameCanvas.addInfoCharCline("Kh\u00f4ng \u0111\u1ee7 \u0111i\u1ec1u ki\u1ec7n -> T\u1ef1 \u0111\u1ed9ng V\u1ec0 L\u00c0NG!");
                return true;
            }

            // HOI VE LANG
            if (lower.indexOf("v\u1ec1 l\u00e0ng") != -1 || lower.indexOf("ve lang") != -1) {
                isCheckingGoldRevive = false;
                reviveState = 2;
                GameCanvas.end_Dialog();
                GlobalService.gI().gohome((byte)0);
                return true;
            }
        }
        return false;
    }

    public static boolean handleSingleDialog(String string, iCommand iCommand2) {
        loadConfig();
        mVector farmOptions = new mVector("Farm dialog");
        if (iCommand2 != null) farmOptions.addElement(iCommand2);
        if (AutoFarmSupport.dialog(string, farmOptions)) return true;
        if (string == null) {
            return false;
        }
        if (AutoSellManager.handleSingleDialog(string, iCommand2)) {
            return true;
        }
        // 1. Kiem tra mat ket noi
        if (isAutoReconnect && isDisconnectString(string)) {
            onDisconnect();
            return true;
        }

        // 2. Kiem tra hoi sinh khi chet
        boolean isDead = (GameScreen.player != null && (GameScreen.player.hp <= 0 || GameScreen.player.Action == 4));
        if (isDead || isCheckingGoldRevive || reviveState > 0) {
            String lower = string.toLowerCase();

            // YEU CAU NGOC: TUYET DOI KHONG DUNG NGOC -> TU DONG VE LANG!
            if (lower.indexOf("ng\u1ecdc") != -1 || lower.indexOf("ngoc") != -1 || lower.indexOf("gem") != -1 || lower.indexOf("ruby") != -1) {
                isCheckingGoldRevive = false;
                reviveState = 2;
                GameCanvas.end_Dialog();
                GlobalService.gI().gohome((byte)0);
                GameCanvas.addInfoCharCline("Y\u00eau c\u1ea7u NG\u1eccC -> T\u1ef1 \u0111\u1ed9ng V\u1ec0 L\u00c0NG (Kh\u00f4ng d\u00f9ng ng\u1ecdc)!");
                return true;
            }

            // YEU CAU VANG: CHON VANG!
            if (lower.indexOf("v\u00e0ng") != -1 || lower.indexOf("vang") != -1 || lower.indexOf("gold") != -1 || lower.indexOf("xu") != -1 || lower.indexOf("coin") != -1) {
                isCheckingGoldRevive = false;
                reviveState = 2;
                if (iCommand2 != null) {
                    iCommand2.perform();
                }
                GameCanvas.end_Dialog();
                GameCanvas.addInfoCharCline("H\u1ed3i sinh t\u1ea1i ch\u1ed7 b\u1eb1ng V\u00c0NG th\u00e0nh c\u00f4ng!");
                return true;
            }

            // KHONG DU / KHONG CO
            if (lower.indexOf("kh\u00f4ng c\u00f3") != -1 || lower.indexOf("khong co") != -1 || lower.indexOf("kh\u00f4ng \u0111\u1ee7") != -1 || lower.indexOf("khong du") != -1) {
                isCheckingGoldRevive = false;
                reviveState = 2;
                GameCanvas.end_Dialog();
                GlobalService.gI().gohome((byte)0);
                GameCanvas.addInfoCharCline("Kh\u00f4ng \u0111\u1ee7 \u0111i\u1ec1u ki\u1ec7n -> T\u1ef1 \u0111\u1ed9ng V\u1ec0 L\u00c0NG!");
                return true;
            }

            // HOI VE LANG
            if (lower.indexOf("v\u1ec1 l\u00e0ng") != -1 || lower.indexOf("ve lang") != -1) {
                isCheckingGoldRevive = false;
                reviveState = 2;
                GameCanvas.end_Dialog();
                GlobalService.gI().gohome((byte)0);
                return true;
            }
        }
        return false;
    }

    public static boolean onDisconnect() {
        GoBackNavigator.disconnected();
        loadConfig();
        if (!isAutoReconnect) {
            return false;
        }
        saveCurrentChar();
        isWaitingReconnect = true;
        isReconnectingChar = true;
        // Delay random 10 den 15 giay de tranh bi game ban
        reconnectDelaySec = 10 + CRes.random(6);
        timeTargetReconnect = mSystem.currentTimeMillis() + (long)(reconnectDelaySec * 1000);
        ++reconnectAttempts;

        GameCanvas.end_Dialog();
        GameCanvas.login.Show();
        return true;
    }

    public static void onConnectFail() {
        if (isAutoReconnect && (isWaitingReconnect || GameCanvas.currentScreen == GameCanvas.login)) {
            isWaitingReconnect = true;
            reconnectDelaySec = 10 + CRes.random(6);
            timeTargetReconnect = mSystem.currentTimeMillis() + (long)(reconnectDelaySec * 1000);
            GameCanvas.end_Dialog();
        }
    }

    public static void cancelReconnect() {
        isWaitingReconnect = false;
        isReconnectingChar = false;
        timeTargetReconnect = 0L;
        GameCanvas.addInfoCharCline("H\u1ee7y t\u1ef1 \u0111\u0103ng nh\u1eadp l\u1ea1i!");
    }

    public static void cancelRevive() {
        reviveState = 0;
        isReviveCancelled = true;
        isCheckingGoldRevive = false;
        GameCanvas.addInfoCharCline("H\u1ee7y Auto H\u1ed3i sinh l\u1ea7n n\u00e0y!");
    }

    public static boolean hasReviveItem() {
        try {
            if (Item.VecInvetoryPlayer == null || Item.VecInvetoryPlayer.size() == 0) {
                return false;
            }
            for (int i = 0; i < Item.VecInvetoryPlayer.size(); ++i) {
                Object obj = Item.VecInvetoryPlayer.elementAt(i);
                if (obj instanceof Item) {
                    Item item = (Item)obj;
                    if (item.itemName != null) {
                        String name = item.itemName.toLowerCase();
                        if (name.indexOf("h\u1ed3i sinh") != -1
                            || name.indexOf("hoi sinh") != -1
                            || name.indexOf("th\u00e1nh gi\u00e1") != -1
                            || name.indexOf("thanh gia") != -1
                            || name.indexOf("revive") != -1
                            || name.indexOf("cross") != -1) {
                            return true;
                        }
                    }
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return false;
    }

    public static String getReviveItemName() {
        try {
            if (Item.VecInvetoryPlayer == null || Item.VecInvetoryPlayer.size() == 0) {
                return "";
            }
            for (int i = 0; i < Item.VecInvetoryPlayer.size(); ++i) {
                Object obj = Item.VecInvetoryPlayer.elementAt(i);
                if (obj instanceof Item) {
                    Item item = (Item)obj;
                    if (item.itemName != null) {
                        String name = item.itemName.toLowerCase();
                        if (name.indexOf("h\u1ed3i sinh") != -1
                            || name.indexOf("hoi sinh") != -1
                            || name.indexOf("th\u00e1nh gi\u00e1") != -1
                            || name.indexOf("thanh gia") != -1
                            || name.indexOf("revive") != -1
                            || name.indexOf("cross") != -1) {
                            return item.itemName;
                        }
                    }
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return "";
    }

    // Tim binh mau (0) hoac binh mana (1) trong hanh trang hoac hotkey
    public static Item findPotion(int typePotion) {
        try {
            if (Item.VecInvetoryPlayer != null) {
                for (int i = 0; i < Item.VecInvetoryPlayer.size(); ++i) {
                    Object obj = Item.VecInvetoryPlayer.elementAt(i);
                    if (obj instanceof Item) {
                        Item item = (Item)obj;
                        if (item.ItemCatagory == 4 && item.typePotion == typePotion && item.numPotion > 0) {
                            return item;
                        }
                    }
                }
            }
            // Kiem tra trong phim tat (hotkey 4 la HP, 3 la MP)
            if (Player.mhotkey != null && Player.levelTab < Player.mhotkey.length) {
                int slot = (typePotion == 0) ? 4 : 3;
                if (slot < Player.mhotkey[Player.levelTab].length) {
                    HotKey hotKey = Player.mhotkey[Player.levelTab][slot];
                    if (hotKey != null && hotKey.type == HotKey.POTION) {
                        Item item = Item.getItemInventory(4, hotKey.id);
                        if (item != null && item.typePotion == typePotion) {
                            return item;
                        }
                    }
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    // Kiem tra co phai ID ngua mac dinh trong game khong (62: Nau, 63: Trang, 64: Giap, 65: Do, 66: Den)
    public static boolean isHorseId(int id) {
        return id >= 62 && id <= 66;
    }

    // Kiem tra ten vat pham co chua tu khoa thu cuoi
    public static boolean isMountKeyword(String name) {
        if (name == null || name.length() == 0) {
            return false;
        }
        String lower = name.toLowerCase();
        return lower.indexOf("ng\u1ef1a") != -1
            || lower.indexOf("ngua") != -1
            || lower.indexOf("th\u00fa c\u01b0\u1ee1i") != -1
            || lower.indexOf("thu cuoi") != -1
            || lower.indexOf("x\u00edch th\u1ed1") != -1
            || lower.indexOf("xich tho") != -1
            || lower.indexOf("chi\u1ebfn m\u00e3") != -1
            || lower.indexOf("chien ma") != -1
            || lower.indexOf("s\u00f3i") != -1
            || lower.indexOf("soi") != -1
            || lower.indexOf("tu\u1ea7n l\u1ed9c") != -1
            || lower.indexOf("tuan loc") != -1
            || lower.indexOf("xe m\u00e1y") != -1
            || lower.indexOf("xe may") != -1
            || lower.indexOf("r\u1ed3ng") != -1
            || lower.indexOf("rong") != -1
            || lower.indexOf("heo") != -1
            || lower.indexOf("ph\u01b0\u1ee3ng ho\u00e0ng") != -1
            || lower.indexOf("c\u00f2i") != -1
            || lower.indexOf("coi") != -1
            || lower.indexOf("d\u00e2y c\u01b0\u01a1ng") != -1
            || lower.indexOf("day cuong") != -1;
    }

    // Tim vat pham thu cuoi theo loai da chon
    public static Item findMountItem(int type) {
        try {
            if (Item.VecInvetoryPlayer == null || Item.VecInvetoryPlayer.size() == 0) {
                return null;
            }

            int targetId = -1;
            if (type == 1) targetId = 62;      // Ngua Nau
            else if (type == 2) targetId = 63; // Ngua Trang
            else if (type == 3) targetId = 64; // Ngua Chien Giap
            else if (type == 4) targetId = 65; // Ngua Do
            else if (type == 5) targetId = 66; // Ngua Den
            else if (type == 7) targetId = autoMountCustomId; // Custom ID tu tui do

            // 1. Uu tien tim theo ID cu the
            if (targetId > 0) {
                for (int i = 0; i < Item.VecInvetoryPlayer.size(); ++i) {
                    Item item = (Item)Item.VecInvetoryPlayer.elementAt(i);
                    if (item != null && item.Id == targetId) {
                        return item;
                    }
                }
            }

            // Only the explicit "any mount" setting may choose a different item.
            if (type == 6) {
                // Uu tien 1: Tim ngua tieu chuan trong game (ID 62..66)
                for (int i = 0; i < Item.VecInvetoryPlayer.size(); ++i) {
                    Item item = (Item)Item.VecInvetoryPlayer.elementAt(i);
                    if (item != null && isHorseId(item.Id)) {
                        return item;
                    }
                }

                // Uu tien 2: Tim theo tu khoa ten vat pham
                for (int i = 0; i < Item.VecInvetoryPlayer.size(); ++i) {
                    Item item = (Item)Item.VecInvetoryPlayer.elementAt(i);
                    if (item != null && isMountKeyword(item.itemName)) {
                        return item;
                    }
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    // Tim vat pham thu cuoi hien tai
    public static Item findMountItem() {
        return findMountItem(autoMountType);
    }

    // Su dung vat pham thu cuoi dung cach (Packet 32 / Use_Potion cho ngua/potion, hoac dung loai)
    public static void useMountItem(Item item) {
        if (item == null) {
            return;
        }
        try {
            if (item.ItemCatagory == 4) {
                // Ngua va vat pham duoc pham dung Use_Potion (Packet 32)
                GlobalService.gI().Use_Potion((short)item.Id);
            } else if (item.ItemCatagory == 7) {
                GlobalService.gI().doUseMaterial((short)item.Id);
            } else if (item.ItemCatagory == 3) {
                GlobalService.gI().Use_Item((byte)item.Id, (byte)0);
            } else {
                GlobalService.gI().Use_Potion((short)item.Id);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void update() {
        AutoUpgrade.update();
        loadConfig();

        // 1. Ghi nho nhan vat khi dang trong tran
        if (GameCanvas.currentScreen == GameCanvas.game && GameScreen.player != null) {
            saveCurrentChar();
            if (reconnectAttempts > 0) {
                reconnectAttempts = 0;
            }
            isReconnectingChar = false;
        }

        // 2. Kiem tra mat ket noi khi dang choi
        if (isAutoReconnect && !isWaitingReconnect) {
            if (GameCanvas.currentScreen == GameCanvas.game && !Session_ME.gI().isConnected()) {
                onDisconnect();
            }
        }

        // Only the explicit cancel row consumes a touch; the HUD is not a banner.
        if (hudVisible && hudCancelY >= 0 && GameCanvas.currentDialog == null
                && !GameCanvas.menu2.isShowMenu && GameCanvas.isPointerSelect
                && GameCanvas.isPoint(4, hudCancelY, hudBoxW, hudBoxH)) {
            GameCanvas.isPointerSelect = false;
            if (hudCancelAction == 1 && isWaitingReconnect) cancelReconnect();
            else if (hudCancelAction == 2 && reviveState == 1) cancelRevive();
            else if (hudCancelAction == 3 && isGoBackActive) cancelGoBack();
            hudCancelY = -1;
            hudRefresh = 0L;
        }


        // 4. Xu ly dem nguoc tu dong dang nhap lai
        if (isAutoReconnect && isWaitingReconnect) {
            if (GameCanvas.currentDialog != null) {
                GameCanvas.end_Dialog();
            }
            long now = mSystem.currentTimeMillis();
            if (now >= timeTargetReconnect) {
                isWaitingReconnect = false;
                doReconnect();
            }
        }

        AutoPetFeed.update();
        AutoSellManager.update();
        GoBackProfile.bind();
        GoBackNavigator.observe();
        rememberFarmLocation();
        if (isGoBackEnabled && !AutoSellManager.isReturningToSell && AutoSellManager.getQueueCount() == 0
                && farmMapId >= 0 && GameCanvas.currentScreen == GameCanvas.game
                && GameScreen.player != null && GameScreen.player.Action != 4 && GameScreen.player.hp > 0) {
            updateGoBack();
        }

        // 5. Xu ly Auto Hoi Sinh khi dang trong game
        if (shouldAutoRevive() && GameCanvas.currentScreen == GameCanvas.game && GameScreen.player != null) {
            updateAutoRevive();
        } else if (GameCanvas.currentScreen != GameCanvas.game) {
            reviveState = 0;
            isReviveCancelled = false;
            isCheckingGoldRevive = false;
        }

        // 6. Xu ly Auto Danh Quai
        if (isAutoAttack && GameCanvas.currentScreen == GameCanvas.game && GameScreen.player != null && GameScreen.player.Action != 4) {
            updateAutoAttack();
        }

        // 7. Xu ly Auto Bom HP / MP
        if (isAutoHpMp && GameCanvas.currentScreen == GameCanvas.game && GameScreen.player != null && GameScreen.player.Action != 4) {
            updateAutoHpMp();
        }

        // 8. Xu ly Auto Thu Cuoi
        if (autoMountType > 0 && GameCanvas.currentScreen == GameCanvas.game && GameScreen.player != null && GameScreen.player.Action != 4) {
            updateAutoMount();
        }
        AutoFarmSupport.update();
    }

    private static void updateAutoAttack() {
        if (isGoBackActive) {
            return;
        }
        Player player = GameScreen.player;
        if (player == null || player.Action == 4) {
            return;
        }

        // 1. Kich hoat trang thai danh neu chua bat
        if (Player.isAutoFire != 1) {
            Player.isAutoFire = 1;
            Player.isCurAutoFire = true;
            player.dofire_();
            Player.xBeginAutoFire = player.x;
            Player.yBeginAutofire = player.y;
        }

        // 2. Neu chua co muc tieu hoac muc tieu da chet -> Tim quai moi
        if (GameScreen.ObjFocus == null || GameScreen.ObjFocus.Action == 4 || GameScreen.ObjFocus.isDie || GameScreen.ObjFocus.typeObject != 1) {
            player.nextMonster();
        }

        // 3. Neu da co quai va nhan vat dang ranh -> Goi danh quai
        if (GameScreen.ObjFocus != null && player.Action == 0) {
            player.setAutoFire();
        }
    }

    private static void updateAutoHpMp() {
        Player player = GameScreen.player;
        if (player == null || player.maxHp <= 0 || player.maxMp <= 0) {
            return;
        }

        long now = mSystem.currentTimeMillis();

        // Kiem tra HP
        if (player.hp * 100 / player.maxHp < autoHpPercent) {
            if (Player.timeDelayPotion != null && Player.timeDelayPotion.length > 0 && Player.timeDelayPotion[0].value <= 0) {
                Item hpPotion = findPotion(0);
                if (hpPotion != null) {
                    GlobalService.gI().Use_Potion((short)hpPotion.Id);
                    Player.timeDelayPotion[0].value = 2000;
                    Player.timeDelayPotion[0].limit = 2000;
                    Player.timeDelayPotion[0].timebegin = now;
                }
            }
        }

        // Kiem tra MP
        if (player.mp * 100 / player.maxMp < autoMpPercent) {
            if (Player.timeDelayPotion != null && Player.timeDelayPotion.length > 1 && Player.timeDelayPotion[1].value <= 0) {
                Item mpPotion = findPotion(1);
                if (mpPotion != null) {
                    GlobalService.gI().Use_Potion((short)mpPotion.Id);
                    Player.timeDelayPotion[1].value = 2000;
                    Player.timeDelayPotion[1].limit = 2000;
                    Player.timeDelayPotion[1].timebegin = now;
                }
            }
        }
    }

    private static void updateAutoMount() {
        Player player = GameScreen.player;
        if (player == null || player.typeMount != -1 || player.hp <= 0 || player.Action == 4) {
            return;
        }

        long now = mSystem.currentTimeMillis();
        if (now - lastMountAttemptTime < 3000L) {
            return;
        }
        lastMountAttemptTime = now;

        Item mountItem = findMountItem(autoMountType);
        if (mountItem != null) {
            useMountItem(mountItem);
        }
    }

    private static boolean shouldAutoRevive() {
        return autoReviveMode > 0 || (isGoBackEnabled && !isGoBackPaused && farmMapId >= 0);
    }

    private static void updateAutoRevive() {
        boolean isDead = (GameScreen.player != null && (GameScreen.player.hp <= 0 || GameScreen.player.Action == 4));

        if (!isDead) {
            reviveState = 0;
            isReviveCancelled = false;
            isCheckingGoldRevive = false;
            return;
        }

        if (isReviveCancelled) {
            return;
        }

        long now = mSystem.currentTimeMillis();

        if (reviveState == 0) {
            reviveState = 1;
            reviveDelaySec = 3;
            timeTargetRevive = now + 3000L;
            return;
        }

        if (reviveState == 1) {
            if (now >= timeTargetRevive) {
                reviveState = 2;
                timeSentRevive = now;

                if (autoReviveMode == 1 || (autoReviveMode == 0 && shouldAutoRevive())) {
                    GlobalService.gI().gohome((byte)0);
                    GameCanvas.end_Dialog();
                    GameCanvas.addInfoCharCline("[HSO] T\u1ef1 \u0111\u1ed9ng V\u1ec0 L\u00c0NG!");
                } else if (autoReviveMode == 2) {
                    boolean hasItem = hasReviveItem();
                    if (hasItem) {
                        GlobalService.gI().gohome((byte)1);
                        GameCanvas.end_Dialog();
                        String itemName = getReviveItemName();
                        GameCanvas.addInfoCharCline("H\u1ed3i sinh t\u1ea1i ch\u1ed7 b\u1eb1ng: " + (itemName.length() > 0 ? itemName : "V\u1eadt ph\u1ea9m"));
                    } else {
                        isCheckingGoldRevive = true;
                        GlobalService.gI().gohome((byte)1);
                        GameCanvas.end_Dialog();
                    }
                }
            }
            return;
        }

        if (reviveState == 2) {
            if (isCheckingGoldRevive) {
                if (now - timeSentRevive > 2000L) {
                    isCheckingGoldRevive = false;
                    GlobalService.gI().gohome((byte)0);
                    GameCanvas.end_Dialog();
                    GameCanvas.addInfoCharCline("Kh\u00f4ng c\u00f3 h\u1ed3i sinh v\u00e0ng -> T\u1ef1 \u0111\u1ed9ng V\u1ec0 L\u00c0NG!");
                }
            }
        }
    }

    private static void doReconnect() {
        try {
            LoginScreen.loadUser_Pass();
            GameCanvas.connect();
            MsgDialog.isAutologin = true;
            isReconnectingChar = true;
            String string = LoginScreen.tfusername != null ? LoginScreen.tfusername.getText() : "";
            String string2 = LoginScreen.tfpassword != null ? LoginScreen.tfpassword.getText() : "";
            if (string == null || string.length() == 0) {
                string = LoginScreen.username;
                string2 = LoginScreen.password;
            }
            if (string != null && string.length() > 0) {
                GlobalService.gI().login(string, string2, GameMidlet.version, "0", "0", "0", lastCharId, lastArea);
            }
        }
        catch (Exception exception) {
            onConnectFail();
        }
    }

    public static void triggerRelogToVillage() {
        try {
            Session_ME.gI().close();
        } catch (Exception ex) {}
        GameCanvas.end_Dialog();
        isWaitingReconnect = true;
        isReconnectingChar = true;
        MsgDialog.isAutologin = true;
        timeTargetReconnect = mSystem.currentTimeMillis() + 600L;
    }

    public static void matchLastChar(SelectCharScreen selectCharScreen) {
        if (SelectCharScreen.VecSelectChar == null || SelectCharScreen.VecSelectChar.size() == 0) {
            return;
        }
        if (lastCharName != null && lastCharName.length() > 0) {
            for (int i = 0; i < SelectCharScreen.VecSelectChar.size(); ++i) {
                Other_Players other_Players = (Other_Players)SelectCharScreen.VecSelectChar.elementAt(i);
                if (other_Players == null || other_Players.name == null || !other_Players.name.equals(lastCharName)) continue;
                selectCharScreen.selectChar = i;
                SelectCharScreen.IDCHAR = other_Players.ID;
                return;
            }
        }
        if (lastCharId >= 0) {
            for (int i = 0; i < SelectCharScreen.VecSelectChar.size(); ++i) {
                Other_Players other_Players = (Other_Players)SelectCharScreen.VecSelectChar.elementAt(i);
                if (other_Players == null || other_Players.ID != lastCharId) continue;
                selectCharScreen.selectChar = i;
                SelectCharScreen.IDCHAR = other_Players.ID;
                return;
            }
        }
    }

    public static void paintHUD(mGraphics g) {
        hudCancelY = -1;
        if (g == null || !hudVisible || GameCanvas.currentDialog != null
                || GameCanvas.menu2.isShowMenu
                || (GameCanvas.currentScreen != GameCanvas.game && !isWaitingReconnect)) return;
        long now = mSystem.currentTimeMillis();
        if (now >= hudRefresh) {
            hudRefresh = now + 500L;
            hudBoxW = Math.min(170, GameCanvas.w - 8);
            hudLineCount = 0;
            hudCancelAction = 0;
            hudLines[hudLineCount++] = "Đánh: " + enabled(isAutoAttack) + " | HP/MP: " + enabled(isAutoHpMp);
            hudLines[hudLineCount++] = "Bán đồ: " + enabled(AutoSellManager.isAutoSellEnabled)
                + " | Chat: " + enabled(AutoChat.isAutoChat);
            if (isAutoHpMp) hudLines[hudLineCount++] = "HP < " + autoHpPercent + "% | MP < " + autoMpPercent + "%";
            if (isWaitingReconnect) {
                hudLines[hudLineCount++] = "Đăng nhập lại: " + Math.max(0L, (timeTargetReconnect - now + 999L) / 1000L) + "s";
                hudCancelAction = 1;
            } else if (reviveState == 1) {
                hudLines[hudLineCount++] = "Hồi sinh: " + Math.max(0L, (timeTargetRevive - now + 999L) / 1000L) + "s";
                hudCancelAction = 2;
            } else if (reviveState == 2) {
                hudLines[hudLineCount++] = "Đang hồi sinh...";
            } else if (isGoBackActive) {
                hudLines[hudLineCount++] = "Về: " + farmMapName + " / Khu " + farmAreaLabel();
                hudLines[hudLineCount++] = goBackStepInfo;
                hudCancelAction = 3;
            } else {
                hudLines[hudLineCount++] = "GoBack: " + enabled(isGoBackEnabled);
                hudLines[hudLineCount++] = "Hồi sinh: " + (autoReviveMode == 0 ? "Tắt" : autoReviveMode == 1 ? "Về làng" : "Tại chỗ")
                    + " | Kết nối: " + enabled(isAutoReconnect);
            }
            if (AutoFarmSupport.zoneEnabled || AutoFarmSupport.fireEnabled || AutoFarmSupport.oilEnabled || AutoFarmSupport.bookEnabled)
                hudLines[hudLineCount++] = AutoFarmSupport.status;
            if (hudCancelAction != 0) hudLines[hudLineCount++] = "[Chạm để hủy]";
            for (int i = 0; i < hudLineCount; i++) hudLines[i] = fitGoBackHUD(hudLines[i]);
        }
        int y = hudY();
        for (int i = 0; i < hudLineCount; i++) {
            mFont.tahoma_7_black.drawString(g, hudLines[i], 5, y + 1, 0, false);
            mFont.tahoma_7_yellow.drawString(g, hudLines[i], 4, y, 0, false);
            if (i == hudLineCount - 1 && hudCancelAction != 0) hudCancelY = y;
            y += hudBoxH;
        }
    }

    private static String fitGoBackHUD(String text) {
        if(text==null) return "";
        if(mFont.tahoma_7_white.getWidth(text)<=hudBoxW-12) return text;
        while(text.length()>0 && mFont.tahoma_7_white.getWidth(text+"...")>hudBoxW-12)
            text=text.substring(0,text.length()-1);
        return text+"...";
    }

    public static void toggleAutoReconnect() {
        loadConfig();
        isAutoReconnect = !isAutoReconnect;
        saveConfig();
    }

    public static void toggleAutoRevive() {
        loadConfig();
        autoReviveMode = (autoReviveMode + 1) % 3;
        saveConfig();
        String string = "T\u1eaeT";
        if (autoReviveMode == 1) {
            string = "V\u1ec1 L\u00e0ng";
        } else if (autoReviveMode == 2) {
            string = "T\u1ea1i Ch\u1ed7";
        }
    }

    public static void toggleAutoAttack() {
        loadConfig();
        isAutoAttack = !isAutoAttack;
        saveConfig();
        if (isAutoAttack) {
            if (!isGoBackActive && GameCanvas.currentScreen == GameCanvas.game && GameScreen.player != null && GameCanvas.loadmap != null) {
                if (!isVillage(GameCanvas.loadmap.idMap)) {
                    saveCurrentFarmLocation();
                }
            }
            if (GameCanvas.currentScreen == GameCanvas.game && GameScreen.player != null) {
                GameScreen.player.dofire_();
                Player.xBeginAutoFire = GameScreen.player.x;
                Player.yBeginAutofire = GameScreen.player.y;
                GameScreen.player.nextMonster();
                if (GameScreen.ObjFocus != null) {
                    GameScreen.player.setAutoFire();
                }
            }
        } else {
            Player.isAutoFire = (byte)-1;
            Player.isCurAutoFire = false;
        }
    }

    public static void toggleAutoHpMp() {
        loadConfig();
        isAutoHpMp = !isAutoHpMp;
        saveConfig();
    }

    public static void cycleHpPercent() {
        loadConfig();
        int[] presets = new int[]{20, 30, 40, 50, 60, 70, 80};
        int next = 50;
        for (int i = 0; i < presets.length; ++i) {
            if (presets[i] == autoHpPercent) {
                next = presets[(i + 1) % presets.length];
                break;
            }
        }
        autoHpPercent = next;
        saveConfig();
        GameCanvas.addInfoCharCline("B\u01a1m HP khi d\u01b0\u1edbi " + autoHpPercent + "%");
    }

    public static void cycleMpPercent() {
        loadConfig();
        int[] presets = new int[]{20, 30, 40, 50, 60, 70, 80};
        int next = 40;
        for (int i = 0; i < presets.length; ++i) {
            if (presets[i] == autoMpPercent) {
                next = presets[(i + 1) % presets.length];
                break;
            }
        }
        autoMpPercent = next;
        saveConfig();
        GameCanvas.addInfoCharCline("B\u01a1m MP khi d\u01b0\u1edbi " + autoMpPercent + "%");
    }

    public static void startInputHpPercent() {
        inputHp = new InputDialog();
        inputHp.setinfo("B\u01a1m HP khi d\u01b0\u1edbi % (1-99):", new iCommand("L\u01b0u", new IAction(){

            public void perform() {
                try {
                    int val = Integer.parseInt(inputHp.tfInput.getText().trim());
                    if (val < 1) val = 1;
                    if (val > 99) val = 99;
                    autoHpPercent = val;
                    saveConfig();
                    GameCanvas.addInfoCharCline("\u0110\u00e3 \u0111\u1eb7t b\u01a1m HP khi d\u01b0\u1edbi " + autoHpPercent + "%");
                }
                catch (Exception exception) {
                    GameCanvas.addInfoCharCline("S\u1ed1 % kh\u00f4ng h\u1ee3p l\u1ec7!");
                }
                inputHp.closeDialog();
            }
        }), true, "C\u00e0i % HP");
        inputHp.tfInput.setText(String.valueOf(autoHpPercent));
        GameCanvas.currentDialog = inputHp;
    }

    public static void startInputMpPercent() {
        inputMp = new InputDialog();
        inputMp.setinfo("B\u01a1m MP khi d\u01b0\u1edbi % (1-99):", new iCommand("L\u01b0u", new IAction(){

            public void perform() {
                try {
                    int val = Integer.parseInt(inputMp.tfInput.getText().trim());
                    if (val < 1) val = 1;
                    if (val > 99) val = 99;
                    autoMpPercent = val;
                    saveConfig();
                    GameCanvas.addInfoCharCline("\u0110\u00e3 \u0111\u1eb7t b\u01a1m MP khi d\u01b0\u1edbi " + autoMpPercent + "%");
                }
                catch (Exception exception) {
                    GameCanvas.addInfoCharCline("S\u1ed1 % kh\u00f4ng h\u1ee3p l\u1ec7!");
                }
                inputMp.closeDialog();
            }
        }), true, "C\u00e0i % MP");
        inputMp.tfInput.setText(String.valueOf(autoMpPercent));
        GameCanvas.currentDialog = inputMp;
    }

    public static void showHpMpMenu() {
        loadConfig();
        mVector mVector2 = new mVector("Menu HP/MP");

        String strStatus = isAutoHpMp ? "T\u1ef1 b\u01a1m HP/MP: B\u1eacT" : "T\u1ef1 b\u01a1m HP/MP: T\u1eaeT";
        mVector2.addElement(new iCommand(strStatus, new IAction(){

            public void perform() {
                toggleAutoHpMp();
                showHpMpMenu();
            }
        }));

        mVector2.addElement(new iCommand("B\u01a1m HP: " + autoHpPercent + "% (Ch\u1ea1m \u0111\u1ec3 \u0111\u1ed5i)", new IAction(){

            public void perform() {
                cycleHpPercent();
                showHpMpMenu();
            }
        }));

        mVector2.addElement(new iCommand("Nh\u1eadp % HP t\u00f9y ch\u1ec9nh", new IAction(){

            public void perform() {
                startInputHpPercent();
            }
        }));

        mVector2.addElement(new iCommand("B\u01a1m MP: " + autoMpPercent + "% (Ch\u1ea1m \u0111\u1ec3 \u0111\u1ed5i)", new IAction(){

            public void perform() {
                cycleMpPercent();
                showHpMpMenu();
            }
        }));

        mVector2.addElement(new iCommand("Nh\u1eadp % MP t\u00f9y ch\u1ec9nh", new IAction(){

            public void perform() {
                startInputMpPercent();
            }
        }));

        mVector2.addElement(new iCommand("Quay l\u1ea1i", new IAction(){

            public void perform() {
                showMenu();
            }
        }));

        GameCanvas.menu2.startAt(mVector2, 2, "C\u00c0I \u0110\u1eb6T B\u01a0M HP/MP", false, null);
    }

    public static String getMountName(int type) {
        switch (type) {
            case 1: return "Ng\u1ef1a N\u00e2u (ID 62)";
            case 2: return "Ng\u1ef1a Tr\u1eafng (ID 63)";
            case 3: return "Ng\u1ef1a Gi\u00e1p (ID 64)";
            case 4: return "Ng\u1ef1a \u0110\u1ecf (ID 65)";
            case 5: return "Ng\u1ef1a \u0110en (ID 66)";
            case 6: return "B\u1ea5t k\u1ef3 trong t\u00fai";
            case 7: {
                if (autoMountCustomName != null && autoMountCustomName.length() > 0) {
                    return autoMountCustomName;
                }
                return "T\u00fai \u0110\u1ed3 (ID " + autoMountCustomId + ")";
            }
            default: return "T\u1eaeT";
        }
    }

    public static void setMountType(int type) {
        loadConfig();
        autoMountType = type;
        saveConfig();
        GameCanvas.addInfoCharCline("Auto Th\u00fa C\u01b0\u1ee1i: " + getMountName(autoMountType));
        if (autoMountType == 1) {
            GameCanvas.addInfoCharCline("L\u01b0u \u00fd: Ng\u1ef1a N\u00e2u kh\u00f4ng th\u1ec3 \u0111\u00e1nh qu\u00e1i khi c\u01b0\u1ee1i!");
        }
        if (GameCanvas.currentScreen == GameCanvas.game && GameScreen.player != null) {
            if (autoMountType == 0) {
                // Xuong ngua: dung Packet -97 voi tham so -1
                GlobalService.gI().useMount((byte)-1);
            } else {
                lastMountAttemptTime = 0L;
                Item mountItem = findMountItem(autoMountType);
                if (mountItem != null) {
                    useMountItem(mountItem);
                    GameCanvas.addInfoCharCline("Auto l\u00ean: " + (mountItem.itemName != null ? mountItem.itemName : ("ID " + mountItem.Id)));
                } else {
                    GlobalService.gI().useMount((byte)-1);
                    GameCanvas.addInfoCharCline("Kh\u00f4ng c\u00f3 " + getMountName(autoMountType) + " trong t\u00fai \u0111\u1ed3!");
                }
            }
        }
    }

    public static void setCustomMount(int id, String name) {
        loadConfig();
        autoMountType = 7;
        autoMountCustomId = id;
        autoMountCustomName = (name != null ? name : ("ID " + id));
        saveConfig();
        GameCanvas.addInfoCharCline("Auto Th\u00fa C\u01b0\u1ee1i: " + autoMountCustomName);
        if (GameCanvas.currentScreen == GameCanvas.game && GameScreen.player != null) {
            lastMountAttemptTime = 0L;
            Item mountItem = findMountItem(autoMountType);
            if (mountItem != null) {
                useMountItem(mountItem);
                GameCanvas.addInfoCharCline("Auto l\u00ean: " + (mountItem.itemName != null ? mountItem.itemName : ("ID " + mountItem.Id)));
            } else {
                GlobalService.gI().useMount((byte)-1);
                GameCanvas.addInfoCharCline("Kh\u00f4ng t\u00ecm th\u1ea5y " + autoMountCustomName + " trong t\u00fai!");
            }
        }
    }

    public static void showInventoryMountsMenu() {
        loadConfig();
        mVector list = new mVector("Menu Inven Mounts");

        if (Item.VecInvetoryPlayer != null) {
            for (int i = 0; i < Item.VecInvetoryPlayer.size(); ++i) {
                Item item = (Item)Item.VecInvetoryPlayer.elementAt(i);
                if (item != null && (isHorseId(item.Id) || isMountKeyword(item.itemName))) {
                    final int mId = item.Id;
                    final String mName = item.itemName != null ? item.itemName : ("ID " + item.Id);
                    String caption = mName + (item.numPotion > 1 ? (" (x" + item.numPotion + ")") : "");
                    list.addElement(new iCommand(caption, new IAction() {
                        public void perform() {
                            setCustomMount(mId, mName);
                            showMenu();
                        }
                    }));
                }
            }
        }

        if (list.size() == 0) {
            GameCanvas.start_Ok_Dialog(T.khongcongua);
            return;
        }

        list.addElement(new iCommand("Quay l\u1ea1i", new IAction() {
            public void perform() {
                showMountMenu();
            }
        }));

        GameCanvas.menu2.startAt(list, 2, "TH\u00da C\u01af\u1ee0I TRONG T\u00daI", false, null);
    }

    public static void showMountMenu() {
        loadConfig();
        mVector mVector2 = new mVector("Menu Thu Cuoi");

        mVector2.addElement(new iCommand("T\u1eaeT Th\u00fa C\u01b0\u1ee1i", new IAction(){
            public void perform() {
                setMountType(0);
                showReviveMountMenu();
            }
        }));

        mVector2.addElement(new iCommand("B\u1ea5t k\u1ef3 trong t\u00fai", new IAction(){
            public void perform() {
                setMountType(6);
                showReviveMountMenu();
            }
        }));

        mVector2.addElement(new iCommand("Ch\u1ecdn t\u1eeb t\u00fai \u0111\u1ed3 (Qu\u00e9t)", new IAction(){
            public void perform() {
                showInventoryMountsMenu();
            }
        }));

        mVector2.addElement(new iCommand("Ng\u1ef1a N\u00e2u (ID 62)", new IAction(){
            public void perform() {
                setMountType(1);
                showReviveMountMenu();
            }
        }));

        mVector2.addElement(new iCommand("Ng\u1ef1a Tr\u1eafng (ID 63)", new IAction(){
            public void perform() {
                setMountType(2);
                showReviveMountMenu();
            }
        }));

        mVector2.addElement(new iCommand("Ng\u1ef1a Chi\u1ebfn Gi\u00e1p (ID 64)", new IAction(){
            public void perform() {
                setMountType(3);
                showReviveMountMenu();
            }
        }));

        mVector2.addElement(new iCommand("Ng\u1ef1a \u0110\u1ecf (ID 65)", new IAction(){
            public void perform() {
                setMountType(4);
                showReviveMountMenu();
            }
        }));

        mVector2.addElement(new iCommand("Ng\u1ef1a \u0110en (ID 66)", new IAction(){
            public void perform() {
                setMountType(5);
                showReviveMountMenu();
            }
        }));

        mVector2.addElement(new iCommand("Quay l\u1ea1i", new IAction(){
            public void perform() {
                showReviveMountMenu();
            }
        }));

        GameCanvas.menu2.startAt(mVector2, 2, "CH\u1eccN TH\u00da C\u01af\u1ee0I", false, null);
    }

    // ==========================================
    // CÁC THUẬT TOÁN VÀ HÀM HỖ TRỢ GOBACK
    // ==========================================

    public static String stripAccents(String s) {
        if (s == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            // Bo qua cac dau ket hop trong chuoi Unicode UTF-8
            if (c >= '\u0300' && c <= '\u036f') {
                continue;
            }
            c = Character.toLowerCase(c);
            switch (c) {
                case '\u00e0': case '\u00e1': case '\u1ea1': case '\u1ea3': case '\u00e3':
                case '\u00e2': case '\u1ea7': case '\u1ea5': case '\u1ead': case '\u1ea9': case '\u1eab':
                case '\u0103': case '\u1eb1': case '\u1eaf': case '\u1eb7': case '\u1eb3': case '\u1eb5':
                    sb.append('a'); break;
                case '\u00e8': case '\u00e9': case '\u1eb9': case '\u1ebb': case '\u1ebd':
                case '\u00ea': case '\u1ec1': case '\u1ebf': case '\u1ec7': case '\u1ec3': case '\u1ec5':
                    sb.append('e'); break;
                case '\u00ec': case '\u00ed': case '\u1ecb': case '\u1ec9': case '\u0129':
                    sb.append('i'); break;
                case '\u00f2': case '\u00f3': case '\u1ecd': case '\u1ecf': case '\u00f5':
                case '\u00f4': case '\u1ed3': case '\u1ed1': case '\u1ed9': case '\u1ed5': case '\u1ed7':
                case '\u01a1': case '\u1edd': case '\u1edb': case '\u1ee3': case '\u1edf': case '\u1ee1':
                    sb.append('o'); break;
                case '\u00f9': case '\u00fa': case '\u1ee5': case '\u1ee7': case '\u0169':
                case '\u01b0': case '\u1eeb': case '\u1ee9': case '\u1ef1': case '\u1eed': case '\u1eef':
                    sb.append('u'); break;
                case '\u1ef3': case '\u00fd': case '\u1ef5': case '\u1ef7': case '\u1ef9':
                    sb.append('y'); break;
                case '\u0111': case '\u0110':
                    sb.append('d'); break;
                default:
                    if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                        sb.append(c);
                    } else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                        sb.append(' ');
                    }
                    break;
            }
        }
        return sb.toString().trim();
    }

    public static String farmAreaLabel() {
        return farmArea < 0 ? "Bat ky" : String.valueOf(farmArea + 1);
    }

    public static String getMapName(int mapId) {
        if (GameCanvas.loadmap != null && GameCanvas.loadmap.idMap == mapId
                && GameCanvas.loadmap.nameMap != null && GameCanvas.loadmap.nameMap.length() > 0)
            return GameCanvas.loadmap.nameMap;
        int observed = GoBackNavigator.graph.byId(mapId);
        if (observed >= 0) return GoBackNavigator.graph.get(observed).key;
        if (mapId >= 0 && T.mapName != null && mapId < T.mapName.length && T.mapName[mapId] != null && T.mapName[mapId].length() > 0) {
            return T.mapName[mapId];
        }
        return "Map " + mapId;
    }

    public static int getMapIdByName(String name) {
        String wanted = stripAccents(name);
        if (wanted.length() == 0) return -1;
        int observed = GoBackNavigator.graph.byName(wanted);
        if (observed >= 0 && GoBackNavigator.graph.get(observed).mapId >= 0)
            return GoBackNavigator.graph.get(observed).mapId;
        int found = -1;
        if (T.mapName != null) for (int i = 0; i < T.mapName.length; i++) {
            if (!wanted.equals(stripAccents(T.mapName[i]))) continue;
            if (found >= 0) return -1;
            found = i;
        }
        return found;
    }

    private static long lastFarmSaveTime = 0L;
    private static boolean farmLocationDirty = false;
    private static boolean resumeNativeAutoFire = false;

    private static void stopGoBackMovement() {
        GoBackNavigator.stop();
    }

    // Only capture an active farming session; never overwrite it in a village
    // or with a waypoint used while returning to the saved map.
    private static void rememberFarmLocation() {
        Player p = GameScreen.player;
        long now = mSystem.currentTimeMillis();
        if (farmLocationDirty && (now - lastFarmSaveTime >= 5000L
                || GameCanvas.currentScreen != GameCanvas.game || p == null
                || p.hp <= 0 || p.Action == 4 || !LoadMapScreen.isNextMap)) {
            saveConfig();
            lastFarmSaveTime = now;
            farmLocationDirty = false;
        }
        if (!isGoBackEnabled || isGoBackPaused || isGoBackActive || GoBackNavigator.needsReturn()
                || GameCanvas.currentScreen != GameCanvas.game || p == null
                || GameCanvas.loadmap == null || !LoadMapScreen.isNextMap
                || p.hp <= 0 || p.Action == 4 || GameScreen.isMapLang
                || isVillage(GameCanvas.loadmap.idMap)
                || (!isAutoAttack && Player.isAutoFire != 1)) return;
        int map = GameCanvas.loadmap.idMap;
        int observed = GoBackNavigator.graph.byId(map);
        if (farmMapId < 0 && (observed < 0 || GoBackNavigator.graph.get(observed).maxLevel <= 0)) return;
        if (farmMapId >= 0 && farmMapId != map) return;
        if (farmMapId == map && farmX >= 0 && farmY >= 0) return;
        if (!GoBackPosition.valid()) return;
        farmMapId = map;
        farmMapName = GoBackNavigator.currentName();
        farmArea = LoadMap.Area;
        farmX = GoBackPosition.snap(p.x);
        farmY = GoBackPosition.snap(p.y);
        farmLocationDirty = true;
        if (now - lastFarmSaveTime >= 5000L) {
            lastFarmSaveTime = now;
            saveConfig();
            farmLocationDirty = false;
        }
    }

    private static void beginGoBack() {
        if (!isGoBackActive) {
            resumeNativeAutoFire = Player.isAutoFire == 1 || Player.isCurAutoFire;
            GoBackNavigator.start();
            saveConfig();
        }
        isGoBackActive = true;
        Player.isAutoFire = (byte)-1;
        Player.isCurAutoFire = false;
        GameScreen.ObjFocus = null;
    }

    public static void resumeGoBackAfterSell() {
        if (!isGoBackEnabled || farmMapId < 0) return;
        isGoBackActive = false;
        beginGoBack();
    }

    public static void updateGoBack() {
        if (!isGoBackEnabled || farmMapId < 0 || isGoBackPaused || !GoBackNavigator.ready()) return;
        Player player = GameScreen.player;
        if (player.hp <= 0 || player.Action == 4) return;
        if (GameCanvas.loadmap.idMap == farmMapId && !isGoBackActive
                && !GoBackNavigator.needsReturn()) return;
        beginGoBack();
        boolean arrived = GoBackNavigator.update(farmMapId, farmX, farmY, AutoFarmSupport.goBackArea(farmArea));
        goBackStepInfo = GoBackNavigator.status;
        if (!arrived) return;
        isGoBackActive = false;
        goBackStepInfo = "";
        GoBackNavigator.stop();
        if (farmX < 0 || farmY < 0) {
            farmX = GoBackPosition.snap(player.x); farmY = GoBackPosition.snap(player.y);
            if (farmArea < 0) farmArea = LoadMap.Area;
            farmMapName = GoBackNavigator.currentName(); saveConfig();
        }
        GameCanvas.addInfoCharCline("GoBack: Da ve " + farmMapName + " - Khu " + (LoadMap.Area + 1));
        isAutoAttack = true;
        Player.isAutoFire = 1;
        Player.isCurAutoFire = true;
        player.dofire_();
        Player.xBeginAutoFire = player.x; Player.yBeginAutofire = player.y;
        player.nextMonster();
        if (GameScreen.ObjFocus != null) player.setAutoFire();
    }

    public static void cancelGoBack() {
        isGoBackActive = false;
        isGoBackPaused = true;
        goBackStepInfo = "";
        stopGoBackMovement();
        GameCanvas.addInfoCharCline("H\u1ee7y GoBack! (T\u1ea1m d\u1eebng)");
    }

    public static void toggleGoBack() {
        loadConfig();
        isGoBackEnabled = !isGoBackEnabled;
        isGoBackPaused = false;
        if (!isGoBackEnabled) {
            cancelGoBack();
        }
        saveConfig();
    }

    public static void saveCurrentFarmLocation() {
        if (!GoBackPosition.valid()) {
            GameCanvas.addInfoCharCline("Vui l\u00f2ng v\u00e0o map tr\u01b0\u1edbc khi l\u01b0u v\u1ecb tr\u00ed UP!");
            return;
        }
        int cur = GameCanvas.loadmap.idMap;
        if (isVillage(cur)) {
            GameCanvas.addInfoCharCline("\u0110ang \u1edf L\u00e0ng (" + getMapName(cur) + ")! H\u00e3y ra map qu\u00e1i ho\u1eb7c ch\u1ecdn t\u1eeb danh s\u00e1ch!");
            return;
        }
        stopGoBackMovement();
        isGoBackActive = false;
        farmMapId = cur;
        farmMapName = GoBackNavigator.currentName();
        farmX = GoBackPosition.snap(GameScreen.player.x);
        farmY = GoBackPosition.snap(GameScreen.player.y);
        farmArea = LoadMap.Area;
        isGoBackEnabled = true;
        isGoBackPaused = false;
        saveConfig();
        GameCanvas.addInfoCharCline("\u0110\u00e3 l\u01b0u v\u1ecb tr\u00ed UP: " + farmMapName + " (Khu " + farmAreaLabel() + ", " + GoBackPosition.label(farmX, farmY) + ")");
    }

    public static void setFarmMap(int mapId, int x, int y, byte area) {
        farmMapId = mapId;
        farmMapName = getMapName(mapId);
        farmX = x;
        farmY = y;
        farmArea = area;
        isGoBackEnabled = true;
        beginGoBack();
        GoBackNavigator.start();
        isGoBackPaused = false;
        saveConfig();
        GameCanvas.addInfoCharCline("[HONG PHUC HSO] \u0110\u00e3 ch\u1ecdn Map UP: " + farmMapName + "! \u0110ang di chuy\u1ec3n...");
    }

    public static void startGoBackNow() {
        if (farmMapId < 0) {
            GameCanvas.addInfoCharCline("Ch\u01b0a c\u00f3 v\u1ecb tr\u00ed UP n\u00e0o \u0111\u01b0\u1ee3c l\u01b0u!");
            return;
        }
        isGoBackEnabled = true;
        beginGoBack();
        GoBackNavigator.start();
        isGoBackPaused = false;
        saveConfig();
        GameCanvas.addInfoCharCline("B\u1eaft \u0111\u1ea7u GoBack v\u1ec1: " + farmMapName + " (Khu " + farmAreaLabel() + ")...");
    }

    public static void clearGoBackLocation() {
        loadConfig();
        stopGoBackMovement();
        farmMapId = -1;
        farmMapName = "";
        farmX = 0;
        farmY = 0;
        farmArea = 0;
        isGoBackActive = false;
        isGoBackPaused = false;
        saveConfig();
        GameCanvas.addInfoCharCline("\u0110\u00e3 x\u00f3a v\u1ecb tr\u00ed Map UP \u0111\u00e3 l\u01b0u!");
    }

    public static void showSelectFarmMapMenu() {
        mVector mVector2 = new mVector("Menu Chon Map UP");

        // Only offer maps actually observed with monsters. No fake coordinates or levels.
        for (int i = 0; i < GoBackNavigator.graph.nodes.size(); ++i) {
            GoBackRoutes.Node node = GoBackNavigator.graph.get(i);
            if (node.mapId < 0 || node.maxLevel <= 0) continue;
            final int mapId = node.mapId;
            String label = getMapName(mapId) + " (Lv " + node.minLevel + "-" + node.maxLevel + ")";
            mVector2.addElement(new iCommand(label, new IAction() {
                public void perform() {
                    setFarmMap(mapId, -1, -1, (byte)-1);
                }
            }));
        }
        if (mVector2.size() == 0) {
            GameCanvas.addInfoCharCline("Chua ghi nhan map quai. Den map UP va luu vi tri hien tai.");
        }

        mVector2.addElement(new iCommand("Quay l\u1ea1i", new IAction() {
            public void perform() {
                showGoBackMenu();
            }
        }));

        GameCanvas.menu2.startAt(mVector2, 2, "CH\u1eccN MAP UP T\u1eea DANH S\u00c1CH", false, null);
    }

    public static void cycleFarmArea() {
        loadConfig();
        if (farmArea < 0) {
            farmArea = 0;
        } else if (farmArea < 14) {
            farmArea++;
        } else {
            farmArea = -1;
        }
        saveConfig();
        GameCanvas.addInfoCharCline("Khu UP: " + (farmArea < 0 ? "Bat ky" : ("Khu " + (farmArea + 1))));
    }

    public static void showGoBackAdvancedMenu() {
        loadConfig();
        mVector mVector2 = new mVector("Menu GoBack Nang Cao");

        String strStone = GoBackNavigator.prioritizeStone ? "\u01afu ti\u00ean \u0111\u00e1 d\u1ecbch chuy\u1ec3n: B\u1eacT" : "\u01afu ti\u00ean \u0111\u00e1 d\u1ecbch chuy\u1ec3n: T\u1eaeT";
        mVector2.addElement(new iCommand(strStone, new IAction(){

            public void perform() {
                GoBackNavigator.prioritizeStone = !GoBackNavigator.prioritizeStone;
                saveConfig();
                GameCanvas.addInfoCharCline("\u01afu ti\u00ean \u0111\u00e1 d\u1ecbch chuy\u1ec3n: " + (GoBackNavigator.prioritizeStone ? "B\u1eacT" : "T\u1eaeT"));
                showGoBackAdvancedMenu();
            }
        }));

        mVector2.addElement(new iCommand("L\u01b0u Khu hi\u1ec7n t\u1ea1i (Khu " + (LoadMap.Area + 1) + ")", new IAction(){

            public void perform() {
                farmArea = LoadMap.Area;
                saveConfig();
                GameCanvas.addInfoCharCline("\u0110\u00e3 l\u01b0u Khu UP: Khu " + (farmArea + 1));
                showGoBackAdvancedMenu();
            }
        }));

        mVector2.addElement(new iCommand("Ch\u1ecdn Map UP t\u1eeb danh s\u00e1ch", new IAction(){

            public void perform() {
                showSelectFarmMapMenu();
            }
        }));

        if (farmMapId >= 0) {
            mVector2.addElement(new iCommand("X\u00f3a v\u1ecb tr\u00ed \u0111\u00e3 l\u01b0u", new IAction(){

                public void perform() {
                    clearGoBackLocation();
                    showGoBackMenu();
                }
            }));
        }

        mVector2.addElement(new iCommand("Quay l\u1ea1i", new IAction(){

            public void perform() {
                showGoBackMenu();
            }
        }));

        GameCanvas.menu2.startAt(mVector2, 2, "GOBACK N\u00c2NG CAO", false, null);
    }

    public static void showGoBackMenu() {
        loadConfig();
        mVector mVector2 = new mVector("Menu GoBack");

        // 1. Bat / Tat GoBack
        String strStatus = isGoBackEnabled ? "GoBack: B\u1eacT" : "GoBack: T\u1eaeT";
        mVector2.addElement(new iCommand(strStatus, new IAction(){

            public void perform() {
                toggleGoBack();
                showGoBackMenu();
            }
        }));

        // 2. Luu vi tri UP hien tai (1 cham luu ca map + toa do + khu)
        String locInfo = farmMapId >= 0 
            ? ("L\u01b0u l\u1ea1i v\u1ecb tr\u00ed (" + farmMapName + " - Khu " + farmAreaLabel() + " [" + GoBackPosition.label(farmX, farmY) + "])") 
            : "L\u01b0u v\u1ecb tr\u00ed UP hi\u1ec7n t\u1ea1i";
        mVector2.addElement(new iCommand(locInfo, new IAction(){

            public void perform() {
                saveCurrentFarmLocation();
                showGoBackMenu();
            }
        }));

        // 3. Ve Map UP ngay
        if (farmMapId >= 0) {
            mVector2.addElement(new iCommand("V\u1ec1 Map UP ngay (" + farmMapName + ")", new IAction(){

                public void perform() {
                    startGoBackNow();
                }
            }));
        }

        // 4. Doi Khu UP
        String strArea = "\u0110\u1ed5i Khu UP (Hi\u1ec7n t\u1ea1i: Khu " + farmAreaLabel() + ")";
        mVector2.addElement(new iCommand(strArea, new IAction(){

            public void perform() {
                cycleFarmArea();
                showGoBackMenu();
            }
        }));

        // 5. Cai dat nang cao
        mVector2.addElement(new iCommand("C\u00e0i \u0111\u1eb7t n\u00e2ng cao...", new IAction(){

            public void perform() {
                showGoBackAdvancedMenu();
            }
        }));

        // 6. Quay lai
        mVector2.addElement(new iCommand("Quay l\u1ea1i", new IAction(){

            public void perform() {
                showReviveMountMenu();
            }
        }));

        GameCanvas.menu2.startAt(mVector2, 2, "C\u00c0I \u0110\u1eb6T GOBACK", false, null);
    }

    public static void stopAllAuto() {
        loadConfig();
        GoBackProfile.bind();
        isAutoReconnect = isWaitingReconnect = isReconnectingChar = false;
        timeTargetReconnect = 0L;
        autoReviveMode = 0;
        reviveState = 0;
        isReviveCancelled = true;
        isCheckingGoldRevive = false;
        isAutoAttack = isAutoHpMp = false;
        autoMountType = 0;
        isGoBackEnabled = isGoBackActive = false;
        isGoBackPaused = true;
        resumeNativeAutoFire = false;
        goBackStepInfo = "";
        stopGoBackMovement();
        Player.isAutoFire = (byte)-1;
        Player.isCurAutoFire = false;
        Player.isAutoHPMP = false;
        Player.isAutoBuff = 0;
        Player.autoItem = null;
        AutoChat.isAutoChat = false;
        AutoSellManager.stopAll();
        AutoFarmSupport.stopAll();
        AutoPetFeed.stopAll();
        if (AutoUpgrade.active) AutoUpgrade.stop("Đã tắt tất cả auto");
        MedalFilter.blockWhite = MedalFilter.blockGreen = MedalFilter.blockYellow = MedalFilter.blockPurple = false;
        MainRMS.setSaveAuto();
        saveConfig();
        GameCanvas.addInfoCharCline("Đã tắt tất cả auto");
    }

    public static boolean isAnyActive() {
        return AutoChat.isAutoChat
            || AutoUpgrade.active
            || AutoPetFeed.equipmentEnabled || AutoPetFeed.candyEnabled
            || AutoFarmSupport.zoneEnabled || AutoFarmSupport.fireEnabled || AutoFarmSupport.oilEnabled || AutoFarmSupport.bookEnabled
            || Player.isAutoFire > -1 || Player.isAutoHPMP || Player.isAutoBuff == 1 || Player.autoItem != null
            || isAutoReconnect
            || autoReviveMode > 0
            || isAutoAttack
            || isAutoHpMp
            || autoMountType > 0
            || AutoSellManager.isAutoSellEnabled
            || (MedalFilter.blockWhite || MedalFilter.blockGreen || MedalFilter.blockYellow || MedalFilter.blockPurple)
            || (isGoBackEnabled && farmMapId >= 0);
    }

    public static void showAutoSellMenu() {
        AutoSellManager.loadConfig();
        mVector mVector2 = new mVector("Menu Auto Sell");

        // 1. Bat / Tat Auto Sell
        String sEn = AutoSellManager.isAutoSellEnabled ? "T\u1ef1 \u0111\u1ed9ng b\u00e1n khi r\u01b0\u01a1ng \u0111\u1ea7y: B\u1eacT" : "T\u1ef1 \u0111\u1ed9ng b\u00e1n khi r\u01b0\u01a1ng \u0111\u1ea7y: T\u1eaeT";
        mVector2.addElement(new iCommand(sEn, new IAction(){
            public void perform() {
                AutoSellManager.toggleAutoSell();
                showAutoSellMenu();
            }
        }));

        // 2. Cai dat ve lang khi con X o trong
        String sSlot = AutoSellManager.minEmptySlots == 0 
            ? "V\u1ec1 l\u00e0ng khi c\u00f2n: T\u1eaeT" 
            : ("V\u1ec1 l\u00e0ng khi c\u00f2n: " + AutoSellManager.minEmptySlots + " \u00f4 tr\u1ed1ng");
        mVector2.addElement(new iCommand(sSlot, new IAction(){
            public void perform() {
                AutoSellManager.cycleMinEmptySlots();
                showAutoSellMenu();
            }
        }));

        // 3. Cach ve lang (Relog hoac GoBack di bo)
        String sRet = AutoSellManager.returnMethod == 0 
            ? "C\u00e1ch v\u1ec1 l\u00e0ng: Tho\u00e1t v\u00e0o l\u1ea1i (Relog)" 
            : "C\u00e1ch v\u1ec1 l\u00e0ng: \u0110i b\u1ed9 GoBack";
        mVector2.addElement(new iCommand(sRet, new IAction(){
            public void perform() {
                AutoSellManager.cycleReturnMethod();
                showAutoSellMenu();
            }
        }));

        // 4. Chon mau do can ban >
        mVector2.addElement(new iCommand("Ch\u1ecdn M\u00e0u \u0110\u1ed3 C\u1ea7n B\u00e1n >", new IAction(){
            public void perform() {
                showSellColorMenu();
            }
        }));

        // 4. Tuy chon & Bao ve >
        mVector2.addElement(new iCommand("T\u00f9y Ch\u1ecdn & B\u1ea3o V\u1ec7 >", new IAction(){
            public void perform() {
                showSellOptionsAdvMenu();
            }
        }));

        // 5. Ban rac ngay lap tuc
        mVector2.addElement(new iCommand("B\u00e1n r\u00e1c ngay l\u1eadp t\u1ee9c (1 ch\u1ea1m)", new IAction(){
            public void perform() {
                AutoSellManager.manualSellTrashNow();
            }
        }));

        // 6. Quay lai
        mVector2.addElement(new iCommand("Quay l\u1ea1i", new IAction(){
            public void perform() {
                showInventoryMenu();
            }
        }));

        GameCanvas.menu2.startAt(mVector2, 2, "C\u00c0I \u0110\u1eb6T AUTO B\u00c1N \u0110\u1ed2", false, null);
    }

    public static void showSellColorMenu() {
        mVector mVector2 = new mVector("Mau Do Can Ban");

        String sW = AutoSellManager.isSellWhite ? "B\u00e1n \u0111\u1ed3 tr\u1eafng: B\u1eacT" : "B\u00e1n \u0111\u1ed3 tr\u1eafng: T\u1eaeT";
        mVector2.addElement(new iCommand(sW, new IAction(){
            public void perform() {
                AutoSellManager.toggleSellWhite();
                showSellColorMenu();
            }
        }));

        String sG = AutoSellManager.isSellGreen ? "B\u00e1n \u0111\u1ed3 xanh l\u00e1: B\u1eacT" : "B\u00e1n \u0111\u1ed3 xanh l\u00e1: T\u1eaeT";
        mVector2.addElement(new iCommand(sG, new IAction(){
            public void perform() {
                AutoSellManager.toggleSellGreen();
                showSellColorMenu();
            }
        }));

        String sB = AutoSellManager.isSellBlue ? "B\u00e1n \u0111\u1ed3 xanh d\u01b0\u01a1ng: B\u1eacT" : "B\u00e1n \u0111\u1ed3 xanh d\u01b0\u01a1ng: T\u1eaeT";
        mVector2.addElement(new iCommand(sB, new IAction(){
            public void perform() {
                AutoSellManager.toggleSellBlue();
                showSellColorMenu();
            }
        }));

        String sY = AutoSellManager.isSellYellow ? "B\u00e1n \u0111\u1ed3 v\u00e0ng: B\u1eacT" : "B\u00e1n \u0111\u1ed3 v\u00e0ng: T\u1eaeT";
        mVector2.addElement(new iCommand(sY, new IAction(){
            public void perform() {
                AutoSellManager.toggleSellYellow();
                showSellColorMenu();
            }
        }));

        String sP = AutoSellManager.isSellPurple ? "B\u00e1n \u0111\u1ed3 t\u00edm: B\u1eacT" : "B\u00e1n \u0111\u1ed3 t\u00edm: T\u1eaeT";
        mVector2.addElement(new iCommand(sP, new IAction(){
            public void perform() {
                AutoSellManager.toggleSellPurple();
                showSellColorMenu();
            }
        }));

        String sO = AutoSellManager.isSellOrange ? "B\u00e1n \u0111\u1ed3 cam: B\u1eacT" : "B\u00e1n \u0111\u1ed3 cam: T\u1eaeT";
        mVector2.addElement(new iCommand(sO, new IAction(){
            public void perform() {
                AutoSellManager.toggleSellOrange();
                showSellColorMenu();
            }
        }));

        mVector2.addElement(new iCommand("Quay l\u1ea1i", new IAction(){
            public void perform() {
                showAutoSellMenu();
            }
        }));

        GameCanvas.menu2.startAt(mVector2, 2, "CH\u1eccN M\u00c0U \u0110\u1ed2 B\u00c1N", false, null);
    }

    public static void showSellOptionsAdvMenu() {
        mVector mVector2 = new mVector("Tuy Chon & Bao Ve");

        String sLk = AutoSellManager.isSellLocked ? "B\u00e1n \u0111\u1ed3 \u0111\u00e3 kh\u00f3a: B\u1eacT" : "B\u00e1n \u0111\u1ed3 \u0111\u00e3 kh\u00f3a: T\u1eaeT";
        mVector2.addElement(new iCommand(sLk, new IAction(){
            public void perform() {
                AutoSellManager.toggleSellLocked();
                showSellOptionsAdvMenu();
            }
        }));

        String sUp = AutoSellManager.isSellUpgraded ? "B\u00e1n \u0111\u1ed3 \u0111\u00e3 c\u1ed9ng (+): B\u1eacT" : "B\u00e1n \u0111\u1ed3 \u0111\u00e3 c\u1ed9ng (+): T\u1eaeT";
        mVector2.addElement(new iCommand(sUp, new IAction(){
            public void perform() {
                AutoSellManager.toggleSellUpgraded();
                showSellOptionsAdvMenu();
            }
        }));

        long del = AutoSellManager.delaySellMs;
        String sDel = "Delay b\u00e1n: " + (del / 1000) + "." + ((del % 1000) / 100) + "s (" + del + "ms)";
        mVector2.addElement(new iCommand(sDel, new IAction(){
            public void perform() {
                AutoSellManager.cycleDelaySell();
                showSellOptionsAdvMenu();
            }
        }));

        mVector2.addElement(new iCommand("V\u1ecb tr\u00ed b\u00e1n: C\u1ea1nh NPC (Ch\u1ed1ng ban)", new IAction(){
            public void perform() {
                showSellOptionsAdvMenu();
            }
        }));

        mVector2.addElement(new iCommand("Quay l\u1ea1i", new IAction(){
            public void perform() {
                showAutoSellMenu();
            }
        }));

        GameCanvas.menu2.startAt(mVector2, 2, "T\u00d9Y CH\u1eccN & B\u1ea2O V\u1ec6", false, null);
    }

    public static void showMedalFilterMenu() {
        MedalFilter.loadConfig();
        mVector mVector2 = new mVector("Menu Dong Rot Me Day");

        String sW = MedalFilter.blockWhite ? "\u0110\u00f3ng r\u1edbt NL Tr\u1eafng: B\u1eacT" : "\u0110\u00f3ng r\u1edbt NL Tr\u1eafng: T\u1eaeT";
        mVector2.addElement(new iCommand(sW, new IAction(){
            public void perform() {
                MedalFilter.toggleBlockWhite();
                showMedalFilterMenu();
            }
        }));

        String sG = MedalFilter.blockGreen ? "\u0110\u00f3ng r\u1edbt NL Xanh: B\u1eacT" : "\u0110\u00f3ng r\u1edbt NL Xanh: T\u1eaeT";
        mVector2.addElement(new iCommand(sG, new IAction(){
            public void perform() {
                MedalFilter.toggleBlockGreen();
                showMedalFilterMenu();
            }
        }));

        String sY = MedalFilter.blockYellow ? "\u0110\u00f3ng r\u1edbt NL V\u00e0ng: B\u1eacT" : "\u0110\u00f3ng r\u1edbt NL V\u00e0ng: T\u1eaeT";
        mVector2.addElement(new iCommand(sY, new IAction(){
            public void perform() {
                MedalFilter.toggleBlockYellow();
                showMedalFilterMenu();
            }
        }));

        String sP = MedalFilter.blockPurple ? "\u0110\u00f3ng r\u1edbt NL T\u00edm: B\u1eacT" : "\u0110\u00f3ng r\u1edbt NL T\u00edm: T\u1eaeT";
        mVector2.addElement(new iCommand(sP, new IAction(){
            public void perform() {
                MedalFilter.toggleBlockPurple();
                showMedalFilterMenu();
            }
        }));

        mVector2.addElement(new iCommand("Quay l\u1ea1i", new IAction(){
            public void perform() {
                showInventoryMenu();
            }
        }));

        GameCanvas.menu2.startAt(mVector2, 2, "\u0110\u00d3NG R\u1edaT NL M\u1ec0 \u0110AY", false, null);
    }

    public static void showMenu() {
        loadConfig();
        mVector menu = new mVector("Auto");
        menu.addElement(new iCommand("Hiển thị & giảm lag >", new IAction() {
            public void perform() { RenderSettings.show(); }
        }));
        menu.addElement(new iCommand("Tự động cho pet ăn >", new IAction() {
            public void perform() { AutoPetFeed.show(); }
        }));
        menu.addElement(new iCommand("Lửa, dầu & khu 2 >", new IAction() {
            public void perform() { AutoFarmSupport.show(); }
        }));
        menu.addElement(new iCommand("Đánh quái: " + enabled(isAutoAttack), new IAction() {
            public void perform() { toggleAutoAttack(); showMenu(); }
        }));
        menu.addElement(new iCommand("HP/MP: " + enabled(isAutoHpMp) + " >", new IAction() {
            public void perform() { showHpMpMenu(); }
        }));
        menu.addElement(new iCommand("Di chuyển & hồi sinh >", new IAction() {
            public void perform() { showReviveMountMenu(); }
        }));
        menu.addElement(new iCommand("Túi đồ & cường hóa >", new IAction() {
            public void perform() { showInventoryMenu(); }
        }));
        menu.addElement(new iCommand("Tiện ích & hiển thị >", new IAction() {
            public void perform() { showUtilitiesMenu(); }
        }));
        GameCanvas.menu2.startAt(menu, 2, "AUTO", false, null);
    }

    public static void showInventoryMenu() {
        mVector menu = new mVector("Tui do");
        menu.addElement(new iCommand("Cường hóa: " + enabled(AutoUpgrade.active) + " >", new IAction() {
            public void perform() { AutoUpgradeMenu.show(null); }
        }));
        menu.addElement(new iCommand("Bán đồ: " + enabled(AutoSellManager.isAutoSellEnabled) + " >", new IAction() {
            public void perform() { showAutoSellMenu(); }
        }));
        menu.addElement(new iCommand("Lọc nguyên liệu mề đay >", new IAction() {
            public void perform() { showMedalFilterMenu(); }
        }));
        menu.addElement(new iCommand("Quay lại", new IAction() {
            public void perform() { showMenu(); }
        }));
        GameCanvas.menu2.startAt(menu, 2, "TÚI ĐỒ", false, null);
    }

    public static void showReviveMountMenu() {
        mVector mVector2 = new mVector("Hoi Sinh & Thu Cuoi");
        mVector2.addElement(new iCommand("GoBack: " + enabled(isGoBackEnabled) + " >", new IAction() {
            public void perform() { showGoBackMenu(); }
        }));

        // 1. Auto Hoi Sinh
        String string2 = "Auto H\u1ed3i Sinh: T\u1eaeT";
        if (autoReviveMode == 1) {
            string2 = "Auto H\u1ed3i Sinh: V\u1ec1 L\u00e0ng";
        } else if (autoReviveMode == 2) {
            string2 = "Auto H\u1ed3i Sinh: T\u1ea1i Ch\u1ed7";
        }
        mVector2.addElement(new iCommand(string2, new IAction(){

            public void perform() {
                toggleAutoRevive();
                showReviveMountMenu();
            }
        }));

        // 2. Auto Thu Cuoi
        String strMount = "Auto Th\u00fa C\u01b0\u1ee1i: " + getMountName(autoMountType);
        mVector2.addElement(new iCommand(strMount, new IAction(){

            public void perform() {
                showMountMenu();
            }
        }));

        // 3. Tu dong dang nhap lai (Auto Reconnect)
        String string = isAutoReconnect ? "T\u1ef1 \u0111\u0103ng nh\u1eadp: B\u1eacT" : "T\u1ef1 \u0111\u0103ng nh\u1eadp: T\u1eaeT";
        mVector2.addElement(new iCommand(string, new IAction(){

            public void perform() {
                toggleAutoReconnect();
                showReviveMountMenu();
            }
        }));

        // 4. Quay lai
        mVector2.addElement(new iCommand("Quay l\u1ea1i", new IAction(){

            public void perform() {
                showMenu();
            }
        }));

        GameCanvas.menu2.startAt(mVector2, 2, "DI CHUYỂN & HỒI SINH", false, null);
    }

    public static void showUtilitiesMenu() {
        mVector menu = new mVector("Hien thi");
        menu.addElement(new iCommand("Auto chat: " + enabled(AutoChat.isAutoChat) + " >", new IAction() {
            public void perform() { AutoChat.showMenu(); }
        }));
        menu.addElement(new iCommand("Bảng trạng thái: " + (hudVisible ? "Hiện" : "Ẩn"), new IAction() {
            public void perform() { showInfo(); showUtilitiesMenu(); }
        }));
        menu.addElement(new iCommand("Quay lại", new IAction() {
            public void perform() { showMenu(); }
        }));
        GameCanvas.menu2.startAt(menu, 2, "TIỆN ÍCH & HIỂN THỊ", false, null);
    }

    public static void showInfo() {
        hudVisible = !hudVisible;
        hudRefresh = 0L;
        hudCancelY = -1;
    }
}
