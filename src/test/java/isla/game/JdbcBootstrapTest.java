package isla.game;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import isla.dao.Database;
import isla.dao.Repositories;
import isla.dao.TestDatabases;

/** Same save/continue tests, on a real MySQL/MariaDB (skipped without -Disla.test.jdbcUrl). */
class JdbcBootstrapTest extends GameBootstrapTest {

    @Override
    protected Repositories newRepos() {
        Database db = TestDatabases.cleanOrNull();
        assumeTrue(db != null, "set -Disla.test.jdbcUrl to run the MySQL tests");
        return Repositories.jdbc(db);
    }
}
