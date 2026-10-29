package isla.game;

import isla.Renderer;
import isla.dao.Repositories;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * The layer above GameSession: decides whether the player is looking at the
 * main menu, the settings, a yes/no question, or the island itself, and passes
 * keys and drawing to whichever is active. Like GameSession it never imports
 * JavaFX, so the whole flow (menu, new game, continue, erase save) is unit tested.
 */
public class AppFlow {

    public enum Mode { MENU, HELP, SETTINGS, CONFIRM_NEW, CONFIRM_ERASE, PLAYING }

    public enum MenuItem {
        CONTINUE("Continue"), NEW_GAME("New Game"), HOW_TO_PLAY("How to Play"), SETTINGS("Settings"), QUIT("Quit");

        private final String label;

        MenuItem(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static final List<MenuItem> MENU_ITEMS = List.of(MenuItem.values());
    private static final int SETTINGS_ENTRIES = 3; // autosave, erase, back
    private static final int SETTINGS_AUTOSAVE = 0;
    private static final int SETTINGS_ERASE = 1;
    private static final String GLASS_ALPHA = "E0"; // 88% opaque: readable text, scene still visible
    private static final int NO = 0;
    private static final int YES = 1;

    private final Repositories repos;
    private final Settings settings;
    private final Path settingsFile;

    private Mode mode = Mode.MENU;
    private GameSession session;
    private int menuCursor;
    private int settingsCursor;
    private int confirmCursor;
    private String message = "";
    private String notice = "";
    private boolean quitRequested;

    /** settingsFile may be null, in which case settings are not written to disk. */
    public AppFlow(Repositories repos, Settings settings, Path settingsFile) {
        this.repos = repos;
        this.settings = settings;
        this.settingsFile = settingsFile;
        enterMenu();
    }

    // ---- state for the UI and tests ----

    public Mode getMode() {
        return mode;
    }

    public boolean isPlaying() {
        return mode == Mode.PLAYING;
    }

    /** The running game; null unless the mode is PLAYING. */
    public GameSession getSession() {
        return session;
    }

    public Settings getSettings() {
        return settings;
    }

    public List<MenuItem> menuItems() {
        return MENU_ITEMS;
    }

    public int getMenuCursor() {
        return menuCursor;
    }

    public int getSettingsCursor() {
        return settingsCursor;
    }

    /** 0 = No, 1 = Yes. */
    public int getConfirmCursor() {
        return confirmCursor;
    }

    public String getMessage() {
        return message;
    }

    public boolean isContinueAvailable() {
        return GameBootstrap.hasSave(repos);
    }

    public boolean isQuitRequested() {
        return quitRequested;
    }

    /** A line shown on the menu and on first entering the game (e.g. "No database"). */
    public void setNotice(String text) {
        this.notice = text == null ? "" : text;
    }

    /** Starts a brand-new game right away, discarding any save (the --new flag). */
    public void startNewGame() {
        startPlaying(true);
    }

    /** Called when the window closes: keeps progress if autosave is on. */
    public void onWindowClose() {
        if (mode == Mode.PLAYING && settings.isAutosave() && !session.isDead()) {
            try {
                GameBootstrap.save(repos, session);
            } catch (RuntimeException e) {
                System.err.println("Autosave failed: " + e.getMessage());
            }
        }
    }

    // ---- input ----

    public void handle(Action action) {
        message = "";
        switch (mode) {
            case MENU -> handleMenu(action);
            case HELP -> handleHelp(action);
            case SETTINGS -> handleSettings(action);
            case CONFIRM_NEW -> handleConfirm(action, this::confirmNewGame, Mode.MENU);
            case CONFIRM_ERASE -> handleConfirm(action, this::confirmErase, Mode.SETTINGS);
            case PLAYING -> handlePlaying(action);
        }
    }

    private void handleMenu(Action action) {
        switch (action) {
            case UP -> menuCursor = (menuCursor - 1 + MENU_ITEMS.size()) % MENU_ITEMS.size();
            case DOWN -> menuCursor = (menuCursor + 1) % MENU_ITEMS.size();
            case CONFIRM -> activate(MENU_ITEMS.get(menuCursor));
            default -> { }
        }
    }

    private void activate(MenuItem item) {
        switch (item) {
            case CONTINUE -> {
                if (isContinueAvailable()) {
                    startPlaying(false);
                } else {
                    message = "No saved game yet. Choose New Game.";
                }
            }
            case NEW_GAME -> {
                if (isContinueAvailable()) {
                    confirmCursor = NO; // the safe answer is the default
                    mode = Mode.CONFIRM_NEW;
                } else {
                    startPlaying(true);
                }
            }
            case HOW_TO_PLAY -> mode = Mode.HELP;
            case SETTINGS -> {
                settingsCursor = 0;
                mode = Mode.SETTINGS;
            }
            case QUIT -> quitRequested = true;
        }
    }

    private void handleHelp(Action action) {
        if (action == Action.CONFIRM || action == Action.CANCEL) {
            mode = Mode.MENU; // the cursor stays on "How to Play"
        }
    }

    private void handleSettings(Action action) {
        switch (action) {
            case UP -> settingsCursor = (settingsCursor - 1 + SETTINGS_ENTRIES) % SETTINGS_ENTRIES;
            case DOWN -> settingsCursor = (settingsCursor + 1) % SETTINGS_ENTRIES;
            case CANCEL -> enterMenu();
            case CONFIRM -> {
                switch (settingsCursor) {
                    case SETTINGS_AUTOSAVE -> toggleAutosave();
                    case SETTINGS_ERASE -> askToErase();
                    default -> enterMenu();
                }
            }
            default -> { }
        }
    }

    private void toggleAutosave() {
        settings.setAutosave(!settings.isAutosave());
        if (settingsFile != null) {
            try {
                settings.save(settingsFile);
            } catch (IOException e) {
                message = "Could not save settings (the change lasts until you quit).";
            }
        }
    }

    private void askToErase() {
        if (!isContinueAvailable()) {
            message = "Nothing to erase.";
            return;
        }
        confirmCursor = NO;
        mode = Mode.CONFIRM_ERASE;
    }

    /** Shared by both yes/no questions. onYes runs only when Yes is confirmed. */
    private void handleConfirm(Action action, Runnable onYes, Mode backTo) {
        switch (action) {
            case UP, DOWN -> confirmCursor = confirmCursor == NO ? YES : NO;
            case CANCEL -> returnTo(backTo);
            case CONFIRM -> {
                if (confirmCursor == YES) {
                    onYes.run();
                } else {
                    returnTo(backTo);
                }
            }
            default -> { }
        }
    }

    private void returnTo(Mode target) {
        if (target == Mode.MENU) {
            enterMenu();
        } else {
            mode = target;
        }
    }

    private void confirmNewGame() {
        startPlaying(true);
    }

    private void confirmErase() {
        GameBootstrap.erase(repos);
        mode = Mode.SETTINGS;
        message = "Saved game erased.";
    }

    private void handlePlaying(Action action) {
        if (session.isDead()) {
            if (action == Action.CONFIRM) {
                wakeUpAfterDeath();
            } else if (action == Action.CANCEL) {
                session = null; // never save a dead game
                enterMenu();
                message = "You died. Your last save is kept.";
            }
            return;
        }
        // Cancel only leaves when nothing is open (or on the ending screen, whose "main menu" choice is Cancel);
        // inside a shop, dialogue or the quest list it closes that first.
        boolean canLeave = session.getScreen() == Screen.EXPLORING || session.getScreen() == Screen.ENDING;
        if (action == Action.CANCEL && canLeave) {
            leaveIsland();
        } else {
            session.handle(action);
        }
    }

    private void leaveIsland() {
        String result;
        if (settings.isAutosave()) {
            try {
                GameBootstrap.save(repos, session);
            } catch (RuntimeException e) {
                session.notice("Could not save: " + e.getMessage()); // stay, so nothing is lost
                return;
            }
            result = "Progress saved.";
        } else {
            result = "Progress not saved (autosave is off).";
        }
        session = null;
        enterMenu();
        message = result;
    }

    /** Back to the last save (or a new game if there is none), with full stamina so you cannot die again at once. */
    private void wakeUpAfterDeath() {
        boolean saved = GameBootstrap.hasSave(repos);
        startPlaying(!saved);
        session.getPlayer().restoreStamina(session.getPlayer().getMaxStamina());
        session.notice(saved ? "You wake up where you last saved, fully rested."
                : "You had not saved yet, so your story starts over.");
    }

    private void startPlaying(boolean newGame) {
        session = GameBootstrap.start(repos, newGame);
        session.setCheckpoints(settings.isAutosave());
        if (!notice.isEmpty()) {
            session.notice(notice);
        }
        mode = Mode.PLAYING;
    }

    private void enterMenu() {
        mode = Mode.MENU;
        menuCursor = isContinueAvailable() ? MenuItem.CONTINUE.ordinal() : MenuItem.NEW_GAME.ordinal();
    }

    // ---- drawing ----

    public void render(Renderer r) {
        render(r, 0);
    }

    /**
     * Draws the current screen. frame is a counter that goes up by one per
     * displayed frame; menu backgrounds use it to animate, the game itself ignores it.
     */
    public void render(Renderer r, long frame) {
        if (mode == Mode.PLAYING) {
            session.render(r);
            return;
        }
        drawBackdrop(r, frame);
        switch (mode) {
            case MENU -> renderMenu(r);
            case HELP -> renderHelp(r);
            case SETTINGS -> renderSettings(r);
            case CONFIRM_NEW -> renderConfirm(r, "Overwrite your saved game?");
            case CONFIRM_ERASE -> renderConfirm(r, "Erase your saved game?");
            default -> { }
        }
        String line = message.isEmpty() ? notice : message;
        if (!line.isEmpty()) {
            r.drawPanel(6, GameSession.SCREEN_HEIGHT - 30, GameSession.SCREEN_WIDTH - 12, 24,
                    GameSession.PAPER, GameSession.BORDER);
            r.drawText(14, GameSession.SCREEN_HEIGHT - 14, line, GameSession.INK);
        }
    }

    private void drawBackdrop(Renderer r, long frame) {
        TitleScene.draw(r, frame);
    }

    private void panel(Renderer r, int x, int y, int w, int h) {
        r.drawPanel(x, y, w, h, GameSession.PAPER, GameSession.BORDER);
    }



    /** Like panel(), but the paper is slightly see-through ("#RRGGBBAA") so the title scene glows behind it. */
    private void glassPanel(Renderer r, int x, int y, int w, int h) {
        r.fillRect(x, y, w, h, GameSession.BORDER);
        r.fillRect(x + 2, y + 2, w - 4, h - 4, GameSession.PAPER + GLASS_ALPHA);
    }

    private static String cursorLabel(boolean selected, String text) {
        return (selected ? "> " : "  ") + text;
    }

    private void renderMenu(Renderer r) {
        TitleScene.drawLogo(r);

        glassPanel(r, 140, 88, 200, 122);
        boolean canContinue = isContinueAvailable();
        for (int i = 0; i < MENU_ITEMS.size(); i++) {
            MenuItem item = MENU_ITEMS.get(i);
            String label = item.label();
            boolean unavailable = item == MenuItem.CONTINUE && !canContinue;
            if (unavailable) {
                label += " (no saved game)";
            }
            r.drawText(156, 118 + i * 20, cursorLabel(i == menuCursor, label),
                    unavailable ? GameSession.BORDER : GameSession.INK);
        }
        glassPanel(r, 140, 214, 200, 20);
        r.drawText(150, 228, "Up/Down: choose    Z: select", GameSession.INK);
    }

    private void renderHelp(Renderer r) {
        panel(r, 30, 14, 420, 214);
        String[] lines = {
            "HOW TO PLAY - The Broken Source",
            "Goal: help Elder Kai mend Isla's broken Golden Idol.",
            "Fix cursed code stones, find 3 shards, unmask the culprit.",
            "",
            "Arrow keys/WASD ... walk     Z/Enter ... talk, confirm",
            "X/Esc ... cancel, back       I ... Inventory (use items)",
            "Q ... journal: goal, shards, clues     F5 ... save",
            "R ... Rest at a hut ($10). At 0 stamina you die!",
            "",
            "! = shop   ? = someone (or a stone) needs you.",
            "A wrong code fix shocks you: -10 stamina. Think first!",
            "Story steps save automatically (with autosave on).",
        };
        for (int i = 0; i < lines.length; i++) {
            r.drawText(46, 38 + i * 15, lines[i], GameSession.INK);
        }
        r.drawText(46, 218, "Z or X: back", GameSession.INK);
    }

    private void renderSettings(Renderer r) {
        panel(r, 100, 40, 280, 150);
        r.drawText(120, 66, "SETTINGS", GameSession.INK);
        String autosave = "Autosave on close: " + (settings.isAutosave() ? "On" : "Off");
        String[] entries = {autosave, "Erase saved game", "Back"};
        for (int i = 0; i < entries.length; i++) {
            r.drawText(120, 100 + i * 24, cursorLabel(i == settingsCursor, entries[i]), GameSession.INK);
        }
        r.drawText(120, 178, "Z: change / select    X: back", GameSession.INK);
    }

    private void renderConfirm(Renderer r, String question) {
        panel(r, 110, 70, 260, 110);
        r.drawText(130, 100, question, GameSession.INK);
        r.drawText(130, 130, cursorLabel(confirmCursor == NO, "No"), GameSession.INK);
        r.drawText(130, 152, cursorLabel(confirmCursor == YES, "Yes"), GameSession.INK);
    }
}
