package isla.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Which storage the game ends up using, and what the player is told. */
class StorageOpenerTest {

    @TempDir
    Path dir;

    private Path props(String text) throws IOException {
        return Files.writeString(dir.resolve("db.properties"), text);
    }

    @Test
    void noConfigFile_usesTheBuiltInDatabase_silently() {
        Path data = dir.resolve("isla-data");
        Storage storage = StorageOpener.open(dir.resolve("db.properties"), data);
        assertTrue(storage.repos().isPersistent());
        assertEquals("", storage.notice());
        assertTrue(Files.isDirectory(data));
        storage.shutdown();
    }

    @Test
    void configuredAndReachable_usesThatDatabase_andLeavesTheBuiltInOneAlone() throws IOException {
        Path file = props("url=jdbc:h2:mem:opener_reachable;MODE=MySQL;DB_CLOSE_DELAY=-1\nuser=\npassword=\n");
        Path data = dir.resolve("isla-data");
        Storage storage = StorageOpener.open(file, data);
        assertTrue(storage.repos().isPersistent());
        assertEquals("", storage.notice());
        assertTrue(Files.notExists(data), "the built-in database must not be created when the configured one works");
        storage.shutdown();
    }

    @Test
    void configuredButUnreachable_fallsBackToTheBuiltInDatabase_withANotice() throws IOException {
        Path file = props("url=jdbc:mysql://localhost:1/isla\nuser=root\npassword=\n");
        Storage storage = StorageOpener.open(file, dir.resolve("isla-data"));
        assertTrue(storage.repos().isPersistent());
        assertTrue(storage.notice().contains("MySQL"), storage.notice());
        assertTrue(storage.notice().contains("built-in"), storage.notice());
        storage.shutdown();
    }

    @Test
    void configFileWithoutAUrl_isTreatedAsUnreachable() throws IOException {
        Path file = props("user=root\n");
        Storage storage = StorageOpener.open(file, dir.resolve("isla-data"));
        assertTrue(storage.repos().isPersistent());
        assertTrue(storage.notice().contains("built-in"), storage.notice());
        storage.shutdown();
    }

    @Test
    void builtInDatabaseCannotOpen_fallsBackToMemory_andSaysProgressWillNotBeSaved() throws IOException {
        Path blocker = Files.writeString(dir.resolve("isla-data"), "i am a file, not a folder");
        Storage storage = StorageOpener.open(dir.resolve("db.properties"), blocker);
        assertFalse(storage.repos().isPersistent());
        assertTrue(storage.notice().contains("will not be saved"), storage.notice());
        assertDoesNotThrow(storage::shutdown);
    }

    @Test
    void unreachableMySqlAndBrokenBuiltIn_stillStartsInMemory() throws IOException {
        Path file = props("url=jdbc:mysql://localhost:1/isla\n");
        Path blocker = Files.writeString(dir.resolve("isla-data"), "x");
        Storage storage = StorageOpener.open(file, blocker);
        assertFalse(storage.repos().isPersistent());
        assertTrue(storage.notice().contains("will not be saved"), storage.notice());
    }
}
