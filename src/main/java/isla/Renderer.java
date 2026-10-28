package isla;

/**
 * Drawing surface used by the model and game logic. The model never imports
 * a GUI toolkit: it only talks to this interface, and the JavaFX layer
 * (isla.ui.JavaFxRenderer) implements it.
 *
 * OOP note (ABSTRACTION): callers say WHAT to draw (a tile, a sprite, some
 * text) without knowing HOW it is drawn.
 *
 * Tile methods use world tile coordinates and are shifted by the camera set
 * through setCamera. fillRect/drawText use screen pixels (UI overlays).
 * Colors are "#RRGGBB" strings (or "#RRGGBBAA" when see-through) so no toolkit
 * color type leaks in.
 */
public interface Renderer {

    /** Sets the world tile shown at the top-left corner of the screen. */
    void setCamera(int col, int row);

    void drawTile(int col, int row, String hexColor);

    /** Draws a placeholder sprite; spriteKey is e.g. "player_sprite". */
    void drawSprite(int col, int row, String spriteKey);

    /** Small text anchored above a world tile (e.g. a "!" marker). */
    void drawLabel(int col, int row, String text);

    void fillRect(int px, int py, int width, int height, String hexColor);

    void drawText(int px, int py, String text, String hexColor);

    /**
     * Tells the renderer how many frames have been shown so far, so it can animate (water,
     * walking). The game logic ignores this; only a renderer that animates needs it.
     */
    default void setFrame(int frame) {
    }

    /**
     * A bordered panel in screen pixels: a 2-pixel border in borderHex around a fillHex middle
     * (fillHex may be "#RRGGBBAA" for a see-through panel). A renderer may dress it up (pixel
     * border, drop shadow); this plain version is two rectangles, exactly as before.
     */
    default void drawPanel(int px, int py, int width, int height, String fillHex, String borderHex) {
        fillRect(px, py, width, height, borderHex);
        fillRect(px + 2, py + 2, width - 4, height - 4, fillHex);
    }
}
