package isla.game;

import isla.Renderer;
import java.util.Map;

/**
 * The animated backdrop behind the main menu: sunset sky, drifting clouds, a
 * palm island silhouette and the sea. It only calls Renderer.fillRect, so it
 * needs no JavaFX and no image or font files, and tests can watch it through a
 * recording renderer. Animation comes from the frame counter passed to draw():
 * the same frame always draws the same picture.
 */
public final class TitleScene {

    private static final int W = GameSession.SCREEN_WIDTH;
    private static final int H = GameSession.SCREEN_HEIGHT;

    /** Screen row where the sky meets the sea. */
    static final int HORIZON = 150;

    private static final String[] SKY = {"#251A52", "#6E3A8A", "#D9566A", "#FF9A4D", "#FFD58A"};
    private static final String[] SEA = {"#B8587A", "#5A3F8E", "#233A78", "#14234F"};
    private static final String SUN = "#FFE9B0";
    static final String CLOUD = "#F9B7A3";
    private static final String CLOUD_SHADE = "#C9728C";
    static final String ISLAND = "#1C1438";
    static final String TRUNK = "#150E2E";
    private static final String PALM = "#0F0A24";
    static final String CREST = "#F0A9A0";
    static final String SWELL = "#1B3170";
    static final String FOAM = "#CFE3FA";
    static final String LOGO_FACE = "#FFD54A";
    static final String LOGO_SHADOW = "#3A1450";
    private static final String SUBTITLE_COLOR = "#FFE9B0";

    private static final int SKY_BAND = 5;
    private static final int SEA_BAND = 6;
    private static final int CLOUD_MARGIN = 80; // clouds travel this far past each edge before wrapping
    private static final int WAVE_STEP = 4;     // waves are drawn as columns this many pixels wide

    // Logo: letters are 5x7 grids of cells, so no font file is needed. '#' is a filled cell.
    private static final String LOGO_TEXT = "ISLA";
    private static final int CELL = 6;          // pixels per grid cell
    private static final int LOGO_TOP = 12;
    private static final int SHADOW_OFFSET = 3;
    private static final int SUBTITLE_Y = 72;
    private static final int TEXT_CHAR_WIDTH = 5; // the 9px monospaced UI font is about 5px per character
    private static final String SUBTITLE = "A 2D Island Trading Adventure";
    private static final Map<Character, String[]> GLYPHS = Map.of(
            'I', new String[] {"#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####"},
            'S', new String[] {".####", "#....", "#....", ".###.", "....#", "....#", "####."},
            'L', new String[] {"#....", "#....", "#....", "#....", "#....", "#....", "#####"},
            'A', new String[] {".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"});

    /** {start x, y, width, frames per pixel of drift}: bigger last number = slower cloud. */
    private static final int[][] CLOUDS = {
        {40, 24, 56, 7}, {250, 52, 72, 5}, {400, 30, 48, 9}, {170, 86, 40, 11},
    };

    private TitleScene() {
    }

    /** Draws the whole scene for the given frame (frames count up from 0; any value is accepted). */
    public static void draw(Renderer r, long frame) {
        drawSky(r);
        drawSun(r);
        drawClouds(r, frame);
        drawSea(r);
        drawIsland(r);
        drawWaves(r, frame);
    }

    // ---- colour helpers ----

