import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Grows the original 40x26 island into an 80x52 world: the old island stays exactly where it was
 * (so every NPC and spawn coordinate still works), a bridge leads east to a village island and a
 * bridge leads south to a big southern island. Run from the project folder:
 *
 *     java tools/GenerateMap.java
 *
 * It reads tools/island-40x26.txt and writes src/main/resources/maps/island.txt.
 * Tiles: g grass, s sand, w water, p path, t palm, h hut, j jungle.
 */
public class GenerateMap {

    static final int W = 80;
    static final int H = 52;
    static char[][] g = new char[H][W];

    public static void main(String[] args) throws Exception {
        List<String> old = Files.readAllLines(Path.of("tools/island-40x26.txt")).stream()
                .filter(l -> !l.isBlank()).toList();
        for (char[] row : g) {
            Arrays.fill(row, 'w');
        }
        for (int y = 0; y < old.size(); y++) {
            for (int x = 0; x < old.get(y).length(); x++) {
                g[y][x] = old.get(y).charAt(x);
            }
        }

        ellipse(61, 16, 17, 12);   // east island
        ellipse(40, 40, 26, 10);   // south island

        // East bridge (3 wide) and the main road continuing along row 13.
        fill(38, 12, 46, 14, 's');
        for (int x = 35; x <= 72; x++) g[13][x] = 'p';
        for (int y = 6; y <= 26; y++) g[y][61] = 'p';        // north-south street in the village

        // South bridge and roads on the south island.
        fill(19, 24, 21, 33, 's');
        for (int y = 24; y <= 40; y++) g[y][20] = 'p';
        for (int x = 20; x <= 60; x++) g[40][x] = 'p';
        for (int y = 33; y <= 47; y++) g[y][40] = 'p';

        // Huts beside the roads (only placed on plain grass).
        int[][] huts = {{56, 11}, {66, 11}, {56, 15}, {66, 15}, {52, 20}, {70, 20},
                {30, 37}, {50, 37}, {30, 43}, {50, 43}};
        for (int[] h : huts) {
            if (g[h[1]][h[0]] == 'g') g[h[1]][h[0]] = 'h';
        }

        // Palms: random but never touching another palm or hut, so they can never wall anything in.
        Random rnd = new Random(2026);
        for (int y = 1; y < H - 1; y++) {
            for (int x = 1; x < W - 1; x++) {
                boolean newLand = x >= 40 || y >= 26;
                if (newLand && g[y][x] == 'g' && rnd.nextDouble() < 0.07 && !touches(x, y, "th")) {
                    g[y][x] = 't';
                }
            }
        }

        StringBuilder out = new StringBuilder();
        for (char[] row : g) out.append(new String(row)).append('\n');
        Files.writeString(Path.of("src/main/resources/maps/island.txt"), out.toString());
        System.out.println("Wrote " + W + "x" + H + " map.");
    }

    /** Fills an ellipse of land on open water: sand on the rim, grass inside. */
    static void ellipse(int cx, int cy, int rx, int ry) {
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                double dx = (x - cx) / (double) rx;
                double dy = (y - cy) / (double) ry;
                double d = dx * dx + dy * dy;
                if (d <= 1 && g[y][x] == 'w') {
                    g[y][x] = d > 0.72 ? 's' : 'g';
                }
            }
        }
    }

    static void fill(int x1, int y1, int x2, int y2, char c) {
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) g[y][x] = c;
        }
    }

    static boolean touches(int x, int y, String blockers) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if ((dx != 0 || dy != 0) && blockers.indexOf(g[y + dy][x + dx]) >= 0) return true;
            }
        }
        return false;
    }
}
