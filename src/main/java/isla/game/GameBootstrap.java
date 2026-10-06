package isla.game;

import isla.Item;
import isla.NPC;
import isla.Player;
import isla.Quest;
import isla.QuestGiver;
import isla.Vendor;
import isla.dao.Repositories;
import isla.world.GameWorld;
import isla.world.TileMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Starts and saves games. On first run (empty storage) it seeds the item
 * catalog, vendors and quest from SeedData; afterwards it loads the saved
 * player and the live shop stock and quest state.
 */
public final class GameBootstrap {

    static final String MAP_RESOURCE = "/maps/island.txt";

    private GameBootstrap() {
    }

    /**
     * Builds a session from storage. With newGame=true any saved progress is
     * discarded and the world is reset to its starting state.
     */
    public static GameSession start(Repositories repos, boolean newGame) {
        seed(repos, newGame);

        Player player = repos.players().findById(SeedData.PLAYER_ID).orElseGet(GameBootstrap::newPlayer);
        Quest quest = repos.quests().findById(SeedData.elderQuest().getId()).orElseGet(SeedData::elderQuest);

        List<NPC> npcs = new ArrayList<>(repos.vendors().findAll());
        npcs.add(SeedData.elder(quest));

        GameSession session = new GameSession(new GameWorld(TileMap.loadResource(MAP_RESOURCE), player, npcs));
        session.setSaveHandler(() -> {
            save(repos, session);
            return repos.isPersistent() ? "Game saved." : "Game saved for this session only (no database).";
        });
        return session;
    }

    /** Writes the player, every vendor's stock and the quest state to storage. */
    public static void save(Repositories repos, GameSession session) {
        repos.players().save(session.getPlayer());
        for (NPC npc : session.getWorld().getNpcs()) {
            if (npc instanceof Vendor vendor) {
                repos.vendors().save(vendor);
            } else if (npc instanceof QuestGiver giver) {
                repos.quests().save(giver.getQuest());
            }
        }
    }

    private static Player newPlayer() {
        Player p = new Player(SeedData.PLAYER_ID, SeedData.PLAYER_NAME, SeedData.NEW_GAME_MONEY,
                SeedData.MAX_STAMINA, SeedData.INVENTORY_CAPACITY);
        p.setPosition(SeedData.SPAWN_X, SeedData.SPAWN_Y);
        return p;
    }

    private static void seed(Repositories repos, boolean overwrite) {
        if (overwrite) {
            repos.players().delete(SeedData.PLAYER_ID);
        }
        if (overwrite || repos.items().findAll().isEmpty()) {
            for (Item item : SeedData.items()) {
                repos.items().save(item);
            }
        }
        if (overwrite || repos.vendors().findAll().isEmpty()) {
            for (Vendor vendor : SeedData.vendors()) {
                repos.vendors().save(vendor);
            }
        }
        if (overwrite || repos.quests().findById(SeedData.elderQuest().getId()).isEmpty()) {
            repos.quests().save(SeedData.elderQuest());
        }
    }
}
