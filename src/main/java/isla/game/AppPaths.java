package isla.game;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Where Isla keeps its files (saved game, settings, optional MySQL config).
 *
 * Run from the project folder (or any folder that already has Isla's files) and everything stays
 * right there, as it always did. Launched from anywhere else, for example by double-clicking a
 * packaged app, it uses a ".isla" folder in the user's home, which is always writable and does
 * not depend on the working directory.
 */
public record AppPaths(Path dataFolder, Path settingsFile, Path dbProperties) {

    /** Only looks; creates nothing. */
    public static AppPaths locate(Path workingDir, Path userHome) {
        boolean projectFolder = Files.exists(workingDir.resolve("pom.xml"))
                || Files.exists(workingDir.resolve("isla-data"))
                || Files.exists(workingDir.resolve("db.properties"))
                || Files.exists(workingDir.resolve("settings.properties"));
        Path base = projectFolder ? workingDir : userHome.resolve(".isla");
        return new AppPaths(base.resolve("isla-data"), base.resolve("settings.properties"),
                base.resolve("db.properties"));
    }

    public static AppPaths forThisComputer() {
        return locate(Path.of("").toAbsolutePath(), Path.of(System.getProperty("user.home")));
    }
}
