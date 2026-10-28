package isla.ui;

/**
 * Builds the 16x16 texture of one tile as a grid of palette letters. Everything is decided by a hash
 * of the tile's x and y (plus the water frame), so a tile looks the same every time it is drawn:
 * nothing flickers when the camera moves. Pure logic, no JavaFX.
 */
final class TileTexture {

    static final int SIZE = Sprites.SIZE;
    /** Frames (about 1/60 s each) between the two water animation frames. */
    static final int WATER_TICKS = 30;

    private TileTexture() {
    }

    /** A well-mixed, repeatable number for a tile coordinate. */
    static int hash(int x, int y) {
        int h = x * 0x27D4EB2D ^ y * 0x165667B1;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        h ^= h >>> 15;
        return h;
    }

    /** Which of the two water frames to show at this animation frame. */
    static int waterFrame(int frame) {
        return Math.floorMod(Math.floorDiv(frame, WATER_TICKS), 2);
    }

    /** Texture for one tile. Only water uses {@code frame}; everything else looks the same in every frame. */
    static String[] generate(TileKind kind, int col, int row, int frame) {
        Rng rng = new Rng(hash(col, row));
        char[][] g = new char[SIZE][SIZE];
        switch (kind) {
            case WATER -> water(g, rng, waterFrameOf(frame));
            case SAND -> sand(g, rng);
            case PATH -> path(g, rng);
            case JUNGLE -> jungle(g, rng);
            default -> grass(g, rng); // GRASS, and the ground under PALM and HUT
        }
        String[] rows = new String[SIZE];
        for (int y = 0; y < SIZE; y++) {
            rows[y] = new String(g[y]);
        }
        return rows;
    }

    private static int waterFrameOf(int frame) {
        return frame & 1;
    }

    private static void fill(char[][] g, char c) {
        for (char[] row : g) {
            java.util.Arrays.fill(row, c);
        }
    }

    private static void grass(char[][] g, Rng rng) {
        fill(g, 'G');
        for (int i = 0; i < 3; i++) { // three small "v" shaped tufts
            int x = rng.next(14);
            int y = 1 + rng.next(14);
            g[y - 1][x] = 'g';
            g[y][x + 1] = 'g';
            g[y - 1][x + 2] = 'g';
        }
        g[rng.next(SIZE)][rng.next(SIZE)] = 'g';
    }

    private static void sand(char[][] g, Rng rng) {
        fill(g, 'S');
        for (int i = 0; i < 6; i++) { // speckles
            g[rng.next(SIZE)][rng.next(SIZE)] = 's';
        }
        int x = rng.next(14);
        int y = rng.next(SIZE);
        g[y][x] = 's';
        g[y][x + 1] = 's';
    }

    private static void path(char[][] g, Rng rng) {
        fill(g, 'T');
        for (int i = 0; i < 3; i++) { // pebbles: a light pair with a dark shadow underneath
            int x = rng.next(14);
            int y = rng.next(14);
            g[y][x] = 'S';
            g[y][x + 1] = 'S';
            g[y + 1][x] = 'W';
        }
    }

    private static void jungle(char[][] g, Rng rng) {
        fill(g, 'J');
        for (int i = 0; i < 4; i++) { // leaves: a diagonal of palm green with a lighter dot
            int x = rng.next(13);
            int y = 1 + rng.next(14);
            g[y][x] = 'P';
            g[y - 1][x + 1] = 'P';
            g[y - 1][x + 2] = 'P';
            g[y][x + 2] = 'g';
        }
    }

    private static void water(char[][] g, Rng rng, int frame) {
        fill(g, 'o');
        for (int i = 0; i < 3; i++) { // wave dashes drift two pixels sideways between frames
            int x = rng.next(SIZE) + frame * 2;
            int y = rng.next(14);
            for (int k = 0; k < 3; k++) {
                g[y][(x + k) % SIZE] = 'F';
            }
            g[y + 1][(x + 1) % SIZE] = 'O';
        }
    }

    /** Tiny xorshift random numbers, seeded from the tile hash so results never change. */
    private static final class Rng {
        private int state;

        Rng(int seed) {
            this.state = seed == 0 ? 0x9E3779B9 : seed;
        }

        int next(int bound) {
            state ^= state << 13;
            state ^= state >>> 17;
            state ^= state << 5;
            return Integer.remainderUnsigned(state, bound);
        }
    }
}
