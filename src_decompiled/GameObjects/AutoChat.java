package GameObjects;

import CLib.mSystem;
import CLib.mVector;
import GameScreen.GameScreen;
import InterfaceComponents.IAction;
import InterfaceComponents.InputDialog;
import InterfaceComponents.MsgDialog;
import InterfaceComponents.iCommand;
import Main.GameCanvas;
import Model.CRes;
import netcommand.global.GlobalService;

public class AutoChat {
    public static final String AUTHOR = "HONG PHUC HSO";
    public static final String VERSION = "v1.0";
    public static boolean isAutoChat = false;
    public static boolean isRandomPrefix = true;
    public static String strChat = "Auto Chat HSO";
    public static int delayChat = 3;
    public static long lastTimeChat = 0L;
    public static int countSent = 0;
    public static int msgIndex = 0;
    public static InputDialog inputChat;
    public static InputDialog inputDelay;
    private static boolean isConfigLoaded = false;

    public static void loadConfig() {
        if (isConfigLoaded) {
            return;
        }
        isConfigLoaded = true;
        try {
            String sText = CRes.loadRMSString("hso_autochat_text");
            if (sText != null && sText.trim().length() > 0) {
                strChat = sText;
            }
            String sDelay = CRes.loadRMSString("hso_autochat_delay");
            if (sDelay != null && sDelay.trim().length() > 0) {
                try {
                    int d = Integer.parseInt(sDelay.trim());
                    if (d >= 1) {
                        delayChat = d;
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            String sRand = CRes.loadRMSString("hso_autochat_rand");
            if (sRand != null && sRand.trim().length() > 0) {
                isRandomPrefix = sRand.trim().equals("1");
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void saveConfig() {
        try {
            CRes.saveRMSString_("hso_autochat_text", strChat);
            CRes.saveRMSString_("hso_autochat_delay", String.valueOf(delayChat));
            CRes.saveRMSString_("hso_autochat_rand", isRandomPrefix ? "1" : "0");
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void toggle() {
        loadConfig();
        isAutoChat = !isAutoChat;
        if (isAutoChat) {
            lastTimeChat = mSystem.currentTimeMillis();
            GameCanvas.addInfoCharCline("[HONG PHUC HSO] Auto Chat: B\u1eacT (" + delayChat + "s)");
        } else {
            GameCanvas.addInfoCharCline("[HONG PHUC HSO] Auto Chat: T\u1eaeT");
        }
    }

    public static void toggleRandomPrefix() {
        loadConfig();
        isRandomPrefix = !isRandomPrefix;
        saveConfig();
        GameCanvas.addInfoCharCline("S\u1ed1 ch\u1ed1ng spam: " + (isRandomPrefix ? "B\u1eacT" : "T\u1eaeT"));
    }

    public static void update() {
        if (!isAutoChat) {
            return;
        }
        if (GameScreen.player == null) {
            return;
        }
        long l = mSystem.currentTimeMillis();
        if (l - lastTimeChat >= (long)(delayChat * 1000)) {
            lastTimeChat = l;
            sendNextMessage();
        }
    }

    private static void sendNextMessage() {
        if (strChat == null || strChat.trim().length() == 0) {
            return;
        }
        String[] stringArray = splitMessages(strChat);
        if (stringArray == null || stringArray.length == 0) {
            return;
        }
        if (msgIndex >= stringArray.length) {
            msgIndex = 0;
        }
        String string = stringArray[msgIndex].trim();
        msgIndex = (msgIndex + 1) % stringArray.length;
        if (string.length() > 0) {
            String finalMsg = string;
            if (isRandomPrefix) {
                int r = CRes.random(10, 1000);
                finalMsg = r + ", " + string;
            }
            GameScreen.player.strChatPopup = finalMsg;
            GlobalService.gI().chatPopup(finalMsg);
            ++countSent;
        }
    }

    public static String[] splitMessages(String string) {
        if (string == null) {
            return new String[0];
        }
        mVector mVector2 = new mVector();
        int n = 0;
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (c != ';' && c != '\n' && c != '\r') continue;
            String string2 = string.substring(n, i).trim();
            if (string2.length() > 0) {
                mVector2.addElement(string2);
            }
            n = i + 1;
        }
        if (n < string.length()) {
            String string3 = string.substring(n).trim();
            if (string3.length() > 0) {
                mVector2.addElement(string3);
            }
        }
        if (mVector2.size() == 0) {
            return new String[]{string};
        }
        String[] stringArray = new String[mVector2.size()];
        for (int i = 0; i < mVector2.size(); ++i) {
            stringArray[i] = (String)mVector2.elementAt(i);
        }
        return stringArray;
    }

    public static void showMenu() {
        loadConfig();
        mVector mVector2 = new mVector("AutoChat Menu");
        String sAuto = isAutoChat ? "T\u1eaft Auto Chat" : "B\u1eadt Auto Chat";
        mVector2.addElement(new iCommand(sAuto, new IAction(){

            public void perform() {
                toggle();
            }
        }));
        String sRand = isRandomPrefix ? "S\u1ed1 ng\u1eabu nhi\u00ean: B\u1eacT" : "S\u1ed1 ng\u1eabu nhi\u00ean: T\u1eaeT";
        mVector2.addElement(new iCommand(sRand, new IAction(){

            public void perform() {
                toggleRandomPrefix();
            }
        }));
        mVector2.addElement(new iCommand("C\u00e0i n\u1ed9i dung", new IAction(){

            public void perform() {
                startInputChat();
            }
        }));
        mVector2.addElement(new iCommand("C\u00e0i th\u1eddi gian (s)", new IAction(){

            public void perform() {
                startInputDelay();
            }
        }));
        mVector2.addElement(new iCommand("Th\u00f4ng tin [HONG PHUC]", new IAction(){

            public void perform() {
                showInfo();
            }
        }));
        GameCanvas.menu2.startAt(mVector2, 2, "HONG PHUC HSO", false, null);
    }

    public static void startInputChat() {
        inputChat = new InputDialog();
        inputChat.setinfo("N\u1ed9i dung (d\u00f9ng ';' \u0111\u1ec3 \u0111\u1ed5i c\u00e2u):", new iCommand("L\u01b0u", new IAction(){

            public void perform() {
                String string = inputChat.tfInput.getText();
                if (string != null && string.trim().length() > 0) {
                    strChat = string;
                    saveConfig();
                    GameCanvas.addInfoCharCline("\u0110\u00e3 l\u01b0u n\u1ed9i dung Auto Chat!");
                }
                inputChat.closeDialog();
            }
        }), false, "C\u00e0i n\u1ed9i dung");
        if (strChat != null) {
            inputChat.tfInput.setText(strChat);
        }
        GameCanvas.currentDialog = inputChat;
    }

    public static void startInputDelay() {
        inputDelay = new InputDialog();
        inputDelay.setinfo("Th\u1eddi gian l\u1eb7p l\u1ea1i (gi\u00e2y >= 1):", new iCommand("L\u01b0u", new IAction(){

            public void perform() {
                try {
                    int n = Integer.parseInt(inputDelay.tfInput.getText().trim());
                    if (n < 1) {
                        n = 1;
                    }
                    delayChat = n;
                    saveConfig();
                    GameCanvas.addInfoCharCline("\u0110\u00e3 \u0111\u1eb7t: " + delayChat + " gi\u00e2y");
                }
                catch (Exception exception) {
                    GameCanvas.addInfoCharCline("Th\u1eddi gian kh\u00f4ng h\u1ee3p l\u1ec7!");
                }
                inputDelay.closeDialog();
            }
        }), true, "C\u00e0i th\u1eddi gian");
        inputDelay.tfInput.setText(String.valueOf(delayChat));
        GameCanvas.currentDialog = inputDelay;
    }

    public static void showInfo() {
        String string = isAutoChat ? "B\u1eacT" : "T\u1eaeT";
        String sRand = isRandomPrefix ? "B\u1eacT" : "T\u1eaeT";
        String string2 = "=== HONG PHUC HSO ===\n" +
                         "B\u1ea3n quy\u1ec1n: HONG PHUC HSO\n" +
                         "Tool: Auto Chat v1.0\n" +
                         "Tr\u1ea1ng th\u00e1i: " + string + "\n" +
                         "Th\u1eddi gian: " + delayChat + " gi\u00e2y\n" +
                         "S\u1ed1 ch\u1ed1ng spam: " + sRand + "\n" +
                         "\u0110\u00e3 g\u1eedi: " + countSent + " tin\n" +
                         "N\u1ed9i dung: " + strChat;
        MsgDialog msgDialog = new MsgDialog();
        msgDialog.setinfo(string2, new iCommand("\u0110\u00f3ng", new IAction(){

            public void perform() {
                GameCanvas.end_Dialog();
            }
        }), true);
        GameCanvas.currentDialog = msgDialog;
    }
}
