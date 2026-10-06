package isla.world;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A rectangular grid of TileTypes, loaded from plain text (one character per
 * tile; see TileType for the symbols). Editing the island means editing
 * characters in src/main/resources/maps/island.txt.
 */
public class TileMap {

    private final TileType[][] tiles; // [y][x]
    private final int width;
    private final int height;

    private TileMap(TileType[][] tiles) {
        this.tiles = tiles;
        this.height = tiles.length;
        this.width = tiles[0].length;
    }

    /** Parses map lines. Blank lines are ignored; errors name the 1-based line and column. */
    public static TileMap parse(List<String> lines) {
        List<String> rows = new ArrayList<>();
        for (String line : lines) {
            if (!line.isBlank()) {
                rows.add(line);
            }
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Map has no rows");
        }
        int width = rows.get(0).length();
        TileType[][] grid = new TileType[rows.size()][width];
        for (int y = 0; y < rows.size(); y++) {
            String row = rows.get(y);
            if (row.length() != width) {
                throw new IllegalArgumentException("Map line " + (y + 1) + " has " + row.length()
                        + " columns, expected " + width);
            }
            for (int x = 0; x < width; x++) {
                TileType type = TileType.fromSymbol(row.charAt(x));
                if (type == null) {
                    throw new IllegalArgumentException("Unknown tile '" + row.charAt(x) + "' at line "
                            + (y + 1) + ", column " + (x + 1));
                }
                grid[y][x] = type;
            }
        }
        return new TileMap(grid);
    }

    /** Loads a map from the classpath, e.g. "/maps/island.txt". */
    public static TileMap loadResource(String path) {
        try (InputStream in = TileMap.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("Map resource not found: " + path);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return parse(lines);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read map " + path, e);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public TileType tileAt(int x, int y) {
        return tiles[y][x];
    }

    /** True if a player with these unlocked areas may stand on (x, y). Out of bounds is never walkable. */
    public boolean canWalk(int x, int y, Set<String> unlockedAreas) {
        return inBounds(x, y) && tiles[y][x].isWalkable(unlockedAreas);
    }
}
