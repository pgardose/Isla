package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.RecordingRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** The title scene draws only through Renderer, so a RecordingRenderer can inspect it. */
class TitleSceneTest {

    private static final int W = GameSession.SCREEN_WIDTH;
    private static final int H = GameSession.SCREEN_HEIGHT;

    /** One recorded fillRect: "rect x,y,w,h,color". */
    private record Rect(int x, int y, int w, int h, String color) { }

    private static List<Rect> rects(RecordingRenderer r) {
        List<Rect> out = new ArrayList<>();
        for (String call : r.calls) {
            if (call.startsWith("rect ")) {
                String[] p = call.substring(5).split(",");
                out.add(new Rect(Integer.parseInt(p[0]), Integer.parseInt(p[1]),
                        Integer.parseInt(p[2]), Integer.parseInt(p[3]), p[4]));
            }
        }
        return out;
    }

    private static List<Rect> sceneAt(long frame) {
        RecordingRenderer r = new RecordingRenderer();
        TitleScene.draw(r, frame);
        return rects(r);
    }

    // ---- smoke ----

    @Test
    void draw_doesNotThrow_onAnyFrame_andActuallyDraws() {
        for (long frame : new long[] {-5, 0, 1, 59, 600, 12_345, 1_000_000_007L}) {
            RecordingRenderer r = new RecordingRenderer();
            assertDoesNotThrow(() -> TitleScene.draw(r, frame), "frame " + frame);
            assertTrue(r.count("rect ") > 20, "frame " + frame + " drew " + r.count("rect "));
        }
    }

    @Test
    void draw_staysInsideTheScreen() {
        for (long frame : new long[] {0, 90, 777, 5_000}) {
            for (Rect q : sceneAt(frame)) {
                assertTrue(q.w() > 0 && q.h() > 0, "empty rect " + q + " at frame " + frame);
                assertTrue(q.x() >= 0 && q.y() >= 0 && q.x() + q.w() <= W && q.y() + q.h() <= H,
                        "off screen " + q + " at frame " + frame);
            }
        }
    }

    // ---- sky and sea ----

    @Test
    void everyRowOfTheScreenIsPainted_byFullWidthBands() {
        boolean[] covered = new boolean[H];
        for (Rect q : sceneAt(0)) {
            if (q.x() == 0 && q.w() == W) {
                for (int y = q.y(); y < q.y() + q.h(); y++) {
                    covered[y] = true;
                }
            }
        }
        for (int y = 0; y < H; y++) {
            assertTrue(covered[y], "row " + y + " is not painted");
        }
    }

    @Test
    void sky_isAGradient_fromDarkAtTheTopToWarmAtTheHorizon() {
        Set<String> skyColors = new TreeSet<>();
        Rect top = null;
        Rect lowest = null;
        for (Rect q : sceneAt(0)) {
            if (q.x() == 0 && q.w() == W && q.y() < TitleScene.HORIZON) {
                skyColors.add(q.color());
                if (top == null || q.y() < top.y()) top = q;
                if (lowest == null || q.y() > lowest.y()) lowest = q;
            }
        }
        assertTrue(skyColors.size() >= 8, "only " + skyColors.size() + " sky colours: " + skyColors);
        assertNotEquals(top.color(), lowest.color());
    }

    @Test
    void mix_blendsTwoHexColors() {
        assertEquals("#000000", TitleScene.mix("#000000", "#FFFFFF", 0.0));
        assertEquals("#FFFFFF", TitleScene.mix("#000000", "#FFFFFF", 1.0));
        assertEquals("#808080", TitleScene.mix("#000000", "#FFFFFF", 0.5));
        assertEquals("#FF8000", TitleScene.mix("#FF0000", "#FFFF00", 0.5));
    }

    // ---- clouds ----

    @Test
    void clouds_drift_butTheSkyBehindThemDoesNot() {
        List<Rect> early = sceneAt(0);
        List<Rect> later = sceneAt(600);
        assertNotEquals(early, later, "nothing moved between frame 0 and frame 600");
        List<Rect> earlyBands = early.stream().filter(q -> q.x() == 0 && q.w() == W && q.y() < TitleScene.HORIZON).toList();
        List<Rect> laterBands = later.stream().filter(q -> q.x() == 0 && q.w() == W && q.y() < TitleScene.HORIZON).toList();
        assertEquals(earlyBands, laterBands);
    }

