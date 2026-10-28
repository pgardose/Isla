package isla.ui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The one colour palette for the whole game: a warm tropical set of sixteen colours.
 * Every UI colour comes from here, and sprites are drawn with one letter per colour
 * (see {@link #hex(char)}), so changing a colour here recolours the entire game.
 *
 * <p>Pure Java (no JavaFX): colours are "#RRGGBB" strings, the same form the Renderer uses.
 */
public final class Palette {

    public static final String DEEP_OCEAN = "#2471A3";
    public static final String OCEAN = "#3FA9D6";
    public static final String FOAM = "#D6F0F7";
    public static final String SAND = "#EAD9A0";
    public static final String SAND_DARK = "#D4B96E";
    public static final String GRASS = "#7BC96F";
    public static final String GRASS_DARK = "#4E9F4A";
    public static final String PALM = "#2E8B57";
    public static final String JUNGLE = "#1F6F3D";
    public static final String PATH = "#C8A56A";
    public static final String WOOD = "#B5651D";
    public static final String INK = "#2B1D0E";
    public static final String PAPER = "#F5E9C8";
    public static final String GOLD = "#F4C542";
    public static final String CORAL = "#E8664A";
    public static final String SHADOW = "#1A2733";

    /** The letter that means "see-through" in a sprite grid. */
    public static final char TRANSPARENT = '.';

    private static final Map<Character, String> LETTERS = new LinkedHashMap<>();

    static {
        LETTERS.put('O', DEEP_OCEAN);
        LETTERS.put('o', OCEAN);
        LETTERS.put('F', FOAM);
        LETTERS.put('S', SAND);
        LETTERS.put('s', SAND_DARK);
        LETTERS.put('G', GRASS);
        LETTERS.put('g', GRASS_DARK);
        LETTERS.put('P', PALM);
        LETTERS.put('J', JUNGLE);
        LETTERS.put('T', PATH);
        LETTERS.put('W', WOOD);
        LETTERS.put('I', INK);
        LETTERS.put('A', PAPER);
        LETTERS.put('Y', GOLD);
        LETTERS.put('C', CORAL);
        LETTERS.put('D', SHADOW);
    }

    private Palette() {
    }

    /** The sprite letters that stand for a colour (not including the transparent dot). */
    public static Set<Character> letters() {
        return Collections.unmodifiableSet(LETTERS.keySet());
    }

    /** Hex colour for a sprite letter; null for the transparent dot; error for anything else. */
    public static String hex(char letter) {
        if (letter == TRANSPARENT) {
            return null;
        }
        String hex = LETTERS.get(letter);
        if (hex == null) {
            throw new IllegalArgumentException("Not a palette letter: '" + letter + "'");
        }
        return hex;
    }

    /** ARGB pixel for a sprite letter (0 = fully transparent for the dot). */
    public static int argb(char letter) {
        String hex = hex(letter);
        return hex == null ? 0 : argb(hex);
    }

    /** Parses "#RRGGBB" (opaque) or "#RRGGBBAA" into an ARGB int. */
    public static int argb(String hex) {
        if (hex == null || (hex.length() != 7 && hex.length() != 9) || hex.charAt(0) != '#') {
            throw new IllegalArgumentException("Expected #RRGGBB or #RRGGBBAA but got: " + hex);
        }
        int rgb = Integer.parseInt(hex.substring(1, 7), 16);
        int alpha = hex.length() == 9 ? Integer.parseInt(hex.substring(7, 9), 16) : 0xFF;
        return (alpha << 24) | rgb;
    }
}
