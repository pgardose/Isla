package isla.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Which storage the game ends up using, and what the player is told. */
class StorageOpenerTest {

    @TempDir
    Path dir;

    // The opener prints why it fell back (useful when playing); here that is expected, so keep the test log quiet.
    private final PrintStream realErr = System.err;
    private final ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();

    @BeforeEach
    void captureErr() {
        System.setErr(new PrintStream(capturedErr));
    }

    @AfterEach
    void restoreErr() {
        System.setErr(realErr);
    }

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

    @Test
    void whyItFellBack_isStillPrintedForTheDeveloper() throws IOException {
        Storage storage = StorageOpener.open(props("user=root\n"), dir.resolve("isla-data"));
        assertTrue(capturedErr.toString().contains("MySQL unavailable"), capturedErr.toString());
        storage.shutdown();
    }
}
