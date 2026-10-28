package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.RecordingRenderer;
import isla.dao.Repositories;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The frame counter reaches AppFlow.render without changing what the menu says. */
class AppFlowFrameTest {

    @TempDir
    Path dir;

    private AppFlow flow;

    @BeforeEach
    void setUp() {
        Path settingsFile = dir.resolve("settings.properties");
        flow = new AppFlow(Repositories.inMemory(), Settings.load(settingsFile), settingsFile);
    }

    @Test
    void renderWithoutAFrame_drawsTheSameAsFrameZero() {
        RecordingRenderer plain = new RecordingRenderer();
        RecordingRenderer zero = new RecordingRenderer();
        flow.render(plain);
        flow.render(zero, 0);
        assertEquals(zero.calls, plain.calls);
    }

    @Test
    void menuTextAndCursor_areTheSameOnEveryFrame() {
        for (long frame : new long[] {0, 1, 37, 600, 100_000}) {
            RecordingRenderer r = new RecordingRenderer();
            flow.render(r, frame);
            assertTrue(r.anyTextContains("> New Game"), "frame " + frame + ": " + r.texts);
            assertTrue(r.anyTextContains("(no saved game)"), "frame " + frame + ": " + r.texts);
            assertTrue(r.anyTextContains("A 2D Island Trading Adventure"), "frame " + frame + ": " + r.texts);
        }
    }
}
