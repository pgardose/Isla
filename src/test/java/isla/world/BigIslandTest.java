package isla.world;

import static org.junit.jupiter.api.Assertions.*;

import isla.game.SeedData;
import java.util.ArrayDeque;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Guards for the expanded map: it is big, and every bit of land can actually be walked to. */
class BigIslandTest {

    private final TileMap island = TileMap.loadResource("/maps/island.txt");

    private boolean[][] reachable(Set<String> unlocked) {
        boolean[][] seen = new boolean[island.getHeight()][island.getWidth()];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] {SeedData.SPAWN_X, SeedData.SPAWN_Y});
        seen[SeedData.SPAWN_Y][SeedData.SPAWN_X] = true;
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] at = queue.poll();
            for (int[] d : steps) {
                int nx = at[0] + d[0];
                int ny = at[1] + d[1];
                if (island.inBounds(nx, ny) && !seen[ny][nx] && island.canWalk(nx, ny, unlocked)) {
                    seen[ny][nx] = true;
                    queue.add(new int[] {nx, ny});
                }
            }
        }
        return seen;
    }

    private int countWalkable(Set<String> unlocked) {
        int n = 0;
        for (int y = 0; y < island.getHeight(); y++) {
            for (int x = 0; x < island.getWidth(); x++) {
                if (island.canWalk(x, y, unlocked)) n++;
            }
        }
        return n;
    }

    @Test
    void mapIsAtLeast80By52() {
        assertTrue(island.getWidth() >= 80, "width " + island.getWidth());
        assertTrue(island.getHeight() >= 52, "height " + island.getHeight());
    }

    @Test
    void thereIsMuchMoreLandToExplore() {
        assertTrue(countWalkable(Set.of()) >= 1600, "walkable tiles: " + countWalkable(Set.of()));
    }

    @Test
    void newLandExistsInTheEastAndInTheSouth() {
        boolean east = false;
        boolean south = false;
        for (int y = 0; y < island.getHeight(); y++) {
            for (int x = 0; x < island.getWidth(); x++) {
                if (island.canWalk(x, y, Set.of())) {
                    east |= x >= 45;
                    south |= y >= 30;
                }
            }
        }
        assertTrue(east, "no walkable land east of the old island");
        assertTrue(south, "no walkable land south of the old island");
    }

    private static final Set<String> ALL = Set.of("Jungle Path", "East Bridge", "South Bridge", "Overgrowth Cleared");

    @Test
    void atTheStart_onlyTheHomeIslandIsOpen_theBridgesAreGlitched() {
        boolean[][] seen = reachable(Set.of());
        for (int y = 0; y < island.getHeight(); y++) {
            for (int x = 0; x < island.getWidth(); x++) {
                if (seen[y][x]) {
                    assertTrue(x < 40 && y < 27, "(" + x + "," + y + ") should be locked at the start");
                }
            }
        }
    }

    @Test
    void fixingTheShrineOpensTheVillage_butNotTheSouth() {
        boolean[][] seen = reachable(Set.of("Jungle Path", "East Bridge"));
        assertTrue(seen[13][61], "the village crossroads");
        assertFalse(seen[40][40], "the south island stays shut until the south bridge is fixed");
    }

    @Test
    void theTempleClearing_needsTheOvergrowthCleared() {
        Set<String> beforeVines = Set.of("Jungle Path", "East Bridge", "South Bridge");
        assertTrue(reachable(beforeVines)[40][40], "the south island");
        assertFalse(reachable(beforeVines)[44][54], "the temple clearing is behind the vines");
        assertTrue(reachable(ALL)[44][54]);
    }

    @Test
    void withEveryStoneFixed_everyWalkableTileCanBeReached() {
        boolean[][] seen = reachable(ALL);
        for (int y = 0; y < island.getHeight(); y++) {
            for (int x = 0; x < island.getWidth(); x++) {
                if (island.canWalk(x, y, ALL)) {
                    assertTrue(seen[y][x], "cannot walk to (" + x + "," + y + ") even with every stone fixed");
                }
            }
        }
    }

    @Test
    void theOriginalLandmarksStayWhereTheyWere() {
        assertTrue(island.canWalk(SeedData.SPAWN_X, SeedData.SPAWN_Y, Set.of()));
        assertEquals(TileType.PATH, island.tileAt(20, 9));   // the Elder's path
        assertEquals(TileType.PATH, island.tileAt(24, 13));  // the road above Nia
        assertEquals(TileType.PATH, island.tileAt(30, 7));   // Yara's spot, inside the jungle
        assertEquals(TileType.JUNGLE, island.tileAt(29, 7)); // the jungle around her
    }
}
