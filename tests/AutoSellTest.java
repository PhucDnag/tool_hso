package GameObjects;

import CLib.*;
import GameScreen.GameScreen;
import Main.GameCanvas;
import netcommand.global.GlobalService;

public class AutoSellTest {
    static int assertions;
    static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new RuntimeException("Assertion failed: " + message);
        }
    }

    static MainItem makeItem(int id, String name, int catagory, int color, byte isLock, byte tier) {
        MainItem item = new MainItem();
        item.Id = id;
        item.itemName = name;
        item.ItemCatagory = catagory;
        item.colorNameItem = color;
        item.isLock = isLock;
        item.tier = tier;
        return item;
    }

    static void testFilterMatrix() {
        AutoSellManager.isSellWhite = true;
        AutoSellManager.isSellGreen = true;
        AutoSellManager.isSellBlue = false;
        AutoSellManager.isSellYellow = false;
        AutoSellManager.isSellPurple = false;
        AutoSellManager.isSellOrange = false;
        AutoSellManager.isSellLocked = false;
        AutoSellManager.isSellUpgraded = false;

        // 1. Basic White and Green equipment
        MainItem white = makeItem(1, "Ao Vai", 3, 0, (byte)0, (byte)0);
        MainItem green = makeItem(2, "Kiem Sat", 3, 5, (byte)0, (byte)0);
        MainItem blue = makeItem(3, "Nhan Bac", 3, 1, (byte)0, (byte)0);
        MainItem gold = makeItem(4, "Vu Khi Vang", 3, 2, (byte)0, (byte)0);
        MainItem purple = makeItem(5, "Vu Khi Tim", 3, 3, (byte)0, (byte)0);
        MainItem orange = makeItem(6, "Vu Khi Cam", 3, 4, (byte)0, (byte)0);

        check(AutoSellManager.isTrashItem(white), "White equip should be trash by default");
        check(!AutoSellManager.isTrashItem(green), "Green excluded even with legacy green flag enabled");
        check(!AutoSellManager.isTrashItem(blue), "Blue equip should NOT be trash when isSellBlue is false");
        check(!AutoSellManager.isTrashItem(gold), "Gold equip should NOT be trash when isSellYellow is false");
        check(!AutoSellManager.isTrashItem(purple), "Purple equip should NOT be trash when isSellPurple is false");
        check(!AutoSellManager.isTrashItem(orange), "Orange equip should NOT be trash when isSellOrange is false");

        // Toggle blue
        AutoSellManager.isSellBlue = true;
        check(AutoSellManager.isTrashItem(blue), "Blue equip should be trash when isSellBlue is true");
        AutoSellManager.isSellBlue = false;

        // Toggle yellow
        AutoSellManager.isSellYellow = true;
        check(AutoSellManager.isTrashItem(gold), "Yellow equip should be trash when isSellYellow is true");
        AutoSellManager.isSellYellow = false;

        // Toggle purple
        AutoSellManager.isSellPurple = true;
        check(AutoSellManager.isTrashItem(purple), "Purple equip should be trash when isSellPurple is true");
        AutoSellManager.isSellPurple = false;

        // Toggle orange
        AutoSellManager.isSellOrange = true;
        check(AutoSellManager.isTrashItem(orange), "Orange equip should be trash when isSellOrange is true");
        AutoSellManager.isSellOrange = false;

        // Toggle white
        AutoSellManager.isSellWhite = false;
        check(!AutoSellManager.isTrashItem(white), "White equip should NOT be trash when isSellWhite is false");
        AutoSellManager.isSellWhite = true;

        // Toggle green
        AutoSellManager.isSellGreen = false;
        check(!AutoSellManager.isTrashItem(green), "Green equip should NOT be trash when isSellGreen is false");
        AutoSellManager.isSellGreen = true;

        // 2. Locked equipment protection
        MainItem lockedWhite = makeItem(7, "Ao Khoa", 3, 0, (byte)1, (byte)0);
        MainItem lockedGreen = makeItem(8, "Kiem Khoa", 3, 1, (byte)1, (byte)0);
        check(!AutoSellManager.isTrashItem(lockedWhite), "Locked white equip must not be sold when isSellLocked is false");
        check(!AutoSellManager.isTrashItem(lockedGreen), "Locked green equip must not be sold when isSellLocked is false");
        AutoSellManager.isSellLocked = true;
        check(AutoSellManager.isTrashItem(lockedWhite), "Locked white equip should be sold when isSellLocked is true");
        AutoSellManager.isSellLocked = false;

        // 3. Upgraded / Refined equipment protection (tier > 0)
        MainItem upgradedWhite = makeItem(9, "Ao +1", 3, 0, (byte)0, (byte)1);
        MainItem upgradedGreen = makeItem(10, "Kiem +3", 3, 1, (byte)0, (byte)3);
        check(!AutoSellManager.isTrashItem(upgradedWhite), "Upgraded white equip must not be sold when isSellUpgraded is false");
        check(!AutoSellManager.isTrashItem(upgradedGreen), "Upgraded green equip must not be sold when isSellUpgraded is false");
        AutoSellManager.isSellUpgraded = true;
        check(AutoSellManager.isTrashItem(upgradedWhite), "Upgraded white equip should be sold when isSellUpgraded is true");
        AutoSellManager.isSellUpgraded = false;

        // 4. Equipped gear protection (in VecEquipPlayer)
        Item.VecEquipPlayer = new mHashtable();
        Item.VecEquipPlayer.put("0", white);
        check(!AutoSellManager.isTrashItem(white), "Equipped gear must NEVER be sold");
        Item.VecEquipPlayer.clear();
        check(AutoSellManager.isTrashItem(white), "Gear should be sellable after unequipped");

        // 5. Non-equipment categories (potions = 4, quest = 5, materials = 7)
        MainItem potion = makeItem(11, "Binh Mau", 4, 0, (byte)0, (byte)0);
        MainItem quest = makeItem(12, "Thu Nhiem Vu", 5, 0, (byte)0, (byte)0);
        MainItem mat = makeItem(13, "Da Cuong Hoa", 7, 0, (byte)0, (byte)0);
        check(!AutoSellManager.isTrashItem(potion), "Potions must NEVER be sold");
        check(!AutoSellManager.isTrashItem(quest), "Quest items must NEVER be sold");
        check(!AutoSellManager.isTrashItem(mat), "Materials must NEVER be sold");

        // 6. Null and invalid items
        check(!AutoSellManager.isTrashItem(null), "Null item must return false");
        MainItem emptyName = makeItem(14, "", 3, 0, (byte)0, (byte)0);
        check(!AutoSellManager.isTrashItem(emptyName), "Empty name item must return false");
        MainItem negId = makeItem(-1, "Negative ID", 3, 0, (byte)0, (byte)0);
        check(!AutoSellManager.isTrashItem(negId), "Negative ID item must return false");
    }

    static void setupVillageNpc() {
        GameScreen.Vecplayers.removeAllElements();
        MainObject hammer = new MainObject();
        hammer.name = "Hammer";
        hammer.typeObject = 2;
        hammer.x = 100;
        hammer.y = 100;
        GameScreen.Vecplayers.addElement(hammer);
        GameScreen.player.x = 100;
        GameScreen.player.y = 100;
        GameScreen.isMapLang = true;
    }

    static void testQueueAndRateLimiting() {
        AutoSellManager.resetSession();
        AutoSellManager.isAutoSellEnabled = true;
        AutoSellManager.isSellWhite = true;
        AutoSellManager.isSellGreen = true;
        AutoSellManager.isSellBlue = true;
        AutoSellManager.isSellYellow = false;
        AutoSellManager.isSellPurple = false;
        AutoSellManager.isSellOrange = false;
        AutoSellManager.isSellLocked = false;
        AutoSellManager.isSellUpgraded = false;
        AutoSellManager.delaySellMs = 1000L; // Mặc định min 1s

        setupVillageNpc();

        Item.VecInvetoryPlayer = new mVector();
        MainItem item1 = makeItem(101, "Mu Vai", 3, 0, (byte)0, (byte)0);
        MainItem item2 = makeItem(102, "Giay Vai", 3, 0, (byte)0, (byte)0);
        MainItem item3 = makeItem(103, "Kiem Xanh", 3, 1, (byte)0, (byte)0);
        MainItem valuable = makeItem(104, "Kiem Vang", 3, 3, (byte)0, (byte)0);

        Item.VecInvetoryPlayer.addElement(item1);
        Item.VecInvetoryPlayer.addElement(item2);
        Item.VecInvetoryPlayer.addElement(item3);
        Item.VecInvetoryPlayer.addElement(valuable);

        GlobalService.sellPackets = 0;
        GlobalService.lastSoldCat = -1;
        GlobalService.lastSoldId = -1;
        GlobalService.lastSoldAction = -1;

        // Manual sell
        AutoSellManager.manualSellTrashNow();
        check(AutoSellManager.getQueueCount() == 3, "Queue should contain exactly 3 trash items");

        // First tick at now = 10000: first item sold
        mSystem.now = 10000;
        AutoSellManager.update();
        check(GlobalService.sellPackets == 1, "First item should be sold");
        check(GlobalService.lastSoldCat == 3, "Sold category must be 3");
        check(GlobalService.lastSoldId == 101, "Sold ID must be 101");
        check(GlobalService.lastSoldAction == 1, "Action must be 1 (Sell for gold)");
        check(AutoSellManager.getQueueCount() == 2, "Queue should now have 2 items");

        // Next tick at now = 10500 (only 500ms elapsed, < 1000ms): NO sell
        mSystem.now = 10500;
        AutoSellManager.update();
        check(GlobalService.sellPackets == 1, "Spam prevention: no sell before 1000ms delay");
        check(AutoSellManager.getQueueCount() == 2, "Queue should still have 2 items");

        // Next tick at now = 11050 (> 1000ms elapsed): second item sold
        mSystem.now = 12000;
        Item.VecInvetoryPlayer.removeElement(item1);
        AutoSellManager.update();
        check(GlobalService.sellPackets == 2, "Second item should be sold after 1050ms");
        check(GlobalService.lastSoldId == 102, "Second sold ID must be 102");
        check(AutoSellManager.getQueueCount() == 1, "Queue should now have 1 item");

        // Next tick at now = 12100 (> 1000ms): third item sold
        mSystem.now = 14000;
        Item.VecInvetoryPlayer.removeElement(item2);
        AutoSellManager.update();
        check(GlobalService.sellPackets == 3, "Third item should be sold");
        check(GlobalService.lastSoldId == 103, "Third sold ID must be 103");
        check(AutoSellManager.getQueueCount() == 0, "Queue should now be empty");
        check(AutoSellManager.getTotalSold() == 3, "Total sold should be 3");
    }

    static void testStandingNearNpcRequired() {
        AutoSellManager.resetSession();
        AutoSellManager.isAutoSellEnabled = true;
        AutoSellManager.delaySellMs = 1000L;

        // Đặt NPC ở xa (x=500, y=500) trong khi Player ở (x=100, y=100) -> Khoảng cách > 45px
        GameScreen.Vecplayers.removeAllElements();
        MainObject hammer = new MainObject();
        hammer.name = "Hammer";
        hammer.typeObject = 2;
        hammer.x = 500;
        hammer.y = 500;
        GameScreen.Vecplayers.addElement(hammer);
        GameScreen.player.x = 100;
        GameScreen.player.y = 100;
        GameScreen.isMapLang = true;

        Item.VecInvetoryPlayer = new mVector();
        MainItem item1 = makeItem(301, "Ao Rac", 3, 0, (byte)0, (byte)0);
        Item.VecInvetoryPlayer.addElement(item1);

        AutoSellManager.scanAndEnqueueTrash();
        check(AutoSellManager.getQueueCount() == 1, "Queue has 1 trash item");

        GlobalService.sellPackets = 0;
        mSystem.now = 30000;
        // Update khi ở xa NPC: KHÔNG ĐƯỢC BÁN!
        AutoSellManager.update();
        check(GlobalService.sellPackets == 0, "Must NEVER sell items when far from NPC (anti-ban protection)");
        check(GoBackNavigator.approached!=null && GoBackNavigator.approached.x==500, "selling delegates NPC approach to collision-aware navigator");

        // Giả lập player đã bước đến cạnh NPC (x=480, y=500 -> cách 20px <= 45px)
        GameScreen.player.x = 480;
        GameScreen.player.y = 500;
        mSystem.now = 32000;
        AutoSellManager.update();
        check(GlobalService.sellPackets == 1, "Must sell item once safely standing beside NPC");
    }

    static void testFullBagAutoTrigger() {
        AutoSellManager.resetSession();
        AutoSellManager.isAutoSellEnabled = true;
        Player.maxInven = (short)10;

        Item.VecInvetoryPlayer = new mVector();
        // 7 items: 10 - 7 = 3 free slots (> 2, no auto sell)
        for (int i = 0; i < 7; i++) {
            Item.VecInvetoryPlayer.addElement(makeItem(200 + i, "Trash " + i, 3, 0, (byte)0, (byte)0));
        }

        mSystem.now = 20000;
        setupVillageNpc();
        AutoSellManager.update();
        check(AutoSellManager.getQueueCount() == 0, "No auto sell when 3 free slots remaining");

        // Add 1 more item: 8 items -> 10 - 8 = 2 free slots (<= 2, auto sell triggered!)
        Item.VecInvetoryPlayer.addElement(makeItem(207, "Trash 7", 3, 0, (byte)0, (byte)0));
        mSystem.now = 25000;
        AutoSellManager.update();
        check(AutoSellManager.getQueueCount() > 0, "Auto sell MUST trigger when free slots <= 2");
    }

    static void testReturnToVillageWhenBagFullOutside() {
        AutoSellManager.resetSession();
        AutoSellManager.isAutoSellEnabled = true;
        AutoSellManager.minEmptySlots = 2;
        AutoSellManager.returnMethod = 0; // Relog mode
        AutoSellManager.isReturningToSell = false;
        MenuUP.isWaitingReconnect = false;

        Player.maxInven = (short)10;
        Item.VecInvetoryPlayer = new mVector();
        for (int i = 0; i < 9; i++) { // 10 - 9 = 1 free slot <= 2
            Item.VecInvetoryPlayer.addElement(makeItem(400 + i, "Trash " + i, 3, 0, (byte)0, (byte)0));
        }

        GameScreen.isMapLang = false; // Đang ở ngoài bãi quái
        mSystem.now = 40000;
        AutoSellManager.update();

        check(AutoSellManager.isReturningToSell, "isReturningToSell must be true when bag full outside village");
        check(!MenuUP.isWaitingReconnect && GoBackNavigator.updates>0, "start walking instead of relog");
        int count=GoBackNavigator.updates;
        mSystem.now+=100;AutoSellManager.update();
        check(GoBackNavigator.updates>count,"continue routing on later ticks");
        AutoSellManager.resetSession();AutoSellManager.returnMethod=1;
        mSystem.now+=3000;count=GoBackNavigator.updates;AutoSellManager.update();
        check(GoBackNavigator.updates>count,"missing recall falls back to routing");
        AutoSellManager.resetSession();
        MainItem recall=makeItem(999,"bua ve lang",4,0,(byte)0,(byte)0);recall.numPotion=1;
        Item.VecInvetoryPlayer.addElement(recall);mSystem.now+=3000;
        count=GoBackNavigator.updates;int sent=GlobalService.recalls;AutoSellManager.update();
        check(GlobalService.recalls==sent+1 && GoBackNavigator.updates==count,"use recall once and wait before walking");
        mSystem.now+=100;AutoSellManager.update();check(GlobalService.recalls==sent+1,"no repeated recall while waiting");
        mSystem.now+=16000;AutoSellManager.update();check(GoBackNavigator.updates>count,"unconfirmed recall falls back after timeout");
    }

    static void testSellConfirmationDialogHandling() {
        GlobalService.dialogReplies = 0;
        String dialogText = "B\u1ea1n c\u00f3 mu\u1ed1n b\u00e1n \u00c1o tuy\u1ebft v\u1edbi gi\u00e1 1 V\u00e0ng kh\u00f4ng ?";
        boolean handled = AutoSellManager.handleDialogServer((short)55, (byte)1, dialogText);
        check(handled, "Dialog_server must intercept sell confirmation dialog");
        check(GlobalService.dialogReplies == 1, "Must reply to server dialog with Ok (value 1)");

        boolean handledEn = AutoSellManager.handleDialogServer((short)56, (byte)1, "Do you want to sell Snow Armor for price 1 Gold ?");
        check(handledEn, "Must intercept English sell dialog");
        check(GlobalService.dialogReplies == 2, "Must reply to English dialog");

        boolean handledParty = AutoSellManager.handleDialogServer((short)57, (byte)1, "Moi vao nhom ?");
        check(!handledParty, "Party dialog must NOT be intercepted by AutoSell");
        check(GlobalService.dialogReplies == 2, "Must not reply to party dialog");

        CLib.mVector options = new CLib.mVector();
        options.addElement(new InterfaceComponents.iCommand("Ok", 2, 1));
        options.addElement(new InterfaceComponents.iCommand("Huy", 2, 0));
        boolean selectHandled = AutoSellManager.handleSelectDialog(dialogText, options);
        check(selectHandled, "handleSelectDialog must intercept and confirm");
    }

    static void testMinEmptySlotsConfiguration() {
        AutoSellManager.minEmptySlots = 2;
        AutoSellManager.cycleMinEmptySlots();
        check(AutoSellManager.minEmptySlots == 3, "cycleMinEmptySlots from 2 to 3");
        AutoSellManager.cycleMinEmptySlots();
        check(AutoSellManager.minEmptySlots == 4, "cycleMinEmptySlots from 3 to 4");
        AutoSellManager.cycleMinEmptySlots();
        check(AutoSellManager.minEmptySlots == 5, "cycleMinEmptySlots from 4 to 5");
        AutoSellManager.cycleMinEmptySlots();
        check(AutoSellManager.minEmptySlots == 7, "cycleMinEmptySlots from 5 to 7");
        AutoSellManager.cycleMinEmptySlots();
        check(AutoSellManager.minEmptySlots == 10, "cycleMinEmptySlots from 7 to 10");
        AutoSellManager.cycleMinEmptySlots();
        check(AutoSellManager.minEmptySlots == 0, "cycleMinEmptySlots from 10 to 0 (OFF)");
        AutoSellManager.cycleMinEmptySlots();
        check(AutoSellManager.minEmptySlots == 1, "cycleMinEmptySlots from 0 to 1");
        AutoSellManager.cycleMinEmptySlots();
        check(AutoSellManager.minEmptySlots == 2, "cycleMinEmptySlots from 1 to 2");
    }

    static void testDelayConfiguration() {
        AutoSellManager.delaySellMs=1000L;AutoSellManager.cycleDelaySell();
        check(AutoSellManager.delaySellMs==2000L,"sell delay fixed to two seconds");
    }

    static void testReturnMethodConfiguration() {
        AutoSellManager.returnMethod = 0;
        AutoSellManager.cycleReturnMethod();
        check(AutoSellManager.returnMethod == 1, "cycleReturnMethod from 0 to 1 (GoBack)");
        AutoSellManager.cycleReturnMethod();
        check(AutoSellManager.returnMethod == 0, "cycleReturnMethod from 1 to 0 (Relog)");
    }

    public static void main(String[] args) {
        testFilterMatrix();
        MainItem upgrading = new MainItem(); upgrading.Id=501; upgrading.ItemCatagory=3; upgrading.itemName="Upgrade target";
        AutoUpgrade.active=true; AutoUpgrade.protectedId=501;
        check(!AutoSellManager.isTrashItem(upgrading), "Upgrade target cannot be sold");
        int packetsBefore=GlobalService.sellPackets,homesBefore=GlobalService.homes;
        AutoSellManager.update();
        check(GlobalService.sellPackets==packetsBefore && GlobalService.homes==homesBefore, "AutoSell must pause during upgrade");
        AutoUpgrade.active=false;
        testQueueAndRateLimiting();
        testStandingNearNpcRequired();
        testFullBagAutoTrigger();
        testReturnToVillageWhenBagFullOutside();
        testSellConfirmationDialogHandling();
        testMinEmptySlotsConfiguration();
        testDelayConfiguration();
        testReturnMethodConfiguration();
        System.out.println("PASS: " + assertions + " checks; AutoSell filter matrix, queue rate limit, full bag trigger, NPC proximity check, return to village trigger, dialog auto-confirm, min empty slots, delay settings, return method");
    }
}