    /** Blends two "#RRGGBB" colours; t = 0 gives a, t = 1 gives b. */
    static String mix(String a, String b, double t) {
        int ca = Integer.parseInt(a.substring(1), 16);
        int cb = Integer.parseInt(b.substring(1), 16);
        int out = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int from = (ca >> shift) & 0xFF;
            int to = (cb >> shift) & 0xFF;
            out |= (int) Math.round(from + (to - from) * t) << shift;
        }
        return String.format("#%06X", out);
    }

    /** Colour at position t (0..1) along evenly spaced colour stops. */
    private static String gradient(String[] stops, double t) {
        double pos = Math.max(0, Math.min(1, t)) * (stops.length - 1);
        int i = Math.min((int) pos, stops.length - 2);
        return mix(stops[i], stops[i + 1], pos - i);
    }

    /** fillRect that cuts the rectangle to the screen, so drifting shapes never poke outside it. */
    private static void box(Renderer r, int x, int y, int w, int h, String color) {
        int x0 = Math.max(0, x);
        int y0 = Math.max(0, y);
        int x1 = Math.min(W, x + w);
        int y1 = Math.min(H, y + h);
        if (x1 > x0 && y1 > y0) {
            r.fillRect(x0, y0, x1 - x0, y1 - y0, color);
        }
    }

    // ---- sky, sun, sea ----

    private static void drawSky(Renderer r) {
        for (int y = 0; y < HORIZON; y += SKY_BAND) {
            int h = Math.min(SKY_BAND, HORIZON - y);
            r.fillRect(0, y, W, h, gradient(SKY, (y + h / 2.0) / HORIZON));
        }
    }

    /** A half-set sun: a pixel disc whose lower part is hidden by the sea. */
    private static void drawSun(Renderer r) {
        int cx = 350;
        int cy = 140;
        int radius = 26;
        for (int dy = -radius; dy < radius; dy += 2) {
            int half = (int) Math.round(Math.sqrt((double) radius * radius - (dy + 1.0) * (dy + 1.0)));
            box(r, cx - half, cy + dy, half * 2, 2, SUN);
        }
    }

    private static void drawSea(Renderer r) {
        int depth = H - HORIZON;
        for (int y = HORIZON; y < H; y += SEA_BAND) {
            int h = Math.min(SEA_BAND, H - y);
            r.fillRect(0, y, W, h, gradient(SEA, (y - HORIZON + h / 2.0) / depth));
        }
    }

    // ---- clouds ----

    private static void drawClouds(Renderer r, long frame) {
        int span = W + 2 * CLOUD_MARGIN;
        for (int[] c : CLOUDS) {
            long travelled = frame / c[3];
            int x = (int) Math.floorMod(c[0] + CLOUD_MARGIN + travelled, (long) span) - CLOUD_MARGIN;
            drawCloud(r, x, c[1], c[2]);
        }
    }

    private static void drawCloud(Renderer r, int x, int y, int w) {
        box(r, x, y + 4, w, 6, CLOUD);                      // body
        box(r, x + w / 6, y, w / 2, 5, CLOUD);              // big bump
        box(r, x + w / 2 + w / 8, y + 2, w / 3, 3, CLOUD);  // small bump
        box(r, x + 4, y + 10, w - 8, 2, CLOUD_SHADE);       // underside
    }

    // ---- island ----

    private static void drawIsland(Renderer r) {
        drawMound(r, 92, HORIZON + 6, 84, 30);   // main island
        drawMound(r, 168, HORIZON + 6, 34, 9);   // small sandbank to its right
        drawPalm(r, 66, 130, 8, 8);              // trunk base x, base y, segments, lean
        drawPalm(r, 114, 130, 7, 7);
    }

    /** A dome of one-pixel rows: widest at baseY, narrowing to a point height rows higher. */
    private static void drawMound(Renderer r, int cx, int baseY, int halfWidth, int height) {
        for (int row = 0; row < height; row++) {
            double d = (double) row / height;
            int half = (int) Math.round(halfWidth * Math.sqrt(1 - d * d));
            box(r, cx - half, baseY - 1 - row, half * 2, 1, ISLAND);
        }
    }

    /** A trunk of leaning 4px segments with drooping fronds fanned out from the crown. */
    private static void drawPalm(Renderer r, int baseX, int baseY, int segments, int lean) {
        int x = baseX;
        int y = baseY;
        for (int i = 0; i < segments; i++) {
            y -= 4;
            box(r, x, y, 3, 4, TRUNK);
            if (i * lean / segments != (i + 1) * lean / segments) {
                x++;
            }
        }
        int cx = x + 1;
        int cy = y;
        drawFrond(r, cx, cy, -1, 9, 3);
        drawFrond(r, cx, cy, 1, 9, 3);
        drawFrond(r, cx, cy, -1, 6, 7);
        drawFrond(r, cx, cy, 1, 6, 7);
        box(r, cx - 1, cy - 4, 3, 4, PALM);                 // the bud in the middle
    }

    /** One frond: 4x3 blocks stepping outwards 3px at a time, rising a little then drooping. */
    private static void drawFrond(Renderer r, int cx, int cy, int dir, int length, int lift) {
        for (int i = 0; i < length; i++) {
            int dx = dir * 3 * i;
            int dy = -lift + (i * i) / 6;
            box(r, dir < 0 ? cx + dx - 4 : cx + dx, cy + dy, 4, 3, PALM);
        }
    }

    // ---- waves ----

    private static void drawWaves(Renderer r, long frame) {
        ripple(r, frame, 164, 1, 36, 6, 2, CREST);   // far: thin and slow
        ripple(r, frame, 192, 2, 56, 4, 2, CREST);   // middle
        for (int x = 0; x < W; x += WAVE_STEP) {     // front: a dark swell topped with foam
            int y = waveY(frame, x, 232, 4, 64, 2);
            box(r, x, y + 2, WAVE_STEP, H - y - 2, SWELL);
            box(r, x, y, WAVE_STEP, 3, FOAM);
        }
    }

    /** A thin wavy line across the screen made of short columns. */
    private static void ripple(Renderer r, long frame, int baseY, int amplitude, int period,
                               int framesPerPixel, int thickness, String color) {
        for (int x = 0; x < W; x += WAVE_STEP) {
            box(r, x, waveY(frame, x, baseY, amplitude, period, framesPerPixel), WAVE_STEP, thickness, color);
        }
    }

    /** Height of a sine wave at column x; the pattern slides left by one pixel every framesPerPixel frames. */
    private static int waveY(long frame, int x, int baseY, int amplitude, int period, int framesPerPixel) {
        long shifted = x + frame / framesPerPixel;
        double phase = 2 * Math.PI * Math.floorMod(shifted, (long) period) / period;
        return baseY + (int) Math.round(amplitude * Math.sin(phase));
    }

    // ---- logo ----

    /** The big "ISLA" logo with a drop shadow, and the subtitle under it. Drawn on top of the scene. */
    public static void drawLogo(Renderer r) {
        int letters = LOGO_TEXT.length();
        int width = (letters * 5 + (letters - 1)) * CELL;
        int left = (GameSession.SCREEN_WIDTH - width) / 2;
        drawLogoLetters(r, left + SHADOW_OFFSET, LOGO_TOP + SHADOW_OFFSET, LOGO_SHADOW);
        drawLogoLetters(r, left, LOGO_TOP, LOGO_FACE);

        int textX = (GameSession.SCREEN_WIDTH - SUBTITLE.length() * TEXT_CHAR_WIDTH) / 2;
        r.drawText(textX + 1, SUBTITLE_Y + 1, SUBTITLE, LOGO_SHADOW);
        r.drawText(textX, SUBTITLE_Y, SUBTITLE, SUBTITLE_COLOR);

        // Small plain-text wordmark in the corner: the logo itself is only rectangles.
        r.drawText(GameSession.SCREEN_WIDTH - 34, GameSession.SCREEN_HEIGHT - 36, LOGO_TEXT, SUBTITLE_COLOR);
    }

    private static void drawLogoLetters(Renderer r, int left, int top, String color) {
        for (int i = 0; i < LOGO_TEXT.length(); i++) {
            String[] rows = GLYPHS.get(LOGO_TEXT.charAt(i));
            int letterX = left + i * 6 * CELL;
            for (int row = 0; row < rows.length; row++) {
                int col = 0;
                while (col < rows[row].length()) {
                    if (rows[row].charAt(col) != '#') {
                        col++;
                        continue;
                    }
                    int start = col;
                    while (col < rows[row].length() && rows[row].charAt(col) == '#') {
                        col++;
                    }
                    r.fillRect(letterX + start * CELL, top + row * CELL, (col - start) * CELL, CELL, color);
                }
            }
        }
    }
}
