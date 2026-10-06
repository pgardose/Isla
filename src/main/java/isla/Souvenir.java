package isla;

/** A collectible with no gameplay effect. */
public class Souvenir extends Item {

    public Souvenir(int id, String name, int price) {
        super(id, name, price);
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.SOUVENIR;
    }

    @Override
    public String use(Player player) {
        return getName() + " is just a keepsake. " + player.getName() + " admires it.";
    }
}
