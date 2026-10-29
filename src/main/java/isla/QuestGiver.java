package isla;

import java.util.List;
import java.util.Optional;

/**
 * An NPC that hands out quests, one after another. What interacting does depends on the quest in front of the
 * player: a fetch quest is turned in or repeated, a DebugQuest opens a puzzle.
 *
 * A giver has one or more stages. Elder Kai has two: the coconut, then the Golden Idol. He talks about the first
 * quest that is not done yet, and has a thank-you line for when every quest is finished. The giver marked with
 * endsTheGame() answers with the ending the moment its last quest is completed (once: afterwards it only says thanks).
 *
 * Nothing here is saved: the quests themselves are (in the quests table); stages, dialogue and goal text are
 * rebuilt from SeedData every time the game starts.
 */
public class QuestGiver extends NPC implements Interactable {

    /**
     * One step: the quest, what the giver says while it is open, and the one-line goal shown in the quest list.
     * Optionally: a story flag the player needs before this stage can be attempted (requiresFlag) and what is said
     * until then (lockedText), and what is said the moment the stage is completed (doneText, null = the quest's
     * own line).
     */
    public record Stage(Quest quest, String dialogue, String goal, String requiresFlag, String lockedText,
                        String doneText) {

        public Stage(Quest quest, String dialogue, String goal) {
            this(quest, dialogue, goal, null, null, null);
        }

        /** True if the player may attempt this stage now. */
        public boolean isOpenFor(Player player) {
            return requiresFlag == null || player.hasUnlocked(requiresFlag);
        }
    }

    private final int npcId;
    private final List<Stage> stages;
    private final String finishedDialogue;
    private final String finishedGoal;
    private boolean finale;

    /** A giver with a single quest (the goal text is the quest's description). */
    public QuestGiver(int npcId, int x, int y, String sprite, String dialogue, Quest quest) {
        this(npcId, x, y, sprite, List.of(new Stage(quest, dialogue, quest.getDescription())), null,
                "Done: " + quest.getDescription());
    }

    /**
     * A giver with several quests in order. finishedDialogue is what the giver says once every quest is done
     * (null = let the last quest answer as it always did); finishedGoal is the quest-list line at that point.
     */
    public QuestGiver(int npcId, int x, int y, String sprite, List<Stage> stages, String finishedDialogue,
                      String finishedGoal) {
        super(x, y, sprite, requireStages(stages).get(0).dialogue());
        this.npcId = npcId;
        this.stages = List.copyOf(stages);
        this.finishedDialogue = finishedDialogue;
        this.finishedGoal = finishedGoal;
    }

    private static List<Stage> requireStages(List<Stage> stages) {
        if (stages == null || stages.isEmpty()) {
            throw new IllegalArgumentException("A quest giver needs at least one quest");
        }
        return stages;
    }

    public int getNpcId() {
        return npcId;
    }

    /** The first quest (the only one for an ordinary giver). */
    public Quest getQuest() {
        return stages.get(0).quest();
    }

    /** Every quest this giver hands out, in order (all of them are saved). */
    public List<Quest> getQuests() {
        return stages.stream().map(Stage::quest).toList();
    }

    /** The first stage whose quest is not completed yet; empty once the giver has nothing left to ask. */
    public Optional<Stage> currentStage() {
        return stages.stream().filter(s -> !s.quest().isCompleted()).findFirst();
    }

    /** The one-line goal for the quest list: the current stage's goal, or the finished line. */
    public String getGoal() {
        return currentStage().map(Stage::goal).orElse(finishedGoal);
    }

    /** Marks this giver as the end of the story: completing its last quest shows the ending screen. */
    public QuestGiver endsTheGame() {
        this.finale = true;
        return this;
    }

    /** True if this giver's last quest ends the story. */
    public boolean isFinale() {
        return finale;
    }

    /** True once every quest this giver hands out is done. */
    public boolean isFinished() {
        return currentStage().isEmpty();
    }

    /** The line for a quest of this giver that was just completed: its stage's doneText, or the quest's own line. */
    public String doneTextFor(Quest quest) {
        return stages.stream().filter(s -> s.quest() == quest && s.doneText() != null)
                .map(Stage::doneText).findFirst().orElse(quest.completionText());
    }

    @Override
    public InteractionResult interact(Player player) {
        Stage stage = currentStage().orElse(null);
        if (stage == null) {
            if (finishedDialogue != null) {
                return InteractionResult.dialogue(finishedDialogue);
            }
            Stage last = stages.get(stages.size() - 1);
            return last.quest().interact(player, last.dialogue());
        }
        if (!stage.isOpenFor(player)) {
            return InteractionResult.dialogue(stage.lockedText() != null ? stage.lockedText() : stage.dialogue());
        }
        InteractionResult result = stage.quest().interact(player, stage.dialogue());
        boolean justCompleted = stage.quest().isCompleted(); // it was open before this call
        if (justCompleted && stage.doneText() != null && result.getKind() == InteractionResult.Kind.DIALOGUE) {
            result = InteractionResult.dialogue(stage.doneText());
        }
        boolean isLastStage = stage == stages.get(stages.size() - 1);
        if (finale && isLastStage && justCompleted) {
            return InteractionResult.ending(result.getText());
        }
        return result;
    }

    @Override
    public void render(Renderer renderer) {
        super.render(renderer);
        if (currentStage().isPresent()) {
            renderer.drawLabel(x, y, "?");
        }
    }
}
