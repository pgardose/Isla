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
