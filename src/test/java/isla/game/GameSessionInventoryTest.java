package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.Consumable;
import isla.ItemCategory;
import isla.RecordingRenderer;
import isla.Souvenir;
import isla.Tool;
import org.junit.jupiter.api.Test;

class GameSessionInventoryTest {

    private final Consumable water = new Consumable(1, "Coconut Water", 10, 20);
    private final Tool machete = new Tool(4, "Rusty Machete", 30, "Jungle Path");
    private final Souvenir shell = new Souvenir(5, "Seashell Necklace", 15);

    private GameSession withItems() {
        GameSession s = TestSessions.fresh();
        s.getPlayer().getInventory().addItem(water);
        s.getPlayer().getInventory().addItem(machete);
        s.getPlayer().getInventory().addItem(shell);
        return s;
    }

    @Test
    void inventoryKey_opensAndClosesInventory() {
        GameSession s = withItems();
        s.handle(Action.INVENTORY);
        assertEquals(Screen.INVENTORY, s.getScreen());
        s.handle(Action.INVENTORY);
        assertEquals(Screen.EXPLORING, s.getScreen());
        s.handle(Action.INVENTORY);
        s.handle(Action.CANCEL);
        assertEquals(Screen.EXPLORING, s.getScreen());
    }

    @Test
    void tabsFilterByCategory_andWrap() {
        GameSession s = withItems();
        s.handle(Action.INVENTORY);
        assertEquals(ItemCategory.CONSUMABLE, s.getInventoryTab());
        assertEquals(java.util.List.of(water), s.inventoryEntries());
        s.handle(Action.RIGHT);
        assertEquals(ItemCategory.TOOL, s.getInventoryTab());
        assertEquals(java.util.List.of(machete), s.inventoryEntries());
        s.handle(Action.RIGHT);
        assertEquals(java.util.List.of(shell), s.inventoryEntries());
        s.handle(Action.RIGHT);
        assertEquals(ItemCategory.CONSUMABLE, s.getInventoryTab());
        s.handle(Action.LEFT);
        assertEquals(ItemCategory.SOUVENIR, s.getInventoryTab());
    }

    @Test
    void useConsumable_restoresStaminaAndRemovesItem() {
        GameSession s = withItems();
        s.getPlayer().expendStamina(50);
        s.handle(Action.INVENTORY);
        s.handle(Action.CONFIRM);
        assertEquals(70, s.getPlayer().getStamina());
        assertEquals(0, s.getPlayer().getInventory().countOf(water));
        assertTrue(s.getMessage().contains("restores"), s.getMessage());
        assertEquals(0, s.getInventoryCursor());
        s.handle(Action.CONFIRM); // tab is now empty: nothing happens
        assertEquals("", s.getMessage());
    }

    @Test
    void useConsumableAtFullStamina_keepsItem() {
        GameSession s = withItems();
        s.handle(Action.INVENTORY);
        s.handle(Action.CONFIRM);
        assertEquals(1, s.getPlayer().getInventory().countOf(water));
        assertEquals("Stamina is already full.", s.getMessage());
    }

    @Test
    void useTool_unlocksAreaAndKeepsItem() {
        GameSession s = withItems();
        s.handle(Action.INVENTORY);
        s.handle(Action.RIGHT);
        s.handle(Action.CONFIRM);
        assertTrue(s.getPlayer().hasUnlocked("Jungle Path"));
        assertEquals(1, s.getPlayer().getInventory().countOf(machete));
    }

    @Test
    void useSouvenir_keepsItem() {
        GameSession s = withItems();
        s.handle(Action.INVENTORY);
        s.handle(Action.LEFT);
        s.handle(Action.CONFIRM);
        assertEquals(1, s.getPlayer().getInventory().countOf(shell));
        assertFalse(s.getMessage().isEmpty());
    }

    @Test
    void emptyTab_confirmDoesNothing_cursorKeysSafe() {
        GameSession s = TestSessions.fresh();
        s.handle(Action.INVENTORY);
        s.handle(Action.CONFIRM);
        s.handle(Action.UP);
        s.handle(Action.DOWN);
        assertEquals("", s.getMessage());
        assertEquals(0, s.getInventoryCursor());
    }

