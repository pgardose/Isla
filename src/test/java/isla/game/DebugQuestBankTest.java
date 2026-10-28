package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.DebugQuest;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DebugQuestBankTest {

    private DebugQuestBank.Puzzle puzzleFor(String snippet) {
        return DebugQuestBank.all().stream().filter(p -> p.snippet().equals(snippet)).findFirst().orElseThrow();
    }

    @Test
    void bankHasPlentyOfWellFormedPuzzles() {
        List<DebugQuestBank.Puzzle> all = DebugQuestBank.all();
        assertTrue(all.size() >= 10, "size " + all.size());
        Set<String> snippets = new HashSet<>();
        for (DebugQuestBank.Puzzle p : all) {
            assertFalse(p.snippet().isBlank());
            assertFalse(p.title().isBlank());
            assertFalse(p.fix().isBlank());
            assertTrue(p.wrongAnswers().size() >= 2, p.snippet());
            assertFalse(p.wrongAnswers().contains(p.fix()), "fix repeated among wrong answers: " + p.snippet());
            assertEquals(p.wrongAnswers().size(), new HashSet<>(p.wrongAnswers()).size(), p.snippet());
            assertTrue(p.snippet().length() <= 62, "too wide for the panel: " + p.snippet());
            assertTrue(snippets.add(p.snippet()), "duplicate snippet " + p.snippet());
        }
    }

    @Test
    void pickedQuest_isNiasQuest_withTheStandardReward() {
        DebugQuest q = DebugQuestBank.random(new Random(1), null);
        assertEquals(SeedData.debugQuest().getId(), q.getId());
        assertEquals(SeedData.debugQuest().getGiverNpcId(), q.getGiverNpcId());
        assertEquals(SeedData.debugQuest().getReward(), q.getReward());
        assertFalse(q.isCompleted());
    }

    @Test
    void pickedQuest_keepsTheRightFixAtTheCorrectIndex_whateverTheShuffle() {
        for (long seed = 0; seed < 300; seed++) {
            DebugQuest q = DebugQuestBank.random(new Random(seed), null);
            DebugQuestBank.Puzzle p = puzzleFor(q.getSnippet());
            assertEquals(p.fix(), q.getOptions().get(q.getCorrectIndex()), "seed " + seed);
            assertEquals(p.wrongAnswers().size() + 1, q.getOptions().size());
        }
    }

    @Test
    void sameSeed_givesTheSamePuzzle_differentSeeds_giveVariety() {
        DebugQuest a = DebugQuestBank.random(new Random(42), null);
        DebugQuest b = DebugQuestBank.random(new Random(42), null);
        assertEquals(a.getSnippet(), b.getSnippet());
        assertEquals(a.getOptions(), b.getOptions());

        Set<String> seen = new HashSet<>();
        Set<Integer> positions = new HashSet<>();
        for (long seed = 0; seed < 200; seed++) {
            DebugQuest q = DebugQuestBank.random(new Random(seed), null);
            seen.add(q.getSnippet());
            positions.add(q.getCorrectIndex());
        }
        assertTrue(seen.size() >= 8, "only " + seen.size() + " different puzzles in 200 draws");
        assertTrue(positions.size() >= 3, "the right answer should not always sit in the same slot: " + positions);
    }

    @Test
    void neverRepeatsThePreviousPuzzle() {
        for (DebugQuestBank.Puzzle previous : DebugQuestBank.all()) {
            for (long seed = 0; seed < 40; seed++) {
                DebugQuest q = DebugQuestBank.random(new Random(seed), previous.snippet());
                assertNotEquals(previous.snippet(), q.getSnippet());
            }
        }
    }

    @Test
    void anUnknownPreviousSnippet_isIgnored() {
        assertDoesNotThrow(() -> DebugQuestBank.random(new Random(3), "something no longer in the bank"));
    }
}
