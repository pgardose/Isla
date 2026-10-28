package isla.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import isla.ui.Palette;
import org.junit.jupiter.api.Test;

/** The game package may not import the UI package, so it repeats the hex codes. This keeps the copies honest. */
class PaletteMatchesGameColoursTest {

    @Test
    void panelColoursComeFromThePalette() {
        assertEquals(Palette.INK, GameSession.INK);
        assertEquals(Palette.PAPER, GameSession.PAPER);
        assertEquals(Palette.PAPER, GameSession.CARD);
        assertEquals(Palette.WOOD, GameSession.BORDER);
        assertEquals(Palette.GOLD, GameSession.GOLD);
        assertEquals(Palette.WOOD, GameSession.MONEY_INK);
    }

    @Test
    void hudColoursComeFromThePalette() {
        assertEquals(Palette.SHADOW, GameSession.HUD_FILL);
        assertEquals(Palette.FOAM, GameSession.HUD_TEXT);
        assertEquals(Palette.INK, GameSession.BAR_BACK);
        assertEquals(Palette.GRASS, GameSession.BAR_FILL);
    }

    @Test
    void seededVendorsAndGiversUseDistinctSpriteKeys() {
        java.util.List<String> keys = new java.util.ArrayList<>();
        SeedData.vendors().forEach(v -> keys.add(v.getSprite()));
        keys.add(SeedData.elder().getSprite());
        keys.add(SeedData.coder().getSprite());
        assertEquals(keys.size(), new java.util.HashSet<>(keys).size(), "every NPC needs its own look: " + keys);
    }
}
