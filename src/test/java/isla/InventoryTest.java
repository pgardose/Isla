package isla;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class InventoryTest {

    @Test
    void capacityEnforced() {
        Inventory inv = new Inventory(2);
        Souvenir s = new Souvenir(1, "Shell", 5);
        assertTrue(inv.addItem(s));
        assertTrue(inv.addItem(s));
        assertFalse(inv.addItem(s));
        assertTrue(inv.isFull());
    }

    @Test
    void countOfAndFindByName() {
        Inventory inv = new Inventory(5);
        Souvenir s = new Souvenir(1, "Shell", 5);
        inv.addItem(s);
        inv.addItem(s);
        assertEquals(2, inv.countOf(s));
        assertTrue(inv.findByName("Shell").isPresent());
        assertTrue(inv.findByName("Nope").isEmpty());
        assertTrue(inv.removeItem(s));
        assertEquals(1, inv.countOf(s));
    }
}
