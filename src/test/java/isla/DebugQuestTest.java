package isla;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class DebugQuestTest {

    private final DebugQuest quest = new DebugQuest(2, 2, "Fix Nia's broken code", 40,
            "if (stamina = 0) { rest(); }",
            List.of("Change = to ==", "Change = to !=", "Remove the braces", "Rename stamina"),
            0);

    private Player player() {
        return new Player(1, "Pj", 0, 100, 3);
    }

    @Test
    void isAQuest_withNoRequiredItem() {
        Quest asQuest = quest;
        assertNull(asQuest.getRequiredItemName());
        assertEquals(40, asQuest.getReward());
    }

    @Test
    void exposesSnippetAndOptions() {
        assertEquals("if (stamina = 0) { rest(); }", quest.getSnippet());
        assertEquals(4, quest.getOptions().size());
        assertEquals("Change = to ==", quest.getOptions().get(0));
    }

    @Test
    void optionsAreReadOnly() {
        assertThrows(UnsupportedOperationException.class, () -> quest.getOptions().add("hack"));
    }

    @Test
    void correctAnswer_paysRewardAndCompletes() {
        Player p = player();
        assertTrue(quest.answer(p, 0));
        assertEquals(40, p.getMoney());
        assertTrue(quest.isCompleted());
    }

    @Test
    void wrongAnswer_changesNothing_andRetryIsAllowed() {
        Player p = player();
        assertFalse(quest.answer(p, 2));
        assertEquals(0, p.getMoney());
        assertFalse(quest.isCompleted());
        assertTrue(quest.answer(p, 0));
        assertEquals(40, p.getMoney());
    }

    @Test
    void outOfRangeChoice_isWrong() {
        Player p = player();
        assertFalse(quest.answer(p, -1));
        assertFalse(quest.answer(p, 4));
        assertEquals(0, p.getMoney());
    }

    @Test
    void secondCorrectAnswer_doesNotPayTwice() {
        Player p = player();
        assertTrue(quest.answer(p, 0));
        assertFalse(quest.answer(p, 0));
        assertEquals(40, p.getMoney());
    }

    @Test
    void rejectsBadDefinitions() {
        assertThrows(IllegalArgumentException.class,
                () -> new DebugQuest(3, 2, "x", 10, "code", List.of("only one"), 0));
        assertThrows(IllegalArgumentException.class,
                () -> new DebugQuest(3, 2, "x", 10, "code", List.of("a", "b"), 2));
    }
}
