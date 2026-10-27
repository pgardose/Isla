package isla;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class QuestGiverPuzzleTest {

    private DebugQuest debugQuest() {
        return new DebugQuest(2, 2, "Fix the bug", 40, "if (x = 0) {}",
                List.of("Use ==", "Use !="), 0);
    }

    private Player player() {
        return new Player(1, "Pj", 0, 100, 3);
    }

    @Test
    void giverWithDebugQuest_opensPuzzle() {
        DebugQuest quest = debugQuest();
        QuestGiver nia = new QuestGiver(2, 5, 5, "coder_sprite", "My code is broken!", quest);
        InteractionResult result = nia.interact(player());
        assertEquals(InteractionResult.Kind.PUZZLE, result.getKind());
        assertSame(quest, result.getPuzzle());
        assertTrue(result.getText().contains("My code is broken!"), result.getText());
    }

    @Test
    void giverWithCompletedDebugQuest_justThanks() {
        DebugQuest quest = debugQuest();
        quest.answer(player(), 0);
        QuestGiver nia = new QuestGiver(2, 5, 5, "coder_sprite", "My code is broken!", quest);
        InteractionResult result = nia.interact(player());
        assertEquals(InteractionResult.Kind.DIALOGUE, result.getKind());
        assertTrue(result.getText().contains("Thanks again"), result.getText());
    }

    @Test
    void giverWithFetchQuest_stillGivesDialogue() {
        Quest fetch = new Quest(1, 1, "Bring a coconut", 25, "Coconut");
        QuestGiver elder = new QuestGiver(1, 5, 5, "elder_sprite", "Legs...", fetch);
        InteractionResult result = elder.interact(player());
        assertEquals(InteractionResult.Kind.DIALOGUE, result.getKind());
        assertNull(result.getPuzzle());
    }

    @Test
    void completedDebugQuestGiver_hidesQuestMarker() {
        DebugQuest quest = debugQuest();
        QuestGiver nia = new QuestGiver(2, 5, 5, "coder_sprite", "Hi", quest);
        RecordingRenderer open = new RecordingRenderer();
        nia.render(open);
        assertEquals(1, open.count("label 5,5,?"));
        quest.answer(player(), 0);
        RecordingRenderer done = new RecordingRenderer();
        nia.render(done);
        assertEquals(0, done.count("label 5,5,?"));
    }
}
