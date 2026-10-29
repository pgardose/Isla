package isla.dao;

import isla.Item;
import isla.Player;
import isla.game.SeedData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Saves a player, their inventory counts and unlocked areas in one transaction. */
public class JdbcPlayerDAO implements PlayerDAO {

    private final Database db;
    private final ItemDAO items;

    public JdbcPlayerDAO(Database db, ItemDAO items) {
        this.db = db;
        this.items = items;
    }

    @Override
    public void save(Player p) {
        try (Connection c = db.connection()) {
            c.setAutoCommit(false);
            try {
                upsertPlayer(c, p);
                replaceInventory(c, p);
                replaceAreas(c, p);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save player " + p.getId(), e);
        }
    }

    private void upsertPlayer(Connection c, Player p) throws SQLException {
        String sql = "INSERT INTO players (player_id, name, money, stamina, pos_x, pos_y) VALUES (?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE name = VALUES(name), money = VALUES(money), stamina = VALUES(stamina), "
                + "pos_x = VALUES(pos_x), pos_y = VALUES(pos_y)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, p.getId());
            ps.setString(2, p.getName());
            ps.setInt(3, p.getMoney());
            ps.setInt(4, p.getStamina());
            ps.setInt(5, p.getX());
            ps.setInt(6, p.getY());
            ps.executeUpdate();
        }
    }

    private void replaceInventory(Connection c, Player p) throws SQLException {
        try (PreparedStatement del = c.prepareStatement("DELETE FROM inventory WHERE player_id = ?")) {
            del.setInt(1, p.getId());
            del.executeUpdate();
        }
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (Item item : p.getInventory().getItems()) {
            counts.merge(item, 1, Integer::sum);
        }
        try (PreparedStatement ins = c.prepareStatement(
                "INSERT INTO inventory (player_id, item_id, quantity) VALUES (?, ?, ?)")) {
            for (Map.Entry<Item, Integer> e : counts.entrySet()) {
                ins.setInt(1, p.getId());
                ins.setInt(2, e.getKey().getId());
                ins.setInt(3, e.getValue());
                ins.executeUpdate();
            }
        }
    }

    private void replaceAreas(Connection c, Player p) throws SQLException {
        try (PreparedStatement del = c.prepareStatement("DELETE FROM unlocked_areas WHERE player_id = ?")) {
            del.setInt(1, p.getId());
            del.executeUpdate();
        }
        try (PreparedStatement ins = c.prepareStatement(
                "INSERT INTO unlocked_areas (player_id, area_name) VALUES (?, ?)")) {
            for (String area : p.getUnlockedAreas()) {
                ins.setInt(1, p.getId());
                ins.setString(2, area);
                ins.executeUpdate();
            }
        }
    }

    @Override
    public Optional<Player> findById(int id) {
        try (Connection c = db.connection()) {
            Player p;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT name, money, stamina, pos_x, pos_y FROM players WHERE player_id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    p = new Player(id, rs.getString("name"), rs.getInt("money"),
                            SeedData.MAX_STAMINA, SeedData.INVENTORY_CAPACITY);
                    p.restoreState(rs.getInt("money"), rs.getInt("stamina"), rs.getInt("pos_x"), rs.getInt("pos_y"));
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT item_id, quantity FROM inventory WHERE player_id = ? ORDER BY inventory_id")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int itemId = rs.getInt("item_id");
                        Item item = items.findById(itemId).orElseThrow(
                                () -> new IllegalStateException("Saved inventory refers to unknown item " + itemId));
                        for (int i = 0; i < rs.getInt("quantity"); i++) {
                            p.getInventory().addItem(item);
                        }
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT area_name FROM unlocked_areas WHERE player_id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        p.unlockArea(rs.getString("area_name"));
                    }
                }
            }
            return Optional.of(p);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load player " + id, e);
        }
    }

    @Override
    public List<Player> findAll() {
        List<Integer> ids = new ArrayList<>();
        try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(
                "SELECT player_id FROM players ORDER BY player_id"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not list players", e);
        }
        List<Player> all = new ArrayList<>();
        for (int id : ids) {
            findById(id).ifPresent(all::add);
        }
        return all;
    }

    @Override
    public void delete(int id) {
        try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM players WHERE player_id = ?")) { // inventory/areas cascade
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete player " + id, e);
        }
    }
}
