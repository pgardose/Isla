package isla.game;

import isla.DebugQuest;
import isla.Item;
import isla.Journey;
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
        // Story quests are rebuilt from SeedData (it holds their dialogue); storage only says which are done.
        Quest elderQuest = restored(repos, SeedData.elderQuest());
        Quest keeperQuest = restored(repos, SeedData.keeperQuest());
        Quest lampQuest = restored(repos, SeedData.lampQuest());
        // Nia's puzzle is random, so it is loaded from storage as saved.
        Quest debugQuest = repos.quests().findById(SeedData.debugQuest().getId()).orElseGet(SeedData::debugQuest);
        Journey journey = SeedData.story(elderQuest, keeperQuest, debugQuest, lampQuest);

        List<NPC> npcs = new ArrayList<>(repos.vendors().findAll());
        npcs.add(SeedData.elder(elderQuest));
        npcs.add(SeedData.coder(debugQuest));
        npcs.add(SeedData.keeper(keeperQuest));
        npcs.add(SeedData.lamp(lampQuest));

        GameSession session = new GameSession(new GameWorld(TileMap.loadResource(MAP_RESOURCE), player, npcs));
        session.setJourney(journey);
        session.setSaveHandler(() -> {
            save(repos, session);
            return repos.isPersistent() ? "Game saved." : "Game saved for this session only (no database).";
        });
        return session;
    }

    /** The fresh quest, marked completed if storage says the player already finished it. */
    private static Quest restored(Repositories repos, Quest fresh) {
        repos.quests().findById(fresh.getId()).ifPresent(saved -> {
            if (saved.isCompleted()) {
                fresh.markCompleted();
            }
        });
        return fresh;
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

    private static void seed(Repositories repos, boolean overwrite, Random random) {
        if (overwrite) {
            repos.players().delete(SeedData.PLAYER_ID);
        }
        // The catalog is always refreshed so a save made before the story items existed gains them.
        for (Item item : SeedData.items()) {
            repos.items().save(item);
        }
        // Each vendor is seeded on its own, so an older save gains the new vendors without losing its stock.
        for (Vendor vendor : SeedData.vendors()) {
            if (overwrite || repos.vendors().findById(vendor.getId()).isEmpty()) {
                repos.vendors().save(vendor);
            }
        }
        // Each quest is seeded on its own, so a save made before a quest existed still gains it.
        Quest elder = SeedData.elderQuest();
        if (overwrite || repos.quests().findById(elder.getId()).isEmpty()) {
            repos.quests().save(elder);
        }
        for (Quest story : List.of(SeedData.keeperQuest(), SeedData.lampQuest())) {
            if (overwrite || repos.quests().findById(story.getId()).isEmpty()) {
                repos.quests().save(story);
            }
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
