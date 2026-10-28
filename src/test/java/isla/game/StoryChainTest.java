package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.InteractionResult;
import isla.Item;
import isla.Journey;
import isla.NPC;
import isla.Player;
import isla.QuestGiver;
import isla.dao.Repositories;
import isla.world.TileMap;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The main story: relight the lighthouse (Elder Kai, Keeper Bo, Coder Nia, the lamp). */
class StoryChainTest {

    private Repositories repos;
    private GameSession s;

    @BeforeEach
    void setUp() {
        repos = Repositories.inMemory();
        s = GameBootstrap.start(repos, true, new Random(1));
    }

    private Item item(String name) {
        return SeedData.items().stream().filter(i -> i.getName().equals(name)).findFirst().orElseThrow();
    }

    private QuestGiver giver(int npcId) {
        return s.getWorld().getNpcs().stream()
                .filter(n -> n instanceof QuestGiver g && g.getNpcId() == npcId)
                .map(n -> (QuestGiver) n).findFirst().orElseThrow();
    }

    private String say(int npcId) {
        InteractionResult r = giver(npcId).interact(s.getPlayer());
        return r.getText();
    }

    private void solveNia() {
        QuestGiver nia = giver(2);
        isla.DebugQuest q = (isla.DebugQuest) nia.getQuest();
        assertTrue(q.answer(s.getPlayer(), q.getCorrectIndex()));
    }

    @Test
    void storyStartsAtElderKai_andHasFourChapters() {
        Journey j = s.getJourney();
        assertEquals(4, j.size());
        assertEquals(0, j.completedCount());
        assertTrue(j.objectiveText().contains("Elder Kai"));
    }

    @Test
    void keeperBoAndTheLampStayLockedUntilEarlierChaptersAreDone() {
        s.getPlayer().getInventory().addItem(item("Lamp Oil"));
        s.getPlayer().getInventory().addItem(item("Glass Lens"));
        assertTrue(say(3).contains("Elder Kai"), "Bo is locked until the Elder's quest is done");
        assertFalse(giver(3).getQuest().isCompleted(), "locked quest must not take the oil");
        assertTrue(say(4).contains("Keeper Bo"), "the lamp is locked too");
        assertFalse(giver(4).getQuest().isCompleted());
    }

    @Test
    void fullPlaythrough_relightsTheLighthouseAndPaysEveryChapter() {
        Player p = s.getPlayer();
        int start = p.getMoney();

        p.getInventory().addItem(item("Coconut"));
        assertTrue(say(1).contains("Keeper Bo"), "the Elder points the player to Bo");
        assertTrue(s.getJourney().objectiveText().contains("Lamp Oil"));

        p.getInventory().addItem(item("Lamp Oil"));
        assertTrue(say(3).contains("Nia"), "Bo points the player to Nia");

        // The lamp needs the puzzle too, so it is still locked here.
        p.getInventory().addItem(item("Glass Lens"));
        assertTrue(say(4).contains("Keeper Bo"));
        assertFalse(s.getJourney().isComplete());

        solveNia();
        assertTrue(say(4).contains("THE END"));
        assertTrue(s.getJourney().isComplete());
        assertEquals(4, s.getJourney().completedCount());
        assertEquals(start + 25 + 30 + 40 + 100, p.getMoney());
        assertTrue(s.getJourney().objectiveText().contains("shines"));
    }

    @Test
    void progressSurvivesSaveAndContinue_andNewGameResetsIt() {
        s.getPlayer().getInventory().addItem(item("Coconut"));
        say(1);
        GameBootstrap.save(repos, s);

        GameSession again = GameBootstrap.start(repos, false);
        assertEquals(1, again.getJourney().completedCount());
        assertTrue(again.getJourney().objectiveText().contains("Lamp Oil"));

        GameSession fresh = GameBootstrap.start(repos, true);
        assertEquals(0, fresh.getJourney().completedCount());
    }

    @Test
    void oldSaveWithoutStoryItemsAndVendors_gainsThemOnContinue() {
        Repositories old = Repositories.inMemory();
        for (Item i : SeedData.items().subList(0, 7)) old.items().save(i);
        SeedData.vendors().subList(0, 3).forEach(old.vendors()::save);
        old.quests().save(SeedData.elderQuest());

        GameSession loaded = GameBootstrap.start(old, false);
        assertEquals(9, loaded.getWorld().getNpcs().size());
        assertTrue(old.items().findAll().stream().anyMatch(i -> i.getName().equals("Glass Lens")));
    }

    @Test
    void everyVendorSellsWhatTheStoryNeeds() {
        Set<String> sold = new HashSet<>();
        for (NPC n : s.getWorld().getNpcs()) {
            if (n instanceof isla.Vendor v) {
                v.getShop().getStock().forEach(i -> sold.add(i.getName()));
            }
        }
        assertTrue(sold.contains("Lamp Oil"));
        assertTrue(sold.contains("Glass Lens"));
        assertTrue(sold.contains("Rusty Machete"), "the machete opens the jungle where the lens is sold");
    }

    @Test
    void everyNpcCanBeReached_andTheJungleVendorNeedsTheMachete() {
        TileMap map = s.getWorld().getMap();
        Set<String> none = Set.of();
        Set<String> withMachete = Set.of("Jungle Path");
        Set<String> open = reachable(map, none);
        Set<String> openWithTool = reachable(map, withMachete);
        for (NPC n : s.getWorld().getNpcs()) {
            assertTrue(map.canWalk(n.getX(), n.getY(), withMachete), "NPC stands on a walkable tile: " + n.getX() + "," + n.getY());
            boolean touching = List.of(new int[]{0, 1}, new int[]{0, -1}, new int[]{1, 0}, new int[]{-1, 0}).stream()
                    .anyMatch(d -> openWithTool.contains((n.getX() + d[0]) + "," + (n.getY() + d[1])));
            assertTrue(touching, "player can stand next to NPC at " + n.getX() + "," + n.getY());
        }
        NPC pearl = s.getWorld().getNpcs().stream()
                .filter(n -> n instanceof isla.Vendor v && v.getName().equals("Old Pearl")).findFirst().orElseThrow();
        boolean reachableWithoutTool = List.of(new int[]{0, 1}, new int[]{0, -1}, new int[]{1, 0}, new int[]{-1, 0}).stream()
                .anyMatch(d -> open.contains((pearl.getX() + d[0]) + "," + (pearl.getY() + d[1])));
        assertFalse(reachableWithoutTool, "Old Pearl is deep in the jungle");
    }

    @Test
    void hudObjectivesFitTheStrip() {
        // The strip fits about 56 monospaced characters.
        Journey j = s.getJourney();
        assertTrue(j.objectiveText().length() <= 56);
        for (QuestGiver g : List.of(giver(1), giver(3), giver(4))) {
            assertNotNull(g.getQuest().getDescription());
        }
    }

    private Set<String> reachable(TileMap map, Set<String> unlocked) {
        Set<String> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{SeedData.SPAWN_X, SeedData.SPAWN_Y});
        seen.add(SeedData.SPAWN_X + "," + SeedData.SPAWN_Y);
        Set<String> npcTiles = new HashSet<>();
        s.getWorld().getNpcs().forEach(n -> npcTiles.add(n.getX() + "," + n.getY()));
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int x = c[0] + d[0];
                int y = c[1] + d[1];
                String key = x + "," + y;
                if (!seen.contains(key) && map.canWalk(x, y, unlocked) && !npcTiles.contains(key)) {
                    seen.add(key);
                    queue.add(new int[]{x, y});
                }
            }
        }
        return seen;
    }
}
