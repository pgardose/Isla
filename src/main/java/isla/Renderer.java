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
 * Colors are "#RRGGBB" strings so no toolkit color type leaks in.
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
}
