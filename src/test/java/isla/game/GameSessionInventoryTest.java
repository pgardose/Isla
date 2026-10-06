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
    void rest_restoresStaminaCapped() {
        GameSession s = TestSessions.fresh();
        s.getPlayer().expendStamina(5);
        s.handle(Action.REST);
        assertEquals(100, s.getPlayer().getStamina());
        s.handle(Action.REST);
        assertEquals("You are not tired.", s.getMessage());
        s.getPlayer().expendStamina(50);
        s.handle(Action.REST);
        assertEquals(60, s.getPlayer().getStamina());
        assertEquals("You catch your breath.", s.getMessage());
    }

    @Test
    void rest_letsExhaustedPlayerWalkAgain() {
        GameSession s = TestSessions.fresh();
        s.getPlayer().expendStamina(100);
        s.handle(Action.RIGHT);
        assertEquals(SeedData.SPAWN_X, s.getPlayer().getX());
        s.handle(Action.REST);
        s.handle(Action.RIGHT);
        assertEquals(SeedData.SPAWN_X + 1, s.getPlayer().getX());
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
