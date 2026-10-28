package isla;

import java.util.List;
import java.util.Optional;

/**
 * The island's main story: an ordered list of chapters, each one a Quest plus a short
 * "what do I do next" hint. The game shows the current hint on screen so the player
 * always knows what the goal is and how far along they are.
 *
 * OOP note (COMPOSITION): a Journey HAS quests; it does not change them. Each quest
 * still decides for itself whether it is available and how it is completed.
 */
public final class Journey {

    /** One step of the story. */
    public record Chapter(Quest quest, String objective) {
    }

    private final String title;
    private final String finishedText;
    private final List<Chapter> chapters;

    public Journey(String title, String finishedText, List<Chapter> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            throw new IllegalArgumentException("A journey needs at least one chapter");
        }
        this.title = title;
        this.finishedText = finishedText;
        this.chapters = List.copyOf(chapters);
    }

    public String getTitle() {
        return title;
    }

    public int size() {
        return chapters.size();
    }

    public int completedCount() {
        return (int) chapters.stream().filter(c -> c.quest().isCompleted()).count();
    }

    public boolean isComplete() {
        return completedCount() == chapters.size();
    }

    /** The first unfinished chapter the player can start right now (chapters can be done in any allowed order). */
    public Optional<Chapter> current() {
        return chapters.stream()
                .filter(c -> !c.quest().isCompleted() && c.quest().isAvailable())
                .findFirst();
    }

    /** The one-line hint to show: the next objective, or the closing line once everything is done. */
    public String objectiveText() {
        if (isComplete()) {
            return finishedText;
        }
        return current().map(Chapter::objective).orElse("Keep exploring the island.");
    }
}
