package isla.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import isla.RecordingRenderer;
import org.junit.jupiter.api.Test;

/** The new Renderer methods must be harmless for renderers that do not care (the game and its tests). */
class RendererDefaultsTest {

    @Test
    void setFrameIsIgnoredByDefault() {
        RecordingRenderer r = new RecordingRenderer();
        r.setFrame(42);
        assertEquals(0, r.calls.size());
    }

    @Test
    void drawPanelFallsBackToTheTwoOriginalRectangles() {
        RecordingRenderer r = new RecordingRenderer();
        r.drawPanel(20, 12, 440, 182, "#F5E9C8", "#B5651D");
        assertEquals(2, r.calls.size());
        assertEquals("rect 20,12,440,182,#B5651D", r.calls.get(0));
        assertEquals("rect 22,14,436,178,#F5E9C8", r.calls.get(1));
    }
}
