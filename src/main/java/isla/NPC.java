package isla;

/**
 * Non-player character. Abstract: only concrete NPCs (Vendor, QuestGiver)
 * exist, each defining its own interact().
 */
public abstract class NPC extends Entity {

    protected String dialogue;

    protected NPC(int x, int y, String sprite, String dialogue) {
        super(x, y, sprite);
        this.dialogue = dialogue;
    }

    public String getDialogue() {
        return dialogue;
    }

    public abstract InteractionResult interact(Player player);

    @Override
    public void update() {
        // NPCs are static by default.
    }

    @Override
    public void render(Renderer renderer) {
        renderer.drawSprite(x, y, sprite);
    }
}
