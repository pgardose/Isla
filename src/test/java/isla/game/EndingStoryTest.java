package isla.game;

import static org.junit.jupiter.api.Assertions.*;

import isla.DebugQuest;
import isla.Item;
import isla.RecordingRenderer;
import isla.dao.Repositories;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The goal and the ending: Elder Kai asks for a coconut, then for the Golden Idol; delivering the Idol shows the
 * ending screen once. Runs on the real bootstrap (in-memory storage), so saving and Continue are covered too.
 */
class EndingStoryTest {

    // Spots next to each NPC on the real island (the player may stand on any side).
    private static final int[] ELDER = {20, 10};
    private static final int[] MARA = {12, 13};
    private static final int[] TOMAS = {28, 13};
    private static final int[] YARA = {30, 8};
    private static final int[] NIA = {24, 15};

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

    private void stand(GameSession session, int[] spot) {
        session.getPlayer().setPosition(spot[0], spot[1]);
    }

    private void talkToElder() {
        stand(s, ELDER);
        s.handle(Action.CONFIRM);
    }

    /** Closes whatever dialogue is open so the next talk starts from the island. */
    private void closeDialogue() {
        assertEquals(Screen.DIALOGUE, s.getScreen());
        s.handle(Action.CONFIRM);
        assertEquals(Screen.EXPLORING, s.getScreen());
    }

    private void giveCoconutAndDeliver() {
        s.getPlayer().getInventory().addItem(item("Coconut"));
        talkToElder();
        closeDialogue();
    }

    private void deliverIdol() {
        s.getPlayer().getInventory().addItem(item("Golden Idol"));
        talkToElder();
    }

    // ---- Elder Kai's dialogue changes with the stage ----

    @Test
    void elderDialogue_coconutFirst_thenTheIdol_thenThanks() {
        talkToElder();
        assertTrue(s.getDialogueText().contains("coconut"), s.getDialogueText());
        assertFalse(s.getDialogueText().contains("Golden Idol"), s.getDialogueText());
        closeDialogue();

        s.getPlayer().getInventory().addItem(item("Coconut"));
        talkToElder();
        assertTrue(s.getDialogueText().contains("$25"), s.getDialogueText());
        closeDialogue();

        talkToElder();
        assertTrue(s.getDialogueText().contains("Golden Idol"), s.getDialogueText());
        assertTrue(s.getDialogueText().contains("jungle"), s.getDialogueText());
        closeDialogue();

        deliverIdol();
        assertEquals(Screen.ENDING, s.getScreen());
        s.handle(Action.CONFIRM); // keep exploring

        talkToElder();
        assertEquals(Screen.DIALOGUE, s.getScreen());
        assertTrue(s.getDialogueText().contains("Thank you"), s.getDialogueText());
        assertFalse(s.getDialogueText().contains("Golden Idol"), s.getDialogueText());
    }

    @Test
    void theIdolIsNotAcceptedBeforeTheCoconutQuestIsDone() {
        s.getPlayer().getInventory().addItem(item("Golden Idol"));
        talkToElder();
        assertEquals(Screen.DIALOGUE, s.getScreen());
        assertTrue(s.getDialogueText().contains("coconut"), s.getDialogueText());
        assertEquals(1, s.getPlayer().getInventory().countOf(item("Golden Idol")));
        assertEquals(SeedData.NEW_GAME_MONEY, s.getPlayer().getMoney());
    }

    // ---- reward and ending happen once ----

    @Test
    void idolReward_isPaidOnce_andTheIdolIsTaken() {
        giveCoconutAndDeliver();
        int before = s.getPlayer().getMoney();
        s.getPlayer().getInventory().addItem(item("Golden Idol"));
        s.getPlayer().getInventory().addItem(item("Golden Idol")); // a spare, in case of a second delivery
        talkToElder();
        assertEquals(before + 100, s.getPlayer().getMoney());
        assertEquals(1, s.getPlayer().getInventory().countOf(item("Golden Idol")), "only one Idol is taken");

        s.handle(Action.CONFIRM);
        talkToElder();
        closeDialogue();
        talkToElder();
        assertEquals(before + 100, s.getPlayer().getMoney(), "no second payment");
    }

    @Test
    void ending_triggersExactlyOnce() {
        giveCoconutAndDeliver();
        deliverIdol();
        assertEquals(Screen.ENDING, s.getScreen());

        s.handle(Action.CONFIRM);
        assertEquals(Screen.EXPLORING, s.getScreen());
        for (int i = 0; i < 3; i++) {
            s.getPlayer().getInventory().addItem(item("Golden Idol"));
            talkToElder();
            assertEquals(Screen.DIALOGUE, s.getScreen(), "talk " + i);
            closeDialogue();
        }
    }

