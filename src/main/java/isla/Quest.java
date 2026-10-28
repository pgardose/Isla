package isla;

/**
 * A simple fetch quest: bring the named item to the quest giver and get paid.
 * A quest with no required item completes as soon as it is turned in.
 */
public class Quest {

    private final int id;
    private final int giverNpcId;
    private final String description;
    private final int reward;
    private final String requiredItemName;
    private boolean completed;

    public Quest(int id, int giverNpcId, String description, int reward, String requiredItemName) {
        this.id = id;
        this.giverNpcId = giverNpcId;
        this.description = description;
        this.reward = reward;
        this.requiredItemName = requiredItemName;
    }

    public int getId() {
        return id;
    }

    public int getGiverNpcId() {
        return giverNpcId;
    }

    public String getDescription() {
        return description;
    }

    public int getReward() {
        return reward;
    }

    /** Name of the item that must be handed over, or null if none is needed. */
    public String getRequiredItemName() {
        return requiredItemName;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markCompleted() {
        this.completed = true;
    }

    public boolean isCompletable(Player player) {
        if (completed) {
            return false;
        }
        return requiredItemName == null || player.getInventory().findByName(requiredItemName).isPresent();
    }

    /**
     * Turns the quest in: removes the required item, pays the reward and marks
     * the quest completed. Returns false (changing nothing) if it cannot be
     * completed right now.
     */
    public boolean complete(Player player) {
        if (!isCompletable(player)) {
            return false;
        }
        if (requiredItemName != null) {
            player.getInventory().findByName(requiredItemName)
                    .ifPresent(item -> player.getInventory().removeItem(item));
        }
        player.earn(reward);
        completed = true;
        return true;
    }

    /**
     * What happens when the player talks to the giver of this quest.
     * OOP note (POLYMORPHISM): a fetch quest turns itself in or repeats the
     * request; DebugQuest overrides this to open a puzzle instead.
     */
    public InteractionResult interact(Player player, String giverDialogue) {
        if (completed) {
            return InteractionResult.dialogue("Thanks again for your help!");
        }
        if (complete(player)) {
            return InteractionResult.dialogue("You brought the " + requiredItemName
                    + "! Here is $" + reward + " for your trouble.");
        }
        return InteractionResult.dialogue(giverDialogue + " (Quest: " + description + ")");
    }
}
