package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsTest {

    @TempDir
    Path dir;

    @Test
    void defaults_autosaveOn() {
        assertTrue(new Settings().isAutosave());
    }

    @Test
    void missingFile_givesDefaults() {
        assertTrue(Settings.load(dir.resolve("nope.properties")).isAutosave());
    }

    @Test
    void roundTrip_keepsAutosaveOff() throws IOException {
        Path file = dir.resolve("settings.properties");
        Settings s = new Settings();
        s.setAutosave(false);
        s.save(file);
        assertFalse(Settings.load(file).isAutosave());
        s.setAutosave(true);
        s.save(file);
        assertTrue(Settings.load(file).isAutosave());
    }

    @Test
    void garbageValue_fallsBackToDefault() throws IOException {
        Path file = dir.resolve("settings.properties");
        Files.writeString(file, "autosave=banana\n");
        assertTrue(Settings.load(file).isAutosave());
    }

    @Test
    void unreadableContent_fallsBackToDefault() throws IOException {
        Path file = dir.resolve("settings.properties");
        Files.write(file, new byte[] {(byte) 0xFF, (byte) 0xFE, 0x00, 0x5C, 0x75, 0x00});
        assertTrue(Settings.load(file).isAutosave());
    }

    @Test
    void loadingADirectory_fallsBackToDefault() {
        assertTrue(Settings.load(dir).isAutosave());
    }
}
