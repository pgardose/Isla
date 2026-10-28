package isla.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Finds the pixels for a sprite name. If an artist has put {@code src/main/resources/sprites/<name>.png}
 * in the project it wins; otherwise the code-drawn sprite from {@link Sprites} is used. A PNG is 16 pixels
 * tall and 16 x N pixels wide (N frames side by side); anything else is ignored. The actual PNG reading is
 * handed in as a function so this class needs no GUI toolkit and can be tested with fakes.
 */
final class SpriteLibrary {

    enum Source { PNG, CODE }

    record Sprite(Source source, List<Bitmap> frames) {
    }

    private final Function<String, Bitmap> pngLoader;
    private final Map<String, Sprite> cache = new HashMap<>();

    /** @param pngLoader returns the picture at a classpath path like "/sprites/mara.png", or null if there is none */
    SpriteLibrary(Function<String, Bitmap> pngLoader) {
        this.pngLoader = pngLoader;
    }

    static String pngPath(String name) {
        return "/sprites/" + name + ".png";
    }

    /** Cuts a wide PNG into 16x16 frames; an empty list means the picture is not a valid sprite sheet. */
    static List<Bitmap> splitFrames(Bitmap sheet) {
        int size = Sprites.SIZE;
        if (sheet == null || sheet.height() != size || sheet.width() % size != 0) {
            return List.of();
        }
        List<Bitmap> frames = new ArrayList<>();
        for (int f = 0; f < sheet.width() / size; f++) {
            int[] px = new int[size * size];
            for (int y = 0; y < size; y++) {
                System.arraycopy(sheet.argb(), y * sheet.width() + f * size, px, y * size, size);
            }
            frames.add(new Bitmap(size, size, px));
        }
        return frames;
    }

    /** The sprite to draw for a name: the artist's PNG if usable, else the code-drawn one. */
    Sprite get(String name) {
        Sprite cached = cache.get(name);
        if (cached != null) {
            return cached;
        }
        Sprite sprite = load(name);
        cache.put(name, sprite);
        return sprite;
    }

    private Sprite load(String name) {
        List<Bitmap> frames = List.of();
        try {
            frames = splitFrames(pngLoader.apply(pngPath(name)));
        } catch (RuntimeException e) {
            // a corrupt or unreadable PNG must never stop the game: use the built-in sprite instead
        }
        if (!frames.isEmpty()) {
            return new Sprite(Source.PNG, frames);
        }
        List<Bitmap> drawn = new ArrayList<>();
        for (String[] grid : Sprites.frames(name)) { // throws for a name nobody has drawn
            drawn.add(PixelImages.fromGrid(grid));
        }
        return new Sprite(Source.CODE, List.copyOf(drawn));
    }
}
