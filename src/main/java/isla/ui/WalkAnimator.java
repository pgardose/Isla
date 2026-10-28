package isla.ui;

/**
 * Decides which frame of a walking sprite to show. The game never says "the player is walking";
 * the renderer just notices that the sprite changed tile recently. Pure logic, no JavaFX.
 */
final class WalkAnimator {

    /** After the last step the sprite keeps bobbing for this many frames, then stands still. */
    static final int WALK_HOLD_TICKS = 12;
    /** Frames between one bob step and the next. */
    static final int STEP_TICKS = 6;

    private boolean seen;
    private int lastCol;
    private int lastRow;
    private long lastMoveFrame = Long.MIN_VALUE / 2;

    /** The frame index (0 frameCount-1) to draw at animation frame {@code frame}. */
    int frameFor(int col, int row, int frame, int frameCount) {
        if (seen && (col != lastCol || row != lastRow)) {
            lastMoveFrame = frame;
        }
        seen = true;
        lastCol = col;
        lastRow = row;
        if (frameCount <= 1 || (long) frame - lastMoveFrame > WALK_HOLD_TICKS) {
            return 0;
        }
        return Math.floorMod(Math.floorDiv(frame, STEP_TICKS), frameCount);
    }
}
