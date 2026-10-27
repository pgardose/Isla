package isla;

/**
 * An NPC that hands out a Quest. What interacting does depends on the kind of
 * quest: a fetch quest is turned in or repeated, a DebugQuest opens a puzzle.
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
        return quest.interact(player, dialogue);
    }

    @Override
    public void render(Renderer renderer) {
        super.render(renderer);
        if (!quest.isCompleted()) {
            renderer.drawLabel(x, y, "?");
        }
    }
}
