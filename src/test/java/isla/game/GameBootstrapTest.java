package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.Item;
import isla.NPC;
import isla.Player;
import isla.QuestGiver;
import isla.Vendor;
import isla.dao.Repositories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        assertEquals(4, s.getWorld().getNpcs().size());
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
        // hand the coconut to the elder
        s.getPlayer().setPosition(20, 10);
        s.handle(Action.CONFIRM);
        s.handle(Action.CONFIRM);
        s.getPlayer().unlockArea("Jungle Path");
        s.getPlayer().setPosition(21, 13);

        s.handle(Action.SAVE);
        assertTrue(s.getMessage().startsWith("Game saved"), s.getMessage());

        GameSession loaded = GameBootstrap.start(repos, false);
        Player p = loaded.getPlayer();
        assertEquals(50 - 10 - 8 + 25, p.getMoney());
        assertEquals(21, p.getX());
        assertEquals(13, p.getY());
        assertEquals(1, p.getInventory().countOf(item("Coconut Water")));
        assertEquals(0, p.getInventory().countOf(item("Coconut")));
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
}
