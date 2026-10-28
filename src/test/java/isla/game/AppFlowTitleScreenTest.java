package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.RecordingRenderer;
import isla.dao.Repositories;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The title scene as seen through AppFlow: menu on top of it, other screens reusing it. */
class AppFlowTitleScreenTest {

    private static final String SUBTITLE = "A 2D Island Trading Adventure";

    @TempDir
    Path dir;

    private AppFlow flow;

    @BeforeEach
    void setUp() {
        Path settingsFile = dir.resolve("settings.properties");
        flow = new AppFlow(Repositories.inMemory(), Settings.load(settingsFile), settingsFile);
        flow.setNotice("No database - progress will not be saved.");
    }

    private RecordingRenderer render(long frame) {
        RecordingRenderer r = new RecordingRenderer();
        flow.render(r, frame);
        return r;
    }

    private void open(AppFlow.MenuItem item) {
        while (flow.menuItems().get(flow.getMenuCursor()) != item) {
            flow.handle(Action.DOWN);
        }
        flow.handle(Action.CONFIRM);
    }

    private static boolean hasBackground(RecordingRenderer r) {
        long fullWidthBands = r.calls.stream().filter(c -> c.startsWith("rect 0,") && c.contains(",480,")).count();
        return fullWidthBands >= 20; // sky and sea are drawn as many full-width bands
    }

    // ---- menu ----

    @Test
    void menu_isDrawnOverTheTitleScene_withTheLogoOnTop() {
        RecordingRenderer r = render(0);
        assertTrue(hasBackground(r));
        assertTrue(r.count("rect ") > 100, "scene should be many rectangles, got " + r.count("rect "));
        assertTrue(r.calls.stream().anyMatch(c -> c.endsWith("," + TitleScene.LOGO_FACE)), "no logo letters");
        assertTrue(r.anyTextContains(SUBTITLE));
    }

    @Test
    void menu_scenesAnimate_butTheMenuTextDoesNot() {
        RecordingRenderer early = render(0);
        RecordingRenderer later = render(900);
        assertNotEquals(early.calls, later.calls, "the picture is identical 15 seconds later");
        assertEquals(early.texts, later.texts);
    }

    @Test
    void menu_panelIsSemiTransparent_soTheSceneShowsThrough() {
        List<String> translucent = render(0).calls.stream()
                .filter(c -> c.startsWith("rect ") && c.split(",")[4].length() == 9 && c.split(",")[4].startsWith(GameSession.PAPER))
                .toList();
        assertFalse(translucent.isEmpty(), "no semi-transparent (#RRGGBBAA) panel on the menu");
    }

    @Test
    void menu_everyLineOfText_exceptTheLogosOwn_sitsOnAPanel() {
        RecordingRenderer r = render(0);
        for (int i = 0; i < r.calls.size(); i++) {
            String call = r.calls.get(i);
            if (!call.startsWith("text ")) {
                continue;
            }
            String[] p = call.substring(5).split(",", 3);
            String text = p[2];
            if (text.equals(SUBTITLE) || text.equals("ISLA")) {
                continue; // the logo block has its own drop shadow instead of a panel
            }
            int tx = Integer.parseInt(p[0]) + 2;
            int ty = Integer.parseInt(p[1]) - 3;
            boolean covered = false;
            for (int j = 0; j < i && !covered; j++) {
                String q = r.calls.get(j);
                if (!q.startsWith("rect ")) {
                    continue;
                }
                String[] f = q.substring(5).split(",");
                int x = Integer.parseInt(f[0]);
                int y = Integer.parseInt(f[1]);
                int w = Integer.parseInt(f[2]);
                int h = Integer.parseInt(f[3]);
                covered = f[4].startsWith(GameSession.PAPER) && tx >= x && tx < x + w && ty >= y && ty < y + h;
            }
            assertTrue(covered, "'" + text + "' is drawn straight onto the scene with no panel behind it");
        }
    }

    @Test
    void menu_keepsCursorMarker_noSavedGameLabel_andStartupNotice() {
        RecordingRenderer r = render(0);
        assertTrue(r.anyTextContains("> New Game"), String.valueOf(r.texts));
        assertTrue(r.anyTextContains("Continue (no saved game)"), String.valueOf(r.texts));
        assertTrue(r.anyTextContains("No database - progress will not be saved."), String.valueOf(r.texts));
    }

    // ---- other screens reuse the background ----

    @Test
    void howToPlay_settings_andConfirm_reuseTheBackground_withoutTheLogo() {
        open(AppFlow.MenuItem.HOW_TO_PLAY);
        RecordingRenderer help = render(0);
        assertTrue(hasBackground(help), "How to Play lost the background");
        assertFalse(help.calls.stream().anyMatch(c -> c.endsWith("," + TitleScene.LOGO_FACE)));
        assertTrue(help.anyTextContains("HOW TO PLAY"));

        flow.handle(Action.CANCEL);
        open(AppFlow.MenuItem.SETTINGS);
        RecordingRenderer settings = render(0);
        assertTrue(hasBackground(settings), "Settings lost the background");
        assertTrue(settings.anyTextContains("SETTINGS"));
        assertNotEquals(render(0).calls, render(900).calls, "the Settings background does not animate");
    }
}
