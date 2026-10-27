package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.DebugQuest;
import isla.RecordingRenderer;
import org.junit.jupiter.api.Test;

class GameSessionPuzzleTest {

    /** Standing on the road just above Nia (24,14). */
    private GameSession atNia() {
        return TestSessions.at(24, 13);
    }

    private GameSession openPuzzle() {
        GameSession s = atNia();
        s.handle(Action.CONFIRM);
        return s;
    }

    private int wrongChoice(DebugQuest q) {
        return (q.getCorrectIndex() + 1) % q.getOptions().size();
    }

    private void moveCursorTo(GameSession s, int index) {
        while (s.getPuzzleCursor() != index) {
            s.handle(Action.DOWN);
        }
    }

    @Test
    void talkingToNia_opensDebugPuzzle() {
        GameSession s = openPuzzle();
        assertEquals(Screen.DEBUG_PUZZLE, s.getScreen());
        assertNotNull(s.getActivePuzzle());
        assertEquals(0, s.getPuzzleCursor());
    }

    @Test
    void cursorWrapsInBothDirections() {
        GameSession s = openPuzzle();
        int n = s.getActivePuzzle().getOptions().size();
        s.handle(Action.UP);
        assertEquals(n - 1, s.getPuzzleCursor());
        s.handle(Action.DOWN);
        assertEquals(0, s.getPuzzleCursor());
    }

    @Test
    void wrongAnswer_staysOnPuzzle_noPayout_canRetry() {
        GameSession s = openPuzzle();
        DebugQuest q = s.getActivePuzzle();
        moveCursorTo(s, wrongChoice(q));
        s.handle(Action.CONFIRM);
        assertEquals(Screen.DEBUG_PUZZLE, s.getScreen());
        assertEquals(50, s.getPlayer().getMoney());
        assertFalse(q.isCompleted());
        assertTrue(s.getMessage().contains("Not quite"), s.getMessage());
    }

    @Test
    void correctAnswer_paysRewardAndShowsDialogue() {
        GameSession s = openPuzzle();
        DebugQuest q = s.getActivePuzzle();
        moveCursorTo(s, q.getCorrectIndex());
        s.handle(Action.CONFIRM);
        assertEquals(Screen.DIALOGUE, s.getScreen());
        assertEquals(50 + q.getReward(), s.getPlayer().getMoney());
        assertTrue(s.getDialogueText().contains("$" + q.getReward()), s.getDialogueText());
        assertTrue(q.isCompleted());
    }

    @Test
    void cancel_leavesWithoutPayout_andReopenStartsAtTop() {
        GameSession s = openPuzzle();
        s.handle(Action.DOWN);
        s.handle(Action.CANCEL);
        assertEquals(Screen.EXPLORING, s.getScreen());
        assertEquals(50, s.getPlayer().getMoney());
        s.handle(Action.CONFIRM);
        assertEquals(Screen.DEBUG_PUZZLE, s.getScreen());
        assertEquals(0, s.getPuzzleCursor());
    }

    @Test
    void walkingKeysDoNothingWhilePuzzleIsOpen() {
        GameSession s = openPuzzle();
        int x = s.getPlayer().getX(), y = s.getPlayer().getY();
        s.handle(Action.LEFT);
        s.handle(Action.RIGHT);
        s.handle(Action.INVENTORY);
        s.handle(Action.SAVE);
        assertEquals(Screen.DEBUG_PUZZLE, s.getScreen());
        assertEquals(x, s.getPlayer().getX());
        assertEquals(y, s.getPlayer().getY());
    }

    @Test
    void afterSolving_niaJustThanks_andPaysNothingMore() {
        GameSession s = openPuzzle();
        moveCursorTo(s, s.getActivePuzzle().getCorrectIndex());
        s.handle(Action.CONFIRM);
        int money = s.getPlayer().getMoney();
        s.handle(Action.CONFIRM); // close dialogue
        s.handle(Action.CONFIRM); // talk again
        assertEquals(Screen.DIALOGUE, s.getScreen());
        assertTrue(s.getDialogueText().contains("Thanks again"), s.getDialogueText());
        assertEquals(money, s.getPlayer().getMoney());
    }

    @Test
    void render_showsSnippetAndEveryOption() {
        GameSession s = openPuzzle();
        RecordingRenderer r = new RecordingRenderer();
        s.render(r);
        DebugQuest q = s.getActivePuzzle();
        assertTrue(r.anyTextContains(q.getSnippet()), String.valueOf(r.texts));
        for (String option : q.getOptions()) {
            assertTrue(r.anyTextContains(option), option);
        }
    }

    @Test
    void render_exploringAfterSolving_doesNotDrawQuestMarkerOnNia() {
        GameSession s = openPuzzle();
        moveCursorTo(s, s.getActivePuzzle().getCorrectIndex());
        s.handle(Action.CONFIRM);
        s.handle(Action.CONFIRM);
        RecordingRenderer r = new RecordingRenderer();
        s.render(r);
        assertEquals(0, r.count("label 24,14,?"));
    }

    @Test
    void playThrough_wrongThenRight_getsPaidOnce() {
        GameSession s = TestSessions.fresh(); // spawn (20,13)
        for (int i = 0; i < 4; i++) s.handle(Action.RIGHT);
        assertEquals(24, s.getPlayer().getX());
        s.handle(Action.CONFIRM); // Nia is directly below
        assertEquals(Screen.DEBUG_PUZZLE, s.getScreen());
        DebugQuest q = s.getActivePuzzle();
        moveCursorTo(s, wrongChoice(q));
        s.handle(Action.CONFIRM);
        assertEquals(50, s.getPlayer().getMoney());
        moveCursorTo(s, q.getCorrectIndex());
        s.handle(Action.CONFIRM);
        assertEquals(90, s.getPlayer().getMoney());
    }
}
