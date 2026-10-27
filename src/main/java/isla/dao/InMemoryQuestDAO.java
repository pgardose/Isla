package isla.dao;

import isla.DebugQuest;
import isla.Quest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

public class InMemoryQuestDAO implements QuestDAO {

    /** The extra fields only a DebugQuest has. */
    private record Puzzle(String snippet, List<String> options, int correctIndex) {
    }

    private record Row(int giverNpcId, String description, int reward, String requiredItem, boolean completed,
                       Puzzle puzzle) {
    }

    private final TreeMap<Integer, Row> store = new TreeMap<>();

    @Override
    public void save(Quest q) {
        Puzzle puzzle = q instanceof DebugQuest dq
                ? new Puzzle(dq.getSnippet(), dq.getOptions(), dq.getCorrectIndex()) : null;
        store.put(q.getId(), new Row(q.getGiverNpcId(), q.getDescription(), q.getReward(),
                q.getRequiredItemName(), q.isCompleted(), puzzle));
    }

    @Override
    public Optional<Quest> findById(int id) {
        return Optional.ofNullable(store.get(id)).map(row -> rebuild(id, row));
    }

    @Override
    public List<Quest> findAll() {
        List<Quest> all = new ArrayList<>();
        store.forEach((id, row) -> all.add(rebuild(id, row)));
        return all;
    }

    @Override
    public void delete(int id) {
        store.remove(id);
    }

    private Quest rebuild(int id, Row row) {
        Quest q = row.puzzle() == null
                ? new Quest(id, row.giverNpcId(), row.description(), row.reward(), row.requiredItem())
                : new DebugQuest(id, row.giverNpcId(), row.description(), row.reward(),
                        row.puzzle().snippet(), row.puzzle().options(), row.puzzle().correctIndex());
        if (row.completed()) {
            q.markCompleted();
        }
        return q;
    }
}
