package isla.dao;

/** The four DAOs bundled together, either all in memory or all on MySQL. */
public final class Repositories {

    private final ItemDAO items;
    private final PlayerDAO players;
    private final VendorDAO vendors;
    private final QuestDAO quests;
    private final boolean persistent;

    private Repositories(ItemDAO items, PlayerDAO players, VendorDAO vendors, QuestDAO quests, boolean persistent) {
        this.items = items;
        this.players = players;
        this.vendors = vendors;
        this.quests = quests;
        this.persistent = persistent;
    }

    /** Fast, database-free storage. Progress is lost when the program exits. */
    public static Repositories inMemory() {
        ItemDAO items = new InMemoryItemDAO();
        return new Repositories(items, new InMemoryPlayerDAO(items), new InMemoryVendorDAO(items),
                new InMemoryQuestDAO(), false);
    }

    /** MySQL-backed storage (the schema must already exist; see Database.initializeSchema). */
    public static Repositories jdbc(Database db) {
        ItemDAO items = new JdbcItemDAO(db);
        return new Repositories(items, new JdbcPlayerDAO(db, items), new JdbcVendorDAO(db, items),
                new JdbcQuestDAO(db), true);
    }

    public ItemDAO items() {
        return items;
    }

    public PlayerDAO players() {
        return players;
    }

    public VendorDAO vendors() {
        return vendors;
    }

    public QuestDAO quests() {
        return quests;
    }

    /** True if progress survives the program exiting. */
    public boolean isPersistent() {
        return persistent;
    }
}
