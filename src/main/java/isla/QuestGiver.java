package isla;

/**
 * An NPC that hands out a fetch Quest. Interacting either turns the quest in
 * (if the player has what is asked for) or repeats the request.
 */
public class QuestGiver extends NPC implements Interactable {

    private final int npcId;
    private final Quest quest;

    public QuestGiver(int npcId, int x, int y, String sprite, String dialogue, Quest quest) {
        super(x, y, sprite, dialogue);
        this.npcId = npcId;
        this.quest = quest;
    }

    public int getNpcId() {
        return npcId;
    }

    public Quest getQuest() {
        return quest;
    }

    @Override
    public InteractionResult interact(Player player) {
        if (quest.isCompleted()) {
            return InteractionResult.dialogue("Thanks again for your help!");
        }
        if (quest.complete(player)) {
            return InteractionResult.dialogue("You brought the " + quest.getRequiredItemName()
                    + "! Here is $" + quest.getReward() + " for your trouble.");
        }
        return InteractionResult.dialogue(dialogue + " (Quest: " + quest.getDescription() + ")");
    }

    @Override
    public void render(Renderer renderer) {
        super.render(renderer);
        if (!quest.isCompleted()) {
            renderer.drawLabel(x, y, "?");
        }
    }
}
