package GameObjects;

import Main.GameCanvas;

/** Opens the game's native pickup settings; Player owns all pickup requests. */
public final class AutoLoot {
    public static void show() {
        GameCanvas.menu2.doCloseMenu();
        GameCanvas.start_Auto_Item_Dialog();
    }
}
