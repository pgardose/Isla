package isla.dao;

import isla.DebugQuest;
import isla.Quest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcQuestDAO implements QuestDAO {

    /** debug_quests is LEFT JOINed: its columns are NULL for ordinary fetch quests. */
    private static final String SELECT =
            "SELECT q.quest_id, q.giver_npc_id, q.description, q.reward_amount, q.is_completed, q.required_item, "
                    + "d.snippet, d.correct_index FROM quests q LEFT JOIN debug_quests d ON d.quest_id = q.quest_id";

    private final Database db;

    public JdbcQuestDAO(Database db) {
        this.db = db;
    }

    @Override
    public void save(Quest q) {
        String sql = "INSERT INTO quests (quest_id, giver_npc_id, description, reward_amount, is_completed, required_item) "
                + "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE giver_npc_id = VALUES(giver_npc_id), "
                + "description = VALUES(description), reward_amount = VALUES(reward_amount), "
                + "is_completed = VALUES(is_completed), required_item = VALUES(required_item)";
        try (Connection c = db.connection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, q.getId());
                    ps.setInt(2, q.getGiverNpcId());
                    ps.setString(3, q.getDescription());
                    ps.setInt(4, q.getReward());
                    ps.setBoolean(5, q.isCompleted());
                    ps.setString(6, q.getRequiredItemName());
                    ps.executeUpdate();
                }
                if (q instanceof DebugQuest dq) {
                    savePuzzle(c, dq);
                } else {
                    // A quest that stopped being a puzzle must not keep stale puzzle rows.
                    deletePuzzle(c, q.getId());
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save quest " + q.getId(), e);
        }
    }

    private void savePuzzle(Connection c, DebugQuest dq) throws SQLException {
        String upsert = "INSERT INTO debug_quests (quest_id, snippet, correct_index) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE snippet = VALUES(snippet), correct_index = VALUES(correct_index)";
        try (PreparedStatement ps = c.prepareStatement(upsert)) {
            ps.setInt(1, dq.getId());
            ps.setString(2, dq.getSnippet());
            ps.setInt(3, dq.getCorrectIndex());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM quest_options WHERE quest_id = ?")) {
            ps.setInt(1, dq.getId());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO quest_options (quest_id, option_index, option_text) VALUES (?, ?, ?)")) {
            List<String> options = dq.getOptions();
            for (int i = 0; i < options.size(); i++) {
                ps.setInt(1, dq.getId());
                ps.setInt(2, i);
                ps.setString(3, options.get(i));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void deletePuzzle(Connection c, int questId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM debug_quests WHERE quest_id = ?")) {
            ps.setInt(1, questId);
            ps.executeUpdate(); // quest_options rows go with it (ON DELETE CASCADE)
        }
    }

    @Override
    public Optional<Quest> findById(int id) {
        try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(SELECT + " WHERE q.quest_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(c, rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load quest " + id, e);
        }
    }

    @Override
    public List<Quest> findAll() {
        try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(SELECT + " ORDER BY q.quest_id");
             ResultSet rs = ps.executeQuery()) {
            List<Quest> all = new ArrayList<>();
            while (rs.next()) {
                all.add(map(c, rs));
            }
            return all;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load quests", e);
        }
    }

    @Override
    public void delete(int id) {
        // debug_quests and quest_options rows are removed by ON DELETE CASCADE.
        try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement("DELETE FROM quests WHERE quest_id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete quest " + id, e);
        }
    }

    private Quest map(Connection c, ResultSet rs) throws SQLException {
        int id = rs.getInt("quest_id");
        String snippet = rs.getString("snippet");
        Quest q;
        if (snippet == null) {
            q = new Quest(id, rs.getInt("giver_npc_id"), rs.getString("description"),
                    rs.getInt("reward_amount"), rs.getString("required_item"));
        } else {
            q = new DebugQuest(id, rs.getInt("giver_npc_id"), rs.getString("description"),
                    rs.getInt("reward_amount"), snippet, loadOptions(c, id), rs.getInt("correct_index"));
        }
        if (rs.getBoolean("is_completed")) {
            q.markCompleted();
        }
        return q;
    }

    private List<String> loadOptions(Connection c, int questId) throws SQLException {
        List<String> options = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT option_text FROM quest_options WHERE quest_id = ? ORDER BY option_index")) {
            ps.setInt(1, questId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    options.add(rs.getString(1));
                }
            }
        }
        return options;
    }
}
