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
import Model.CRes;
import Model.T;
import Skill.HotKey;
import Thread_More.LoadMap;
import netcommand.global.GlobalService;

public class MenuUP {
    public static final String AUTHOR = "HONG PHUC HSO";
    public static final String VERSION = "v1.5";

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

    // HUD box coordinates
    public static int hudBoxW = 260;
    public static int hudBoxH = 46;

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
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void saveCurrentChar() {
        if (GameScreen.player != null && GameScreen.player.name != null && GameScreen.player.name.length() > 0) {
            lastCharName = GameScreen.player.name;
            lastCharId = GameScreen.player.ID;
            lastArea = (byte)LoadMap.Area;
            saveConfig();
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
        if (string == null) {
            return false;
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
        if (string == null) {
            return false;
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

            // 2. Neu chon "Bat ky trong tui" (type == 6) hoac chua tim thay ID yeu cau:
            if (type == 6 || (targetId > 0 && type != 7)) {
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

        // 3. Xu ly tuong tac click/cham tren HUD banner
        if (GameCanvas.isPointerSelect) {
            int hudX = GameCanvas.hw - hudBoxW / 2;
            if (isWaitingReconnect && GameCanvas.isPoint(hudX, 6, hudBoxW, hudBoxH)) {
                GameCanvas.isPointerSelect = false;
                cancelReconnect();
            } else if (reviveState == 1 && GameCanvas.isPoint(hudX, 28, hudBoxW, hudBoxH)) {
                GameCanvas.isPointerSelect = false;
                cancelRevive();
            }
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

        // 5. Xu ly Auto Hoi Sinh khi dang trong game
        if (autoReviveMode > 0 && GameCanvas.currentScreen == GameCanvas.game && GameScreen.player != null) {
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
    }

    private static void updateAutoAttack() {
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

                if (autoReviveMode == 1) {
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
        if (g == null) {
            return;
        }

        int boxX = GameCanvas.hw - hudBoxW / 2;

        // 1. HUD dem nguoc Tu dong Dang nhap lai (Auto Reconnect)
        if (isWaitingReconnect) {
            int sec = (int)((timeTargetReconnect - mSystem.currentTimeMillis()) / 1000L) + 1;
            if (sec < 0) {
                sec = 0;
            }
            int boxY = 6;
            g.setColor(0x111111);
            g.fillRect(boxX, boxY, hudBoxW, hudBoxH, false);
            g.setColor(0xFF9900);
            g.drawRect(boxX, boxY, hudBoxW, hudBoxH, false);
            g.setColor(0x332200);
            g.drawRect(boxX + 1, boxY + 1, hudBoxW - 2, hudBoxH - 2, false);

            mFont.tahoma_7b_yellow.drawString(g, "[HONG PHUC HSO] T\u1ef0 \u0110\u0102NG NH\u1eacP", GameCanvas.hw, boxY + 4, 2, false);
            String charStr = (lastCharName != null && lastCharName.length() > 0) ? (" | NV: " + lastCharName) : "";
            mFont.tahoma_7_white.drawString(g, "\u0110\u0103ng nh\u1eadp l\u1ea1i sau: " + sec + "s" + charStr, GameCanvas.hw, boxY + 17, 2, false);
            mFont.tahoma_7_yellow.drawString(g, "(Ch\u1ea1m v\u00e0o \u0111\u00e2y \u0111\u1ec3 H\u1ee6Y)", GameCanvas.hw, boxY + 30, 2, false);
            return;
        }

        // 2. HUD dem nguoc Auto Hoi Sinh
        boolean isDead = (GameScreen.player != null && (GameScreen.player.hp <= 0 || GameScreen.player.Action == 4));
        if (autoReviveMode > 0 && GameCanvas.currentScreen == GameCanvas.game && isDead && !isReviveCancelled) {
            int boxY = 28;

            if (reviveState == 1) {
                int sec = (int)((timeTargetRevive - mSystem.currentTimeMillis()) / 1000L) + 1;
                if (sec < 0) {
                    sec = 0;
                }
                g.setColor(0x151515);
                g.fillRect(boxX, boxY, hudBoxW, hudBoxH, false);
                g.setColor(0x00E5FF);
                g.drawRect(boxX, boxY, hudBoxW, hudBoxH, false);
                g.setColor(0x003344);
                g.drawRect(boxX + 1, boxY + 1, hudBoxW - 2, hudBoxH - 2, false);

                mFont.tahoma_7b_yellow.drawString(g, "[HONG PHUC HSO] AUTO H\u1ed2I SINH", GameCanvas.hw, boxY + 4, 2, false);

                String infoStr = "";
                if (autoReviveMode == 1) {
                    infoStr = "T\u1ef1 \u0111\u1ed9ng V\u1ec0 L\u00c0NG sau: " + sec + "s";
                } else if (autoReviveMode == 2) {
                    if (hasReviveItem()) {
                        String itemName = getReviveItemName();
                        infoStr = "H\u1ed3i sinh t\u1ea1i ch\u1ed7 (" + (itemName.length() > 0 ? itemName : "V\u1eadt ph\u1ea9m") + "): " + sec + "s";
                    } else {
                        infoStr = "H\u1ed3i sinh t\u1ea1i ch\u1ed7 (\u01afa chu\u1ed9ng V\u00e0ng): " + sec + "s";
                    }
                }
                mFont.tahoma_7_white.drawString(g, infoStr, GameCanvas.hw, boxY + 17, 2, false);
                mFont.tahoma_7_yellow.drawString(g, "(Ch\u1ea1m v\u00e0o \u0111\u00e2y \u0111\u1ec3 H\u1ee6Y)", GameCanvas.hw, boxY + 30, 2, false);
            } else if (reviveState == 2) {
                g.setColor(0x151515);
                g.fillRect(boxX, boxY, hudBoxW, 26, false);
                g.setColor(0x00FF66);
                g.drawRect(boxX, boxY, hudBoxW, 26, false);
                mFont.tahoma_7b_green.drawString(g, "[HSO] \u0110ang th\u1ef1c hi\u1ec7n h\u1ed3i sinh...", GameCanvas.hw, boxY + 7, 2, false);
            }
        }
    }

    public static void toggleAutoReconnect() {
        loadConfig();
        isAutoReconnect = !isAutoReconnect;
        saveConfig();
        GameCanvas.addInfoCharCline("T\u1ef1 \u0111\u0103ng nh\u1eadp l\u1ea1i: " + (isAutoReconnect ? "B\u1eacT" : "T\u1eaeT"));
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
        GameCanvas.addInfoCharCline("Auto H\u1ed3i Sinh: " + string);
    }

    public static void toggleAutoAttack() {
        loadConfig();
        isAutoAttack = !isAutoAttack;
        saveConfig();
        if (isAutoAttack) {
            if (GameCanvas.currentScreen == GameCanvas.game && GameScreen.player != null) {
                GameScreen.player.dofire_();
                Player.xBeginAutoFire = GameScreen.player.x;
                Player.yBeginAutofire = GameScreen.player.y;
                GameScreen.player.nextMonster();
                if (GameScreen.ObjFocus != null) {
                    GameScreen.player.setAutoFire();
                }
            }
            GameCanvas.addInfoCharCline("Auto \u0110\u00e1nh Qu\u00e1i: B\u1eacT");
        } else {
            Player.isAutoFire = (byte)-1;
            Player.isCurAutoFire = false;
            GameCanvas.addInfoCharCline("Auto \u0110\u00e1nh Qu\u00e1i: T\u1eaeT");
        }
    }

    public static void toggleAutoHpMp() {
        loadConfig();
        isAutoHpMp = !isAutoHpMp;
        saveConfig();
        GameCanvas.addInfoCharCline("Auto B\u01a1m HP/MP: " + (isAutoHpMp ? "B\u1eacT" : "T\u1eaeT"));
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
                showMenu();
            }
        }));

        mVector2.addElement(new iCommand("B\u1ea5t k\u1ef3 trong t\u00fai", new IAction(){
            public void perform() {
                setMountType(6);
                showMenu();
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
                showMenu();
            }
        }));

        mVector2.addElement(new iCommand("Ng\u1ef1a Tr\u1eafng (ID 63)", new IAction(){
            public void perform() {
                setMountType(2);
                showMenu();
            }
        }));

        mVector2.addElement(new iCommand("Ng\u1ef1a Chi\u1ebfn Gi\u00e1p (ID 64)", new IAction(){
            public void perform() {
                setMountType(3);
                showMenu();
            }
        }));

        mVector2.addElement(new iCommand("Ng\u1ef1a \u0110\u1ecf (ID 65)", new IAction(){
            public void perform() {
                setMountType(4);
                showMenu();
            }
        }));

        mVector2.addElement(new iCommand("Ng\u1ef1a \u0110en (ID 66)", new IAction(){
            public void perform() {
                setMountType(5);
                showMenu();
            }
        }));

        mVector2.addElement(new iCommand("Quay l\u1ea1i", new IAction(){
            public void perform() {
                showMenu();
            }
        }));

        GameCanvas.menu2.startAt(mVector2, 2, "CH\u1eccN TH\u00da C\u01af\u1ee0I", false, null);
    }

    public static boolean isAnyActive() {
        return AutoChat.isAutoChat
            || isAutoReconnect
            || autoReviveMode > 0
            || isAutoAttack
            || isAutoHpMp
            || autoMountType > 0;
    }

    public static void showMenu() {
        loadConfig();
        mVector mVector2 = new mVector("Menu UP");

        // 1. Auto Danh Quai
        String strAtt = isAutoAttack ? "Auto \u0110\u00e1nh Qu\u00e1i: B\u1eacT" : "Auto \u0110\u00e1nh Qu\u00e1i: T\u1eaeT";
        mVector2.addElement(new iCommand(strAtt, new IAction(){

            public void perform() {
                toggleAutoAttack();
                showMenu();
            }
        }));

        // 2. Cai dat Auto Bom HP/MP
        String strHpMp = isAutoHpMp ? ("Auto B\u01a1m HP/MP: B\u1eacT (" + autoHpPercent + "%/" + autoMpPercent + "%)") : "Auto B\u01a1m HP/MP: T\u1eaeT";
        mVector2.addElement(new iCommand(strHpMp, new IAction(){

            public void perform() {
                showHpMpMenu();
            }
        }));

        // 3. Auto Thu Cuoi
        String strMount = "Auto Th\u00fa C\u01b0\u1ee1i: " + getMountName(autoMountType);
        mVector2.addElement(new iCommand(strMount, new IAction(){

            public void perform() {
                showMountMenu();
            }
        }));

        // 4. Auto Hoi Sinh
        String string2 = "Auto H\u1ed3i Sinh: T\u1eaeT";
        if (autoReviveMode == 1) {
            string2 = "Auto H\u1ed3i Sinh: V\u1ec1 L\u00e0ng";
        } else if (autoReviveMode == 2) {
            string2 = "Auto H\u1ed3i Sinh: T\u1ea1i Ch\u1ed7";
        }
        mVector2.addElement(new iCommand(string2, new IAction(){

            public void perform() {
                toggleAutoRevive();
                showMenu();
            }
        }));

        // 5. Tu dong dang nhap lai (Auto Reconnect)
        String string = isAutoReconnect ? "T\u1ef1 \u0111\u0103ng nh\u1eadp: B\u1eacT" : "T\u1ef1 \u0111\u0103ng nh\u1eadp: T\u1eaeT";
        mVector2.addElement(new iCommand(string, new IAction(){

            public void perform() {
                toggleAutoReconnect();
                showMenu();
            }
        }));

        // 6. Cai dat Auto Chat
        mVector2.addElement(new iCommand("C\u00e0i \u0111\u1eb7t Auto Chat", new IAction(){

            public void perform() {
                AutoChat.showMenu();
            }
        }));

        // 7. Thong tin ban quyen
        mVector2.addElement(new iCommand("Th\u00f4ng tin [HONG PHUC]", new IAction(){

            public void perform() {
                showInfo();
            }
        }));

        GameCanvas.menu2.startAt(mVector2, 2, "MENU UP - HONG PHUC", false, null);
    }

    public static void showInfo() {
        String string = isAutoReconnect ? "B\u1eacT (Delay 10-15s)" : "T\u1eaeT";
        String string2 = "T\u1eaeT";
        if (autoReviveMode == 1) {
            string2 = "V\u1ec1 L\u00e0ng";
        } else if (autoReviveMode == 2) {
            string2 = "T\u1ea1i Ch\u1ed7";
        }
        String string3 = AutoChat.isAutoChat ? "B\u1eacT" : "T\u1eaeT";
        String string4 = isAutoAttack ? "B\u1eacT (T\u1ef1 focus & \u0111\u00e1nh)" : "T\u1eaeT";
        String string5 = isAutoHpMp ? ("B\u1eacT (HP<" + autoHpPercent + "%, MP<" + autoMpPercent + "%)") : "T\u1eaeT";
        String string6 = getMountName(autoMountType);

        String info = "=== MENU UP - HONG PHUC HSO ===\n" +
                      "B\u1ea3n quy\u1ec1n: HONG PHUC HSO\n" +
                      "Phi\u00ean b\u1ea3n: UP Tool v1.5\n" +
                      "Auto \u0110\u00e1nh qu\u00e1i: " + string4 + "\n" +
                      "Auto B\u01a1m HP/MP: " + string5 + "\n" +
                      "Auto Th\u00fa c\u01b0\u1ee1i: " + string6 + "\n" +
                      "Auto H\u1ed3i sinh: " + string2 + "\n" +
                      "T\u1ef1 \u0111\u0103ng nh\u1eadp: " + string + "\n" +
                      "Auto Chat: " + string3 + "\n" +
                      "Nh\u00e2n v\u1eadt ghi nh\u1edb: " + (lastCharName.length() > 0 ? lastCharName : "(Ch\u01b0a c\u00f3)");
        MsgDialog msgDialog = new MsgDialog();
        msgDialog.setinfo(info, new iCommand("\u0110\u00f3ng", new IAction(){

            public void perform() {
                GameCanvas.end_Dialog();
            }
        }), true);
        GameCanvas.currentDialog = msgDialog;
    }
}
