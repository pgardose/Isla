package isla;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class QuestTest {

    private final Consumable coconut = new Consumable(2, "Coconut", 8, 10);
    private final Quest quest = new Quest(1, 1, "Bring a coconut", 25, "Coconut");

    @Test
    void withoutItem_notCompletable_noReward() {
        Player p = new Player(1, "Pj", 0, 100, 3);
        assertFalse(quest.isCompletable(p));
        assertFalse(quest.complete(p));
        assertEquals(0, p.getMoney());
        assertFalse(quest.isCompleted());
    }

    @Test
    void withItem_completeConsumesItemPaysRewardOnce() {
        Player p = new Player(1, "Pj", 0, 100, 3);
        p.getInventory().addItem(coconut);
        assertTrue(quest.complete(p));
        assertEquals(25, p.getMoney());
        assertEquals(0, p.getInventory().countOf(coconut));
        assertTrue(quest.isCompleted());
    }

    @Test
    void secondComplete_returnsFalse() {
        Player p = new Player(1, "Pj", 0, 100, 3);
        p.getInventory().addItem(coconut);
        p.getInventory().addItem(coconut);
        assertTrue(quest.complete(p));
        assertFalse(quest.complete(p));
        assertEquals(25, p.getMoney());
        assertEquals(1, p.getInventory().countOf(coconut));
    }
}
