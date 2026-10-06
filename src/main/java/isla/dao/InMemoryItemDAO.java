package isla.dao;

import isla.Item;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/** Items are immutable, so they are stored as they are. */
public class InMemoryItemDAO implements ItemDAO {

    private final TreeMap<Integer, Item> store = new TreeMap<>();

    @Override
    public void save(Item item) {
        store.put(item.getId(), item);
    }

    @Override
    public Optional<Item> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Item> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void delete(int id) {
        store.remove(id);
    }
}
