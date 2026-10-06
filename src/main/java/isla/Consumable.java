package isla;

/** A one-use item that restores stamina. */
public class Consumable extends Item {

    private final int staminaRestore;

    public Consumable(int id, String name, int price, int staminaRestore) {
        super(id, name, price);
        this.staminaRestore = staminaRestore;
    }

    public int getStaminaRestore() {
        return staminaRestore;
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.CONSUMABLE;
    }

    @Override
    public boolean isConsumedOnUse() {
        return true;
    }

    @Override
    public String getUnusableReason(Player player) {
        return player.getStamina() >= player.getMaxStamina() ? "Stamina is already full." : null;
    }

    @Override
    public String use(Player player) {
        player.restoreStamina(staminaRestore);
        return player.getName() + " uses " + getName() + " and restores " + staminaRestore + " stamina.";
    }
}
