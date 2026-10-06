package isla;

/** A reusable item that unlocks an area of the map. */
public class Tool extends Item {

    private final String unlocksArea;

    public Tool(int id, String name, int price, String unlocksArea) {
        super(id, name, price);
        this.unlocksArea = unlocksArea;
    }

    public String getUnlocksArea() {
        return unlocksArea;
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.TOOL;
    }

    @Override
    public String use(Player player) {
        player.unlockArea(unlocksArea);
        return player.getName() + " uses " + getName() + " and unlocks: " + unlocksArea + ".";
    }
}
