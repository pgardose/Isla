package isla.world;

import static org.junit.jupiter.api.Assertions.*;

import isla.NPC;
import isla.QuestGiver;
import isla.Vendor;
import isla.game.SeedData;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IslandReachabilityTest {

    private final TileMap map = TileMap.loadResource("/maps/island.txt");
    private final List<Vendor> vendors = SeedData.vendors();
    private final QuestGiver elder = SeedData.elder();

    private List<NPC> allNpcs() {
        List<NPC> all = new ArrayList<>(vendors);
        all.add(elder);
        return all;
    }

    private Set<Long> reachable(Set<String> unlocked) {
        Set<Long> blocked = new HashSet<>();
        for (NPC n : allNpcs()) blocked.add(key(n.getX(), n.getY()));
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{SeedData.SPAWN_X, SeedData.SPAWN_Y});
        seen.add(key(SeedData.SPAWN_X, SeedData.SPAWN_Y));
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int[] d : dirs) {
                int nx = cur[0] + d[0], ny = cur[1] + d[1];
                long k = key(nx, ny);
                if (!seen.contains(k) && !blocked.contains(k) && map.canWalk(nx, ny, unlocked)) {
                    seen.add(k);
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        return seen;
    }

    private boolean canInteractWith(NPC npc, Set<Long> reach) {
        return reach.contains(key(npc.getX() + 1, npc.getY())) || reach.contains(key(npc.getX() - 1, npc.getY()))
                || reach.contains(key(npc.getX(), npc.getY() + 1)) || reach.contains(key(npc.getX(), npc.getY() - 1));
    }

    private static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    @Test
    void spawnAndNpcTilesAreWalkableAndDistinct() {
        assertTrue(map.canWalk(SeedData.SPAWN_X, SeedData.SPAWN_Y, Set.of()));
        Set<Long> tiles = new HashSet<>();
        for (NPC n : allNpcs()) {
            assertTrue(map.canWalk(n.getX(), n.getY(), Set.of("Jungle Path")), n + " stands on a blocked tile");
            assertTrue(tiles.add(key(n.getX(), n.getY())), n + " shares a tile");
        }
        assertFalse(tiles.contains(key(SeedData.SPAWN_X, SeedData.SPAWN_Y)));
    }

    @Test
    void spawn_reachesAllNpcsWithoutJungle_exceptJungleTrader() {
        Set<Long> reach = reachable(Set.of());
        for (Vendor v : vendors) {
            boolean expected = !v.getName().equals("Yara");
            assertEquals(expected, canInteractWith(v, reach), v.getName());
        }
        assertTrue(canInteractWith(elder, reach));
    }

    @Test
    void jungleTrader_reachableOnlyAfterUnlock() {
        Set<Long> reach = reachable(Set.of("Jungle Path"));
        for (NPC n : allNpcs()) {
            assertTrue(canInteractWith(n, reach), n + " unreachable even with Jungle Path");
        }
    }
}
