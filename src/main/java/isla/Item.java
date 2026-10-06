package isla;

import java.util.Objects;

/**
 * Item is the abstract base for anything that can be bought, held in an
 * Inventory, and used by the Player.
 *
 * OOP note (ENCAPSULATION): id, name and price are private and read-only.
 *
 * OOP note (POLYMORPHISM): use(Player) behaves differently per subclass and
 * returns the message to show, so callers never need an if/else chain on the
 * item type.
 *
 * Items are value objects: two items with the same class, id, name and price
 * are equal, so an item loaded from the database equals the one in a shop.
 */
public abstract class Item {

    private final int id;
    private final String name;
    private final int price;

    protected Item(int id, String name, int price) {
        if (price < 0) {
            throw new IllegalArgumentException("Item price cannot be negative");
        }
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public abstract ItemCategory getCategory();

    /** Applies this item's effect and returns a message describing what happened. */
    public abstract String use(Player player);

    /**
     * Why this item cannot be used right now (so the player is not charged an
     * item for nothing), or null if it can be used. Default: always usable.
     */
    public String getUnusableReason(Player player) {
        return null;
    }

    /** True if using the item uses it up (removed from the inventory). */
    public boolean isConsumedOnUse() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item other = (Item) o;
        return id == other.id && price == other.price && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id, name, price);
    }

    @Override
    public String toString() {
        return String.format("%s ($%d)", name, price);
    }
}
