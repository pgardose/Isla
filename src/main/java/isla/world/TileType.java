package isla.world;

import java.util.Set;

/**
 * Terrain kinds. Each has the character used in the map text file, a
 * placeholder color, and walking rules. A tile with a required area (jungle)
 * is walkable only once the player has unlocked that area with a Tool.
 */
public enum TileType {
    GRASS('g', "#7BC96F", true, null),
    SAND('s', "#EAD9A0", true, null),
    WATER('w', "#3FA9D6", false, null),
    PATH('p', "#C8A56A", true, null),
    PALM('t', "#2E8B57", false, null),
    HUT('h', "#B5651D", false, null),
    JUNGLE('j', "#1F6F3D", true, "Jungle Path");

    private final char symbol;
    private final String color;
    private final boolean walkable;
    private final String requiredArea;

    TileType(char symbol, String color, boolean walkable, String requiredArea) {
        this.symbol = symbol;
        this.color = color;
        this.walkable = walkable;
        this.requiredArea = requiredArea;
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
