package isla.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Test helper: connects to the database named by -Disla.test.jdbcUrl (or returns null) and empties it. */
public final class TestDatabases {

    private TestDatabases() {
    }

    public static Database cleanOrNull() {
        String url = System.getProperty("isla.test.jdbcUrl");
        if (url == null) {
            return null;
        }
        Database db = new Database(url, System.getProperty("isla.test.jdbcUser", "isla"),
                System.getProperty("isla.test.jdbcPassword", "isla"));
        db.initializeSchema();
        try (Connection c = db.connection(); Statement st = c.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS=0");
            for (String table : List.of("shop_stock", "vendors", "inventory", "unlocked_areas",
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
