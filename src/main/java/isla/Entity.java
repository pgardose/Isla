package isla;

/**
 * Entity is the abstract base class for anything that exists on the island
 * map and needs a position, a sprite and a per-frame update/render cycle.
 *
 * OOP note (ABSTRACTION): Entity defines the shared shape of every on-screen
 * object without committing to what it is. It cannot be instantiated.
 *
 * OOP note (ENCAPSULATION): x, y and sprite are protected, so only the Entity
 * family changes them directly.
 */
public abstract class Entity {

    protected int x;
    protected int y;
    protected String sprite;

    protected Entity(int x, int y, String sprite) {
        this.x = x;
        this.y = y;
        this.sprite = sprite;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getSprite() {
        return sprite;
    }

    /** Advances this entity by one game tick. */
    public abstract void update();

    /** Draws this entity through the Renderer (it never knows about JavaFX). */
    public abstract void render(Renderer renderer);

    @Override
    public String toString() {
        return String.format("%s@(%d,%d)", getClass().getSimpleName(), x, y);
    }
}
