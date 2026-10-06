package isla.dao;

import isla.Item;
import isla.Shop;
import isla.Vendor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class InMemoryVendorDAO implements VendorDAO {

    private record Row(String name, int x, int y, String sprite, String dialogue,
                       LinkedHashMap<Integer, Integer> stock) {
    }

    private final ItemDAO items;
    private final TreeMap<Integer, Row> store = new TreeMap<>();

    public InMemoryVendorDAO(ItemDAO items) {
        this.items = items;
    }

    @Override
    public void save(Vendor v) {
        LinkedHashMap<Integer, Integer> stock = new LinkedHashMap<>();
        for (Item item : v.getShop().getStock()) {
            stock.put(item.getId(), v.getShop().quantityOf(item));
        }
        store.put(v.getId(), new Row(v.getName(), v.getX(), v.getY(), v.getSprite(), v.getDialogue(), stock));
    }

    @Override
    public Optional<Vendor> findById(int id) {
        return Optional.ofNullable(store.get(id)).map(row -> rebuild(id, row));
    }

    @Override
    public List<Vendor> findAll() {
        List<Vendor> all = new ArrayList<>();
        store.forEach((id, row) -> all.add(rebuild(id, row)));
        return all;
    }

    @Override
    public void delete(int id) {
        store.remove(id);
    }

    private Vendor rebuild(int id, Row row) {
        Shop shop = new Shop();
        for (Map.Entry<Integer, Integer> e : row.stock().entrySet()) {
            Item item = items.findById(e.getKey()).orElseThrow(
                    () -> new IllegalStateException("Stock refers to unknown item " + e.getKey()));
            shop.addStock(item, e.getValue());
        }
        return new Vendor(id, row.name(), row.x(), row.y(), row.sprite(), row.dialogue(), shop);
    }
}
