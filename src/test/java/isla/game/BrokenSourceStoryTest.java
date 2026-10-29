package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.Accusation;
import isla.DebugQuest;
import isla.Item;
import isla.NPC;
import isla.Quest;
import isla.QuestGiver;
import isla.RecordingRenderer;
import isla.Vendor;
import isla.dao.Repositories;
import isla.world.TileMap;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * "The Broken Source", played for real: the tests below walk the island tile by tile with the same Actions the
 * keyboard sends, so the map, the glitched bridges, the money, stamina and every story gate are all exercised.
 */
class BrokenSourceStoryTest {

    private Repositories repos;
    private GameSession s;
    private int steps;

    @BeforeEach
    void setUp() {
        repos = Repositories.inMemory();
        s = GameBootstrap.start(repos, true, new Random(3));
        steps = 0;
    }

    // ---------------------------------------------------------------- helpers

    private QuestGiver giver(int npcId) {
        return s.getWorld().getNpcs().stream()
                .filter(n -> n instanceof QuestGiver g && g.getNpcId() == npcId)
                .map(n -> (QuestGiver) n).findFirst().orElseThrow();
    }

    private Vendor vendor(String name) {
        return TestSessions.vendor(s, name);
    }

    private Item item(String name) {
        return SeedData.items().stream().filter(i -> i.getName().equals(name)).findFirst().orElseThrow();
    }

    private boolean blocked(int x, int y) {
        TileMap map = s.getWorld().getMap();
        return !map.canWalk(x, y, s.getPlayer().getUnlockedAreas()) || s.getWorld().npcAt(x, y).isPresent();
    }