    @Test
    void clouds_driftSlowlyToTheRight() {
        int x0 = firstCloudX(sceneAt(0));
        int x60 = firstCloudX(sceneAt(60));
        int shift = x60 - x0;
        assertTrue(shift > 0 && shift <= 12, "a cloud moved " + shift + "px in 60 frames (1 second)");
    }

    private static int firstCloudX(List<Rect> rects) {
        return rects.stream().filter(q -> q.color().equals(TitleScene.CLOUD)).findFirst().orElseThrow().x();
    }

    @Test
    void sameFrame_alwaysDrawsTheSame() {
        assertEquals(sceneAt(321), sceneAt(321));
    }

    // ---- island ----

    @Test
    void island_hasTwoPalmTrunks_wellApart() {
        TreeSet<Integer> trunkX = new TreeSet<>();
        for (Rect q : sceneAt(0)) {
            if (q.color().equals(TitleScene.TRUNK)) {
                trunkX.add(q.x());
            }
        }
        assertFalse(trunkX.isEmpty(), "no palm trunks drawn");
        int clusters = 1;
        Integer previous = null;
        for (int x : trunkX) {
            if (previous != null && x - previous > 20) {
                clusters++;
            }
            previous = x;
        }
        assertEquals(2, clusters, "trunk x positions: " + trunkX);
    }

    @Test
    void island_isASilhouetteSittingOnTheHorizon() {
        boolean onHorizon = sceneAt(0).stream().anyMatch(q -> q.color().equals(TitleScene.ISLAND)
                && q.y() < TitleScene.HORIZON && q.y() + q.h() >= TitleScene.HORIZON);
        assertTrue(onHorizon, "no island rows cross the horizon line");
    }

    // ---- waves ----

    private static boolean isWave(Rect q) {
        return q.color().equals(TitleScene.FOAM) || q.color().equals(TitleScene.SWELL) || q.color().equals(TitleScene.CREST);
    }

    @Test
    void waves_moveFromFrameToFrame() {
        List<Rect> a = sceneAt(0).stream().filter(TitleSceneTest::isWave).toList();
        List<Rect> b = sceneAt(30).stream().filter(TitleSceneTest::isWave).toList();
        assertFalse(a.isEmpty(), "no waves drawn");
        assertNotEquals(a, b, "waves look the same at frame 0 and frame 30");
    }

    @Test
    void waves_changeGently_betweenNeighbouringFrames() {
        List<Rect> a = sceneAt(100).stream().filter(TitleSceneTest::isWave).toList();
        List<Rect> b = sceneAt(101).stream().filter(TitleSceneTest::isWave).toList();
        assertEquals(a.size(), b.size(), "the number of wave pieces must not flicker");
        for (int i = 0; i < a.size(); i++) {
            assertTrue(Math.abs(a.get(i).y() - b.get(i).y()) <= 1, a.get(i) + " -> " + b.get(i));
        }
    }

    @Test
    void waves_stayInTheSea_andTheFrontOneReachesTheBottomOfTheScreen() {
        for (long frame : new long[] {0, 45, 1_000}) {
            List<Rect> waves = sceneAt(frame).stream().filter(TitleSceneTest::isWave).toList();
            for (Rect q : waves) {
                assertTrue(q.y() >= TitleScene.HORIZON, "wave above the horizon: " + q);
            }
            assertTrue(waves.stream().anyMatch(q -> q.y() + q.h() == H), "no wave reaches the bottom edge at frame " + frame);
        }
    }

    @Test
    void waves_haveFoamOnTop_andAreDrawnInFrontOfTheIsland() {
        List<Rect> all = sceneAt(0);
        int lastIsland = -1;
        int firstWave = -1;
        for (int i = 0; i < all.size(); i++) {
            String c = all.get(i).color();
            if (c.equals(TitleScene.ISLAND) || c.equals(TitleScene.TRUNK)) lastIsland = i;
            if (isWave(all.get(i)) && firstWave < 0) firstWave = i;
        }
        assertTrue(all.stream().anyMatch(q -> q.color().equals(TitleScene.FOAM)), "no foam");
        assertTrue(firstWave > lastIsland, "waves must be drawn after the island (first wave " + firstWave + ", last island " + lastIsland + ")");
    }

    // ---- logo ----

    private static final String SUBTITLE = "A 2D Island Trading Adventure";

