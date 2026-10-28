package isla.ui;

/** Pure helpers that turn sprite grids into pixels and scale pixels up without blurring them. */
final class PixelImages {

    private PixelImages() {
    }

    /** Draws a 16x16 grid of palette letters ('.' is see-through). */
    static Bitmap fromGrid(String[] grid) {
        int size = Sprites.SIZE;
        if (grid == null || grid.length != size) {
            throw new IllegalArgumentException("A sprite grid needs " + size + " rows");
        }
        int[] pixels = new int[size * size];
        for (int y = 0; y < size; y++) {
            String row = grid[y];
            if (row == null || row.length() != size) {
                throw new IllegalArgumentException("Row " + y + " must be " + size + " letters wide");
            }
            for (int x = 0; x < size; x++) {
                pixels[y * size + x] = Palette.argb(row.charAt(x));
            }
        }
        return new Bitmap(size, size, pixels);
    }

    /** Nearest-neighbour upscale: each pixel becomes a factor x factor block, so pixel art stays crisp. */
    static Bitmap scale(Bitmap source, int factor) {
        if (factor < 1) {
            throw new IllegalArgumentException("Scale factor must be at least 1");
        }
        int w = source.width() * factor;
        int h = source.height() * factor;
        int[] out = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out[y * w + x] = source.argb()[(y / factor) * source.width() + (x / factor)];
            }
        }
        return new Bitmap(w, h, out);
    }
}
