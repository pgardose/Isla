package isla.ui;

import isla.Renderer;
import isla.game.GameSession;
import java.util.Map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Draws through a JavaFX GraphicsContext. Sprites are colored placeholder
 * squares chosen by sprite key; swap this class's drawSprite for real image
 * drawing when art exists. Nothing outside isla.ui knows about JavaFX.
 */
final class JavaFxRenderer implements Renderer {

    private static final int T = GameSession.TILE_SIZE;
    private static final Map<String, String> SPRITE_COLORS = Map.of(
            "player_sprite", "#E74C3C",
            "vendor_sprite", "#F39C12",
            "trader_sprite", "#8E44AD",
            "elder_sprite", "#ECF0F1");

    private final GraphicsContext gc;
    private int camCol;
    private int camRow;

    JavaFxRenderer(GraphicsContext gc) {
        this.gc = gc;
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 9));
    }

    @Override
    public void setCamera(int col, int row) {
        this.camCol = col;
        this.camRow = row;
    }

    @Override
    public void drawTile(int col, int row, String hexColor) {
        gc.setFill(Color.web(hexColor));
        gc.fillRect((col - camCol) * T, (row - camRow) * T, T, T);
    }

    @Override
    public void drawSprite(int col, int row, String spriteKey) {
        int px = (col - camCol) * T;
        int py = (row - camRow) * T;
        gc.setFill(Color.web("#2B2B2B"));
        gc.fillRect(px + 1, py + 1, T - 2, T - 2);
        gc.setFill(Color.web(SPRITE_COLORS.getOrDefault(spriteKey, "#FF00FF")));
        gc.fillRect(px + 2, py + 2, T - 4, T - 4);
    }

    @Override
    public void drawLabel(int col, int row, String text) {
        gc.setFill(Color.WHITE);
        gc.fillText(text, (col - camCol) * T + 5, (row - camRow) * T - 1);
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
}