    private static RecordingRenderer logo() {
        RecordingRenderer r = new RecordingRenderer();
        TitleScene.drawLogo(r);
        return r;
    }

    private static List<Rect> ofColor(RecordingRenderer r, String color) {
        return rects(r).stream().filter(q -> q.color().equals(color)).toList();
    }

    private record Box(int minX, int minY, int maxX, int maxY) { }

    private static Box boundsOf(List<Rect> rs) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = 0, maxY = 0;
        for (Rect q : rs) {
            minX = Math.min(minX, q.x());
            minY = Math.min(minY, q.y());
            maxX = Math.max(maxX, q.x() + q.w());
            maxY = Math.max(maxY, q.y() + q.h());
        }
        return new Box(minX, minY, maxX, maxY);
    }

    @Test
    void logo_drawsWithoutThrowing_andStaysOnScreen() {
        RecordingRenderer r = new RecordingRenderer();
        assertDoesNotThrow(() -> TitleScene.drawLogo(r));
        assertTrue(r.count("rect ") > 10);
        for (Rect q : rects(r)) {
            assertTrue(q.x() >= 0 && q.y() >= 0 && q.x() + q.w() <= W && q.y() + q.h() <= H, "off screen: " + q);
        }
    }

    @Test
    void logo_isLarge_andCentredAcrossTheScreen() {
        Box face = boundsOf(ofColor(logo(), TitleScene.LOGO_FACE));
        assertTrue(face.maxX() - face.minX() >= 100, "logo is only " + (face.maxX() - face.minX()) + "px wide");
        assertTrue(face.maxY() - face.minY() >= 30, "logo is only " + (face.maxY() - face.minY()) + "px tall");
        int middle = (face.minX() + face.maxX()) / 2;
        assertTrue(Math.abs(middle - W / 2) <= 3, "logo centre is at x=" + middle);
    }

    @Test
    void logo_isFourSeparateLetters() {
        boolean[] column = new boolean[W];
        for (Rect q : ofColor(logo(), TitleScene.LOGO_FACE)) {
            for (int x = q.x(); x < q.x() + q.w(); x++) {
                column[x] = true;
            }
        }
        int letters = 0;
        for (int x = 0; x < W; x++) {
            if (column[x] && (x == 0 || !column[x - 1])) letters++;
        }
        assertEquals(4, letters, "I, S, L and A should be four separate blocks");
    }

    @Test
    void logo_hasADropShadow_downAndToTheRight_drawnBehindTheLetters() {
        RecordingRenderer r = logo();
        Box face = boundsOf(ofColor(r, TitleScene.LOGO_FACE));
        Box shadow = boundsOf(ofColor(r, TitleScene.LOGO_SHADOW));
        int dx = shadow.minX() - face.minX();
        int dy = shadow.minY() - face.minY();
        assertTrue(dx >= 2 && dx <= 6 && dy >= 2 && dy <= 6, "shadow offset is " + dx + "," + dy);
        List<Rect> all = rects(r);
        int lastShadow = -1;
        int firstFace = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).color().equals(TitleScene.LOGO_SHADOW)) lastShadow = i;
            if (all.get(i).color().equals(TitleScene.LOGO_FACE) && firstFace < 0) firstFace = i;
        }
        assertTrue(lastShadow < firstFace, "the shadow must be drawn before the letters");
    }

    @Test
    void subtitle_sitsUnderTheLogo_centred_withItsOwnShadow() {
        RecordingRenderer r = logo();
        Box face = boundsOf(ofColor(r, TitleScene.LOGO_FACE));
        List<String> subtitleCalls = r.calls.stream().filter(c -> c.startsWith("text ") && c.endsWith("," + SUBTITLE)).toList();
        assertEquals(2, subtitleCalls.size(), "expected a shadow copy and the real line: " + r.calls);
        for (String call : subtitleCalls) {
            String[] p = call.substring(5).split(",", 3);
            int x = Integer.parseInt(p[0]);
            int y = Integer.parseInt(p[1]);
            assertTrue(y > face.maxY(), "subtitle baseline " + y + " is not below the logo (bottom " + face.maxY() + ")");
            int textWidth = SUBTITLE.length() * 5;
            assertTrue(Math.abs(x + textWidth / 2 - W / 2) <= 6, "subtitle starts at x=" + x);
        }
    }

    @Test
    void logo_alsoDrawsTheGameNameAsPlainText() {
        assertTrue(logo().anyTextContains("ISLA"));
    }
}
