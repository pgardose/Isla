package isla;

import java.util.List;

/**
 * A debugging puzzle: the quest giver shows a broken code snippet and the
 * player picks the fix from a list. A correct pick pays the reward; a wrong
 * pick changes nothing, so the player can simply try again.
 *
 * OOP note (INHERITANCE): a DebugQuest IS a Quest, so it is saved, completed
 * and paid out through the same code as the Elder's fetch quest.
 */
public class DebugQuest extends Quest {

    private final String snippet;
    private final List<String> options;
    private final int correctIndex;
    private List<String> feedback = List.of();

    /** Stamina lost on a wrong fix: a cursed stone gives you a little glitch shock. */
    public static final int WRONG_FIX_STAMINA = 10;

    public DebugQuest(int id, int giverNpcId, String description, int reward,
                      String snippet, List<String> options, int correctIndex) {
        super(id, giverNpcId, description, reward, null);
        if (options == null || options.size() < 2) {
            throw new IllegalArgumentException("A debug quest needs at least two options");
        }
        if (correctIndex < 0 || correctIndex >= options.size()) {
            throw new IllegalArgumentException("correctIndex is not one of the options");
        }
        this.snippet = snippet;
        this.options = List.copyOf(options);
        this.correctIndex = correctIndex;
    }

    public String getSnippet() {
        return snippet;
    }

    /** The fix choices, in display order (read-only). */
    public List<String> getOptions() {
        return options;
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    /**
     * Sets one explanation per option (same order as the options), shown when that option is picked
     * wrongly, so a mistake teaches something. Returns this, for chaining.
     */
    public DebugQuest withFeedback(List<String> perOption) {
        if (perOption.size() != options.size()) {
            throw new IllegalArgumentException("Feedback needs one line per option");
        }
        this.feedback = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(perOption)); // nulls allowed
        return this;
    }

    /** Why this option is wrong, or a general nudge when the puzzle has no feedback written for it. */
    public String feedbackFor(int choice) {
        if (choice >= 0 && choice < feedback.size() && feedback.get(choice) != null) {
            return feedback.get(choice);
        }
        return "Not quite. Read the line again and try another fix.";
    }

    /** Title of the puzzle panel. OOP note (POLYMORPHISM): an Accusation shows its own heading. */
    public String getHeading() {
        return "DEBUG: pick the fix";
    }

    /** What a wrong pick costs. A code fix shocks you; Accusation overrides this. */
    public int wrongAnswerStaminaCost() {
        return WRONG_FIX_STAMINA;
    }

    /**
     * Tries one option. Returns true (and pays the reward) only if it is the
     * right fix and the quest is not already done.
     */
    public boolean answer(Player player, int choice) {
        if (isCompleted() || choice != correctIndex) {
            return false;
        }
        return complete(player);
    }

    @Override
    public InteractionResult interact(Player player, String giverDialogue) {
        if (isCompleted()) {
            return InteractionResult.dialogue("Thanks again for your help!");
        }
        return InteractionResult.puzzle(giverDialogue + " (Quest: " + getDescription() + ")", this);
    }
}
