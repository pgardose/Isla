package isla.world;

import java.util.Set;

/**
 * Terrain kinds. Each has the character used in the map text file, a
 * placeholder color, and walking rules. A tile with a required area (jungle)
 * is walkable only once the player has unlocked that area with a Tool.
 *
 * Glitch tiles are the island's curse made visible: a bridge that will not draw, jungle that will not stop
 * growing. They block the way until the matching code stone is fixed (which unlocks their area), and from
 * then on they look like ordinary ground (openColor).
 */
public enum TileType {
    GRASS('g', "#7BC96F", true, null, null),
    SAND('s', "#EAD9A0", true, null, null),
    WATER('w', "#3FA9D6", false, null, null),
    PATH('p', "#C8A56A", true, null, null),
    PALM('t', "#2E8B57", false, null, null),
    HUT('h', "#B5651D", false, null, null),
    JUNGLE('j', "#1F6F3D", true, "Jungle Path", null),
    /** The east bridge, broken until the Jungle Shrine stone is fixed. */
    EAST_GLITCH('e', "#1A2733", true, "East Bridge", "#C8A56A"),
    /** The south bridge, broken until the South Bridge stone is fixed. */
    SOUTH_GLITCH('v', "#1A2733", true, "South Bridge", "#C8A56A"),
    /** Endless cursed jungle on the south island, cleared by the Overgrowth stone. */
    OVERGROWTH('o', "#E8664A", true, "Overgrowth Cleared", "#7BC96F");

    private final char symbol;
    private final String color;
    private final boolean walkable;
    private final String requiredArea;
    private final String openColor;

    TileType(char symbol, String color, boolean walkable, String requiredArea, String openColor) {
        this.symbol = symbol;
        this.color = color;
        this.walkable = walkable;
        this.requiredArea = requiredArea;
        this.openColor = openColor;
    }

    /** True for a cursed tile that changes its look once its area is unlocked. */
    public boolean isGlitch() {
        return openColor != null;
    }

    /** The colour to draw: a fixed glitch tile looks like the ground it became. */
    public String colorFor(Set<String> unlockedAreas) {
        if (openColor != null && unlockedAreas.contains(requiredArea)) {
            return openColor;
        }
        return color;
    }

    public char getSymbol() {
        return symbol;
    }

    public String getColor() {
        return color;
    }

    public String getRequiredArea() {
        return requiredArea;
    }

    public boolean isWalkable(Set<String> unlockedAreas) {
        return walkable && (requiredArea == null || unlockedAreas.contains(requiredArea));
    }

    /** Returns the type for a map-file character, or null if there is none. */
    public static TileType fromSymbol(char symbol) {
        for (TileType t : values()) {
            if (t.symbol == symbol) {
                return t;
            }
        }
        return null;
    }
}