    @Test
    void ending_isNotShownAgainAfterSaveAndContinue() {
        giveCoconutAndDeliver();
        deliverIdol();
        GameBootstrap.save(repos, s);

        GameSession again = GameBootstrap.start(repos, false);
        assertEquals(Screen.EXPLORING, again.getScreen());
        stand(again, ELDER);
        again.handle(Action.CONFIRM);
        assertEquals(Screen.DIALOGUE, again.getScreen());
        assertTrue(again.getDialogueText().contains("Thank you"), again.getDialogueText());
    }

    @Test
    void endingScreen_showsTheStoryTheSummaryAndBothChoices() {
        giveCoconutAndDeliver();
        solveNia();
        deliverIdol();
        assertEquals(Screen.ENDING, s.getScreen());

        RecordingRenderer r = new RecordingRenderer();
        s.render(r);
        assertTrue(r.anyTextContains("THE END"), String.valueOf(r.texts));
        assertTrue(r.anyTextContains("Quests completed: 3 of 3"), String.valueOf(r.texts));
        assertTrue(r.anyTextContains("Money earned from quests: $165"), String.valueOf(r.texts));
        assertTrue(r.anyTextContains("Money in hand: $" + s.getPlayer().getMoney()), String.valueOf(r.texts));
        assertTrue(r.anyTextContains("keep exploring"), String.valueOf(r.texts));
        assertTrue(r.anyTextContains("main menu"), String.valueOf(r.texts));
    }

    @Test
    void endingSummary_countsOnlyWhatWasDone() {
        giveCoconutAndDeliver();
        deliverIdol(); // Nia's puzzle left alone
        List<String> summary = s.endingSummary();
        assertTrue(summary.contains("Quests completed: 2 of 3"), summary.toString());
        assertTrue(summary.contains("Money earned from quests: $125"), summary.toString());
    }

    @Test
    void afterTheEnding_theGameContinuesNormally() {
        giveCoconutAndDeliver();
        deliverIdol();
        s.handle(Action.CONFIRM);
        assertEquals(Screen.EXPLORING, s.getScreen());
        s.getPlayer().setPosition(20, 13);
        s.handle(Action.DOWN);
        assertEquals(14, s.getPlayer().getY(), "walking still works");
        s.handle(Action.INVENTORY);
        assertEquals(Screen.INVENTORY, s.getScreen());
    }

    // ---- the quest list ----

    @Test
    void questList_showsTheActiveGoalForEachStage() {
        assertEquals("Bring Elder Kai a coconut", s.questLines().get(0));

        s.getPlayer().getInventory().addItem(item("Coconut"));
        talkToElder();
        closeDialogue();
        assertEquals("Find the Golden Idol in the jungle", s.questLines().get(0));

        deliverIdol();
        assertTrue(s.questLines().get(0).startsWith("Done"), s.questLines().toString());
    }

    @Test
    void questList_alsoShowsNiasSideQuest_andWhenItIsDone() {
        assertEquals(2, s.questLines().size());
        assertTrue(s.questLines().get(1).contains("Nia"), s.questLines().toString());
        assertFalse(s.questLines().get(1).startsWith("Done"));
        solveNia();
        assertTrue(s.questLines().get(1).startsWith("Done"), s.questLines().toString());
    }

    @Test
    void qKey_opensTheQuestList_andItCanBeClosed() {
        s.handle(Action.QUESTS);
        assertEquals(Screen.QUESTS, s.getScreen());
        RecordingRenderer r = new RecordingRenderer();
        s.render(r);
        assertTrue(r.anyTextContains("QUESTS"), String.valueOf(r.texts));
        assertTrue(r.anyTextContains("Bring Elder Kai a coconut"), String.valueOf(r.texts));

        s.handle(Action.CANCEL);
        assertEquals(Screen.EXPLORING, s.getScreen());
        s.handle(Action.QUESTS);
        s.handle(Action.QUESTS); // Q again also closes it
        assertEquals(Screen.EXPLORING, s.getScreen());
    }

    @Test
    void questList_cannotBeOpenedOverAShop() {
        stand(s, MARA);
        s.handle(Action.CONFIRM);
        assertEquals(Screen.SHOP, s.getScreen());
        s.handle(Action.QUESTS);
        assertEquals(Screen.SHOP, s.getScreen());
    }

    // ---- storage: no schema change, older saves gain the quest ----

    @Test
    void idolQuest_isStoredAsAnOrdinaryQuestRow_withId3() {
        assertEquals(3, SeedData.idolQuest().getId());
        assertEquals(1, SeedData.idolQuest().getGiverNpcId());
        assertEquals(100, SeedData.idolQuest().getReward());
        assertEquals("Golden Idol", SeedData.idolQuest().getRequiredItemName());
        assertEquals(isla.Quest.class, repos.quests().findById(3).orElseThrow().getClass(), "a plain Quest row");
    }

