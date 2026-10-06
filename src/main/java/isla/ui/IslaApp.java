package isla.ui;

import isla.dao.Database;
import isla.dao.Repositories;
import isla.game.Action;
import isla.game.GameBootstrap;
import isla.game.GameSession;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
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

    private GameSession session;
    private final Deque<Action> heldDirections = new ArrayDeque<>();
    private final Set<KeyCode> pressedKeys = new HashSet<>();
    private long lastRepeat;
    private String startupNotice = "";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Repositories repos = openRepositories();
        boolean newGame = getParameters().getRaw().contains("--new");
        session = GameBootstrap.start(repos, newGame);
        if (!startupNotice.isEmpty()) {
            System.err.println(startupNotice);
            session.notice(startupNotice);
        }

        Canvas canvas = new Canvas(GameSession.SCREEN_WIDTH * SCALE, GameSession.SCREEN_HEIGHT * SCALE);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.scale(SCALE, SCALE);
        JavaFxRenderer renderer = new JavaFxRenderer(gc);

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
            session.handle(action);
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
                if (!heldDirections.isEmpty() && now - lastRepeat >= MOVE_REPEAT_NANOS) {
                    lastRepeat = now;
                    session.handle(heldDirections.peekLast());
                }
                session.render(renderer);
            }
        }.start();

        stage.setOnCloseRequest(e -> {
            if (repos.isPersistent()) {
                try {
                    GameBootstrap.save(repos, session); // autosave on exit
                } catch (RuntimeException ex) {
                    System.err.println("Autosave failed: " + ex.getMessage());
                }
            }
        });

        stage.setTitle("Isla");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Uses MySQL when db.properties exists and the database is reachable;
     * otherwise falls back to in-memory storage and tells the player.
     */
    private Repositories openRepositories() {
        Path config = Path.of("db.properties");
        if (!Files.exists(config)) {
            startupNotice = "No database configured - progress will not be saved.";
            return Repositories.inMemory();
        }
        try {
            Database db = Database.fromProperties(config);
            db.initializeSchema();
            return Repositories.jdbc(db);
        } catch (RuntimeException e) {
            startupNotice = "No database - progress will not be saved.";
            System.err.println("Database unavailable: " + e.getMessage());
            return Repositories.inMemory();
        }
    }
}
