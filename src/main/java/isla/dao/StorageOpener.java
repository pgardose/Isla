package isla.dao;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Chooses where progress is saved, trying the best option first:
 *   1. the MySQL database named in db.properties (if that file exists and the server answers),
 *   2. the built-in database in the data folder (works with nothing installed),
 *   3. memory only, as a last resort (progress is lost when the game closes).
 * It never throws: whatever goes wrong, the game still starts, and the notice says what happened.
 */
public final class StorageOpener {

    private StorageOpener() {
    }

    public static Storage open(Path dbProperties, Path dataFolder) {
        String notice = "";
        if (Files.exists(dbProperties)) {
            try {
                Database db = Database.fromProperties(dbProperties);
                db.initializeSchema();
                return new Storage(Repositories.jdbc(db), "", db::shutdown);
            } catch (RuntimeException e) {
                System.err.println("MySQL unavailable: " + e.getMessage());
                notice = "MySQL unavailable - using the built-in database instead.";
            }
        }
        try {
            Database db = Database.embedded(dataFolder);
            db.initializeSchema();
            return new Storage(Repositories.jdbc(db), notice, db::shutdown);
        } catch (RuntimeException e) {
            System.err.println("Built-in database unavailable: " + e.getMessage());
            return new Storage(Repositories.inMemory(),
                    "No database available - progress will not be saved.", () -> { });
        }
    }
}
