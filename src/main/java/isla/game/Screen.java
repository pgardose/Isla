package isla.game;

/** Which screen currently owns input and drawing. */
public enum Screen {
    EXPLORING,
    DIALOGUE,
    SHOP,
    INVENTORY,
    DEBUG_PUZZLE,
    QUESTS,
    ENDING,
    /** The hero died of exhaustion; AppFlow offers the last save or the main menu. */
    GAME_OVER
}
