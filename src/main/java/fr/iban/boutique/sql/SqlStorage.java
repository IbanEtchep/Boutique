package fr.iban.boutique.sql;

import fr.iban.boutique.ShopItem;
import org.bukkit.ChatColor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class SqlStorage extends AbstractStorage {

    private final DataSource dataSource;

    public SqlStorage() {
        this.dataSource = DbAccess.getDataSource();
        DbTables.createTables();
    }

    public int getTokens(UUID uuid) {
        String sql = "SELECT tokens FROM igshop_players WHERE uuid=?;";
        int tokens = 0;
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        tokens = rs.getInt("tokens");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tokens;
    }

    private void updateMoney(UUID uuid, int amount, String sql) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, amount);
                ps.setInt(3, amount);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setTokens(UUID uuid, int amount) {
        String sql = "INSERT INTO igshop_players (uuid, tokens) VALUES (?, ?) ON DUPLICATE KEY UPDATE tokens=?";
        updateMoney(uuid, amount, sql);
    }

    public void addTokens(UUID uuid, int amount) {
        String sql = "INSERT INTO igshop_players (uuid, tokens) VALUES (?, ?) ON DUPLICATE KEY UPDATE tokens=tokens+?";
        updateMoney(uuid, amount, sql);
    }

    public void removeTokens(UUID uuid, int amount) {
        String sql = "UPDATE igshop_players SET tokens=tokens-? WHERE uuid=?;";
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, amount);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addPurchaseHistory(UUID uuid, ShopItem item) {
        String sql = "INSERT INTO `igshop_history`(`uuid`, `product_name`, `price`, `created_at`) VALUES (?, ?, ?, NOW());";
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, ChatColor.stripColor(item.getDisplay().getItemStack().getItemMeta().getDisplayName()));
                ps.setInt(3, item.getPrice());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
