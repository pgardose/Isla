package isla.ui;

import isla.dao.Storage;
import isla.dao.StorageOpener;
import isla.game.Action;
import isla.game.AppFlow;
import isla.game.AppPaths;
import isla.game.GameSession;
import isla.game.Settings;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** JavaFX entry point: window, game loop and keyboard input. */
public class IslaApp extends Application {

    private static final int SCALE = 2;
    private static final long MOVE_REPEAT_NANOS = 120_000_000L;

    private AppFlow flow;
    private final Deque<Action> heldDirections = new ArrayDeque<>();
    private final Set<KeyCode> pressedKeys = new HashSet<>();
    private long lastRepeat;
    private long frame;
    private Storage storage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // MySQL if db.properties points at a working server, else the built-in database in isla-data/.
        AppPaths paths = AppPaths.forThisComputer();
        storage = StorageOpener.open(paths.dbProperties(), paths.dataFolder());
        Path settingsFile = paths.settingsFile();
        flow = new AppFlow(storage.repos(), Settings.load(settingsFile), settingsFile);
        if (!storage.notice().isEmpty()) {
            System.err.println(storage.notice());
            flow.setNotice(storage.notice());
        }
        if (getParameters().getRaw().contains("--new")) {
            flow.startNewGame(); // --new skips the menu and starts over
        }

        Canvas canvas = new Canvas(GameSession.SCREEN_WIDTH * SCALE, GameSession.SCREEN_HEIGHT * SCALE);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.scale(SCALE, SCALE);
        JavaFxRenderer renderer = new JavaFxRenderer(gc, SCALE);

        Scene scene = new Scene(new StackPane(canvas));
        scene.setOnKeyPressed(e -> {
            Action action = KeyBindings.actionFor(e.getCode());
            if (action == null) {
                return;
            }
            e.consume();
            if (!pressedKeys.add(e.getCode())) {
                return; // ignore OS key-repeat; movement repeat is timed below
            }
            if (action.isDirection()) {
                heldDirections.remove(action);
                heldDirections.addLast(action);
                lastRepeat = System.nanoTime();
            }
            flow.handle(action);
        });
        scene.setOnKeyReleased(e -> {
            pressedKeys.remove(e.getCode());
            Action action = KeyBindings.actionFor(e.getCode());
            if (action != null) {
                heldDirections.remove(action);
            }
        });

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Held-key repeat is for walking only; in menus one press moves one step.
                if (flow.isPlaying() && !heldDirections.isEmpty() && now - lastRepeat >= MOVE_REPEAT_NANOS) {
                    lastRepeat = now;
                    flow.handle(heldDirections.peekLast());
                }
                flow.render(renderer, frame++);
                if (flow.isQuitRequested()) {
                    Platform.exit();
                }
            }
        }.start();

        stage.setOnCloseRequest(e -> flow.onWindowClose()); // autosave if on and playing

        stage.setTitle("Isla");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (storage != null) {
            storage.shutdown(); // flush the built-in database files
        }
    }
}
