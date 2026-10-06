package isla;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shop holds a Vendor's stock (item -> quantity) and handles the purchase
 * flow from the "Player buys an item from a Vendor" sequence diagram:
 * charge via Player.spend(), then add to the Inventory, refunding if the
 * inventory turns out to be full.
 */
public class Shop {

    private final Map<Item, Integer> stock = new LinkedHashMap<>();

    /** Items currently available (quantity > 0), in the order they were added. */
    public List<Item> getStock() {
        return new ArrayList<>(stock.keySet());
    }

    public int quantityOf(Item item) {
        return stock.getOrDefault(item, 0);
    }

    public void addStock(Item item, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        stock.merge(item, quantity, Integer::sum);
    }

    public PurchaseResult buyItem(Player player, Item item) {
        if (quantityOf(item) <= 0) {
            return PurchaseResult.NOT_IN_STOCK;
        }
        if (!player.spend(item.getPrice())) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }
        if (!player.getInventory().addItem(item)) {
            player.earn(item.getPrice()); // refund: never charge for nothing
            return PurchaseResult.INVENTORY_FULL;
        }
        int left = stock.get(item) - 1;
        if (left == 0) {
            stock.remove(item);
        } else {
            stock.put(item, left);
        }
        return PurchaseResult.SUCCESS;
    }
}