    @Test
    void aSaveFromBeforeTheIdolQuest_gainsItOnContinue() {
        Repositories old = Repositories.inMemory();
        for (Item i : SeedData.items()) old.items().save(i);
        SeedData.vendors().forEach(old.vendors()::save);
        old.quests().save(SeedData.elderQuest());
        assertTrue(old.quests().findById(3).isEmpty());

        GameSession loaded = GameBootstrap.start(old, false);
        assertTrue(old.quests().findById(3).isPresent());
        assertEquals("Bring Elder Kai a coconut", loaded.questLines().get(0));
    }

    @Test
    void aSaveWhoseCoconutQuestWasAlreadyDone_nowAsksForTheIdol() {
        Repositories old = Repositories.inMemory();
        for (Item i : SeedData.items()) old.items().save(i);
        SeedData.vendors().forEach(old.vendors()::save);
        isla.Quest done = SeedData.elderQuest();
        done.markCompleted();
        old.quests().save(done);

        GameSession loaded = GameBootstrap.start(old, false);
        assertEquals("Find the Golden Idol in the jungle", loaded.questLines().get(0));
        stand(loaded, ELDER);
        loaded.handle(Action.CONFIRM);
        assertTrue(loaded.getDialogueText().contains("Golden Idol"), loaded.getDialogueText());
    }

    @Test
    void idolQuestProgress_isSaved_andNewGameResetsIt() {
        giveCoconutAndDeliver();
        deliverIdol();
        GameBootstrap.save(repos, s);
        assertTrue(repos.quests().findById(3).orElseThrow().isCompleted());
        assertTrue(repos.quests().findById(1).orElseThrow().isCompleted());

        GameSession fresh = GameBootstrap.start(repos, true);
        assertFalse(repos.quests().findById(3).orElseThrow().isCompleted());
        assertEquals("Bring Elder Kai a coconut", fresh.questLines().get(0));
    }

    // ---- the whole game, start to finish ----

    private void solveNia() {
        stand(s, NIA);
        s.handle(Action.CONFIRM);
        assertEquals(Screen.DEBUG_PUZZLE, s.getScreen());
        DebugQuest puzzle = s.getActivePuzzle();
        while (s.getPuzzleCursor() != puzzle.getCorrectIndex()) {
            s.handle(Action.DOWN);
        }
        s.handle(Action.CONFIRM);
        closeDialogue();
    }

    private void buyFirstItemOf(int[] vendorSpot, int cursorDowns) {
        stand(s, vendorSpot);
        s.handle(Action.CONFIRM);
        assertEquals(Screen.SHOP, s.getScreen());
        for (int i = 0; i < cursorDowns; i++) s.handle(Action.DOWN);
        s.handle(Action.CONFIRM);
        assertTrue(s.getMessage().startsWith("Bought"), s.getMessage());
        s.handle(Action.CANCEL);
        assertEquals(Screen.EXPLORING, s.getScreen());
    }

    @Test
    void fullPlayThrough_coconut_machete_jungle_idol_ending() {
        // 1. Buy a Coconut from Mara ($8) and deliver it to Elder Kai (+$25).
        buyFirstItemOf(MARA, 1);
        assertEquals(42, s.getPlayer().getMoney());
        talkToElder();
        assertEquals(67, s.getPlayer().getMoney());
        closeDialogue();
        assertEquals("Find the Golden Idol in the jungle", s.questLines().get(0));

        // 2. $67 cannot cover the Machete ($30) AND the Idol ($60), so Nia's debugging puzzle (+$40) comes first.
        solveNia();
        assertEquals(107, s.getPlayer().getMoney());

        // 3. Buy the Machete from Tomas and use it to open the Jungle Path.
        buyFirstItemOf(TOMAS, 0);
        assertEquals(77, s.getPlayer().getMoney());
        s.handle(Action.INVENTORY);
        s.handle(Action.RIGHT); // Tools tab
        s.handle(Action.CONFIRM); // use the Machete
        assertTrue(s.getMessage().contains("Jungle Path"), s.getMessage());
        s.handle(Action.CANCEL);
        assertTrue(s.getPlayer().hasUnlocked("Jungle Path"));

        // 4. Walk into the jungle for real, then buy the Golden Idol from Yara ($60).
        s.getPlayer().setPosition(28, 10);
        s.handle(Action.UP);
        assertEquals(9, s.getPlayer().getY(), "the jungle tile is walkable now");
        buyFirstItemOf(YARA, 0);
        assertEquals(17, s.getPlayer().getMoney());
        assertEquals(1, s.getPlayer().getInventory().countOf(item("Golden Idol")));

        // 5. Deliver it to Elder Kai: +$100 and the ending screen.
        talkToElder();
        assertEquals(Screen.ENDING, s.getScreen());
        assertEquals(117, s.getPlayer().getMoney());
        assertEquals(0, s.getPlayer().getInventory().countOf(item("Golden Idol")));
        assertTrue(s.questLines().get(0).startsWith("Done"), s.questLines().toString());

        // 6. Keep exploring: the game carries on.
        s.handle(Action.CONFIRM);
        assertEquals(Screen.EXPLORING, s.getScreen());
    }
}
