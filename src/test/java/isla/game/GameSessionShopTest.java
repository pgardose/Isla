package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.Item;
import isla.RecordingRenderer;
import isla.Shop;
import isla.Vendor;
import isla.world.GameWorld;
import isla.world.TileMap;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameSessionShopTest {

    /** Standing directly below Mara (vendor id 1 at 12,12). */
    private GameSession atMara() {
        return TestSessions.at(12, 13);
    }

    private GameSession openMaraShop() {
        GameSession s = atMara();
        s.handle(Action.CONFIRM);
        assertEquals(Screen.SHOP, s.getScreen());
        return s;
    }

    @Test
    void confirmNearVendor_opensShopWithGreeting() {
        GameSession s = openMaraShop();
        assertNotNull(s.getActiveShop());
        assertEquals(0, s.getShopCursor());
        assertTrue(s.getDialogueText().contains("Fresh fruit"));
    }

    @Test
    void confirmNearNobody_staysExploring() {
        GameSession s = TestSessions.fresh();
        s.handle(Action.CONFIRM);
        assertEquals(Screen.EXPLORING, s.getScreen());
        assertEquals("", s.getMessage());
    }

    @Test
    void buy_updatesMoneyInventoryAndStock() {
        GameSession s = openMaraShop();
        Item water = s.getActiveShop().getStock().get(0);
        int before = s.getActiveShop().quantityOf(water);
        s.handle(Action.CONFIRM);
        assertEquals(40, s.getPlayer().getMoney());
        assertEquals(1, s.getPlayer().getInventory().countOf(water));
        assertEquals(before - 1, s.getActiveShop().quantityOf(water));
        assertEquals("Bought Coconut Water!", s.getMessage());
    }

    @Test
    void buyWithExactMoney_succeeds() {
        GameSession s = openMaraShop();
        s.getPlayer().spend(40); // leaves exactly $10 = Coconut Water
        s.handle(Action.CONFIRM);
        assertEquals(0, s.getPlayer().getMoney());
        assertEquals(1, s.getPlayer().getInventory().size());
    }

    @Test
    void buyWithoutMoney_message_noChange() {
        GameSession s = openMaraShop();
        s.getPlayer().spend(45);
        s.handle(Action.CONFIRM);
        assertEquals("Not enough money.", s.getMessage());
        assertEquals(5, s.getPlayer().getMoney());
        assertEquals(0, s.getPlayer().getInventory().size());
    }

    @Test
    void buyWithFullInventory_message_noCharge() {
        GameSession s = openMaraShop();
        Item banana = s.getActiveShop().getStock().get(2);
        for (int i = 0; i < SeedData.INVENTORY_CAPACITY; i++) {
            s.getPlayer().getInventory().addItem(banana);
        }
        s.handle(Action.CONFIRM);
        assertEquals("Inventory full.", s.getMessage());
        assertEquals(50, s.getPlayer().getMoney());
    }

    @Test
    void cancel_returnsToExploring() {
        GameSession s = openMaraShop();
        s.handle(Action.CANCEL);
        assertEquals(Screen.EXPLORING, s.getScreen());
        assertNull(s.getActiveShop());
    }

    @Test
    void cursorWrapsWithinStock() {
        GameSession s = openMaraShop();
        int n = s.getActiveShop().getStock().size();
        assertEquals(3, n);
        s.handle(Action.UP);
        assertEquals(2, s.getShopCursor());
        s.handle(Action.DOWN);
        assertEquals(0, s.getShopCursor());
        s.handle(Action.DOWN);
        s.handle(Action.DOWN);
        s.handle(Action.DOWN);
        assertEquals(0, s.getShopCursor());
    }

    @Test
    void buyingEverything_leavesShopEmpty_cursorSafe_confirmDoesNothing() {
        GameSession s = TestSessions.at(28, 13); // Tomas: 1 machete + 2 necklaces + 2 tikis
        s.getPlayer().earn(1000);
        s.handle(Action.CONFIRM);
        assertEquals(Screen.SHOP, s.getScreen());
        for (int i = 0; i < 5; i++) {
            s.handle(Action.CONFIRM);
        }
        assertTrue(s.getActiveShop().getStock().isEmpty());
        assertEquals(0, s.getShopCursor());
        s.handle(Action.CONFIRM);
        s.handle(Action.DOWN);
        assertEquals(5, s.getPlayer().getInventory().size());
    }

    @Test
    void emptyShop_confirmDoesNothing() {
        isla.Player p = TestSessions.newPlayer();
        p.setPosition(12, 13);
        Vendor empty = new Vendor(9, "Empty", 12, 12, "vendor_sprite", "Sold out!", new Shop());
        GameSession s = new GameSession(new GameWorld(TileMap.loadResource("/maps/island.txt"), p,
                new ArrayList<>(List.of(empty))));
        s.handle(Action.CONFIRM);
        assertEquals(Screen.SHOP, s.getScreen());
        s.handle(Action.CONFIRM);
        s.handle(Action.UP);
        assertEquals(50, p.getMoney());
        assertEquals(0, s.getShopCursor());
    }

    @Test
    void confirmNearElder_opensDialogue_thenClosesOnConfirm() {
        GameSession s = TestSessions.at(20, 10);
        s.handle(Action.CONFIRM);
        assertEquals(Screen.DIALOGUE, s.getScreen());
        assertTrue(s.getDialogueText().contains("coconut"));
        s.handle(Action.CONFIRM);
        assertEquals(Screen.EXPLORING, s.getScreen());
    }

    @Test
    void shopScreen_rendersItemsPricesGreetingAndMoney() {
        GameSession s = openMaraShop();
        RecordingRenderer r = new RecordingRenderer();
        s.render(r);
        assertTrue(r.anyTextContains("Coconut Water"), r.texts.toString());
        assertTrue(r.anyTextContains("$10"), r.texts.toString());
        assertTrue(r.anyTextContains("Fresh fruit"), r.texts.toString());
        assertTrue(r.anyTextContains("$50"), r.texts.toString());
    }

    @Test
    void dialogueScreen_rendersText() {
        GameSession s = TestSessions.at(20, 10);
        s.handle(Action.CONFIRM);
        RecordingRenderer r = new RecordingRenderer();
        s.render(r);
        assertTrue(r.anyTextContains("coconut"), r.texts.toString());
    }

    @Test
    void wrap_splitsOnWordBoundariesWithinWidth() {
        List<String> lines = GameSession.wrap("one two three four five", 9);
        assertEquals(List.of("one two", "three", "four five"), lines);
        assertEquals(List.of("abcdefghij"), GameSession.wrap("abcdefghij", 4)); // long word never dropped
        assertTrue(GameSession.wrap("", 10).isEmpty());
    }
}
