package isla;

/**
 * A vendor NPC with a Shop. Interacting opens the shop with the greeting.
 * The id matches the vendor_id column in the database.
 */
public class Vendor extends NPC implements Interactable {

    private final int id;
    private final String name;
    private final Shop shop;

    public Vendor(int id, String name, int x, int y, String sprite, String dialogue, Shop shop) {
        super(x, y, sprite, dialogue);
        this.id = id;
        this.name = name;
        this.shop = shop;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Shop getShop() {
        return shop;
    }

    @Override
    public InteractionResult interact(Player player) {
        return InteractionResult.shop(dialogue, shop);
    }

    @Override
    public void render(Renderer renderer) {
        super.render(renderer);
        renderer.drawLabel(x, y, "!");
    }
}
