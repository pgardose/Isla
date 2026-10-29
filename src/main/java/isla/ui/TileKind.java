package isla.ui;

/**
 * The kinds of ground the renderer knows how to texture. The game only sends a colour per tile
 * (TileType.getColor), so the renderer recognises the kind from that colour; a test checks that
 * every TileType colour is known here.
 */
enum TileKind {
    GRASS(Palette.GRASS, null),
    SAND(Palette.SAND, null),
    WATER(Palette.OCEAN, null),
    PATH(Palette.PATH, null),
    PALM(Palette.PALM, "palm"),
    HUT(Palette.WOOD, "hut"),
    JUNGLE(Palette.JUNGLE, null),
    /** A broken bridge: flickering corrupted code over the sea. */
    GLITCH(Palette.SHADOW, null),
    /** Cursed jungle that grows forever, tinged coral. */
    OVERGROWTH(Palette.CORAL, null);

    private final String hex;
    private final String overlaySprite;

    TileKind(String hex, String overlaySprite) {
        this.hex = hex;
        this.overlaySprite = overlaySprite;
    }

    String hex() {
        return hex;
    }

    /** Sprite drawn on top of the grass (palms and huts stand on grass); null for plain ground. */
    String overlaySprite() {
        return overlaySprite;
    }

    /** True for kinds whose texture changes over time (the sea and the cursed tiles). */
    boolean isAnimated() {
        return this == WATER || this == GLITCH || this == OVERGROWTH;
    }

    /** The kind for a tile colour, or null if the colour is not one we know. */
    static TileKind fromHex(String hex) {
        for (TileKind k : values()) {
            if (k.hex.equalsIgnoreCase(hex)) {
                return k;
            }
        }
        return null;
    }
}
