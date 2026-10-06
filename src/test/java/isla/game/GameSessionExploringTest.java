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
    void tiredMove_setsTiredMessageAndDoesNotMove() {
        GameSession s = TestSessions.fresh();
        s.getPlayer().expendStamina(100);
        s.handle(Action.RIGHT);
        assertEquals(SeedData.SPAWN_X, s.getPlayer().getX());
        assertTrue(s.getMessage().toLowerCase().contains("tired"), s.getMessage());
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
        s.getPlayer().expendStamina(100);
        s.handle(Action.RIGHT);
        RecordingRenderer r = new RecordingRenderer();
        s.render(r);
        assertTrue(r.anyTextContains("tired"), r.texts.toString());
    }
}
