package isla;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** A QuestGiver with several quests in a row (Elder Kai: coconut, then the Golden Idol). */
class QuestGiverStagesTest {

    private final Quest first = new Quest(1, 1, "Bring a coconut", 25, "Coconut");
    private final Quest second = new Quest(3, 1, "Bring the idol", 100, "Golden Idol");
    private final Item coconut = new Consumable(2, "Coconut", 8, 10);
    private final Item idol = new Souvenir(7, "Golden Idol", 60);

    private QuestGiver elder() {
        return new QuestGiver(1, 5, 5, "elder_sprite", List.of(
                new QuestGiver.Stage(first, "Please bring a coconut.", "Bring Elder Kai a coconut"),
                new QuestGiver.Stage(second, "Now the idol, please.", "Find the Golden Idol")),
                "Thank you for everything.", "Done: all wishes granted").endsTheGame();
    }

    private Player player() {
        return new Player(1, "Pj", 50, 100, 6);
    }

    @Test
    void talksAboutTheFirstQuestFirst_andTheSecondOnlyAfterIt() {
        QuestGiver g = elder();
        Player p = player();
        assertTrue(g.interact(p).getText().contains("Please bring a coconut."));
        p.getInventory().addItem(coconut);
        g.interact(p); // delivers the coconut
        assertTrue(first.isCompleted());
        assertTrue(g.interact(p).getText().contains("Now the idol, please."));
    }

    @Test
    void secondQuestItemIsNotTakenWhileTheFirstQuestIsOpen() {
        QuestGiver g = elder();
        Player p = player();
        p.getInventory().addItem(idol);
        g.interact(p);
        assertFalse(second.isCompleted());
        assertEquals(1, p.getInventory().countOf(idol));
        assertEquals(50, p.getMoney());
    }

    @Test
    void finishingTheLastQuestReturnsTheEnding_exactlyOnce() {
        QuestGiver g = elder();
        Player p = player();
        p.getInventory().addItem(coconut);
        assertEquals(InteractionResult.Kind.DIALOGUE, g.interact(p).getKind(), "coconut is not the end");
        p.getInventory().addItem(idol);
        InteractionResult end = g.interact(p);
        assertEquals(InteractionResult.Kind.ENDING, end.getKind());
        assertEquals(50 + 25 + 100, p.getMoney());
        InteractionResult after = g.interact(p);
        assertEquals(InteractionResult.Kind.DIALOGUE, after.getKind());
        assertTrue(after.getText().contains("Thank you for everything."));
    }

    @Test
    void rewardIsPaidOnce() {
        QuestGiver g = elder();
        Player p = player();
        p.getInventory().addItem(coconut);
        g.interact(p);
        p.getInventory().addItem(idol);
        p.getInventory().addItem(idol);
        g.interact(p);
        g.interact(p);
        g.interact(p);
        assertEquals(50 + 25 + 100, p.getMoney());
    }

    @Test
    void aGiverNotMarkedAsTheFinaleNeverEnds() {
        QuestGiver g = new QuestGiver(1, 5, 5, "elder_sprite", List.of(
                new QuestGiver.Stage(second, "Idol?", "Find it")), null, "Done");
        Player p = player();
        p.getInventory().addItem(idol);
        assertEquals(InteractionResult.Kind.DIALOGUE, g.interact(p).getKind());
    }

    @Test
    void goalTextFollowsTheStage() {
        QuestGiver g = elder();
        Player p = player();
        assertEquals("Bring Elder Kai a coconut", g.getGoal());
        first.markCompleted();
        assertEquals("Find the Golden Idol", g.getGoal());
        second.markCompleted();
        assertEquals("Done: all wishes granted", g.getGoal());
    }

    @Test
    void questMarkerShowsUntilEveryQuestIsDone() {
        QuestGiver g = elder();
        RecordingRendererProbe probe = new RecordingRendererProbe();
        g.render(probe);
        assertTrue(probe.labels > 0);
        first.markCompleted();
        probe.labels = 0;
        g.render(probe);
        assertTrue(probe.labels > 0, "the second quest is still open");
        second.markCompleted();
        probe.labels = 0;
        g.render(probe);
        assertEquals(0, probe.labels);
    }

    @Test
    void getQuestIsStillTheFirstQuest_andGetQuestsHasThemAll() {
        QuestGiver g = elder();
        assertSame(first, g.getQuest());
        assertEquals(List.of(first, second), g.getQuests());
    }

    @Test
    void anOrdinarySingleQuestGiverBehavesAsBefore() {
        QuestGiver g = new QuestGiver(1, 5, 5, "elder_sprite", "Legs...", first);
        Player p = player();
        assertTrue(g.interact(p).getText().contains("Quest:"));
        p.getInventory().addItem(coconut);
        assertEquals(InteractionResult.Kind.DIALOGUE, g.interact(p).getKind());
        assertTrue(g.interact(p).getText().contains("Thanks again"));
        assertEquals("Done: Bring a coconut", g.getGoal());
    }

    /** Counts drawLabel calls; everything else is ignored. */
    private static final class RecordingRendererProbe implements Renderer {
        int labels;

        @Override public void setCamera(int col, int row) { }
        @Override public void drawTile(int col, int row, String hexColor) { }
        @Override public void drawSprite(int col, int row, String spriteKey) { }
        @Override public void drawLabel(int col, int row, String text) { labels++; }
        @Override public void fillRect(int px, int py, int width, int height, String hexColor) { }
        @Override public void drawText(int px, int py, String text, String hexColor) { }
    }
}
