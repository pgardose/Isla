package isla;

/**
 * An NPC that hands out a Quest. What interacting does depends on the kind of
 * quest: a fetch quest is turned in or repeated, a DebugQuest opens a puzzle.
 */
public class QuestGiver extends NPC implements Interactable {

    private final int npcId;
    private final Quest quest;
    private String lockedDialogue = "Come back later. I am not ready yet.";

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

    /** What this NPC says while the quest's earlier chapters are still unfinished. */
    public QuestGiver withLockedDialogue(String text) {
        this.lockedDialogue = text;
        return this;
    }

    @Override
    public InteractionResult interact(Player player) {
        if (!quest.isAvailable()) {
            return InteractionResult.dialogue(lockedDialogue);
        }
        return quest.interact(player, dialogue);
    }

    @Override
    public void render(Renderer renderer) {
        super.render(renderer);
        if (!quest.isCompleted() && quest.isAvailable()) {
            renderer.drawLabel(x, y, "?");
        }
    }
}
