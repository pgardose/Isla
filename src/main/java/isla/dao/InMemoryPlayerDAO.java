package isla.dao;

import isla.Item;
import isla.Player;
import isla.game.SeedData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Stores plain data (not the live Player), exactly like a database row set,
 * so a loaded player is always an independent copy.
 */
public class InMemoryPlayerDAO implements PlayerDAO {

    private record Row(String name, int money, int stamina, int x, int y,
                       List<Integer> itemIds, Set<String> areas) {
    }

    private final ItemDAO items;
    private final TreeMap<Integer, Row> store = new TreeMap<>();

    public InMemoryPlayerDAO(ItemDAO items) {
        this.items = items;
    }

    @Override
    public void save(Player p) {
        List<Integer> ids = new ArrayList<>();
        for (Item item : p.getInventory().getItems()) {
            ids.add(item.getId());
        }
        store.put(p.getId(), new Row(p.getName(), p.getMoney(), p.getStamina(), p.getX(), p.getY(),
                ids, new HashSet<>(p.getUnlockedAreas())));
    }

    @Override
    public Optional<Player> findById(int id) {
        return Optional.ofNullable(store.get(id)).map(row -> rebuild(id, row));
    }

    @Override
    public List<Player> findAll() {
        List<Player> all = new ArrayList<>();
        store.forEach((id, row) -> all.add(rebuild(id, row)));
        return all;
    }

    @Override
    public void delete(int id) {
        store.remove(id);
    }

    private Player rebuild(int id, Row row) {
        Player p = new Player(id, row.name(), row.money(), SeedData.MAX_STAMINA, SeedData.INVENTORY_CAPACITY);
        p.restoreState(row.money(), row.stamina(), row.x(), row.y());
        for (int itemId : row.itemIds()) {
            p.getInventory().addItem(items.findById(itemId).orElseThrow(
                    () -> new IllegalStateException("Saved inventory refers to unknown item " + itemId)));
        }
        row.areas().forEach(p::unlockArea);
        return p;
    }
}
