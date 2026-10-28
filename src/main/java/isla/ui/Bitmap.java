package isla.ui;

/** A small picture as plain ARGB ints, so sprite logic never needs a GUI toolkit. */
record Bitmap(int width, int height, int[] argb) {

    Bitmap {
        if (width <= 0 || height <= 0 || argb == null || argb.length != width * height) {
            throw new IllegalArgumentException("Bitmap size does not match its pixels");
        }
    }
}