    /** Walks (with real key presses) to a tile next to the NPC, where no other NPC is also adjacent. */
    private void walkTo(NPC npc) {
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        int sx = s.getPlayer().getX();
        int sy = s.getPlayer().getY();
        Map<Long, long[]> from = new HashMap<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{sx, sy});
        from.put(key(sx, sy), null);
        int[] goal = null;
        while (!queue.isEmpty() && goal == null) {
            int[] c = queue.poll();
            if (Math.abs(c[0] - npc.getX()) + Math.abs(c[1] - npc.getY()) == 1 && onlyNeighbour(c[0], c[1], npc)) {
                goal = c;
                break;
            }
            for (int[] d : dirs) {
                int nx = c[0] + d[0];
                int ny = c[1] + d[1];
                if (!from.containsKey(key(nx, ny)) && !blocked(nx, ny)) {
                    from.put(key(nx, ny), new long[]{c[0], c[1]});
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        assertNotNull(goal, "no way to walk to " + npc + " with " + s.getPlayer().getUnlockedAreas());
        java.util.LinkedList<int[]> path = new java.util.LinkedList<>();
        for (int[] c = goal; c != null; ) {
            path.addFirst(c);
            long[] prev = from.get(key(c[0], c[1]));
            c = prev == null ? null : new int[]{(int) prev[0], (int) prev[1]};
        }
        path.removeFirst();
        for (int[] c : path) {
            int dx = c[0] - s.getPlayer().getX();
            int dy = c[1] - s.getPlayer().getY();
            Action a = dx == 1 ? Action.RIGHT : dx == -1 ? Action.LEFT : dy == 1 ? Action.DOWN : Action.UP;
            s.handle(a);
            if (s.getPlayer().getX() != c[0] || s.getPlayer().getY() != c[1]) {
                // too tired: eat something if we have it, else collapse and rest, then try again
                assertTrue(s.getPlayer().isExhausted(), "blocked at " + c[0] + "," + c[1]);
                s.handle(Action.REST);
                s.handle(a);
            }
            steps++;
            assertEquals(c[0], s.getPlayer().getX());
            assertEquals(c[1], s.getPlayer().getY());
        }
    }

    private boolean onlyNeighbour(int x, int y, NPC npc) {
        return List.of(new int[]{0, -1}, new int[]{0, 1}, new int[]{-1, 0}, new int[]{1, 0}).stream()
                .map(d -> s.getWorld().npcAt(x + d[0], y + d[1]))
                .filter(java.util.Optional::isPresent).count() == 1;
    }

    private static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    /** Talks to the NPC and reads every page of what they say; returns the text. */
    private String talk(NPC npc) {
        walkTo(npc);
        s.handle(Action.CONFIRM);
        String text = s.getDialogueText();
        if (s.getScreen() == Screen.DIALOGUE) {
            int pages = s.dialoguePageCount();
            for (int i = 0; i < pages; i++) {
                s.handle(Action.CONFIRM);
            }
            assertEquals(Screen.EXPLORING, s.getScreen(), "Z on the last page closes the dialogue");
        }
        return text;
    }

    /** Opens the NPC's puzzle, picks the right answer, returns what is said after. */
    private String solve(NPC npc) {
        walkTo(npc);
        s.handle(Action.CONFIRM);
        assertEquals(Screen.DEBUG_PUZZLE, s.getScreen(), s.getDialogueText());
        DebugQuest p = s.getActivePuzzle();
        while (s.getPuzzleCursor() != p.getCorrectIndex()) {
            s.handle(Action.DOWN);
        }
        s.handle(Action.CONFIRM);
        String text = s.getDialogueText();
        if (s.getScreen() == Screen.DIALOGUE) {
            s.handle(Action.CANCEL);
        }
        return text;
    }

    private void buy(String vendorName, String itemName) {
        walkTo(vendor(vendorName));
        s.handle(Action.CONFIRM);
        assertEquals(Screen.SHOP, s.getScreen());
        List<Item> stock = s.getActiveShop().getStock();
        int index = stock.indexOf(item(itemName));
        assertTrue(index >= 0, vendorName + " does not sell " + itemName);
        for (int i = 0; i < index; i++) {
            s.handle(Action.DOWN);
        }
        s.handle(Action.CONFIRM);
        assertEquals("Bought " + itemName + "!", s.getMessage(), "money: $" + s.getPlayer().getMoney());
        s.handle(Action.CANCEL);
    }

    private void use(String itemName) {
        s.handle(Action.INVENTORY);
        for (int t = 0; t < 3 && !s.inventoryEntries().contains(item(itemName)); t++) {
            s.handle(Action.RIGHT);
        }
        while (!s.inventoryEntries().get(s.getInventoryCursor()).equals(item(itemName))) {
            s.handle(Action.DOWN);
        }
        s.handle(Action.CONFIRM);
        s.handle(Action.CANCEL);
    }

    // ---------------------------------------------------------------- the whole game

    @Test
    void theWholeStory_canBePlayedFromStartToEnding_byWalking() {
        // Chapter 1: the missing bridge
        assertTrue(s.getJourney().objectiveText().contains("Elder Kai"));
        String intro = talk(giver(1));
        assertTrue(intro.contains("three shards"), intro);
        assertTrue(s.dialoguePageCount() >= 1);
        assertEquals(70, s.getPlayer().getMoney(), "Kai gives $20 for supplies");
        assertTrue(s.getJourney().objectiveText().contains("Jungle Shrine"));

        assertTrue(solve(giver(2)).contains("natural"), "Nia's practice puzzle");
        assertEquals(110, s.getPlayer().getMoney());
        assertTrue(talk(giver(2)).contains("spare pair"), "Nia lets slip a clue");

        buy("Tomas", "Rusty Machete");
        buy("Tomas", "Seashell Necklace");
        use("Rusty Machete");
        String shrine = solve(giver(7));
        assertTrue(shrine.contains("SUN SHARD"), shrine);
        assertTrue(s.getPlayer().hasUnlocked(SeedData.EAST_BRIDGE));
        assertEquals(List.of("Sun"), s.shardsFound());

        // Chapter 2: the looping village
        assertTrue(talk(giver(5)).startsWith("Lovely day!"), "Lio is stuck in the loop");
        assertTrue(solve(giver(8)).contains("MOON SHARD"));
        assertTrue(talk(giver(5)).contains("blue light"), "Lio is awake and remembers");
        assertTrue(talk(giver(12)).contains("Ula"), "Pip points to Grandma Ula");
        assertTrue(talk(giver(6)).contains("'ula'"), "Ula takes the necklace and tells the password");
        assertEquals(0, s.getPlayer().getInventory().countOf(item("Seashell Necklace")));
        buy("Ofa", "Lantern");
        buy("Ofa", "Grilled Fish");

        // Chapter 3: the endless jungle
        assertTrue(solve(giver(9)).contains("bridge draws itself"));
        assertTrue(talk(giver(10)).contains("too dark"), "the vine stone needs light");
        use("Lantern");
        assertTrue(solve(giver(10)).contains("wither"));
        String altar = solve(giver(11));
        assertTrue(altar.contains("SEA SHARD") && altar.contains("lenses"), altar);
        assertEquals(List.of("Sun", "Moon", "Sea"), s.shardsFound());
        assertEquals(SeedData.CLUES.size(), s.cluesFound().size(), s.cluesFound().toString());

        // Finale: the truth
        assertTrue(talk(giver(1)).contains("who cut the Idol"));
        walkTo(giver(1));
        s.handle(Action.CONFIRM);
        assertEquals(Screen.DEBUG_PUZZLE, s.getScreen());
        assertTrue(s.getActivePuzzle() instanceof Accusation);
        int staminaBefore = s.getPlayer().getStamina();
        s.handle(Action.CONFIRM); // Tomas: wrong
        assertTrue(s.getMessage().contains("Tomas"), s.getMessage());
        assertEquals(staminaBefore, s.getPlayer().getStamina(), "a wrong accusation costs no stamina");
        s.handle(Action.CANCEL);
        String confession = solve(giver(1));
        assertTrue(confession.contains("It was me"), confession);

        walkTo(giver(1));
        s.handle(Action.CONFIRM);
        DebugQuest mend = s.getActivePuzzle();
        while (s.getPuzzleCursor() != mend.getCorrectIndex()) {
            s.handle(Action.DOWN);
        }
        s.handle(Action.CONFIRM);
        assertEquals(Screen.ENDING, s.getScreen());
        assertTrue(s.getJourney().isComplete());
        assertTrue(s.endingSummary().contains("Cursed stones repaired: 5 of 5"), s.endingSummary().toString());

        RecordingRenderer r = new RecordingRenderer();
        s.render(r);
        assertTrue(r.anyTextContains("THE END"));
        s.handle(Action.CONFIRM);
        assertEquals(Screen.EXPLORING, s.getScreen());
        assertTrue(talk(giver(1)).contains("shines again"), "after the ending Kai only says thanks");
        assertTrue(steps > 300, "a real journey across three islands: " + steps + " steps");
    }

    // ---------------------------------------------------------------- the gates

    @Test
    void theEastBridge_isGlitchedUntilTheShrineIsFixed() {
        assertFalse(s.getWorld().getMap().canWalk(41, 13, s.getPlayer().getUnlockedAreas()));
        s.getPlayer().unlockArea(SeedData.EAST_BRIDGE);
        assertTrue(s.getWorld().getMap().canWalk(41, 13, s.getPlayer().getUnlockedAreas()));
    }

    @Test
    void theFixedBridge_isDrawnAsPath_theBrokenOneAsGlitch() {
        s.getPlayer().setPosition(38, 13);
        RecordingRenderer before = new RecordingRenderer();
        s.render(before);
        assertTrue(before.calls.stream().anyMatch(t -> t.contains("41,13") && t.contains(isla.world.TileType.EAST_GLITCH.getColor())),
                "glitch colour before the fix");
        s.getPlayer().unlockArea(SeedData.EAST_BRIDGE);
        RecordingRenderer after = new RecordingRenderer();
        s.render(after);
        assertTrue(after.calls.stream().anyMatch(t -> t.contains("41,13") && t.contains(isla.world.TileType.PATH.getColor())), "path colour after the fix");
    }

    @Test
    void theSouthStone_staysLockedUntilUlaTellsThePassword() {
        s.getPlayer().setPosition(21, 23);
        s.handle(Action.CONFIRM);
        assertEquals(Screen.DIALOGUE, s.getScreen());
        assertTrue(s.getDialogueText().contains("ENTER PASSWORD"));
        s.handle(Action.CANCEL);
        s.getPlayer().unlockArea(SeedData.KNOWS_PASSWORD);
        s.handle(Action.CONFIRM);
        assertEquals(Screen.DEBUG_PUZZLE, s.getScreen());
    }

    @Test
    void villagers_loopUntilTheVillageStoneIsFixed_andUlaNeedsHerNecklace() {
        QuestGiver ula = giver(6);
        assertTrue(ula.interact(s.getPlayer()).getText().startsWith("Lovely day!"));
        s.getPlayer().unlockArea(SeedData.VILLAGE_AWAKE);
        assertTrue(ula.interact(s.getPlayer()).getText().contains("necklace"));
        assertFalse(s.getPlayer().hasUnlocked(SeedData.KNOWS_PASSWORD));
        s.getPlayer().getInventory().addItem(item("Seashell Necklace"));
        assertTrue(ula.interact(s.getPlayer()).getText().contains("'ula'"));
        assertTrue(s.getPlayer().hasUnlocked(SeedData.KNOWS_PASSWORD));
        assertTrue(s.getPlayer().hasUnlocked(SeedData.CLUE_MACHETE));
    }

    @Test
    void aWrongCodeFix_teachesWhy_andCostsStamina_aRightOneOpensTheWay() {
        s.getPlayer().unlockArea(SeedData.JUNGLE_PATH);
        s.getPlayer().setPosition(28, 6); // under the shrine stone
        s.handle(Action.CONFIRM);
        DebugQuest p = s.getActivePuzzle();
        int wrong = (p.getCorrectIndex() + 1) % p.getOptions().size();
        while (s.getPuzzleCursor() != wrong) {
            s.handle(Action.DOWN);
        }
        s.handle(Action.CONFIRM);
        assertEquals(Screen.DEBUG_PUZZLE, s.getScreen(), "you can try again");
        assertEquals(SeedData.MAX_STAMINA - DebugQuest.WRONG_FIX_STAMINA, s.getPlayer().getStamina());
        assertTrue(s.getMessage().contains("Glitch shock"), s.getMessage());
        assertEquals(p.feedbackFor(wrong), s.getMessage().substring(0, p.feedbackFor(wrong).length()));
        assertFalse(s.getPlayer().hasUnlocked(SeedData.EAST_BRIDGE));
    }

    @Test
    void kai_wantsAllThreeShards_andCountsThem() {
        QuestGiver kai = giver(1);
        kai.interact(s.getPlayer()); // intro
        s.getPlayer().unlockArea(SeedData.SHARD_SUN);
        assertTrue(kai.interact(s.getPlayer()).getText().contains("1 of 3"));
        s.getPlayer().unlockArea(SeedData.SHARD_MOON);
        s.getPlayer().unlockArea(SeedData.SHARD_SEA);
        assertTrue(kai.interact(s.getPlayer()).getText().contains("who cut the Idol"));
    }

    // ---------------------------------------------------------------- saving the story

    @Test
    void storyProgress_bridgesAndClues_surviveSaveAndContinue() {
        talk(giver(1));
        s.getPlayer().unlockArea(SeedData.JUNGLE_PATH);
        solve(giver(7));
        s.handle(Action.SAVE);

        GameSession again = GameBootstrap.start(repos, false);
        assertTrue(again.getPlayer().hasUnlocked(SeedData.EAST_BRIDGE));
        assertTrue(again.getWorld().getMap().canWalk(41, 13, again.getPlayer().getUnlockedAreas()));
        assertEquals(List.of("Sun"), again.shardsFound());
        assertEquals(2, again.getJourney().completedCount());
        assertTrue(again.getJourney().objectiveText().contains("village stone"));
        assertTrue(again.getJourney().partText().startsWith("Chapter 2"));

        GameSession fresh = GameBootstrap.start(repos, true);
        assertEquals(0, fresh.getJourney().completedCount());
        assertTrue(fresh.shardsFound().isEmpty());
    }

    // ---------------------------------------------------------------- content that has to fit the screen

    @Test
    void everyObjectiveAndClue_fitsItsStrip() {
        for (var c : s.getJourney().getChapters()) {
            assertTrue(c.objective().length() <= 56, c.objective());
            assertNotNull(c.part());
        }
        SeedData.CLUES.values().forEach(c -> assertTrue(c.length() <= 52, c));
    }

    @Test
    void everyStoryPuzzle_hasFourAnswers_andExplainsEveryWrongOne() {
        for (NPC n : s.getWorld().getNpcs()) {
            if (n instanceof QuestGiver g && g.getNpcId() != 2) {
                for (Quest q : g.getQuests()) {
                    if (q instanceof DebugQuest p) {
                        assertEquals(4, p.getOptions().size(), p.getDescription());
                        assertTrue(p.getSnippet().length() <= 62, p.getSnippet());
                        for (int i = 0; i < 4; i++) {
                            if (i != p.getCorrectIndex()) {
                                assertNotEquals("Not quite. Read the line again and try another fix.",
                                        p.feedbackFor(i), p.getDescription() + " option " + i);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void theCulprit_isTheRightAnswerToTheAccusation() {
        Accusation a = SeedData.accusation();
        assertEquals(SeedData.CULPRIT, a.getOptions().get(a.getCorrectIndex()));
        assertEquals("WHO BROKE THE IDOL?", a.getHeading());
    }

    // ---------------------------------------------------------------- screens

    @Test
    void longDialogue_isPaged_zTurnsThePage_xSkips() {
        s.getPlayer().setPosition(20, 10);
        s.handle(Action.CONFIRM);
        assertTrue(s.dialoguePageCount() > 1);
        s.handle(Action.CONFIRM);
        assertEquals(1, s.getDialoguePage());
        assertEquals(Screen.DIALOGUE, s.getScreen());
        RecordingRenderer r = new RecordingRenderer();
        s.render(r);
        assertTrue(r.anyTextContains("Z: next") || r.anyTextContains("Z: close"));
        s.handle(Action.CANCEL);
        assertEquals(Screen.EXPLORING, s.getScreen());
    }

    @Test
    void objectiveStrip_andJournal_areDrawn() {
        RecordingRenderer r = new RecordingRenderer();
        s.render(r);
        assertTrue(r.anyTextContains("Talk to Elder Kai"), String.valueOf(r.texts));

        s.getPlayer().unlockArea(SeedData.SHARD_SUN);
        s.getPlayer().unlockArea(SeedData.CLUE_SIGNED);
        s.handle(Action.QUESTS);
        RecordingRenderer j = new RecordingRenderer();
        s.render(j);
        assertTrue(j.anyTextContains("JOURNAL"));
        assertTrue(j.anyTextContains("Idol shards (1 of 3)"));
        assertTrue(j.anyTextContains("signed '-N'"));
    }
}
