package isla.ui;

import isla.Renderer;
import isla.game.GameSession;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Draws through a JavaFX GraphicsContext in a consistent pixel-art style. The pictures themselves come
 * from pure, tested classes (Sprites, SpriteLibrary, TileTexture, WalkAnimator, Palette); this class only
 * turns their pixels into JavaFX images and puts them on the canvas. Nothing outside isla.ui knows about JavaFX.
 *
 * <p>Pictures are enlarged to the window scale ahead of time (nearest neighbour) so the canvas draws them
 * one-to-one and pixel art stays sharp.
 */
final class JavaFxRenderer implements Renderer {

    private static final int T = GameSession.TILE_SIZE;
    private static final int MAX_TILE_IMAGES = 4096;
    private static final int MARKER_RISE = 13;
    private static final int MARKER_BOB_TICKS = 20;

    private final GraphicsContext gc;
    private final int scale;
    private final SpriteLibrary sprites = new SpriteLibrary(JavaFxRenderer::loadPng);
    private final WalkAnimator playerWalk = new WalkAnimator();
    private final Map<String, Image> spriteImages = new HashMap<>();
    private final Map<Long, Image> tileImages = new LinkedHashMap<>(1024, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Image> eldest) {
            return size() > MAX_TILE_IMAGES;
        }
    };
    private int camCol;
    private int camRow;
    private int frame;

    /** @param scale how many screen pixels one game pixel is (the canvas is scaled by the same amount) */
    JavaFxRenderer(GraphicsContext gc, int scale) {
        this.gc = gc;
        this.scale = scale;
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 9));
    }

    @Override
    public void setCamera(int col, int row) {
        this.camCol = col;
        this.camRow = row;
    }

    @Override
    public void setFrame(int frame) {
        this.frame = frame;
    }

    @Override
    public void drawTile(int col, int row, String hexColor) {
        int px = (col - camCol) * T;
        int py = (row - camRow) * T;
        TileKind kind = TileKind.fromHex(hexColor);
        if (kind == null) { // a colour with no texture: draw it flat, as before
            gc.setFill(Color.web(hexColor));
            gc.fillRect(px, py, T, T);
            return;
        }
        gc.drawImage(tileImage(kind, col, row), px, py, T, T);
        if (kind.overlaySprite() != null) {
            drawNamed(kind.overlaySprite(), 0, px, py);
        }
    }

    @Override
    public void drawSprite(int col, int row, String spriteKey) {
        int px = (col - camCol) * T;
        int py = (row - camRow) * T;
        String name = Sprites.nameForKey(spriteKey);
        if (name == null) { // unknown key: bright square so a missing sprite is obvious
            gc.setFill(Color.web(Palette.CORAL));
            gc.fillRect(px + 2, py + 2, T - 4, T - 4);
            return;
        }
        int frames = sprites.get(name).frames().size();
        int index = "player".equals(name) ? playerWalk.frameFor(col, row, frame, frames) : 0;
        gc.setFill(Color.web(Palette.SHADOW, 0.30)); // soft shadow under the feet
        gc.fillOval(px + 3, py + 12, 10, 4);
        drawNamed(name, index, px, py);
    }

    @Override
    public void drawLabel(int col, int row, String text) {
        int px = (col - camCol) * T;
        int py = (row - camRow) * T;
        String marker = Sprites.nameForLabel(text);
        if (marker != null) { // "!" and "?" are little speech bubbles that float above the character
            int bob = (frame / MARKER_BOB_TICKS) % 2;
            drawNamed(marker, 0, px, py - MARKER_RISE + bob);
            return;
        }
        gc.setFill(Color.web(Palette.INK));
        gc.fillText(text, px + 6, py);
        gc.setFill(Color.web(Palette.PAPER));
        gc.fillText(text, px + 5, py - 1);
    }

    @Override
    public void fillRect(int px, int py, int width, int height, String hexColor) {
        gc.setFill(Color.web(hexColor));
        gc.fillRect(px, py, width, height);
    }

    @Override
    public void drawText(int px, int py, String text, String hexColor) {
        gc.setFill(Color.web(hexColor));
        gc.fillText(text, px, py);
    }

    /**
     * Pixel-art panel: a soft two-step drop shadow, a dark outline with notched corners, a 1-pixel ring in
     * the border colour, then the fill. The shadow sits only outside the panel, so see-through (glass)
     * panels are not darkened. The inside starts 2 pixels in, exactly where the plain version's did.
     */
    @Override
    public void drawPanel(int px, int py, int width, int height, String fillHex, String borderHex) {
        if (width < 6 || height < 6) {
            Renderer.super.drawPanel(px, py, width, height, fillHex, borderHex);
            return;
        }
        shadow(px, py, width, height, 3, 0.10);
        shadow(px, py, width, height, 2, 0.16);

        gc.setFill(Color.web(Palette.INK)); // outline; the four corner pixels are left out
        gc.fillRect(px + 1, py, width - 2, 1);
        gc.fillRect(px + 1, py + height - 1, width - 2, 1);
        gc.fillRect(px, py + 1, 1, height - 2);
        gc.fillRect(px + width - 1, py + 1, 1, height - 2);

        gc.setFill(Color.web(borderHex)); // inner ring
        gc.fillRect(px + 1, py + 1, width - 2, 1);
        gc.fillRect(px + 1, py + height - 2, width - 2, 1);
        gc.fillRect(px + 1, py + 2, 1, height - 4);
        gc.fillRect(px + width - 2, py + 2, 1, height - 4);

        gc.setFill(Color.web(fillHex));
        gc.fillRect(px + 2, py + 2, width - 4, height - 4);
    }

    private void shadow(int px, int py, int width, int height, int offset, double opacity) {
        gc.setFill(Color.web(Palette.SHADOW, opacity));
        gc.fillRect(px + width, py + offset, offset, height); // right edge
        gc.fillRect(px + offset, py + height, width - offset, offset); // bottom edge
    }

    // ---- pictures ----

    private void drawNamed(String name, int frameIndex, int px, int py) {
        gc.drawImage(spriteImage(name, frameIndex), px, py, T, T);
    }

    private Image spriteImage(String name, int frameIndex) {
        String key = name + "#" + frameIndex;
        Image image = spriteImages.get(key);
        if (image == null) {
            java.util.List<Bitmap> frames = sprites.get(name).frames();
            image = toImage(frames.get(Math.min(frameIndex, frames.size() - 1)));
            spriteImages.put(key, image);
        }
        return image;
    }

    private Image tileImage(TileKind kind, int col, int row) {
        int water = kind == TileKind.WATER ? TileTexture.waterFrame(frame) : 0; // only water animates
        long key = ((long) kind.ordinal() << 40) | ((long) water << 36) | ((long) (col & 0xFFFF) << 16) | (row & 0xFFFF);
        Image image = tileImages.get(key);
        if (image == null) {
            image = toImage(PixelImages.fromGrid(TileTexture.generate(kind, col, row, water)));
            tileImages.put(key, image);
        }
        return image;
    }

    private Image toImage(Bitmap bitmap) {
        Bitmap big = PixelImages.scale(bitmap, scale);
        WritableImage image = new WritableImage(big.width(), big.height());
        image.getPixelWriter().setPixels(0, 0, big.width(), big.height(),
                PixelFormat.getIntArgbInstance(), big.argb(), 0, big.width());
        return image;
    }

    /** Reads an optional artist PNG from the classpath; null if it is missing or unreadable. */
    private static Bitmap loadPng(String path) {
        try (InputStream in = JavaFxRenderer.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            Image image = new Image(in);
            int w = (int) image.getWidth();
            int h = (int) image.getHeight();
            if (image.isError() || w <= 0 || h <= 0 || image.getPixelReader() == null) {
                return null;
            }
            int[] pixels = new int[w * h];
            image.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w);
            return new Bitmap(w, h, pixels);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
