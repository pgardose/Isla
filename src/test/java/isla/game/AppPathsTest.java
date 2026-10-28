package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Where the game keeps its files, so it works wherever it is launched from. */
class AppPathsTest {

    @TempDir
    Path dir;

    private Path work() throws IOException {
        return Files.createDirectories(dir.resolve("somewhere"));
    }

    private Path home() throws IOException {
        return Files.createDirectories(dir.resolve("home"));
    }

    @Test
    void launchedFromAnArbitraryFolder_usesAnIslaFolderInTheUsersHome() throws IOException {
        AppPaths p = AppPaths.locate(work(), home());
        Path base = home().resolve(".isla");
        assertEquals(base.resolve("isla-data"), p.dataFolder());
        assertEquals(base.resolve("settings.properties"), p.settingsFile());
        assertEquals(base.resolve("db.properties"), p.dbProperties());
    }

    @Test
    void launchedFromTheProjectFolder_keepsEverythingThere() throws IOException {
        Files.writeString(work().resolve("pom.xml"), "<project/>");
        AppPaths p = AppPaths.locate(work(), home());
        assertEquals(work().resolve("isla-data"), p.dataFolder());
        assertEquals(work().resolve("settings.properties"), p.settingsFile());
        assertEquals(work().resolve("db.properties"), p.dbProperties());
    }

    @Test
    void anExistingDataFolderOrConfigFile_alsoMeansUseThisFolder() throws IOException {
        Files.createDirectories(work().resolve("isla-data"));
        assertEquals(work().resolve("isla-data"), AppPaths.locate(work(), home()).dataFolder());

        Path other = Files.createDirectories(dir.resolve("other"));
        Files.writeString(other.resolve("db.properties"), "url=x");
        assertEquals(other.resolve("db.properties"), AppPaths.locate(other, home()).dbProperties());

        Path third = Files.createDirectories(dir.resolve("third"));
        Files.writeString(third.resolve("settings.properties"), "autosave=false");
        assertEquals(third.resolve("settings.properties"), AppPaths.locate(third, home()).settingsFile());
    }

    @Test
    void justLookingUpThePaths_createsNothing() throws IOException {
        AppPaths.locate(work(), home());
        assertTrue(Files.notExists(home().resolve(".isla")));
    }
}
