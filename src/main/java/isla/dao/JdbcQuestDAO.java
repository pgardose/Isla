package isla.dao;

import isla.Quest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcQuestDAO implements QuestDAO {

    private static final String SELECT =
            "SELECT quest_id, giver_npc_id, description, reward_amount, is_completed, required_item FROM quests";

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
        try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, q.getId());
            ps.setInt(2, q.getGiverNpcId());
            ps.setString(3, q.getDescription());
            ps.setInt(4, q.getReward());
            ps.setBoolean(5, q.isCompleted());
            ps.setString(6, q.getRequiredItemName());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save quest " + q.getId(), e);
        }
    }

    @Override
    public Optional<Quest> findById(int id) {
        try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(SELECT + " WHERE quest_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load quest " + id, e);
        }
    }

    @Override
    public List<Quest> findAll() {
        try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(SELECT + " ORDER BY quest_id");
             ResultSet rs = ps.executeQuery()) {
            List<Quest> all = new ArrayList<>();
            while (rs.next()) {
                all.add(map(rs));
            }
            return all;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load quests", e);
        }
    }

    @Override
    public void delete(int id) {
        try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement("DELETE FROM quests WHERE quest_id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete quest " + id, e);
        }
    }

    private Quest map(ResultSet rs) throws SQLException {
        Quest q = new Quest(rs.getInt("quest_id"), rs.getInt("giver_npc_id"), rs.getString("description"),
                rs.getInt("reward_amount"), rs.getString("required_item"));
        if (rs.getBoolean("is_completed")) {
            q.markCompleted();
        }
        return q;
    }
}
