package GameObjects;

import GameScreen.GameScreen;

/** Local rendering only: never change character identity or network data. */
public final class DisplayPrivacy {
    public static final String ALIAS = "PhucHso";

    private static boolean word(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
            || c >= '0' && c <= '9' || c == '_';
    }

    public static String text(String value) {
        if (value == null || GameScreen.player == null) return value;
        String name = GameScreen.player.name;
        if (name == null || name.length() == 0 || name.equalsIgnoreCase(ALIAS)) return value;
        int length = name.length(), from = 0;
        StringBuffer out = null;
        for (int i = 0; i <= value.length() - length; i++) {
            if (!value.regionMatches(true, i, name, 0, length)
                    || i > 0 && word(value.charAt(i - 1))
                    || i + length < value.length() && word(value.charAt(i + length))) continue;
            if (out == null) out = new StringBuffer();
            out.append(value.substring(from, i)).append(ALIAS);
            from = i + length;
            i = from - 1;
        }
        return out == null ? value : out.append(value.substring(from)).toString();
    }
}
