package isla;

/**
 * The entry point for the packaged jar. JavaFX refuses to start from a main class that itself extends
 * Application when run from a plain jar ("JavaFX runtime components are missing"), so the jar's
 * Main-Class is this tiny class that just starts the real app.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        isla.ui.IslaApp.main(args);
    }
}
