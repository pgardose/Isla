package isla.dao;

import isla.Consumable;
import isla.Item;
import isla.Souvenir;
import isla.Tool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcItemDAO implements ItemDAO {

    private static final String SELECT = "SELECT item_id, name, type, price, effect_value, unlocks_area FROM items";

    private final Database db;

    public JdbcItemDAO(Database db) {
        this.db = db;
    }

    @Override
    public void save(Item item) {
        String sql = "INSERT INTO items (item_id, name, type, price, effect_value, unlocks_area) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE name = VALUES(name), type = VALUES(type), price = VALUES(price), "
                + "effect_value = VALUES(effect_value), unlocks_area = VALUES(unlocks_area)";
        int effect = 0;
        String area = null;
        if (item instanceof Consumable c) {
            effect = c.getStaminaRestore();
        } else if (item instanceof Tool t) {
            area = t.getUnlocksArea();
        }
        try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, item.getId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getCategory().name());
            ps.setInt(4, item.getPrice());
            ps.setInt(5, effect);
            ps.setString(6, area);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save item " + item.getId(), e);
        }
    }

    @Override
    public Optional<Item> findById(int id) {
        try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(SELECT + " WHERE item_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load item " + id, e);
        }
    }

    @Override
    public List<Item> findAll() {
        try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(SELECT + " ORDER BY item_id");
             ResultSet rs = ps.executeQuery()) {
            List<Item> all = new ArrayList<>();
            while (rs.next()) {
                all.add(map(rs));
            }
            return all;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load items", e);
        }
    }

    @Override
    public void delete(int id) {
        try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement("DELETE FROM items WHERE item_id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete item " + id, e);
        }
    }

    private Item map(ResultSet rs) throws SQLException {
        int id = rs.getInt("item_id");
        String name = rs.getString("name");
        int price = rs.getInt("price");
        return switch (rs.getString("type")) {
            case "CONSUMABLE" -> new Consumable(id, name, price, rs.getInt("effect_value"));
            case "TOOL" -> new Tool(id, name, price, rs.getString("unlocks_area"));
            case "SOUVENIR" -> new Souvenir(id, name, price);
            default -> throw new IllegalStateException("Unknown item type for item " + id);
        };
    }
}
