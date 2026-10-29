package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.DebugQuest;
import isla.Item;
import isla.NPC;
import isla.Player;
import isla.QuestGiver;
import isla.Vendor;
import isla.dao.Repositories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;

/** Save/continue behaviour. JdbcBootstrapTest runs the same tests on MySQL. */
class GameBootstrapTest {

    protected Repositories repos;

    /** Fresh, empty storage for each test. */
    protected Repositories newRepos() {
        return Repositories.inMemory();
    }

    @BeforeEach
    void setUp() {
        repos = newRepos();
    }

    private Item item(String name) {
        return SeedData.items().stream().filter(i -> i.getName().equals(name)).findFirst().orElseThrow();
    }

    private Vendor mara(GameSession s) {
        return TestSessions.vendor(s, "Mara");
    }

    private QuestGiver elder(GameSession s) {
        return s.getWorld().getNpcs().stream().filter(n -> n instanceof QuestGiver)
                .map(n -> (QuestGiver) n).findFirst().orElseThrow();
    }

    private QuestGiver nia(GameSession s) {
        return s.getWorld().getNpcs().stream()
                .filter(n -> n instanceof QuestGiver g && g.getQuest() instanceof DebugQuest)
                .map(n -> (QuestGiver) n).findFirst().orElseThrow();
    }

    @Test
    void noSave_startsNewGame_withSeedStateAndSpawn() {
        GameSession s = GameBootstrap.start(repos, false);
        Player p = s.getPlayer();
        assertEquals(SeedData.NEW_GAME_MONEY, p.getMoney());
        assertEquals(SeedData.MAX_STAMINA, p.getStamina());
        assertEquals(SeedData.SPAWN_X, p.getX());
        assertEquals(SeedData.SPAWN_Y, p.getY());
        assertEquals(0, p.getInventory().size());
        assertTrue(p.getUnlockedAreas().isEmpty());
        assertEquals(14, s.getWorld().getNpcs().size());
        assertFalse(nia(s).getQuest().isCompleted());
        assertFalse(elder(s).getQuest().isCompleted());
        assertEquals(5, mara(s).getShop().quantityOf(item("Coconut Water")));
    }

    @Test
    void saveThenStart_restoresMoneyPositionInventoryAreasStockAndQuest() {
        GameSession s = GameBootstrap.start(repos, false);
        // buy a Coconut Water and a Coconut from Mara
        s.getPlayer().setPosition(12, 13);
        s.handle(Action.CONFIRM);
        s.handle(Action.CONFIRM);
        s.handle(Action.DOWN);
        s.handle(Action.CONFIRM);
        s.handle(Action.CANCEL);
        // hear Elder Kai out (he pays $20 for supplies)
        s.getPlayer().setPosition(20, 10);
        s.handle(Action.CONFIRM);
        s.handle(Action.CANCEL);
        s.getPlayer().unlockArea("Jungle Path");
        s.getPlayer().setPosition(21, 13);

        s.handle(Action.SAVE);
        assertTrue(s.getMessage().startsWith("Game saved"), s.getMessage());

        GameSession loaded = GameBootstrap.start(repos, false);
        Player p = loaded.getPlayer();
        assertEquals(50 - 10 - 8 + 20, p.getMoney());
        assertEquals(21, p.getX());
        assertEquals(13, p.getY());
        assertEquals(1, p.getInventory().countOf(item("Coconut Water")));
        assertEquals(1, p.getInventory().countOf(item("Coconut")));
        assertTrue(p.hasUnlocked("Jungle Path"));
        assertEquals(4, mara(loaded).getShop().quantityOf(item("Coconut Water")));
        assertEquals(2, mara(loaded).getShop().quantityOf(item("Coconut")));
        assertTrue(elder(loaded).getQuest().isCompleted());
    }

    @Test
    void saveWithEmptyInventory_thenStart_works() {
        GameSession s = GameBootstrap.start(repos, false);
        s.handle(Action.SAVE);
        GameSession loaded = GameBootstrap.start(repos, false);
        assertEquals(0, loaded.getPlayer().getInventory().size());
        assertTrue(loaded.getPlayer().getUnlockedAreas().isEmpty());
    }

