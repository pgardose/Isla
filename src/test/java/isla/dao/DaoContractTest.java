package isla.dao;

import static org.junit.jupiter.api.Assertions.*;

import isla.Item;
import isla.Player;
import isla.Quest;
import isla.Tool;
import isla.Vendor;
import isla.game.SeedData;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Behaviour every DAO implementation must have. InMemoryDaoTest and
 * JdbcDaoTest extend this, so the two implementations are held to the same
 * rules.
 */
abstract class DaoContractTest {

    protected ItemDAO items;
    protected PlayerDAO players;
    protected VendorDAO vendors;
    protected QuestDAO quests;

    /** Creates empty DAOs; called before every test. */
    protected abstract void createDaos();

    @BeforeEach
    void setUp() {
        createDaos();
        for (Item item : SeedData.items()) {
            items.save(item);
        }
    }

    private Item item(String name) {
        return items.findAll().stream().filter(i -> i.getName().equals(name)).findFirst().orElseThrow();
    }

    private Player richPlayer() {
        Player p = new Player(1, "Pj", 50, SeedData.MAX_STAMINA, SeedData.INVENTORY_CAPACITY);
        p.restoreState(33, 77, 4, 9);
        p.getInventory().addItem(item("Coconut"));
        p.getInventory().addItem(item("Coconut"));
        p.getInventory().addItem(item("Rusty Machete"));
        p.unlockArea("Jungle Path");
        return p;
    }

    // ---- items ----

    @Test
    void items_roundTrip_withSubclassData() {
        assertEquals(SeedData.items().size(), items.findAll().size());
        Item machete = items.findById(4).orElseThrow();
        assertEquals("Jungle Path", ((Tool) machete).getUnlocksArea());
        assertEquals(new Tool(4, "Rusty Machete", 30, "Jungle Path"), machete);
        assertEquals(20, ((isla.Consumable) items.findById(1).orElseThrow()).getStaminaRestore());
        assertEquals(isla.ItemCategory.SOUVENIR, items.findById(7).orElseThrow().getCategory());
    }

    @Test
    void items_findMissing_returnsEmpty() {
        assertEquals(Optional.empty(), items.findById(999));
    }

    // ---- player ----

    @Test
    void player_saveThenFind_roundTrips() {
        players.save(richPlayer());
        Player loaded = players.findById(1).orElseThrow();
        assertEquals("Pj", loaded.getName());
        assertEquals(33, loaded.getMoney());
        assertEquals(77, loaded.getStamina());
        assertEquals(4, loaded.getX());
        assertEquals(9, loaded.getY());
        assertEquals(2, loaded.getInventory().countOf(item("Coconut")));
        assertEquals(1, loaded.getInventory().countOf(item("Rusty Machete")));
        assertEquals(3, loaded.getInventory().size());
        assertTrue(loaded.hasUnlocked("Jungle Path"));
        assertEquals(SeedData.INVENTORY_CAPACITY, loaded.getInventory().getCapacity());
    }

    @Test
    void player_emptyInventoryAndNoAreas_roundTrip() {
        Player p = new Player(1, "Pj", 50, SeedData.MAX_STAMINA, SeedData.INVENTORY_CAPACITY);
        players.save(p);
        Player loaded = players.findById(1).orElseThrow();
        assertEquals(0, loaded.getInventory().size());
        assertTrue(loaded.getUnlockedAreas().isEmpty());
        assertEquals(100, loaded.getStamina());
    }

    @Test
    void player_findMissing_returnsEmpty() {
        assertTrue(players.findById(1).isEmpty());
    }

    @Test
    void player_saveTwice_updatesAndRemovesStaleRows() {
        players.save(richPlayer());
        Player p = players.findById(1).orElseThrow();
        p.getInventory().removeItem(item("Coconut"));
        p.getInventory().removeItem(item("Coconut"));
        p.getInventory().removeItem(item("Rusty Machete"));
        p.earn(10);
        players.save(p);
        Player again = players.findById(1).orElseThrow();
        assertEquals(43, again.getMoney());
        assertEquals(0, again.getInventory().size());
        assertEquals(1, players.findAll().size());
    }

    @Test
    void player_delete_removes() {
        players.save(richPlayer());
        players.delete(1);
        assertTrue(players.findById(1).isEmpty());
    }

    @Test
    void player_loadedCopyIsIndependent() {
        players.save(richPlayer());
        Player loaded = players.findById(1).orElseThrow();
        loaded.earn(1000);
        assertEquals(33, players.findById(1).orElseThrow().getMoney());
    }

    // ---- vendors ----

    @Test
    void vendor_stockQuantitiesAndFieldsRoundTrip() {
        Vendor mara = SeedData.vendors().get(0);
        Player buyer = new Player(1, "Pj", 50, 100, 6);
        mara.getShop().buyItem(buyer, item("Coconut Water")); // 5 -> 4
        vendors.save(mara);

        Vendor loaded = vendors.findById(1).orElseThrow();
        assertEquals("Mara", loaded.getName());
        assertEquals(12, loaded.getX());
        assertEquals(12, loaded.getY());
        assertEquals("vendor_sprite", loaded.getSprite());
        assertEquals(mara.getDialogue(), loaded.getDialogue());
        assertEquals(4, loaded.getShop().quantityOf(item("Coconut Water")));
        assertEquals(3, loaded.getShop().quantityOf(item("Coconut")));
        assertEquals(3, loaded.getShop().getStock().size());
    }

    @Test
    void vendor_soldOutItemDisappearsFromStock_andFindAllReturnsAll() {
        for (Vendor v : SeedData.vendors()) {
            vendors.save(v);
        }
        assertEquals(3, vendors.findAll().size());

        Vendor tomas = vendors.findById(2).orElseThrow();
        Player rich = new Player(1, "Pj", 1000, 100, 6);
        tomas.getShop().buyItem(rich, item("Rusty Machete"));
        vendors.save(tomas);
        Vendor again = vendors.findById(2).orElseThrow();
        assertEquals(0, again.getShop().quantityOf(item("Rusty Machete")));
        assertEquals(2, again.getShop().getStock().size());
    }

    @Test
    void vendor_findMissing_returnsEmpty() {
        assertTrue(vendors.findById(42).isEmpty());
    }

    // ---- quests ----

    @Test
    void quest_completionAndFieldsRoundTrip() {
        Quest q = SeedData.elderQuest();
        quests.save(q);
        Quest loaded = quests.findById(1).orElseThrow();
        assertFalse(loaded.isCompleted());
        assertEquals("Coconut", loaded.getRequiredItemName());
        assertEquals(25, loaded.getReward());
        assertEquals(1, loaded.getGiverNpcId());

        q.markCompleted();
        quests.save(q);
        assertTrue(quests.findById(1).orElseThrow().isCompleted());
    }

    @Test
    void quest_withoutRequiredItem_roundTripsNull() {
        quests.save(new Quest(5, 2, "Say hello", 3, null));
        assertNull(quests.findById(5).orElseThrow().getRequiredItemName());
        assertEquals(List.of(5), quests.findAll().stream().map(Quest::getId).toList());
    }
}
