package isla.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Test helper: connects to the database named by -Disla.test.jdbcUrl and empties it. With no property
 * it uses a throwaway in-memory H2 database, so these tests run on every machine.
 */
public final class TestDatabases {

    private TestDatabases() {
    }

    public static Database cleanOrNull() {
        String url = System.getProperty("isla.test.jdbcUrl");
        boolean builtIn = url == null;
        if (builtIn) {
            url = "jdbc:h2:mem:isla_test;MODE=MySQL;DB_CLOSE_DELAY=-1";
        }
        Database db = new Database(url, System.getProperty("isla.test.jdbcUser", builtIn ? "" : "isla"),
                System.getProperty("isla.test.jdbcPassword", builtIn ? "" : "isla"));
        db.initializeSchema();
        try (Connection c = db.connection(); Statement st = c.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS=0");
            for (String table : List.of("quest_options", "debug_quests", "shop_stock", "vendors", "inventory", "unlocked_areas", "player_stats",
                    "players", "quests", "items")) {
                st.execute("TRUNCATE TABLE " + table);
            }
            st.execute("SET FOREIGN_KEY_CHECKS=1");
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return db;
    }
}