    @Test
    void newGameFlag_ignoresExistingSave() {
        GameSession s = GameBootstrap.start(repos, false);
        s.getPlayer().spend(30);
        s.getPlayer().setPosition(5, 13);
        TestSessions.vendor(s, "Mara").getShop().buyItem(s.getPlayer(), item("Coconut Water"));
        s.handle(Action.SAVE);

        GameSession fresh = GameBootstrap.start(repos, true);
        assertEquals(SeedData.NEW_GAME_MONEY, fresh.getPlayer().getMoney());
        assertEquals(SeedData.SPAWN_X, fresh.getPlayer().getX());
        assertEquals(5, mara(fresh).getShop().quantityOf(item("Coconut Water")));
        assertFalse(elder(fresh).getQuest().isCompleted());
    }

    @Test
    void saveAction_withoutHandler_saysUnavailable_andFailureIsReported() {
        GameSession plain = TestSessions.fresh();
        plain.handle(Action.SAVE);
        assertTrue(plain.getMessage().contains("not available"), plain.getMessage());

        plain.setSaveHandler(() -> {
            throw new isla.dao.DataAccessException("boom", null);
        });
        plain.handle(Action.SAVE);
        assertTrue(plain.getMessage().startsWith("Save failed"), plain.getMessage());
    }

    @Test
    void inMemoryRepositories_reportNotPersistent() {
        assertFalse(Repositories.inMemory().isPersistent());
        NPC any = GameBootstrap.start(repos, false).getWorld().getNpcs().get(0);
        assertNotNull(any);
    }

    @Test
    void solvedPuzzle_survivesSaveAndContinue_andPaysOnlyOnce() {
        GameSession s = GameBootstrap.start(repos, false);
        s.getPlayer().setPosition(24, 13);
        s.handle(Action.CONFIRM);
        assertEquals(Screen.DEBUG_PUZZLE, s.getScreen());
        while (s.getPuzzleCursor() != s.getActivePuzzle().getCorrectIndex()) {
            s.handle(Action.DOWN);
        }
        s.handle(Action.CONFIRM);
        assertEquals(90, s.getPlayer().getMoney());
        s.handle(Action.CONFIRM);
        s.handle(Action.SAVE);

        GameSession loaded = GameBootstrap.start(repos, false);
        assertTrue(nia(loaded).getQuest().isCompleted());
        assertEquals(90, loaded.getPlayer().getMoney());
        loaded.handle(Action.CONFIRM);
        assertEquals(Screen.DIALOGUE, loaded.getScreen());
        assertEquals(90, loaded.getPlayer().getMoney());
    }

    @Test
    void unsolvedPuzzle_isStillOpenAfterContinue() {
        GameSession s = GameBootstrap.start(repos, false);
        s.handle(Action.SAVE);
        GameSession loaded = GameBootstrap.start(repos, false);
        assertFalse(nia(loaded).getQuest().isCompleted());
    }

    @Test
    void aSaveFromTheOldCoconutStory_cannotBeContinued_andStartsFresh() {
        // Storage seeded by the game's first version: a player, the old quests, no story quests.
        for (Item item : SeedData.items()) repos.items().save(item);
        for (Vendor v : SeedData.vendors()) repos.vendors().save(v);
        repos.quests().save(Fixtures.elderQuest());
        Player old = new Player(SeedData.PLAYER_ID, "Old", 999, 100, 6);
        old.setPosition(30, 7);
        repos.players().save(old);
        assertFalse(GameBootstrap.hasSave(repos), "Continue is greyed out for an old-version save");

        GameSession s = GameBootstrap.start(repos, false);
        assertEquals(14, s.getWorld().getNpcs().size());
        assertEquals(SeedData.NEW_GAME_MONEY, s.getPlayer().getMoney());
        assertTrue(repos.quests().findById(SeedData.KAI_INTRO_ID).isPresent());
    }

    @Test
    void aSaveMissingAQuestAddedLater_gainsItOnContinue() {
        GameSession s = GameBootstrap.start(repos, true);
        s.handle(Action.SAVE);
        repos.quests().delete(SeedData.PIP_ID);
        GameSession loaded = GameBootstrap.start(repos, false);
        assertTrue(repos.quests().findById(SeedData.PIP_ID).isPresent());
        assertEquals(14, loaded.getWorld().getNpcs().size());
    }

    @Test
    void newGameFlag_reopensSolvedPuzzle() {
        GameSession s = GameBootstrap.start(repos, false);
        nia(s).getQuest().markCompleted();
        s.handle(Action.SAVE);
        GameSession fresh = GameBootstrap.start(repos, true);
        assertFalse(nia(fresh).getQuest().isCompleted());
    }

