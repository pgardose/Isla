package isla.world;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TileMapTest {

    private final TileMap map = TileMap.parse(List.of(
            "wwwww",
            "wgpjw",
            "wwwww"));

    @Test
    void parse_readsDimensionsAndTiles() {
        assertEquals(5, map.getWidth());
        assertEquals(3, map.getHeight());
        assertEquals(TileType.GRASS, map.tileAt(1, 1));
        assertEquals(TileType.PATH, map.tileAt(2, 1));
    }

    @Test
    void parse_unknownSymbol_throwsWithLineAndColumn() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> TileMap.parse(List.of("ggg", "gxg")));
        assertTrue(e.getMessage().contains("line 2"), e.getMessage());
        assertTrue(e.getMessage().contains("column 2"), e.getMessage());
        assertTrue(e.getMessage().contains("'x'"), e.getMessage());
    }

    @Test
    void parse_raggedRows_throws() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> TileMap.parse(List.of("ggg", "gg")));
        assertTrue(e.getMessage().contains("line 2"), e.getMessage());
    }

    @Test
    void parse_empty_throws() {
        assertThrows(IllegalArgumentException.class, () -> TileMap.parse(List.of()));
    }

    @Test
    void canWalk_outOfBounds_false() {
        assertFalse(map.canWalk(-1, 1, Set.of()));
        assertFalse(map.canWalk(1, -1, Set.of()));
        assertFalse(map.canWalk(5, 1, Set.of()));
        assertFalse(map.canWalk(1, 3, Set.of()));
    }

    @Test
    void canWalk_water_false_grassAndPath_true() {
        assertFalse(map.canWalk(0, 0, Set.of()));
        assertTrue(map.canWalk(1, 1, Set.of()));
        assertTrue(map.canWalk(2, 1, Set.of()));
    }

    @Test
    void canWalk_jungle_onlyWhenAreaUnlocked() {
        assertFalse(map.canWalk(3, 1, Set.of()));
        assertFalse(map.canWalk(3, 1, Set.of("Other Area")));
        assertTrue(map.canWalk(3, 1, Set.of("Jungle Path")));
    }

    @Test
    void loadResource_islandMapLoadsAndIsLargerThanViewport() {
        TileMap island = TileMap.loadResource("/maps/island.txt");
        assertTrue(island.getWidth() > 30);
        assertTrue(island.getHeight() > 17);
    }
}
