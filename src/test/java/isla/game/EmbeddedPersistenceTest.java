package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.dao.DataAccessException;
import isla.dao.Database;
import isla.dao.Repositories;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The built-in (embedded) database: real files on disk, no server needed. */
class EmbeddedPersistenceTest {

    @TempDir
    Path dir;

    private Repositories open(Database db) {
        db.initializeSchema();
        return Repositories.jdbc(db);
    }

    @Test
    void progressSurvivesShuttingTheDatabaseDownAndReopeningIt() throws IOException {
        Path data = dir.resolve("isla-data");
        Database db = Database.embedded(data);
        Repositories repos = open(db);
        GameSession s = GameBootstrap.start(repos, true);
        s.getPlayer().earn(33);
        s.handle(Action.SAVE);
        db.shutdown();

        try (Stream<Path> files = Files.list(data)) {
            assertTrue(files.findAny().isPresent(), "database files should be on disk");
        }
        Database reopened = Database.embedded(data);
        Repositories repos2 = open(reopened);
        assertTrue(GameBootstrap.hasSave(repos2));
        assertEquals(83, GameBootstrap.start(repos2, false).getPlayer().getMoney());
        reopened.shutdown();
    }

    @Test
    void createsTheFolderWhenItDoesNotExist() {
        Path data = dir.resolve("a").resolve("b").resolve("isla-data");
        Database db = Database.embedded(data);
        open(db);
        assertTrue(Files.isDirectory(data));
        db.shutdown();
    }

    @Test
    void reportsAClearErrorWhenTheFolderCannotBeCreated() throws IOException {
        Path blocker = Files.writeString(dir.resolve("not-a-folder"), "x");
        assertThrows(DataAccessException.class, () -> Database.embedded(blocker));
    }

    @Test
    void shutdownOnANonEmbeddedDatabase_doesNothing_andDoesNotConnect() {
        Database remote = new Database("jdbc:mysql://localhost:1/isla", "u", "p");
        assertDoesNotThrow(remote::shutdown);
    }
}
