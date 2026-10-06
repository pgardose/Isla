package isla;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Inventory holds a Player's Items, up to a fixed capacity.
 *
 * OOP note (ENCAPSULATION): the item list is private; getItems() returns an
 * unmodifiable view so outside code can only change it through
 * addItem()/removeItem(), which enforce the capacity rule.
 */
public class Inventory {

    private final List<Item> items = new ArrayList<>();
    private final int capacity;

    public Inventory(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public int size() {
        return items.size();
    }

    public boolean isFull() {
        return items.size() >= capacity;
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    /** Adds an item; returns false (and changes nothing) if the inventory is full. */
    public boolean addItem(Item item) {
        if (isFull()) {
            return false;
        }
        items.add(item);
        return true;
    }

    /** Removes one matching item; returns false if there was none. */
    public boolean removeItem(Item item) {
        return items.remove(item);
    }

    public int countOf(Item item) {
        return (int) items.stream().filter(i -> i.equals(item)).count();
    }

    public Optional<Item> findByName(String name) {
        return items.stream().filter(i -> i.getName().equals(name)).findFirst();
    }
}