    @Test
    void hasSave_falseOnEmptyStorage_trueOnceSaved() {
        assertFalse(GameBootstrap.hasSave(repos));
        GameSession s = GameBootstrap.start(repos, false);
        assertFalse(GameBootstrap.hasSave(repos)); // starting is not saving
        s.handle(Action.SAVE);
        assertTrue(GameBootstrap.hasSave(repos));
    }

    @Test
    void erase_removesSave_andNextStartIsAFreshGame() {
        GameSession s = GameBootstrap.start(repos, false);
        s.getPlayer().earn(100);
        nia(s).getQuest().markCompleted();
        s.handle(Action.SAVE);
        assertTrue(GameBootstrap.hasSave(repos));

        GameBootstrap.erase(repos);

        assertFalse(GameBootstrap.hasSave(repos));
        GameSession fresh = GameBootstrap.start(repos, false);
        assertEquals(50, fresh.getPlayer().getMoney());
        assertFalse(nia(fresh).getQuest().isCompleted());
    }

    @Test
    void erase_onEmptyStorage_isHarmless() {
        GameBootstrap.erase(repos);
        assertFalse(GameBootstrap.hasSave(repos));
        assertEquals(50, GameBootstrap.start(repos, false).getPlayer().getMoney());
    }

    // ---- different debug question per game ----

    private String snippetOf(GameSession s) {
        return ((DebugQuest) nia(s).getQuest()).getSnippet();
    }

    @Test
    void firstEverStart_drawsNiasPuzzleFromTheBank() {
        GameSession s = GameBootstrap.start(repos, false, new Random(5));
        assertTrue(DebugQuestBank.all().stream().anyMatch(p -> p.snippet().equals(snippetOf(s))));
    }

    @Test
    void everyNewGame_getsADifferentPuzzleThanTheOneBefore_evenWithTheSameLuck() {
        String previous = snippetOf(GameBootstrap.start(repos, true, new Random(7)));
        for (int i = 0; i < 25; i++) {
            String next = snippetOf(GameBootstrap.start(repos, true, new Random(7)));
            assertNotEquals(previous, next, "game " + i);
            previous = next;
        }
    }

    @Test
    void continuing_keepsTheSamePuzzleAndAnswerOrder() {
        GameSession s = GameBootstrap.start(repos, true, new Random(11));
        DebugQuest first = (DebugQuest) nia(s).getQuest();
        s.handle(Action.SAVE);

        GameSession loaded = GameBootstrap.start(repos, false, new Random(999));
        DebugQuest again = (DebugQuest) nia(loaded).getQuest();
        assertEquals(first.getSnippet(), again.getSnippet());
        assertEquals(first.getOptions(), again.getOptions());
        assertEquals(first.getCorrectIndex(), again.getCorrectIndex());
    }

    @Test
    void differentPlayers_withTheirOwnStorage_canGetDifferentPuzzles() {
        Set<String> seen = new HashSet<>();
        for (long seed = 0; seed < 20; seed++) {
            seen.add(snippetOf(GameBootstrap.start(isla.dao.Repositories.inMemory(), false, new Random(seed))));
        }
        assertTrue(seen.size() > 3, "only " + seen.size() + " different puzzles across 20 players");
    }

    @Test
    void aSaveFromBeforeTheBank_keepsItsOwnPuzzle() {
        // The original single puzzle is still in the bank and is left alone on continue.
        for (Item item : SeedData.items()) repos.items().save(item);
        for (Vendor v : SeedData.vendors()) repos.vendors().save(v);
        GameBootstrap.storyQuests().forEach(repos.quests()::save);
        repos.quests().save(SeedData.debugQuest());
        GameSession s = GameBootstrap.start(repos, false, new Random(1));
        assertEquals(SeedData.debugQuest().getSnippet(), snippetOf(s));
    }

    @Test
    void niaTalksGenerically_soAnyPuzzleFits() {
        GameSession s = GameBootstrap.start(repos, true, new Random(2));
        s.getPlayer().setPosition(24, 13);
        s.handle(Action.CONFIRM);
        assertTrue(s.getDialogueText().contains("practice bug"), s.getDialogueText());
        assertFalse(s.getDialogueText().contains("stamina"), s.getDialogueText());
    }
}
