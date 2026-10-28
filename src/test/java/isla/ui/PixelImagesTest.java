package isla.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PixelImagesTest {

    @Test
    void gridBecomesArgbPixelsUsingThePalette() {
        String[] grid = new String[16];
        java.util.Arrays.fill(grid, "................");
        grid[0] = "Y...............";
        grid[15] = "...............I";
        Bitmap b = PixelImages.fromGrid(grid);
        assertEquals(16, b.width());
        assertEquals(16, b.height());
        assertEquals(Palette.argb(Palette.GOLD), b.argb()[0]);
        assertEquals(0, b.argb()[1]);
        assertEquals(Palette.argb(Palette.INK), b.argb()[16 * 16 - 1]);
    }

    @Test
    void badGridsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> PixelImages.fromGrid(new String[15]));
        String[] shortRow = new String[16];
        java.util.Arrays.fill(shortRow, "................");
        shortRow[3] = "...";
        assertThrows(IllegalArgumentException.class, () -> PixelImages.fromGrid(shortRow));
    }

    @Test
    void scalingRepeatsEachPixelWithoutBlurring() {
        Bitmap b = new Bitmap(2, 1, new int[] {0xFFAA0000, 0xFF00BB00});
        Bitmap big = PixelImages.scale(b, 2);
        assertEquals(4, big.width());
        assertEquals(2, big.height());
        assertEquals(0xFFAA0000, big.argb()[0]);
        assertEquals(0xFFAA0000, big.argb()[1]);
        assertEquals(0xFF00BB00, big.argb()[2]);
        assertEquals(0xFF00BB00, big.argb()[7]);
    }

    @Test
    void scalingByOneKeepsThePixels() {
        Bitmap b = new Bitmap(1, 1, new int[] {0xFF123456});
        assertEquals(b.argb()[0], PixelImages.scale(b, 1).argb()[0]);
        assertThrows(IllegalArgumentException.class, () -> PixelImages.scale(b, 0));
    }
}
