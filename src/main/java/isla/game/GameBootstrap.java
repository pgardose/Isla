package isla.game;

import isla.DebugQuest;
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
import java.util.Random;

/**
 * Starts and saves games. On first run (empty storage) it seeds the item
 * catalog, vendors and quests from SeedData; afterwards it loads the saved
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
        return start(repos, newGame, new Random());
    }

    /** Same, with a given source of luck (tests pass a fixed seed to pick Nia's puzzle). */
    public static GameSession start(Repositories repos, boolean newGame, Random random) {
        seed(repos, newGame, random);

        Player player = repos.players().findById(SeedData.PLAYER_ID).orElseGet(GameBootstrap::newPlayer);
        Quest elderQuest = repos.quests().findById(SeedData.elderQuest().getId()).orElseGet(SeedData::elderQuest);
        Quest idolQuest = repos.quests().findById(SeedData.idolQuest().getId()).orElseGet(SeedData::idolQuest);
        Quest debugQuest = repos.quests().findById(SeedData.debugQuest().getId()).orElseGet(SeedData::debugQuest);

        List<NPC> npcs = new ArrayList<>(repos.vendors().findAll());
        npcs.add(SeedData.elderWithIdol(elderQuest, idolQuest));
        npcs.add(SeedData.coder(debugQuest));

        GameSession session = new GameSession(new GameWorld(TileMap.loadResource(MAP_RESOURCE), player, npcs));
        session.setSaveHandler(() -> {
            save(repos, session);
            return repos.isPersistent() ? "Game saved." : "Game saved for this session only (no database).";
        });
        return session;
    }

    /** True when a player has been saved, i.e. there is something to continue. */
    public static boolean hasSave(Repositories repos) {
        return repos.players().findById(SeedData.PLAYER_ID).isPresent();
    }

    /** Throws away all saved progress and resets shops and quests to their starting state. */
    public static void erase(Repositories repos) {
        seed(repos, true, new Random());
    }

    /** Writes the player, every vendor's stock and the quest state to storage. */
    public static void save(Repositories repos, GameSession session) {
        repos.players().save(session.getPlayer());
        for (NPC npc : session.getWorld().getNpcs()) {
            if (npc instanceof Vendor vendor) {
                repos.vendors().save(vendor);
            } else if (npc instanceof QuestGiver giver) {
                giver.getQuests().forEach(repos.quests()::save); // Elder Kai has two quests
            }
        }
    }

    private static Player newPlayer() {
        Player p = new Player(SeedData.PLAYER_ID, SeedData.PLAYER_NAME, SeedData.NEW_GAME_MONEY,
                SeedData.MAX_STAMINA, SeedData.INVENTORY_CAPACITY);
        p.setPosition(SeedData.SPAWN_X, SeedData.SPAWN_Y);
        return p;
    }

    private static void seed(Repositories repos, boolean overwrite, Random random) {
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
        // Each quest is seeded on its own, so a save made before a quest existed still gains it.
        Quest elder = SeedData.elderQuest();
        if (overwrite || repos.quests().findById(elder.getId()).isEmpty()) {
            repos.quests().save(elder);
        }
        // The Golden Idol quest, likewise, so a save made before it existed gains it on Continue.
        Quest idol = SeedData.idolQuest();
        if (overwrite || repos.quests().findById(idol.getId()).isEmpty()) {
            repos.quests().save(idol);
        }
        // Nia's puzzle is drawn at random, but never the same one the player had last time.
        int puzzleId = SeedData.debugQuest().getId();
        Quest existing = repos.quests().findById(puzzleId).orElse(null);
        if (overwrite || existing == null) {
            String previous = existing instanceof DebugQuest dq ? dq.getSnippet() : null;
            repos.quests().save(DebugQuestBank.random(random, previous));
        }
    }
}
