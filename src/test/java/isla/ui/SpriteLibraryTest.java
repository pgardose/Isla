package isla.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class SpriteLibraryTest {

    private static Bitmap solid(int w, int h, int argb) {
        int[] px = new int[w * h];
        java.util.Arrays.fill(px, argb);
        return new Bitmap(w, h, px);
    }

    @Test
    void usesCodeDrawnSpriteWhenThereIsNoPng() {
        SpriteLibrary lib = new SpriteLibrary(path -> null);
        SpriteLibrary.Sprite s = lib.get("mara");
        assertEquals(SpriteLibrary.Source.CODE, s.source());
        assertEquals(1, s.frames().size());
        assertEquals(16, s.frames().get(0).width());
    }

    @Test
    void usesThePngWhenOneExists() {
        List<String> asked = new ArrayList<>();
        Function<String, Bitmap> loader = path -> {
            asked.add(path);
            return path.equals("/sprites/mara.png") ? solid(16, 16, 0xFF112233) : null;
        };
        SpriteLibrary.Sprite s = new SpriteLibrary(loader).get("mara");
        assertEquals(SpriteLibrary.Source.PNG, s.source());
        assertEquals(0xFF112233, s.frames().get(0).argb()[0]);
        assertEquals(List.of("/sprites/mara.png"), asked);
    }

    @Test
    void aWidePngIsSplitIntoWalkingFrames() {
        int[] px = new int[32 * 16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 32; x++) {
                px[y * 32 + x] = x < 16 ? 0xFF0000FF : 0xFF00FF00;
            }
        }
        SpriteLibrary.Sprite s = new SpriteLibrary(path -> new Bitmap(32, 16, px)).get("player");
        assertEquals(SpriteLibrary.Source.PNG, s.source());
        assertEquals(2, s.frames().size());
        assertEquals(0xFF0000FF, s.frames().get(0).argb()[0]);
        assertEquals(0xFF00FF00, s.frames().get(1).argb()[0]);
    }

    @Test
    void aPngOfTheWrongSizeFallsBackToTheCodeSprite() {
        for (Bitmap bad : new Bitmap[] {solid(15, 16, 1), solid(16, 17, 1), solid(24, 16, 1), solid(16, 32, 1)}) {
            SpriteLibrary.Sprite s = new SpriteLibrary(path -> bad).get("hut");
            assertEquals(SpriteLibrary.Source.CODE, s.source(), bad.width() + "x" + bad.height());
        }
    }

    @Test
    void aLoaderThatBlowsUpFallsBackToTheCodeSprite() {
        SpriteLibrary lib = new SpriteLibrary(path -> {
            throw new IllegalStateException("corrupt png");
        });
        assertEquals(SpriteLibrary.Source.CODE, lib.get("palm").source());
    }

    @Test
    void playerKeepsItsTwoCodeDrawnFramesWithoutAPng() {
        assertEquals(2, new SpriteLibrary(path -> null).get("player").frames().size());
    }

    @Test
    void resultsAreCachedSoThePngIsReadOnce() {
        int[] calls = {0};
        SpriteLibrary lib = new SpriteLibrary(path -> {
            calls[0]++;
            return null;
        });
        SpriteLibrary.Sprite a = lib.get("yara");
        SpriteLibrary.Sprite b = lib.get("yara");
        assertSame(a, b);
        assertEquals(1, calls[0]);
    }

    @Test
    void unknownNameWithoutPngIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> new SpriteLibrary(path -> null).get("dragon"));
    }

    @Test
    void anArtistCanAddASpriteTheCodeDoesNotKnow() {
        SpriteLibrary.Sprite s = new SpriteLibrary(path -> solid(16, 16, 0xFFABCDEF)).get("dragon");
        assertEquals(SpriteLibrary.Source.PNG, s.source());
    }

    @Test
    void pngPathUsesTheSpritesFolder() {
        assertEquals("/sprites/elder_kai.png", SpriteLibrary.pngPath("elder_kai"));
        assertTrue(SpriteLibrary.splitFrames(solid(48, 16, 1)).size() == 3);
        assertTrue(SpriteLibrary.splitFrames(solid(20, 16, 1)).isEmpty());
    }
}
