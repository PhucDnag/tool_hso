package GameObjects;

import Main.GameCanvas;
import Model.CRes;

public class MedalFilter {
    public static boolean blockWhite = false;  // Đóng rớt NL mề đay trắng (true = không nhặt)
    public static boolean blockGreen = false;  // Đóng rớt NL mề đay xanh lá
    public static boolean blockYellow = false; // Đóng rớt NL mề đay vàng
    public static boolean blockPurple = false; // Đóng rớt NL mề đay tím
    private static boolean isLoaded = false;

    public static void loadConfig() {
        if (isLoaded) {
            return;
        }
        isLoaded = true;
        try {
            String sW = CRes.loadRMSString("hso_block_medal_w");
            if (sW != null && sW.trim().length() > 0) {
                blockWhite = sW.trim().equals("1");
            }
            String sG = CRes.loadRMSString("hso_block_medal_g");
            if (sG != null && sG.trim().length() > 0) {
                blockGreen = sG.trim().equals("1");
            }
            String sY = CRes.loadRMSString("hso_block_medal_y");
            if (sY != null && sY.trim().length() > 0) {
                blockYellow = sY.trim().equals("1");
            }
            String sP = CRes.loadRMSString("hso_block_medal_p");
            if (sP != null && sP.trim().length() > 0) {
                blockPurple = sP.trim().equals("1");
            }
        } catch (Exception ex) {
            // empty catch block
        }
    }

    public static void saveConfig() {
        try {
            CRes.saveRMSString_("hso_block_medal_w", blockWhite ? "1" : "0");
            CRes.saveRMSString_("hso_block_medal_g", blockGreen ? "1" : "0");
            CRes.saveRMSString_("hso_block_medal_y", blockYellow ? "1" : "0");
            CRes.saveRMSString_("hso_block_medal_p", blockPurple ? "1" : "0");
        } catch (Exception ex) {
            // empty catch block
        }
    }

    public static boolean shouldPickMedal(int color) {
        loadConfig();
        if (color == 0 && blockWhite) {
            return false;
        }
        if (color == 1 && blockGreen) {
            return false;
        }
        if (color == 2 && blockYellow) {
            return false;
        }
        if (color == 3 && blockPurple) {
            return false;
        }
        return true;
    }

    public static void toggleBlockWhite() {
        loadConfig();
        blockWhite = !blockWhite;
        saveConfig();
        GameCanvas.addInfoCharCline("Dong rot Me Day Trang: " + (blockWhite ? "BAT (Khong nhat)" : "TAT (Co nhat)"));
    }

    public static void toggleBlockGreen() {
        loadConfig();
        blockGreen = !blockGreen;
        saveConfig();
        GameCanvas.addInfoCharCline("Dong rot Me Day Xanh: " + (blockGreen ? "BAT (Khong nhat)" : "TAT (Co nhat)"));
    }

    public static void toggleBlockYellow() {
        loadConfig();
        blockYellow = !blockYellow;
        saveConfig();
        GameCanvas.addInfoCharCline("Dong rot Me Day Vang: " + (blockYellow ? "BAT (Khong nhat)" : "TAT (Co nhat)"));
    }

    public static void toggleBlockPurple() {
        loadConfig();
        blockPurple = !blockPurple;
        saveConfig();
        GameCanvas.addInfoCharCline("Dong rot Me Day Tim: " + (blockPurple ? "BAT (Khong nhat)" : "TAT (Co nhat)"));
    }
}
