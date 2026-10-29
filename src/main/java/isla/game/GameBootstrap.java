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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Starts and saves games. On first run (empty storage) it seeds the item catalog, vendors and quests from
 * SeedData; afterwards it loads the saved player, the live shop stock and which quests are done.
 *
 * Story quests are always rebuilt from SeedData (their text, answers and rewards live in code); storage only
 * says whether each one is completed. Nia's practice puzzle is the exception: it is drawn at random, so the
 * drawn puzzle itself is saved and loaded.
 */
public final class GameBootstrap {

    static final String MAP_RESOURCE = "/maps/island.txt";

    private GameBootstrap() {
    }

    /**
     * Builds a session from storage. With newGame=true any saved progress is discarded and the world is reset to
     * its starting state.
     */
    public static GameSession start(Repositories repos, boolean newGame) {
        return start(repos, newGame, new Random());
    }

    /** Same, with a given source of luck (tests pass a fixed seed to pick Nia's puzzle). */
    public static GameSession start(Repositories repos, boolean newGame, Random random) {
        // A save from before "The Broken Source" has none of the story quests: it cannot be continued.
        seed(repos, newGame || !isCurrentStory(repos), random);

        Player player = repos.players().findById(SeedData.PLAYER_ID).orElseGet(GameBootstrap::newPlayer);

        Quest intro = loaded(repos, SeedData.kaiIntroQuest());
        Quest shards = loaded(repos, SeedData.shardsQuest());
        Quest accusation = loaded(repos, SeedData.accusation());
        Quest mend = loaded(repos, SeedData.mendIdolQuest());
        Quest practice = repos.quests().findById(SeedData.NIA_PRACTICE_ID)
                .filter(q -> q instanceof DebugQuest).orElseGet(SeedData::debugQuest);
        Quest chat = loaded(repos, SeedData.niaChatQuest());
        Quest shrine = loaded(repos, SeedData.shrinePuzzle());
        Quest village = loaded(repos, SeedData.villagePuzzle());
        Quest south = loaded(repos, SeedData.southPuzzle());
        Quest vines = loaded(repos, SeedData.vinePuzzle());
        Quest altar = loaded(repos, SeedData.altarPuzzle());
        Quest lio = loaded(repos, SeedData.lioQuest());
        Quest ula = loaded(repos, SeedData.ulaQuest());
        Quest pip = loaded(repos, SeedData.pipQuest());

        List<NPC> npcs = new ArrayList<>(repos.vendors().findAll());
        npcs.add(SeedData.elder(intro, shards, accusation, mend));
        npcs.add(SeedData.coder(practice, chat));
        npcs.add(SeedData.shrineStone(shrine));
        npcs.add(SeedData.villageStone(village));
        npcs.add(SeedData.southStone(south));
        npcs.add(SeedData.vineStone(vines));
        npcs.add(SeedData.altar(altar));
        npcs.add(SeedData.lio(lio));
        npcs.add(SeedData.ula(ula));
        npcs.add(SeedData.pip(pip));

        Journey journey = SeedData.journey(intro, shrine, village, ula, south, vines, altar, shards, accusation,
                mend);

        GameSession session = new GameSession(new GameWorld(TileMap.loadResource(MAP_RESOURCE), player, npcs));
        session.setJourney(journey);
        session.setSaveHandler(() -> {
            save(repos, session);
            return repos.isPersistent() ? "Game saved." : "Game saved for this session only (no database).";
        });
        return session;
    }

    /** The fresh quest, marked completed if storage says it was. */
    private static Quest loaded(Repositories repos, Quest fresh) {
        boolean done = repos.quests().findById(fresh.getId()).map(Quest::isCompleted).orElse(false);
        if (done) {
            fresh.markCompleted();
        }
        return fresh;
    }

    /** True when a player has been saved in the current story, i.e. there is something to continue. */
    public static boolean hasSave(Repositories repos) {
        return repos.players().findById(SeedData.PLAYER_ID).isPresent() && isCurrentStory(repos);
    }

    private static boolean isCurrentStory(Repositories repos) {
        return repos.quests().findById(SeedData.KAI_INTRO_ID).isPresent();
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
                giver.getQuests().forEach(repos.quests()::save);
            }
        }
    }

    private static Player newPlayer() {
        Player p = new Player(SeedData.PLAYER_ID, SeedData.PLAYER_NAME, SeedData.NEW_GAME_MONEY,
                SeedData.MAX_STAMINA, SeedData.INVENTORY_CAPACITY);
        p.setPosition(SeedData.SPAWN_X, SeedData.SPAWN_Y);
        return p;
    }

    /** Every story quest as it is at the start of a game (Nia's random practice puzzle is handled separately). */
    static List<Quest> storyQuests() {
        return List.of(SeedData.kaiIntroQuest(), SeedData.shardsQuest(), SeedData.accusation(),
                SeedData.mendIdolQuest(), SeedData.niaChatQuest(), SeedData.shrinePuzzle(),
                SeedData.villagePuzzle(), SeedData.southPuzzle(), SeedData.vinePuzzle(), SeedData.altarPuzzle(),
                SeedData.lioQuest(), SeedData.ulaQuest(), SeedData.pipQuest());
    }

    private static void seed(Repositories repos, boolean overwrite, Random random) {
        if (overwrite) {
            repos.players().delete(SeedData.PLAYER_ID);
        }
        // The catalog is always brought up to date (it is an upsert), so new items exist for every save.
        for (Item item : SeedData.items()) {
            repos.items().save(item);
        }
        Set<Integer> savedVendors = repos.vendors().findAll().stream().map(Vendor::getId).collect(Collectors.toSet());
        for (Vendor vendor : SeedData.vendors()) {
            if (overwrite || !savedVendors.contains(vendor.getId())) {
                repos.vendors().save(vendor);
            }
        }
        for (Quest q : storyQuests()) {
            if (overwrite || repos.quests().findById(q.getId()).isEmpty()) {
                repos.quests().save(q);
            }
        }
        // Nia's practice puzzle is drawn at random, but never the same one the player had last time.
        Quest existing = repos.quests().findById(SeedData.NIA_PRACTICE_ID).orElse(null);
        if (overwrite || !(existing instanceof DebugQuest)) {
            String previous = existing instanceof DebugQuest dq ? dq.getSnippet() : null;
            repos.quests().save(DebugQuestBank.random(random, previous));
        }
    }
}
