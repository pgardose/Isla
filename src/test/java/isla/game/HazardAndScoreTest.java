package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.DebugQuest;
import isla.Player;
import isla.RecordingRenderer;
import isla.dao.Repositories;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Glitch static next to broken cursed tiles, and the score/rank at the end. */
class HazardAndScoreTest {

    private Repositories repos;
    private GameSession s;

    @BeforeEach
    void setUp() {
        repos = Repositories.inMemory();
        s = GameBootstrap.start(repos, true, new Random(3));
    }

    // ---- hazards ----

    @Test
    void steppingNextToTheBrokenBridge_stings() {
        s.getPlayer().setPosition(38, 13);
        s.handle(Action.RIGHT); // (39,13) touches the glitch at (40,13)
        // one step is too short for the normal drain (1 per 4 steps), so all of this is the static
        assertEquals(SeedData.MAX_STAMINA - GameSession.GLITCH_STATIC_STAMINA, s.getPlayer().getStamina());
        assertTrue(s.getMessage().contains("Glitch static"), s.getMessage());
    }

    @Test
    void walkingAwayFromTheGlitch_stopsTheSting() {
        s.getPlayer().setPosition(39, 13);
        s.handle(Action.LEFT); // (38,13): no glitch next to it
        assertEquals(SeedData.MAX_STAMINA, s.getPlayer().getStamina());
        assertEquals("", s.getMessage());
    }

    @Test
    void onceTheStoneIsFixed_theBridgeNoLongerStings() {
        s.getPlayer().unlockArea(SeedData.EAST_BRIDGE);
        s.getPlayer().setPosition(38, 13);
        s.handle(Action.RIGHT);
        assertEquals(SeedData.MAX_STAMINA, s.getPlayer().getStamina());
    }

    @Test
    void theRedVines_stingToo() {
        s.getPlayer().unlockArea(SeedData.SOUTH_BRIDGE);
        s.getPlayer().setPosition(48, 41);
        s.handle(Action.DOWN); // (48,42) touches the overgrowth at (49,42)
        assertTrue(s.getMessage().contains("Glitch static"), s.getMessage());
    }

    @Test
    void theStatic_canKill() {
        s.getPlayer().setPosition(38, 13);
        s.getPlayer().expendStamina(SeedData.MAX_STAMINA - GameSession.GLITCH_STATIC_STAMINA);
        s.handle(Action.RIGHT);
        assertTrue(s.isDead());
        assertTrue(s.getDeathCause().contains("glitch static"), s.getDeathCause());
        assertEquals(1, s.getPlayer().getDeaths());
    }

    // ---- score rules ----

    @Test
    void aFlawlessRun_isRankS() {
        Score perfect = new Score(0, 0);
        assertEquals(1000, perfect.points());
        assertEquals("S", perfect.rank());
        assertEquals("Master Debugger", perfect.title());
    }

    @Test
    void mistakesAndDeaths_lowerTheRank() {
        assertEquals("A", new Score(1, 0).rank());   // 960
        assertEquals("A", new Score(0, 1).rank());   // 850
        assertEquals("B", new Score(5, 1).rank());   // 650
        assertEquals("C", new Score(5, 2).rank());   // 500
        assertEquals("D", new Score(10, 3).rank());  // 150
        assertEquals(0, new Score(50, 50).points(), "never below zero");
        assertThrows(IllegalArgumentException.class, () -> new Score(-1, 0));
    }

    @Test
    void wrongAnswers_areCounted() {
        s.getPlayer().unlockArea(SeedData.JUNGLE_PATH);
        s.getPlayer().setPosition(28, 6);
        s.handle(Action.CONFIRM);
        DebugQuest p = s.getActivePuzzle();
        while (s.getPuzzleCursor() == p.getCorrectIndex()) {
            s.handle(Action.DOWN);
        }
        s.handle(Action.CONFIRM);
        s.handle(Action.CONFIRM);
        assertEquals(2, s.getPlayer().getWrongAnswers());
        assertEquals("A", s.getScore().rank());
    }

    @Test
    void theJournal_showsTheRankSoFar() {
        s.getPlayer().recordWrongAnswer();
        s.handle(Action.QUESTS);
        RecordingRenderer r = new RecordingRenderer();
        s.render(r);
        assertTrue(r.anyTextContains("Wrong answers: 1"), String.valueOf(r.texts));
        assertTrue(r.anyTextContains("Rank so far: A"));
    }

    @Test
    void stats_surviveSaveAndContinue() {
        s.getPlayer().recordWrongAnswer();
        s.getPlayer().recordWrongAnswer();
        s.getPlayer().recordDeath();
        s.handle(Action.SAVE);
        Player loaded = GameBootstrap.start(repos, false).getPlayer();
        assertEquals(2, loaded.getWrongAnswers());
        assertEquals(1, loaded.getDeaths());
        assertEquals(0, GameBootstrap.start(repos, true).getPlayer().getWrongAnswers(), "a new game starts clean");
    }

    @Test
    void settings_rememberTheBestScore(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("settings.properties");
        Settings settings = Settings.load(file);
        assertEquals(-1, settings.getBestScore());
        assertTrue(settings.offerScore(850));
        assertFalse(settings.offerScore(600), "a worse score is not a best");
        settings.save(file);
        assertEquals(850, Settings.load(file).getBestScore());
        Files.writeString(file, "bestScore=oops\n");
        assertEquals(-1, Settings.load(file).getBestScore(), "a broken value falls back to none");
    }
}
