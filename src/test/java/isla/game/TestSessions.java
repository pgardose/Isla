package isla.game;

import isla.NPC;
import isla.Player;
import isla.Vendor;
import isla.world.GameWorld;
import isla.world.TileMap;
import java.util.ArrayList;
import java.util.List;

/** Test helper: builds sessions on the real island with the seeded content. */
final class TestSessions {

    private TestSessions() {
    }

    static Player newPlayer() {
        Player p = new Player(SeedData.PLAYER_ID, "Pj", SeedData.NEW_GAME_MONEY,
                SeedData.MAX_STAMINA, SeedData.INVENTORY_CAPACITY);
        p.setPosition(SeedData.SPAWN_X, SeedData.SPAWN_Y);
        return p;
    }

    static GameSession fresh() {
        return at(SeedData.SPAWN_X, SeedData.SPAWN_Y);
    }

    static GameSession at(int x, int y) {
        Player p = newPlayer();
        p.setPosition(x, y);
        return new GameSession(world(p));
    }

    static GameWorld world(Player p) {
        List<NPC> npcs = new ArrayList<>(SeedData.vendors());
        npcs.add(SeedData.elder());
        return new GameWorld(TileMap.loadResource("/maps/island.txt"), p, npcs);
    }

    static Vendor vendor(GameSession s, String name) {
        return s.getWorld().getNpcs().stream()
                .filter(n -> n instanceof Vendor v && v.getName().equals(name))
                .map(n -> (Vendor) n).findFirst().orElseThrow();
    }
}
