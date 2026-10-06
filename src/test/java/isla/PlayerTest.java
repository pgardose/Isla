package isla;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PlayerTest {

    @Test
    void tenSteps_drainOneStamina() {
        Player p = new Player(1, "Pj", 50, 100, 3);
        for (int i = 0; i < 9; i++) p.move(1, 0);
        assertEquals(100, p.getStamina());
        p.move(1, 0);
        assertEquals(99, p.getStamina());
        assertEquals(10, p.getX());
    }

    @Test
    void exhaustedAtZero() {
        Player p = new Player(1, "Pj", 50, 5, 3);
        assertFalse(p.isExhausted());
        for (int i = 0; i < 50; i++) p.move(0, 1);
        assertEquals(0, p.getStamina());
        assertTrue(p.isExhausted());
    }

    @Test
    void spendMoreThanOwned_returnsFalse() {
        Player p = new Player(1, "Pj", 5, 100, 3);
        assertFalse(p.spend(6));
        assertEquals(5, p.getMoney());
        assertTrue(p.spend(5));
        assertEquals(0, p.getMoney());
    }

    @Test
    void restoreState_setsValuesAndClampsStamina() {
        Player p = new Player(1, "Pj", 50, 100, 3);
        p.restoreState(77, 250, 4, 9);
        assertEquals(77, p.getMoney());
        assertEquals(100, p.getStamina());
        assertEquals(4, p.getX());
        assertEquals(9, p.getY());
    }
}
