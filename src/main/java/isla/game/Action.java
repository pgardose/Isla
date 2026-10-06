package isla.game;

/** Everything the player can do, independent of which key or button triggers it. */
public enum Action {
    UP, DOWN, LEFT, RIGHT,
    CONFIRM, CANCEL,
    INVENTORY, REST, SAVE;

    public boolean isDirection() {
        return this == UP || this == DOWN || this == LEFT || this == RIGHT;
    }
}
