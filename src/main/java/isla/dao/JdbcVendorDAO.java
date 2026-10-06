package isla.dao;

import isla.Item;
import isla.Shop;
import isla.Vendor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Saves a vendor row plus its stock (shop_stock) in one transaction. */
public class JdbcVendorDAO implements VendorDAO {

    private final Database db;
    private final ItemDAO items;

    public JdbcVendorDAO(Database db, ItemDAO items) {
        this.db = db;
        this.items = items;
    }

    @Override
    public void save(Vendor v) {
        try (Connection c = db.connection()) {
            c.setAutoCommit(false);
            try {
                String sql = "INSERT INTO vendors (vendor_id, name, location_x, location_y, sprite, dialogue) "
                        + "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE name = VALUES(name), "
                        + "location_x = VALUES(location_x), location_y = VALUES(location_y), "
                        + "sprite = VALUES(sprite), dialogue = VALUES(dialogue)";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, v.getId());
                    ps.setString(2, v.getName());
                    ps.setInt(3, v.getX());
                    ps.setInt(4, v.getY());
                    ps.setString(5, v.getSprite());
                    ps.setString(6, v.getDialogue());
                    ps.executeUpdate();
                }
                try (PreparedStatement del = c.prepareStatement("DELETE FROM shop_stock WHERE vendor_id = ?")) {
                    del.setInt(1, v.getId());
                    del.executeUpdate();
                }
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO shop_stock (vendor_id, item_id, quantity_available) VALUES (?, ?, ?)")) {
                    for (Item item : v.getShop().getStock()) {
                        ins.setInt(1, v.getId());
                        ins.setInt(2, item.getId());
                        ins.setInt(3, v.getShop().quantityOf(item));
                        ins.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save vendor " + v.getId(), e);
        }
    }

    @Override
    public Optional<Vendor> findById(int id) {
        try (Connection c = db.connection()) {
            String name;
            int x;
            int y;
            String sprite;
            String dialogue;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT name, location_x, location_y, sprite, dialogue FROM vendors WHERE vendor_id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    name = rs.getString("name");
                    x = rs.getInt("location_x");
                    y = rs.getInt("location_y");
                    sprite = rs.getString("sprite");
                    dialogue = rs.getString("dialogue");
                }
            }
            Shop shop = new Shop();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT item_id, quantity_available FROM shop_stock WHERE vendor_id = ? ORDER BY stock_id")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int itemId = rs.getInt("item_id");
                        Item item = items.findById(itemId).orElseThrow(
                                () -> new IllegalStateException("Stock refers to unknown item " + itemId));
                        shop.addStock(item, rs.getInt("quantity_available"));
                    }
                }
            }
            return Optional.of(new Vendor(id, name, x, y, sprite, dialogue, shop));
        } catch (SQLException e) {
            throw new DataAccessException("Could not load vendor " + id, e);
        }
    }

    @Override
    public List<Vendor> findAll() {
        List<Integer> ids = new ArrayList<>();
        try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(
                "SELECT vendor_id FROM vendors ORDER BY vendor_id"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not list vendors", e);
        }
        List<Vendor> all = new ArrayList<>();
        for (int id : ids) {
            findById(id).ifPresent(all::add);
        }
        return all;
    }

    @Override
    public void delete(int id) {
        try (Connection c = db.connection(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM vendors WHERE vendor_id = ?")) { // stock cascades
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete vendor " + id, e);
        }
    }
}
