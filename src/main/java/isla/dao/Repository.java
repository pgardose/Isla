package isla.dao;

import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object contract. Game logic depends on these interfaces
 * only, so SQL stays out of the model (further ABSTRACTION): an in-memory and
 * a MySQL implementation can be swapped without touching the game.
 */
public interface Repository<T> {

    /** Inserts or updates the entity (matched by id). */
    void save(T entity);

    Optional<T> findById(int id);

    /** All entities, ordered by id. */
    List<T> findAll();

    void delete(int id);
}
