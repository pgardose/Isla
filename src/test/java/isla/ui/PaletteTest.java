package isla.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PaletteTest {

    @Test
    void namedColoursMatchTheWarmTropicalPalette() {
        assertEquals("#2471A3", Palette.DEEP_OCEAN);
        assertEquals("#3FA9D6", Palette.OCEAN);
        assertEquals("#D6F0F7", Palette.FOAM);
        assertEquals("#EAD9A0", Palette.SAND);
        assertEquals("#D4B96E", Palette.SAND_DARK);
        assertEquals("#7BC96F", Palette.GRASS);
        assertEquals("#4E9F4A", Palette.GRASS_DARK);
        assertEquals("#2E8B57", Palette.PALM);
        assertEquals("#1F6F3D", Palette.JUNGLE);
        assertEquals("#C8A56A", Palette.PATH);
        assertEquals("#B5651D", Palette.WOOD);
        assertEquals("#2B1D0E", Palette.INK);
        assertEquals("#F5E9C8", Palette.PAPER);
        assertEquals("#F4C542", Palette.GOLD);
        assertEquals("#E8664A", Palette.CORAL);
        assertEquals("#1A2733", Palette.SHADOW);
    }

    @Test
    void everyPaletteColourHasItsOwnSpriteLetter() {
        Set<String> hexes = new HashSet<>();
        for (char letter : Palette.letters()) {
            hexes.add(Palette.hex(letter));
        }
        assertEquals(16, Palette.letters().size());
        assertEquals(16, hexes.size(), "two letters must not share a colour");
    }

    @Test
    void dotIsTransparentAndUnknownLettersAreRejected() {
        assertNull(Palette.hex('.'));
        assertEquals(0, Palette.argb('.'));
        assertThrows(IllegalArgumentException.class, () -> Palette.hex('?'));
        assertThrows(IllegalArgumentException.class, () -> Palette.argb('#'));
    }

    @Test
    void hexParsesToOpaqueOrAlphaArgb() {
        assertEquals(0xFF2471A3, Palette.argb("#2471A3"));
        assertEquals(0xE0F5E9C8, Palette.argb("#F5E9C8E0"));
        assertEquals(Palette.argb(Palette.GOLD), Palette.argb('Y'));
        assertTrue(Palette.argb('I') != 0);
    }

    @Test
    void badHexIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Palette.argb("2471A3"));
        assertThrows(IllegalArgumentException.class, () -> Palette.argb("#24"));
    }
}
