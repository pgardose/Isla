package isla.world;

import static org.junit.jupiter.api.Assertions.*;

import isla.NPC;
import isla.QuestGiver;
import isla.Vendor;
import isla.dao.Repositories;
import isla.game.GameBootstrap;
import isla.game.SeedData;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IslandReachabilityTest {

    private final TileMap map = TileMap.loadResource("/maps/island.txt");
    /** Every NPC of the real story, as a new game places them. */
    private final List<NPC> npcs = GameBootstrap.start(Repositories.inMemory(), true).getWorld().getNpcs();

    private List<NPC> allNpcs() {
        return npcs;
    }

    private static final Set<String> JUNGLE = Set.of("Jungle Path");
    private static final Set<String> EAST = Set.of("Jungle Path", "East Bridge");
    private static final Set<String> SOUTH = Set.of("Jungle Path", "East Bridge", "South Bridge");
    private static final Set<String> ALL = Set.of("Jungle Path", "East Bridge", "South Bridge", "Overgrowth Cleared");

    /** The fewest story flags needed to reach each NPC: this is the order the story is played in. */
    private Set<String> needed(NPC n) {
        String who = n instanceof Vendor v ? v.getName() : n instanceof QuestGiver g ? "giver" + g.getNpcId() : "?";
        return switch (who) {
            case "Yara", "giver7" -> JUNGLE;                                  // Yara, Jungle Shrine stone
            case "Ofa", "giver8", "giver5", "giver6", "giver12" -> EAST;      // the village
            case "giver10" -> SOUTH;                                          // the vine stone
            case "giver11" -> ALL;                                            // the temple altar
            default -> Set.of();                                              // the home island
        };
    }

    private Set<String> oneStepBefore(Set<String> flags) {
        if (flags.equals(ALL)) return SOUTH;
        if (flags.equals(SOUTH)) return EAST;
        if (flags.equals(EAST)) return JUNGLE;
        return Set.of();
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
            assertTrue(map.canWalk(n.getX(), n.getY(), ALL), n + " stands on a blocked tile");
            assertTrue(tiles.add(key(n.getX(), n.getY())), n + " shares a tile");
        }
        assertFalse(tiles.contains(key(SeedData.SPAWN_X, SeedData.SPAWN_Y)));
    }

    @Test
    void everyNpc_isReachableExactlyWhenTheStorySaysSo() {
        for (NPC n : allNpcs()) {
            Set<String> flags = needed(n);
            assertTrue(canInteractWith(n, reachable(flags)), n + " unreachable with " + flags);
            if (!flags.isEmpty()) {
                assertFalse(canInteractWith(n, reachable(oneStepBefore(flags))), n + " reachable too early");
            }
        }
    }

    @Test
    void theStoryHasAllItsPeopleAndStones() {
        assertEquals(4, allNpcs().stream().filter(n -> n instanceof Vendor).count());
        assertEquals(10, allNpcs().stream().filter(n -> n instanceof QuestGiver).count());
    }
}
