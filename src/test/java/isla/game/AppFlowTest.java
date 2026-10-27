package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.RecordingRenderer;
import isla.dao.Repositories;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppFlowTest {

    @TempDir
    Path dir;

    private Repositories repos;
    private Path settingsFile;

    @BeforeEach
    void setUp() {
        repos = Repositories.inMemory();
        settingsFile = dir.resolve("settings.properties");
    }

    private AppFlow flow() {
        return new AppFlow(repos, Settings.load(settingsFile), settingsFile);
    }

    /** Puts a saved game with the given extra money into storage. */
    private void createSave(int extraMoney) {
        GameSession s = GameBootstrap.start(repos, true);
        s.getPlayer().earn(extraMoney);
        GameBootstrap.save(repos, s);
    }

    private void select(AppFlow f, AppFlow.MenuItem item) {
        while (f.menuItems().get(f.getMenuCursor()) != item) {
            f.handle(Action.DOWN);
        }
    }

    private void choose(AppFlow f, AppFlow.MenuItem item) {
        select(f, item);
        f.handle(Action.CONFIRM);
    }

    private void openSettingsEntry(AppFlow f, int index) {
        while (f.getSettingsCursor() != index) {
            f.handle(Action.DOWN);
        }
        f.handle(Action.CONFIRM);
    }

    // ---- main menu ----

    @Test
    void startsInMenu_withNewGameSelected_whenThereIsNoSave() {
        AppFlow f = flow();
        assertEquals(AppFlow.Mode.MENU, f.getMode());
        assertNull(f.getSession());
        assertFalse(f.isContinueAvailable());
        assertEquals(AppFlow.MenuItem.NEW_GAME, f.menuItems().get(f.getMenuCursor()));
    }

    @Test
    void startsWithContinueSelected_whenASaveExists() {
        createSave(0);
        AppFlow f = flow();
        assertTrue(f.isContinueAvailable());
        assertEquals(AppFlow.MenuItem.CONTINUE, f.menuItems().get(f.getMenuCursor()));
    }

    @Test
    void menuHasFourEntriesInOrder() {
        assertEquals(java.util.List.of(AppFlow.MenuItem.CONTINUE, AppFlow.MenuItem.NEW_GAME,
                AppFlow.MenuItem.SETTINGS, AppFlow.MenuItem.QUIT), flow().menuItems());
    }

    @Test
    void cursorWrapsBothWays() {
        AppFlow f = flow();
        int n = f.menuItems().size();
        int start = f.getMenuCursor();
        f.handle(Action.UP);
        assertEquals((start - 1 + n) % n, f.getMenuCursor());
        f.handle(Action.DOWN);
        f.handle(Action.DOWN);
        assertEquals((start + 1) % n, f.getMenuCursor());
    }

    @Test
    void continueWithoutSave_doesNothing_andSaysWhy() {
        AppFlow f = flow();
        choose(f, AppFlow.MenuItem.CONTINUE);
        assertEquals(AppFlow.Mode.MENU, f.getMode());
        assertTrue(f.getMessage().contains("No saved game"), f.getMessage());
    }

    @Test
    void continue_loadsTheSavedProgress() {
        createSave(25);
        AppFlow f = flow();
        choose(f, AppFlow.MenuItem.CONTINUE);
        assertEquals(AppFlow.Mode.PLAYING, f.getMode());
        assertEquals(75, f.getSession().getPlayer().getMoney());
    }

    @Test
    void newGame_withoutSave_startsPlayingImmediately() {
        AppFlow f = flow();
        choose(f, AppFlow.MenuItem.NEW_GAME);
        assertEquals(AppFlow.Mode.PLAYING, f.getMode());
        assertEquals(50, f.getSession().getPlayer().getMoney());
    }

    @Test
    void newGame_overASave_asksFirst_andDefaultsToNo() {
        createSave(25);
        AppFlow f = flow();
        choose(f, AppFlow.MenuItem.NEW_GAME);
        assertEquals(AppFlow.Mode.CONFIRM_NEW, f.getMode());
        assertEquals(0, f.getConfirmCursor()); // 0 = No
        f.handle(Action.CONFIRM);              // pressing confirm on the default
        assertEquals(AppFlow.Mode.MENU, f.getMode());
        assertTrue(GameBootstrap.hasSave(repos));
        choose(f, AppFlow.MenuItem.CONTINUE);
        assertEquals(75, f.getSession().getPlayer().getMoney()); // save untouched
    }

    @Test
    void newGame_cancelAtTheQuestion_keepsTheSave() {
        createSave(25);
        AppFlow f = flow();
        choose(f, AppFlow.MenuItem.NEW_GAME);
        f.handle(Action.CANCEL);
        assertEquals(AppFlow.Mode.MENU, f.getMode());
        assertTrue(GameBootstrap.hasSave(repos));
    }

    @Test
    void newGame_confirmedYes_startsOver() {
        createSave(25);
        AppFlow f = flow();
        choose(f, AppFlow.MenuItem.NEW_GAME);
        f.handle(Action.DOWN); // Yes
        assertEquals(1, f.getConfirmCursor());
        f.handle(Action.CONFIRM);
        assertEquals(AppFlow.Mode.PLAYING, f.getMode());
        assertEquals(50, f.getSession().getPlayer().getMoney());
    }

    @Test
    void quit_requestsExit() {
        AppFlow f = flow();
        assertFalse(f.isQuitRequested());
        choose(f, AppFlow.MenuItem.QUIT);
        assertTrue(f.isQuitRequested());
    }

    @Test
    void cancelInTheMenu_doesNothing() {
        AppFlow f = flow();
        f.handle(Action.CANCEL);
        assertEquals(AppFlow.Mode.MENU, f.getMode());
        assertFalse(f.isQuitRequested());
    }

    @Test
    void startNewGame_forTheNewFlag_skipsTheMenuAndDiscardsTheSave() {
        createSave(25);
        AppFlow f = flow();
        f.startNewGame();
        assertEquals(AppFlow.Mode.PLAYING, f.getMode());
        assertEquals(50, f.getSession().getPlayer().getMoney());
    }

    // ---- settings ----

    @Test
    void settings_opensAndGoesBackWithCancel() {
        AppFlow f = flow();
        choose(f, AppFlow.MenuItem.SETTINGS);
        assertEquals(AppFlow.Mode.SETTINGS, f.getMode());
        f.handle(Action.CANCEL);
        assertEquals(AppFlow.Mode.MENU, f.getMode());
    }

    @Test
    void settings_backEntryReturnsToMenu() {
        AppFlow f = flow();
        choose(f, AppFlow.MenuItem.SETTINGS);
        openSettingsEntry(f, 2);
        assertEquals(AppFlow.Mode.MENU, f.getMode());
    }

    @Test
    void settings_autosaveToggle_flipsAndIsRememberedOnDisk() {
        AppFlow f = flow();
        choose(f, AppFlow.MenuItem.SETTINGS);
        openSettingsEntry(f, 0);
        assertFalse(f.getSettings().isAutosave());
        assertFalse(Settings.load(settingsFile).isAutosave());
        openSettingsEntry(f, 0);
        assertTrue(Settings.load(settingsFile).isAutosave());
    }

    @Test
    void settings_failingToWriteTheFile_isReported_butTheToggleStillApplies() {
        Path unwritable = dir.resolve("no-such-folder").resolve("settings.properties");
        AppFlow f = new AppFlow(repos, new Settings(), unwritable);
        choose(f, AppFlow.MenuItem.SETTINGS);
        openSettingsEntry(f, 0);
        assertFalse(f.getSettings().isAutosave());
        assertTrue(f.getMessage().contains("Could not save settings"), f.getMessage());
    }

    @Test
    void erase_withNothingSaved_saysSo() {
        AppFlow f = flow();
        choose(f, AppFlow.MenuItem.SETTINGS);
        openSettingsEntry(f, 1);
        assertEquals(AppFlow.Mode.SETTINGS, f.getMode());
        assertTrue(f.getMessage().contains("Nothing to erase"), f.getMessage());
    }

    @Test
    void erase_asksFirst_andNoKeepsTheSave() {
        createSave(25);
        AppFlow f = flow();
        choose(f, AppFlow.MenuItem.SETTINGS);
        openSettingsEntry(f, 1);
        assertEquals(AppFlow.Mode.CONFIRM_ERASE, f.getMode());
        assertEquals(0, f.getConfirmCursor());
        f.handle(Action.CONFIRM); // No
        assertEquals(AppFlow.Mode.SETTINGS, f.getMode());
        assertTrue(GameBootstrap.hasSave(repos));
    }

    @Test
    void erase_confirmedYes_deletesTheSave_andContinueIsGone() {
        createSave(25);
        AppFlow f = flow();
        choose(f, AppFlow.MenuItem.SETTINGS);
        openSettingsEntry(f, 1);
        f.handle(Action.DOWN); // Yes
        f.handle(Action.CONFIRM);
        assertEquals(AppFlow.Mode.SETTINGS, f.getMode());
        assertFalse(GameBootstrap.hasSave(repos));
        assertTrue(f.getMessage().contains("erased"), f.getMessage());
        f.handle(Action.CANCEL);
        assertFalse(f.isContinueAvailable());
        assertEquals(AppFlow.MenuItem.NEW_GAME, f.menuItems().get(f.getMenuCursor()));
    }

    // ---- leaving the island ----

    private AppFlow playing() {
        AppFlow f = flow();
        choose(f, AppFlow.MenuItem.NEW_GAME);
        return f;
    }

    @Test
    void cancelWhileWalking_returnsToTheMenu_andSavesWhenAutosaveIsOn() {
        AppFlow f = playing();
        f.getSession().getPlayer().earn(7);
        f.handle(Action.CANCEL);
        assertEquals(AppFlow.Mode.MENU, f.getMode());
        assertNull(f.getSession());
        assertTrue(GameBootstrap.hasSave(repos));
        assertTrue(f.getMessage().contains("saved"), f.getMessage());
        choose(f, AppFlow.MenuItem.CONTINUE);
        assertEquals(57, f.getSession().getPlayer().getMoney());
    }

    @Test
    void cancelWhileWalking_withAutosaveOff_doesNotSave_andSaysSo() {
        Settings off = new Settings();
        off.setAutosave(false);
        AppFlow f = new AppFlow(repos, off, settingsFile);
        choose(f, AppFlow.MenuItem.NEW_GAME);
        f.handle(Action.CANCEL);
        assertEquals(AppFlow.Mode.MENU, f.getMode());
        assertFalse(GameBootstrap.hasSave(repos));
        assertTrue(f.getMessage().contains("not saved"), f.getMessage());
    }

    @Test
    void cancelInsideADialogue_closesIt_butDoesNotLeaveTheGame() {
        AppFlow f = playing();
        for (int i = 0; i < 3; i++) f.handle(Action.UP); // next to the Elder
        f.handle(Action.CONFIRM);
        assertEquals(Screen.DIALOGUE, f.getSession().getScreen());
        f.handle(Action.CANCEL);
        assertEquals(AppFlow.Mode.PLAYING, f.getMode());
    }

    @Test
    void otherActionsAreForwardedToTheSession() {
        AppFlow f = playing();
        int y = f.getSession().getPlayer().getY();
        f.handle(Action.UP);
        assertEquals(y - 1, f.getSession().getPlayer().getY());
    }

    @Test
    void leaveThenContinue_restoresPosition() {
        AppFlow f = playing();
        f.handle(Action.UP);
        int x = f.getSession().getPlayer().getX();
        int y = f.getSession().getPlayer().getY();
        f.handle(Action.CANCEL);
        choose(f, AppFlow.MenuItem.CONTINUE);
        assertEquals(x, f.getSession().getPlayer().getX());
        assertEquals(y, f.getSession().getPlayer().getY());
    }

    // ---- closing the window ----

    @Test
    void closingWhilePlaying_autosaves() {
        AppFlow f = playing();
        f.getSession().getPlayer().earn(9);
        f.onWindowClose();
        assertTrue(GameBootstrap.hasSave(repos));
        assertEquals(59, GameBootstrap.start(repos, false).getPlayer().getMoney());
    }

    @Test
    void closingWhilePlaying_withAutosaveOff_doesNotSave() {
        Settings off = new Settings();
        off.setAutosave(false);
        AppFlow f = new AppFlow(repos, off, settingsFile);
        choose(f, AppFlow.MenuItem.NEW_GAME);
        f.onWindowClose();
        assertFalse(GameBootstrap.hasSave(repos));
    }

    @Test
    void closingFromTheMenu_savesNothing() {
        AppFlow f = flow();
        f.onWindowClose();
        assertFalse(GameBootstrap.hasSave(repos));
    }

    // ---- notice from startup ----

    @Test
    void startupNotice_isShownOnTheMenu_andPassedToTheSession() {
        AppFlow f = flow();
        f.setNotice("No database - progress will not be saved.");
        RecordingRenderer r = new RecordingRenderer();
        f.render(r);
        assertTrue(r.anyTextContains("No database"), String.valueOf(r.texts));
        choose(f, AppFlow.MenuItem.NEW_GAME);
        assertTrue(f.getSession().getMessage().contains("No database"), f.getSession().getMessage());
    }

    // ---- drawing ----

    @Test
    void menuRender_showsTitleEntriesAndCursor() {
        AppFlow f = flow();
        RecordingRenderer r = new RecordingRenderer();
        f.render(r);
        assertTrue(r.anyTextContains("ISLA"));
        assertTrue(r.anyTextContains("Continue"));
        assertTrue(r.anyTextContains("> New Game"), String.valueOf(r.texts));
        assertTrue(r.anyTextContains("Settings"));
        assertTrue(r.anyTextContains("Quit"));
    }

    @Test
    void menuRender_flagsContinueAsUnavailable_onlyWithoutASave() {
        RecordingRenderer none = new RecordingRenderer();
        flow().render(none);
        assertTrue(none.anyTextContains("no saved game"), String.valueOf(none.texts));

        createSave(0);
        RecordingRenderer some = new RecordingRenderer();
        flow().render(some);
        assertFalse(some.anyTextContains("no saved game"), String.valueOf(some.texts));
    }

    @Test
    void menuRender_showsMessages() {
        AppFlow f = flow();
        choose(f, AppFlow.MenuItem.CONTINUE); // no save
        RecordingRenderer r = new RecordingRenderer();
        f.render(r);
        assertTrue(r.anyTextContains("No saved game"), String.valueOf(r.texts));
    }

    @Test
    void settingsRender_showsAutosaveStateEraseAndBack() {
        AppFlow f = flow();
        choose(f, AppFlow.MenuItem.SETTINGS);
        RecordingRenderer on = new RecordingRenderer();
        f.render(on);
        assertTrue(on.anyTextContains("Autosave on close: On"), String.valueOf(on.texts));
        assertTrue(on.anyTextContains("Erase saved game"));
        assertTrue(on.anyTextContains("Back"));
        openSettingsEntry(f, 0);
        RecordingRenderer off = new RecordingRenderer();
        f.render(off);
        assertTrue(off.anyTextContains("Autosave on close: Off"), String.valueOf(off.texts));
    }

    @Test
    void confirmRender_asksTheRightQuestion() {
        createSave(0);
        AppFlow f = flow();
        choose(f, AppFlow.MenuItem.NEW_GAME);
        RecordingRenderer r = new RecordingRenderer();
        f.render(r);
        assertTrue(r.anyTextContains("Overwrite your saved game?"), String.valueOf(r.texts));
        assertTrue(r.anyTextContains("> No"), String.valueOf(r.texts));
        assertTrue(r.anyTextContains("Yes"));
        f.handle(Action.CANCEL);
        choose(f, AppFlow.MenuItem.SETTINGS);
        openSettingsEntry(f, 1);
        RecordingRenderer erase = new RecordingRenderer();
        f.render(erase);
        assertTrue(erase.anyTextContains("Erase your saved game?"), String.valueOf(erase.texts));
    }

    @Test
    void playingRender_drawsTheIsland() {
        AppFlow f = playing();
        RecordingRenderer r = new RecordingRenderer();
        f.render(r);
        assertTrue(r.count("tile ") > 0);
        assertFalse(r.anyTextContains("ISLA"));
    }

    // ---- a whole visit ----

    @Test
    void playThrough_menuToIslandAndBackAgain() {
        AppFlow f = flow();
        choose(f, AppFlow.MenuItem.NEW_GAME);          // menu -> island
        for (int i = 0; i < 4; i++) f.handle(Action.RIGHT);
        f.handle(Action.CONFIRM);                      // Nia's puzzle
        assertEquals(Screen.DEBUG_PUZZLE, f.getSession().getScreen());
        f.handle(Action.CANCEL);                       // closes the puzzle only
        assertEquals(AppFlow.Mode.PLAYING, f.getMode());
        f.handle(Action.CANCEL);                       // leaves to the menu
        assertEquals(AppFlow.Mode.MENU, f.getMode());
        choose(f, AppFlow.MenuItem.CONTINUE);          // back to the same spot
        assertEquals(24, f.getSession().getPlayer().getX());
        assertEquals(13, f.getSession().getPlayer().getY());
    }
}
