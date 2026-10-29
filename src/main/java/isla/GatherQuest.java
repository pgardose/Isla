package isla;

import java.util.List;

/**
 * A quest that needs proof the player has done several things, such as finding all three Idol shards. The
 * proof is a set of story flags (see Quest#grants), not inventory items, so key story objects never take an
 * inventory slot and can never be sold or used up.
 *
 * OOP note (INHERITANCE + POLYMORPHISM): a GatherQuest IS a Quest and is saved, paid and turned in by the same
 * code; only "can it be completed?" differs.
 */
public class GatherQuest extends Quest {

    private final List<String> requiredFlags;

    public GatherQuest(int id, int giverNpcId, String description, int reward, List<String> requiredFlags) {
        super(id, giverNpcId, description, reward, null);
        if (requiredFlags == null || requiredFlags.isEmpty()) {
            throw new IllegalArgumentException("A gather quest needs at least one flag");
        }
        this.requiredFlags = List.copyOf(requiredFlags);
    }

    public List<String> getRequiredFlags() {
        return requiredFlags;
    }

    /** How many of the required flags the player already has. */
    public int progress(Player player) {
        return (int) requiredFlags.stream().filter(player::hasUnlocked).count();
    }

    @Override
    public boolean isCompletable(Player player) {
        return !isCompleted() && progress(player) == requiredFlags.size();
    }

    @Override
    public InteractionResult interact(Player player, String giverDialogue) {
        if (isCompleted()) {
            return InteractionResult.dialogue("Thanks again for your help!");
        }
        if (complete(player)) {
            return InteractionResult.dialogue(completionText());
        }
        return InteractionResult.dialogue(giverDialogue + " (" + progress(player) + " of "
                + requiredFlags.size() + " found)");
    }
}
