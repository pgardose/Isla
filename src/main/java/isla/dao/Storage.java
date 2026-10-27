package isla.dao;

/**
 * Where the game keeps its data for this run: the repositories, a line to show the
 * player if something is worth telling them (empty when all is well), and how to close it.
 */
public record Storage(Repositories repos, String notice, Runnable closer) {

    /** Call when the program ends; flushes the built-in database. Safe to call for any storage. */
    public void shutdown() {
        closer.run();
    }
}
