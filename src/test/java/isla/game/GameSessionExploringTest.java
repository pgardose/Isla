package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.RecordingRenderer;
import org.junit.jupiter.api.Test;

class GameSessionExploringTest {

    @Test
    void startsExploring() {
        assertEquals(Screen.EXPLORING, TestSessions.fresh().getScreen());
    }

    @Test
    void directionAction_movesPlayerOneTile() {
        GameSession s = TestSessions.fresh();
        s.handle(Action.RIGHT);
        assertEquals(SeedData.SPAWN_X + 1, s.getPlayer().getX());
        s.handle(Action.LEFT);
        s.handle(Action.LEFT);
        assertEquals(SeedData.SPAWN_X - 1, s.getPlayer().getX());
        assertEquals(SeedData.SPAWN_Y, s.getPlayer().getY());
    }

    @Test
    void blockedMove_setsNoMessage() {
        GameSession s = TestSessions.at(SeedData.SPAWN_X, 3); // beach row; north of it is water
        s.handle(Action.UP);
        s.handle(Action.UP);
        s.handle(Action.UP);
        s.handle(Action.UP);
        assertEquals("", s.getMessage());
    }

    @Test
    void walkingWithNoStaminaLeft_kills() {
        GameSession s = TestSessions.fresh();
        s.getPlayer().expendStamina(100);
        s.handle(Action.RIGHT);
        assertEquals(SeedData.SPAWN_X, s.getPlayer().getX());
        assertEquals(Screen.GAME_OVER, s.getScreen());
        assertTrue(s.isDead());
    }

    @Test
    void theLastStepThatDrainsStamina_kills() {
        GameSession s = TestSessions.fresh();
        s.getPlayer().expendStamina(99);
        for (int i = 0; i < isla.Player.STEPS_PER_STAMINA && !s.isDead(); i++) {
            s.handle(i % 2 == 0 ? Action.RIGHT : Action.LEFT);
        }
        assertTrue(s.isDead());
        assertTrue(s.getDeathCause().contains("exhaustion"), s.getDeathCause());
        s.handle(Action.RIGHT);
        s.handle(Action.REST);
        assertEquals(Screen.GAME_OVER, s.getScreen(), "the dead do not walk or rest");
    }

    @Test
    void lowStamina_warnsOnce_andTurnsTheBarRed() {
        GameSession s = TestSessions.fresh();
        s.getPlayer().expendStamina(100 - GameSession.LOW_STAMINA - 1); // one point above the danger zone
        for (int i = 0; i < isla.Player.STEPS_PER_STAMINA; i++) {
            s.handle(i % 2 == 0 ? Action.RIGHT : Action.LEFT);
        }
        assertTrue(s.getMessage().contains("faint"), s.getMessage());
        RecordingRenderer r = new RecordingRenderer();
        s.render(r);
        assertTrue(r.calls.stream().anyMatch(c -> c.contains(GameSession.DANGER)), "red stamina bar");
        s.handle(Action.RIGHT);
        assertFalse(s.getMessage().contains("faint"), "warned only once");
    }

    @Test
    void gameOverScreen_saysYouDied() {
        GameSession s = TestSessions.fresh();
        s.getPlayer().expendStamina(100);
        s.handle(Action.RIGHT);
        RecordingRenderer r = new RecordingRenderer();
        s.render(r);
        assertTrue(r.anyTextContains("YOU DIED"), r.texts.toString());
        assertTrue(r.anyTextContains("last save"), r.texts.toString());
    }

    @Test
    void render_drawsWorldAndHudWithMoneyAndStamina() {
        RecordingRenderer r = new RecordingRenderer();
        TestSessions.fresh().render(r);
        assertTrue(r.count("tile ") > 0);
        assertTrue(r.anyTextContains("$50"), r.texts.toString());
        assertTrue(r.anyTextContains("100/100"), r.texts.toString());
    }

    @Test
    void render_showsMessageWhenSet() {
        GameSession s = TestSessions.fresh();
        s.notice("A message");
        RecordingRenderer r = new RecordingRenderer();
        s.render(r);
        assertTrue(r.anyTextContains("A message"), r.texts.toString());
    }
}
