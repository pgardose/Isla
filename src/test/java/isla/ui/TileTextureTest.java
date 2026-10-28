package isla.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import isla.world.TileType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TileTextureTest {

    @Test
    void sameTileAlwaysGetsTheSameTexture() {
        for (TileKind kind : TileKind.values()) {
            for (int frame = 0; frame < 2; frame++) {
                assertArrayEquals(TileTexture.generate(kind, 17, 9, frame), TileTexture.generate(kind, 17, 9, frame));
            }
        }
    }

    @Test
    void hashIsStableAndSpreadsNeighbours() {
        assertEquals(TileTexture.hash(3, 4), TileTexture.hash(3, 4));
        assertNotEquals(TileTexture.hash(3, 4), TileTexture.hash(4, 3));
        Set<Integer> seen = new HashSet<>();
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 20; x++) {
                seen.add(TileTexture.hash(x, y));
            }
        }
        assertTrue(seen.size() > 390, "400 tiles should give almost 400 different hashes, got " + seen.size());
    }

    @Test
    void differentCoordinatesGiveDifferentGrass() {
        Set<String> looks = new HashSet<>();
        for (int x = 0; x < 12; x++) {
            looks.add(String.join("", TileTexture.generate(TileKind.GRASS, x, 5, 0)));
        }
        assertTrue(looks.size() >= 10, "grass tiles should not all look the same");
    }

    @Test
    void everyTextureIsA16x16GridOfPaletteLetters() {
        for (TileKind kind : TileKind.values()) {
            String[] grid = TileTexture.generate(kind, 5, 8, 0);
            assertEquals(16, grid.length);
            for (String row : grid) {
                assertEquals(16, row.length());
                for (char c : row.toCharArray()) {
                    assertTrue(Palette.letters().contains(c), kind + " has non-palette letter " + c);
                }
            }
        }
    }

    @Test
    void eachTerrainUsesItsOwnColours() {
        assertOnly(TileKind.GRASS, "Gg");
        assertOnly(TileKind.SAND, "Ss");
        assertOnly(TileKind.PATH, "TSW");
        assertOnly(TileKind.JUNGLE, "JPg");
        assertOnly(TileKind.WATER, "oFO");
    }

    @Test
    void texturesHaveDetailNotJustAFlatColour() {
        for (TileKind kind : new TileKind[] {TileKind.GRASS, TileKind.SAND, TileKind.PATH, TileKind.JUNGLE, TileKind.WATER}) {
            for (int x = 0; x < 30; x++) {
                String all = String.join("", TileTexture.generate(kind, x, 3, 0));
                assertTrue(all.chars().distinct().count() >= 2, kind + " tile " + x + " is flat");
            }
        }
    }

    @Test
    void landDoesNotAnimateButWaterDoes() {
        assertArrayEquals(TileTexture.generate(TileKind.GRASS, 4, 4, 0), TileTexture.generate(TileKind.GRASS, 4, 4, 1));
        assertArrayEquals(TileTexture.generate(TileKind.SAND, 4, 4, 0), TileTexture.generate(TileKind.SAND, 4, 4, 1));
        int differing = 0;
        for (int x = 0; x < 20; x++) {
            if (!Arrays.equals(TileTexture.generate(TileKind.WATER, x, 2, 0), TileTexture.generate(TileKind.WATER, x, 2, 1))) {
                differing++;
            }
        }
        assertTrue(differing >= 15, "water frames should differ on almost every tile, got " + differing);
    }

    @Test
    void waterFrameFlipsEveryHalfSecondAndOnlyUsesTwoFrames() {
        assertEquals(0, TileTexture.waterFrame(0));
        assertEquals(0, TileTexture.waterFrame(TileTexture.WATER_TICKS - 1));
        assertEquals(1, TileTexture.waterFrame(TileTexture.WATER_TICKS));
        assertEquals(0, TileTexture.waterFrame(2 * TileTexture.WATER_TICKS));
        assertEquals(0, TileTexture.waterFrame(-1 * 2 * TileTexture.WATER_TICKS));
    }

    @Test
    void palmAndHutTilesStandOnGrass() {
        assertArrayEquals(TileTexture.generate(TileKind.GRASS, 7, 7, 0), TileTexture.generate(TileKind.PALM, 7, 7, 0));
        assertArrayEquals(TileTexture.generate(TileKind.GRASS, 7, 7, 0), TileTexture.generate(TileKind.HUT, 7, 7, 0));
        assertEquals("palm", TileKind.PALM.overlaySprite());
        assertEquals("hut", TileKind.HUT.overlaySprite());
        assertNull(TileKind.GRASS.overlaySprite());
    }

    @Test
    void everyWorldTileColourIsKnownToTheRenderer() {
        for (TileType type : TileType.values()) {
            assertNotEquals(null, TileKind.fromHex(type.getColor()), type + " has no texture");
        }
        assertEquals(TileKind.WATER, TileKind.fromHex("#3fa9d6"), "case does not matter");
        assertFalse(TileKind.fromHex("#123456") != null);
    }

    private static void assertOnly(TileKind kind, String allowed) {
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 6; x++) {
                for (int frame = 0; frame < 2; frame++) {
                    for (String row : TileTexture.generate(kind, x, y, frame)) {
                        for (char c : row.toCharArray()) {
                            assertTrue(allowed.indexOf(c) >= 0, kind + " used letter " + c);
                        }
                    }
                }
            }
        }
    }
}
