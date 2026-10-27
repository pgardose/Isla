package isla.game;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Player-adjustable options, kept in a small settings.properties file so they
 * survive restarts. Anything missing or unreadable falls back to the defaults,
 * so a bad file can never stop the game from starting.
 *
 * OOP note (ENCAPSULATION): callers use isAutosave()/setAutosave(); how and
 * where the value is stored stays private to this class.
 */
public class Settings {

    private static final String AUTOSAVE_KEY = "autosave";

    private boolean autosave = true;

    public boolean isAutosave() {
        return autosave;
    }

    public void setAutosave(boolean autosave) {
        this.autosave = autosave;
    }

    /** Reads settings from file; returns the defaults if it is missing or unreadable. */
    public static Settings load(Path file) {
        Settings settings = new Settings();
        if (file == null || !Files.isRegularFile(file)) {
            return settings;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException | IllegalArgumentException e) {
            return settings;
        }
        String value = props.getProperty(AUTOSAVE_KEY);
        if ("false".equalsIgnoreCase(value)) {
            settings.autosave = false;
        }
        return settings;
    }

    public void save(Path file) throws IOException {
        Properties props = new Properties();
        props.setProperty(AUTOSAVE_KEY, Boolean.toString(autosave));
        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, "Isla settings");
        }
    }
}
