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

    /** One step of the story: its quest, the "what to do next" line, and the chapter it belongs to. */
    public record Chapter(Quest quest, String objective, String part) {

        public Chapter(Quest quest, String objective) {
            this(quest, objective, null);
        }
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
                .filter(c -> !c.quest().isCompleted())
                .findFirst();
    }

    /** The chapter name of the current step ("Chapter 2: ..."), or the journey title once it is complete. */
    public String partText() {
        return current().map(Chapter::part).filter(p -> p != null).orElse(title);
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    /** The one-line hint to show: the next objective, or the closing line once everything is done. */
    public String objectiveText() {
        if (isComplete()) {
            return finishedText;
        }
        return current().map(Chapter::objective).orElse("Keep exploring the island.");
    }
}