    @Test
    void duplicateItemsAreGroupedWithCounts() {
        GameSession s = TestSessions.fresh();
        s.getPlayer().getInventory().addItem(shell);
        s.getPlayer().getInventory().addItem(shell);
        s.handle(Action.INVENTORY);
        s.handle(Action.LEFT);
        assertEquals(1, s.inventoryEntries().size());
        RecordingRenderer r = new RecordingRenderer();
        s.render(r);
        assertTrue(r.anyTextContains("x2"), r.texts.toString());
    }

    @Test
    void rest_nextToAHut_costsMoney_andRestoresStaminaFully() {
        GameSession s = TestSessions.at(21, 8); // next to the hut above Elder Kai
        s.getPlayer().expendStamina(60);
        s.handle(Action.REST);
        assertEquals(100, s.getPlayer().getStamina());
        assertEquals(SeedData.NEW_GAME_MONEY - GameSession.HUT_NAP_PRICE, s.getPlayer().getMoney());
        assertTrue(s.getMessage().contains("hut"), s.getMessage());
        s.handle(Action.REST);
        assertEquals("You are already well rested.", s.getMessage());
        assertEquals(SeedData.NEW_GAME_MONEY - GameSession.HUT_NAP_PRICE, s.getPlayer().getMoney(), "no charge");
    }

    @Test
    void rest_atAHut_withoutMoney_isRefused() {
        GameSession s = TestSessions.at(21, 8);
        s.getPlayer().spend(s.getPlayer().getMoney());
        s.getPlayer().expendStamina(60);
        s.handle(Action.REST);
        assertEquals(40, s.getPlayer().getStamina());
        assertTrue(s.getMessage().contains("can't pay"), s.getMessage());
    }

    @Test
    void pressingRestOverAndOver_inTheOpen_neverGivesStaminaBack() {
        GameSession s = TestSessions.fresh();
        s.getPlayer().expendStamina(90);
        for (int i = 0; i < 50; i++) {
            s.handle(Action.REST);
        }
        assertEquals(10, s.getPlayer().getStamina());
    }

    @Test
    void rest_outInTheOpen_isNotPossible() {
        GameSession s = TestSessions.fresh(); // spawn: open grass, no hut
        s.getPlayer().expendStamina(50);
        s.handle(Action.REST);
        assertEquals(50, s.getPlayer().getStamina(), "no free naps in the open");
        assertTrue(s.getMessage().contains("No shade"), s.getMessage());
    }

    @Test
    void rest_whenNotTired_saysSo() {
        GameSession s = TestSessions.fresh();
        s.handle(Action.REST);
        assertEquals("You are not tired.", s.getMessage());
    }

    @Test
    void eating_whenFaint_keepsYouAlive() {
        GameSession s = TestSessions.fresh();
        s.getPlayer().getInventory().addItem(SeedData.items().get(0)); // Coconut Water, +20
        s.getPlayer().expendStamina(99);
        s.handle(Action.INVENTORY);
        s.handle(Action.CONFIRM);
        s.handle(Action.CANCEL);
        assertEquals(21, s.getPlayer().getStamina());
        for (int i = 0; i < 40; i++) {
            s.handle(i % 2 == 0 ? Action.RIGHT : Action.LEFT);
        }
        assertFalse(s.isDead());
    }

    @Test
    void inventoryScreen_rendersTabsAndItems() {
        GameSession s = withItems();
        s.handle(Action.INVENTORY);
        RecordingRenderer r = new RecordingRenderer();
        s.render(r);
        assertTrue(r.anyTextContains("Consumables"), r.texts.toString());
        assertTrue(r.anyTextContains("Tools"), r.texts.toString());
        assertTrue(r.anyTextContains("Souvenirs"), r.texts.toString());
        assertTrue(r.anyTextContains("Coconut Water"), r.texts.toString());
    }

    @Test
    void playThrough_buyMachete_useIt_enterJungle() {
        GameSession s = TestSessions.at(28, 13); // below Tomas
        s.handle(Action.CONFIRM);
        s.handle(Action.CONFIRM); // machete is first in stock, $30
        assertEquals(20, s.getPlayer().getMoney());
        s.handle(Action.CANCEL);
        s.getPlayer().setPosition(25, 8); // grass next to the jungle
        s.handle(Action.RIGHT);
        assertEquals(25, s.getPlayer().getX()); // locked
        s.handle(Action.INVENTORY);
        s.handle(Action.RIGHT);
        s.handle(Action.CONFIRM);
        s.handle(Action.CANCEL);
        s.handle(Action.RIGHT);
        assertEquals(26, s.getPlayer().getX()); // unlocked
    }
}
