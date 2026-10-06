package isla;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ShopTest {

    private final Consumable water = new Consumable(1, "Coconut Water", 10, 20);

    private Shop shopWith(Item item, int qty) {
        Shop shop = new Shop();
        shop.addStock(item, qty);
        return shop;
    }

    @Test
    void buyExactMoney_succeeds() {
        Player p = new Player(1, "Pj", 10, 100, 3);
        assertEquals(PurchaseResult.SUCCESS, shopWith(water, 1).buyItem(p, water));
        assertEquals(0, p.getMoney());
        assertEquals(1, p.getInventory().countOf(water));
    }

    @Test
    void insufficientFunds_returnsInsufficientAndNoCharge() {
        Player p = new Player(1, "Pj", 9, 100, 3);
        assertEquals(PurchaseResult.INSUFFICIENT_FUNDS, shopWith(water, 1).buyItem(p, water));
        assertEquals(9, p.getMoney());
        assertEquals(0, p.getInventory().size());
    }

    @Test
    void fullInventory_returnsInventoryFullAndRefunds() {
        Player p = new Player(1, "Pj", 50, 100, 1);
        Shop shop = shopWith(water, 2);
        assertEquals(PurchaseResult.SUCCESS, shop.buyItem(p, water));
        assertEquals(PurchaseResult.INVENTORY_FULL, shop.buyItem(p, water));
        assertEquals(40, p.getMoney());
        assertEquals(1, shop.quantityOf(water));
    }

    @Test
    void lastUnit_removesItemFromStock() {
        Player p = new Player(1, "Pj", 50, 100, 3);
        Shop shop = shopWith(water, 1);
        shop.buyItem(p, water);
        assertTrue(shop.getStock().isEmpty());
        assertEquals(PurchaseResult.NOT_IN_STOCK, shop.buyItem(p, water));
    }

    @Test
    void quantityTwo_allowsTwoPurchases() {
        Player p = new Player(1, "Pj", 50, 100, 3);
        Shop shop = shopWith(water, 2);
        assertEquals(PurchaseResult.SUCCESS, shop.buyItem(p, water));
        assertEquals(PurchaseResult.SUCCESS, shop.buyItem(p, water));
        assertEquals(2, p.getInventory().countOf(water));
        assertEquals(PurchaseResult.NOT_IN_STOCK, shop.buyItem(p, water));
    }
}
