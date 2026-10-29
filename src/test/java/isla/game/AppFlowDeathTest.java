package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.dao.Repositories;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Dying of exhaustion, as the window sees it: wake at the last save, or go to the menu without saving. */
class AppFlowDeathTest {

    private Repositories repos;
    private AppFlow f;

    @BeforeEach
    void setUp() {
        repos = Repositories.inMemory();
        f = new AppFlow(repos, Settings.load(Path.of("no-such-settings-file.properties")), null);
        f.startNewGame();
    }

    private void die() {
        f.getSession().getPlayer().expendStamina(f.getSession().getPlayer().getStamina());
        f.handle(Action.RIGHT);
        assertTrue(f.getSession().isDead());
    }

    @Test
    void finishingAStoryStep_savesACheckpoint() {
        assertFalse(GameBootstrap.hasSave(repos));
        f.getSession().getPlayer().setPosition(20, 10);
        f.handle(Action.CONFIRM); // Elder Kai's intro completes
        assertTrue(GameBootstrap.hasSave(repos), "checkpoint after the intro");
        assertTrue(repos.quests().findById(SeedData.KAI_INTRO_ID).orElseThrow().isCompleted());
    }

    @Test
    void afterDying_zWakesYouAtTheLastSave_fullyRested() {
        f.getSession().getPlayer().setPosition(20, 10);
        f.handle(Action.CONFIRM); // checkpoint: money $70, at (20,10)
        f.handle(Action.CANCEL);
        f.getSession().getPlayer().setPosition(30, 13);
        die();

        f.handle(Action.CONFIRM);
        GameSession woke = f.getSession();
        assertFalse(woke.isDead());
        assertEquals(Screen.EXPLORING, woke.getScreen());
        assertEquals(70, woke.getPlayer().getMoney());
        assertEquals(20, woke.getPlayer().getX());
        assertEquals(SeedData.MAX_STAMINA, woke.getPlayer().getStamina());
        assertTrue(woke.getMessage().contains("wake up"), woke.getMessage());
    }

    @Test
    void afterDying_withoutAnySave_theStoryStartsOver() {
        die();
        f.handle(Action.CONFIRM);
        assertEquals(SeedData.NEW_GAME_MONEY, f.getSession().getPlayer().getMoney());
        assertTrue(f.getSession().getMessage().contains("starts over"), f.getSession().getMessage());
    }

    @Test
    void afterDying_xGoesToTheMenu_withoutSavingTheDeadGame() {
        die();
        f.handle(Action.CANCEL);
        assertEquals(AppFlow.Mode.MENU, f.getMode());
        assertFalse(GameBootstrap.hasSave(repos), "a dead game is never saved");
    }

    @Test
    void closingTheWindowWhileDead_doesNotSave() {
        die();
        f.onWindowClose();
        assertFalse(GameBootstrap.hasSave(repos));
    }

    @Test
    void aDeath_countsInTheScore_evenThoughTheDeadGameIsNotSaved() {
        f.getSession().getPlayer().setPosition(20, 10);
        f.handle(Action.CONFIRM); // checkpoint
        f.handle(Action.CANCEL);
        f.getSession().getPlayer().recordWrongAnswer(); // a mistake after the last save
        die();
        f.handle(Action.CONFIRM);
        assertEquals(1, f.getSession().getPlayer().getDeaths());
        assertEquals(1, f.getSession().getPlayer().getWrongAnswers());

        die();
        f.handle(Action.CANCEL); // to the menu this time
        assertEquals(2, repos.players().findById(SeedData.PLAYER_ID).orElseThrow().getDeaths(),
                "the second death is written into the save");
    }

    @Test
    void theDeadCannotPressSave() {
        die();
        f.handle(Action.SAVE);
        assertFalse(GameBootstrap.hasSave(repos));
    }
}
