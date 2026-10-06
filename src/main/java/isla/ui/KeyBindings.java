package isla.ui;

import isla.game.Action;
import javafx.scene.input.KeyCode;

/** Maps keyboard keys to game Actions. The only place that knows about keys. */
final class KeyBindings {

    private KeyBindings() {
    }

    /** Returns the Action for a key, or null if the key is not bound. */
    static Action actionFor(KeyCode code) {
        switch (code) {
            case UP: case W: return Action.UP;
            case DOWN: case S: return Action.DOWN;
            case LEFT: case A: return Action.LEFT;
            case RIGHT: case D: return Action.RIGHT;
            case Z: case ENTER: case SPACE: return Action.CONFIRM;
            case X: case ESCAPE: case BACK_SPACE: return Action.CANCEL;
            case I: return Action.INVENTORY;
            case R: return Action.REST;
            case F5: return Action.SAVE;
            default: return null;
        }
    }
}
