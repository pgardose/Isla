package isla.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpritesTest {

    private static final List<String> CHARACTERS = List.of("player", "mara", "tomas", "yara", "elder_kai", "coder_nia");

    @Test
    void everyRequiredSpriteExists() {
        List<String> names = Sprites.names();
        for (String n : List.of("player", "mara", "tomas", "yara", "elder_kai", "coder_nia",
                "palm", "hut", "exclaim", "question")) {
            assertTrue(names.contains(n), "missing sprite " + n);
        }
    }

    @Test
    void everyFrameIsA16x16GridOfPaletteLetters() {
        for (String name : Sprites.names()) {
            for (String[] frame : Sprites.frames(name)) {
                assertEquals(16, frame.length, name + " rows");
                for (String row : frame) {
                    assertEquals(16, row.length(), name + " row width: " + row);
                    for (char c : row.toCharArray()) {
                        assertTrue(c == '.' || Palette.letters().contains(c), name + " bad letter " + c);
                    }
                }
            }
        }
    }

    @Test
    void playerHasTwoDifferentWalkingFrames() {
        List<String[]> frames = Sprites.frames("player");
        assertEquals(2, frames.size());
        assertTrue(differences(frames.get(0), frames.get(1)) > 0);
    }

    @Test
    void everySpriteHasVisiblePixels() {
        for (String name : Sprites.names()) {
            for (String[] frame : Sprites.frames(name)) {
                long solid = java.util.Arrays.stream(frame).flatMapToInt(String::chars).filter(c -> c != '.').count();
                assertTrue(solid >= 20, name + " is almost empty");
            }
        }
    }

    @Test
    void charactersAreVisuallyDistinct() {
        for (int i = 0; i < CHARACTERS.size(); i++) {
            for (int j = i + 1; j < CHARACTERS.size(); j++) {
                int diff = differences(Sprites.frames(CHARACTERS.get(i)).get(0), Sprites.frames(CHARACTERS.get(j)).get(0));
                assertTrue(diff >= 60, CHARACTERS.get(i) + " vs " + CHARACTERS.get(j) + " differ in only " + diff + " cells");
            }
        }
    }

    @Test
    void gameSpriteKeysMapToCanonicalNames() {
        assertEquals("player", Sprites.nameForKey("player_sprite"));
        assertEquals("mara", Sprites.nameForKey("mara_sprite"));
        assertEquals("tomas", Sprites.nameForKey("tomas_sprite"));
        assertEquals("yara", Sprites.nameForKey("trader_sprite"));
        assertEquals("elder_kai", Sprites.nameForKey("elder_sprite"));
        assertEquals("coder_nia", Sprites.nameForKey("coder_sprite"));
        assertEquals("mara", Sprites.nameForKey("vendor_sprite"), "old saves keep working");
        assertNull(Sprites.nameForKey("nonsense_sprite"));
    }

    @Test
    void markerLabelsMapToMarkerSprites() {
        assertEquals("exclaim", Sprites.nameForLabel("!"));
        assertEquals("question", Sprites.nameForLabel("?"));
        assertNull(Sprites.nameForLabel("hello"));
    }

    @Test
    void unknownSpriteNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Sprites.frames("dragon"));
    }

    @Test
    void marksDifferFromEachOther() {
        assertNotEquals(String.join("", Sprites.frames("exclaim").get(0)), String.join("", Sprites.frames("question").get(0)));
    }

    private static int differences(String[] a, String[] b) {
        int d = 0;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (a[y].charAt(x) != b[y].charAt(x)) {
                    d++;
                }
            }
        }
        return d;
    }
}
