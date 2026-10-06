package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.Item;
import org.junit.jupiter.api.Test;

class GameSessionQuestTest {

    private Item coconut() {
        return SeedData.items().stream().filter(i -> i.getName().equals("Coconut")).findFirst().orElseThrow();
    }

    private GameSession atElder() {
        return TestSessions.at(20, 10);
    }

    @Test
    void elderWithoutCoconut_showsQuestText_noReward() {
        GameSession s = atElder();
        s.handle(Action.CONFIRM);
        assertEquals(Screen.DIALOGUE, s.getScreen());
        assertTrue(s.getDialogueText().contains("Quest:"), s.getDialogueText());
        assertEquals(50, s.getPlayer().getMoney());
        assertFalse(s.getWorld().getNpcs().isEmpty());
    }

    @Test
    void elderWithCoconut_paysOnceAndConsumesItem() {
        GameSession s = atElder();
        s.getPlayer().getInventory().addItem(coconut());
        s.handle(Action.CONFIRM);
        assertEquals(75, s.getPlayer().getMoney());
        assertEquals(0, s.getPlayer().getInventory().countOf(coconut()));
        assertTrue(s.getDialogueText().contains("$25"), s.getDialogueText());
        s.handle(Action.CONFIRM); // close
        s.handle(Action.CONFIRM); // talk again
        assertEquals(75, s.getPlayer().getMoney());
        assertTrue(s.getDialogueText().contains("Thanks again"), s.getDialogueText());
    }

    @Test
    void elderWithTwoCoconuts_paysOnlyOnce() {
        GameSession s = atElder();
        s.getPlayer().getInventory().addItem(coconut());
        s.getPlayer().getInventory().addItem(coconut());
        s.handle(Action.CONFIRM);
        s.handle(Action.CONFIRM);
        s.handle(Action.CONFIRM);
        assertEquals(75, s.getPlayer().getMoney());
        assertEquals(1, s.getPlayer().getInventory().countOf(coconut()));
    }

    @Test
    void playThrough_buyCoconut_walkToElder_getPaid() {
        GameSession s = TestSessions.at(12, 13); // below Mara
        s.handle(Action.CONFIRM);
        s.handle(Action.DOWN); // cursor -> Coconut ($8)
        s.handle(Action.CONFIRM);
        assertEquals(42, s.getPlayer().getMoney());
        s.handle(Action.CANCEL);
        for (int i = 0; i < 8; i++) s.handle(Action.RIGHT);
        for (int i = 0; i < 3; i++) s.handle(Action.UP);
        assertEquals(20, s.getPlayer().getX());
        assertEquals(10, s.getPlayer().getY());
        s.handle(Action.CONFIRM);
        assertEquals(67, s.getPlayer().getMoney());
    }
}
