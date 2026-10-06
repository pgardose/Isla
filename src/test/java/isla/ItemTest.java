package isla;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ItemTest {

    @Test
    void use_returnsMessage_perSubclass() {
        Player p = new Player(1, "Pj", 50, 100, 3);
        p.expendStamina(50);

        Consumable water = new Consumable(1, "Coconut Water", 10, 20);
        Tool machete = new Tool(2, "Rusty Machete", 30, "Jungle Path");
        Souvenir shell = new Souvenir(3, "Seashell", 15);

        assertTrue(water.use(p).contains("20"));
        assertEquals(70, p.getStamina());
        assertTrue(machete.use(p).contains("Jungle Path"));
        assertTrue(p.hasUnlocked("Jungle Path"));
        assertFalse(shell.use(p).isEmpty());
    }

    @Test
    void categoriesAndConsumption() {
        assertEquals(ItemCategory.CONSUMABLE, new Consumable(1, "a", 1, 1).getCategory());
        assertEquals(ItemCategory.TOOL, new Tool(2, "b", 1, "x").getCategory());
        assertEquals(ItemCategory.SOUVENIR, new Souvenir(3, "c", 1).getCategory());
        assertTrue(new Consumable(1, "a", 1, 1).isConsumedOnUse());
        assertFalse(new Tool(2, "b", 1, "x").isConsumedOnUse());
    }

    @Test
    void equalsById() {
        assertEquals(new Souvenir(3, "Shell", 15), new Souvenir(3, "Shell", 15));
        assertNotEquals(new Souvenir(3, "Shell", 15), new Souvenir(4, "Shell", 15));
        assertEquals(new Souvenir(3, "Shell", 15).hashCode(), new Souvenir(3, "Shell", 15).hashCode());
    }

    @Test
    void unusableReason_consumableAtFullStaminaOnly() {
        Player p = new Player(1, "Pj", 50, 100, 3);
        Consumable water = new Consumable(1, "Coconut Water", 10, 20);
        assertNotNull(water.getUnusableReason(p));
        p.expendStamina(1);
        assertNull(water.getUnusableReason(p));
        assertNull(new Tool(2, "b", 1, "x").getUnusableReason(p));
        assertNull(new Souvenir(3, "c", 1).getUnusableReason(p));
    }
}
