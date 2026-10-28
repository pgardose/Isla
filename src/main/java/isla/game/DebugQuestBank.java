package isla.game;

import isla.DebugQuest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A pool of debugging puzzles for Coder Nia. Each new game draws a different one
 * (never the same as the game before) and shuffles the answer order, so two players,
 * or the same player twice, do not see the same question in the same slot.
 *
 * To add a puzzle, add one line to PUZZLES. Keep the snippet under about 60 characters
 * so it fits the puzzle panel.
 */
public final class DebugQuestBank {

    /** One puzzle: a broken line, the correct fix, and plausible wrong answers. */
    public record Puzzle(String title, String snippet, String fix, List<String> wrongAnswers) {
    }

    static final int QUEST_ID = 2;
    static final int GIVER_NPC_ID = 2;
    static final int REWARD = 40;

    private static final List<Puzzle> PUZZLES = List.of(
            new Puzzle("Fix Nia's broken stamina check",
                    "if (stamina = 0) { rest(); }",
                    "Change = to ==",
                    List.of("Change = to !=", "Remove the semicolon", "Rename stamina to energy")),
            new Puzzle("Fix the loop that crashes",
                    "for (int i = 0; i <= items.length; i++) use(items[i]);",
                    "Change <= to <",
                    List.of("Change i++ to i--", "Change int to double", "Remove items.length")),
            new Puzzle("Fix the crash on a missing name",
                    "if (name.equals(\"Pj\")) { greet(); }   // name may be null",
                    "Write \"Pj\".equals(name) instead",
                    List.of("Write name = \"Pj\" instead", "Change equals to ==", "Add a semicolon after the if")),
            new Puzzle("Fix the wrong average",
                    "double half = total / 2;   // total is an int",
                    "Change 2 to 2.0",
                    List.of("Change double to int", "Change / to %", "Put parentheses around total")),
            new Puzzle("Fix the endless loop",
                    "while (count < 10) { buy(); }",
                    "Add count++ inside the loop",
                    List.of("Change < to >", "Remove the braces", "Change 10 to 100")),
            new Puzzle("Fix the answer check",
                    "if (answer == \"yes\") { go(); }",
                    "Use answer.equals(\"yes\")",
                    List.of("Change == to ===", "Use answer = \"yes\"", "Remove the quotes")),
            new Puzzle("Fix the missing result",
                    "int price() { cost * 2; }",
                    "Write return cost * 2;",
                    List.of("Remove the braces", "Change * to +", "Add static before int")),
            new Puzzle("Fix the abstract class error",
                    "Item i = new Item(\"Coconut\", 8);   // Item is abstract",
                    "Create a subclass, e.g. new Consumable(...)",
                    List.of("Make Item public", "Add static to Item", "Remove the new keyword")),
            new Puzzle("Fix the private field error",
                    "player.money = 5;   // money is private",
                    "Use a method such as player.earn(5)",
                    List.of("Make money static", "Change = to ==", "Put final before player")),
            new Puzzle("Fix the array crash",
                    "int[] a = new int[3]; a[3] = 7;",
                    "Use a[2], the last valid index",
                    List.of("Change new int[3] to new int(3)", "Change 7 to 3", "Remove the brackets")),
            new Puzzle("Fix the range check",
                    "if (money > 10 && < 20) { buy(); }",
                    "Write money > 10 && money < 20",
                    List.of("Change && to ||", "Remove the braces", "Change money to Money")),
            new Puzzle("Fix the text that never changes",
                    "String s = \"Hi\"; s.toUpperCase(); print(s);",
                    "Assign it: s = s.toUpperCase();",
                    List.of("Change print to println", "Make s final", "Change \"Hi\" to 'Hi'")),
            new Puzzle("Fix the list that crashes mid-loop",
                    "for (Item x : items) { items.remove(x); }",
                    "Use items.removeIf(...) or an Iterator",
                    List.of("Use items.add(x) instead", "Change Item to var", "Make items final")));

    private DebugQuestBank() {
    }

    public static List<Puzzle> all() {
        return PUZZLES;
    }

    /**
     * Draws a random puzzle as Nia's quest, with the answers shuffled. previousSnippet (may be null)
     * is the puzzle the player had last time; it is skipped so a new game never repeats it.
     */
    public static DebugQuest random(Random random, String previousSnippet) {
        List<Puzzle> candidates = new ArrayList<>(PUZZLES);
        candidates.removeIf(p -> p.snippet().equals(previousSnippet));
        Puzzle puzzle = candidates.get(random.nextInt(candidates.size()));

        List<String> options = new ArrayList<>(puzzle.wrongAnswers());
        options.add(puzzle.fix());
        Collections.shuffle(options, random);
        return new DebugQuest(QUEST_ID, GIVER_NPC_ID, puzzle.title(), REWARD,
                puzzle.snippet(), options, options.indexOf(puzzle.fix()));
    }
}
