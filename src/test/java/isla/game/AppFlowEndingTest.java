package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.Item;
import isla.RecordingRenderer;
import isla.dao.Repositories;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The ending, the quest list and How to Play as seen through AppFlow (what the window really talks to). */
class AppFlowEndingTest {

    private Repositories repos;
    private AppFlow f;

    @BeforeEach
    void setUp() {
        repos = Repositories.inMemory();
        f = new AppFlow(repos, Settings.load(Path.of("no-such-settings-file.properties")), null);
        f.startNewGame();
    }

    private Item item(String name) {
        return SeedData.items().stream().filter(i -> i.getName().equals(name)).findFirst().orElseThrow();
    }

    /** Plays Elder Kai's two quests and leaves the game on the ending screen. */
    private void reachTheEnding() {
        GameSession s = f.getSession();
        s.getPlayer().setPosition(20, 10);
        s.getPlayer().getInventory().addItem(item("Coconut"));
        f.handle(Action.CONFIRM);
        f.handle(Action.CONFIRM);
        s.getPlayer().getInventory().addItem(item("Golden Idol"));
        f.handle(Action.CONFIRM);
        assertEquals(Screen.ENDING, s.getScreen());
    }

    @Test
    void endingScreen_x_returnsToTheMainMenu_andSavesTheFinishedGame() {
        reachTheEnding();
        f.handle(Action.CANCEL);
        assertEquals(AppFlow.Mode.MENU, f.getMode());
        assertTrue(f.getMessage().contains("saved"), f.getMessage());
        assertTrue(repos.quests().findById(3).orElseThrow().isCompleted());
    }

    @Test
    void endingScreen_z_keepsExploring_inTheSameGame() {
        reachTheEnding();
        f.handle(Action.CONFIRM);
        assertEquals(AppFlow.Mode.PLAYING, f.getMode());
        assertEquals(Screen.EXPLORING, f.getSession().getScreen());
    }

    @Test
    void endingScreen_isDrawnThroughTheFlow() {
        reachTheEnding();
        RecordingRenderer r = new RecordingRenderer();
        f.render(r);
        assertTrue(r.anyTextContains("THE END"), String.valueOf(r.texts));
    }

    @Test
    void continueAfterTheEnding_putsThePlayerBackOnTheIsland_withoutTheEnding() {
        reachTheEnding();
        f.handle(Action.CANCEL); // back to the menu, saved
        AppFlow again = new AppFlow(repos, Settings.load(Path.of("no-such-settings-file.properties")), null);
        again.handle(Action.CONFIRM); // Continue is selected when a save exists
        assertTrue(again.isPlaying());
        assertEquals(Screen.EXPLORING, again.getSession().getScreen());
    }

    @Test
    void questList_opensWithQ_andXClosesItWithoutLeavingTheIsland() {
        f.handle(Action.QUESTS);
        assertEquals(Screen.QUESTS, f.getSession().getScreen());
        f.handle(Action.CANCEL);
        assertEquals(AppFlow.Mode.PLAYING, f.getMode());
        assertEquals(Screen.EXPLORING, f.getSession().getScreen());
    }

    @Test
    void howToPlay_explainsTheGoalAndTheQuestKey() {
        AppFlow menu = new AppFlow(repos, Settings.load(Path.of("no-such-settings-file.properties")), null);
        while (menu.menuItems().get(menu.getMenuCursor()) != AppFlow.MenuItem.HOW_TO_PLAY) {
            menu.handle(Action.DOWN);
        }
        menu.handle(Action.CONFIRM);
        RecordingRenderer r = new RecordingRenderer();
        menu.render(r);
        String all = String.join(" | ", r.texts);
        assertTrue(all.contains("Elder Kai"), all);
        assertTrue(all.contains("coconut"), all);
        assertTrue(all.contains("Golden Idol"), all);
        assertTrue(all.contains("Q ..."), all);
    }
}
