package isla.dao;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the shared DAO contract against a real MySQL/MariaDB server.
 * Skipped unless -Disla.test.jdbcUrl is given, e.g.
 *   mvn test -Disla.test.jdbcUrl=jdbc:mysql://localhost:3306/isla_test
 *            -Disla.test.jdbcUser=isla -Disla.test.jdbcPassword=isla
 * WARNING: the tables in that database are emptied before every test.
 */
class JdbcDaoTest extends DaoContractTest {

    @Override
    protected void createDaos() {
        Database db = TestDatabases.cleanOrNull();
        assumeTrue(db != null, "set -Disla.test.jdbcUrl to run the MySQL DAO tests");
        items = new JdbcItemDAO(db);
        players = new JdbcPlayerDAO(db, items);
        vendors = new JdbcVendorDAO(db, items);
        quests = new JdbcQuestDAO(db);
    }
}
