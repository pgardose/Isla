package isla;

import java.util.HashSet;
import java.util.Set;

/**
 * The user-controlled character: holds money, stamina and an Inventory.
 *
 * OOP note (INHERITANCE): extends Entity and adds everything player-specific.
 *
 * OOP note (ENCAPSULATION): money and stamina are private. Money only changes
 * through spend() (which validates funds) or earn(); stamina only through
 * restoreStamina()/expendStamina()/move(). "Can this purchase happen" and
 * "stamina is never negative" are enforced in exactly one place.
 */
public class Player extends Entity {

    /** Walking this many tiles costs one stamina point. */
    public static final int STEPS_PER_STAMINA = 10;

    private final int id;
    private final String name;
    private int money;
    private int stamina;
    private final int maxStamina;
    private final Inventory inventory;
    private final Set<String> unlockedAreas = new HashSet<>();
    private int stepsSinceDrain;

    public Player(int id, String name, int startingMoney, int maxStamina, int inventoryCapacity) {
        super(0, 0, "player_sprite");
        this.id = id;
        this.name = name;
        this.money = startingMoney;
        this.maxStamina = maxStamina;
        this.stamina = maxStamina;
        this.inventory = new Inventory(inventoryCapacity);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMoney() {
        return money;
    }

    public int getStamina() {
        return stamina;
    }

    public int getMaxStamina() {
        return maxStamina;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Set<String> getUnlockedAreas() {
        return unlockedAreas;
    }

    /**
     * Moves by a tile offset (the world checks collisions first) and drains
     * one stamina point every STEPS_PER_STAMINA steps.
     */
    public void move(int dx, int dy) {
        this.x += dx;
        this.y += dy;
        stepsSinceDrain++;
        if (stepsSinceDrain >= STEPS_PER_STAMINA) {
            stepsSinceDrain = 0;
            stamina = Math.max(0, stamina - 1);
        }
    }

    public boolean isExhausted() {
        return stamina <= 0;
    }

    /** Places the player at a tile (spawn point or loaded save). */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** Restores saved state; stamina is clamped to [0, maxStamina]. */
    public void restoreState(int money, int stamina, int x, int y) {
        if (money < 0) {
            throw new IllegalArgumentException("Money cannot be negative");
        }
        this.money = money;
        this.stamina = Math.max(0, Math.min(maxStamina, stamina));
        this.x = x;
        this.y = y;
        this.stepsSinceDrain = 0;
    }

    /** Deducts money if affordable; returns false (changing nothing) otherwise. */
    public boolean spend(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (amount > money) {
            return false;
        }
        money -= amount;
        return true;
    }

    public void earn(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        money += amount;
    }

    public void restoreStamina(int amount) {
        stamina = Math.min(maxStamina, stamina + amount);
    }

    public boolean expendStamina(int amount) {
        if (amount > stamina) {
            return false;
        }
        stamina -= amount;
        return true;
    }

    public void unlockArea(String areaName) {
        unlockedAreas.add(areaName);
    }

    public boolean hasUnlocked(String areaName) {
        return unlockedAreas.contains(areaName);
    }

    @Override
    public void update() {
        // Input is handled by GameSession; nothing per-tick yet.
    }

    @Override
    public void render(Renderer renderer) {
        renderer.drawSprite(x, y, sprite);
    }
}
