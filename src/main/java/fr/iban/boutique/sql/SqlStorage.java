package fr.iban.boutique.sql;

import fr.iban.boutique.ShopItem;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SqlStorage extends AbstractStorage {

    private final DataSource dataSource;

    public SqlStorage() {
        this.dataSource = DbAccess.getDataSource();
        DbTables.createTables();
    }

    public int getTokens(String playerName) {
        String sql = "SELECT tokens FROM igshop_players WHERE user_id=(SELECT id FROM users WHERE name=?);";
        int tokens = 0;
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, playerName);
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

    private void updateMoney(String playerName, int amount, String sql) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, playerName);
                ps.setInt(2, amount);
                ps.setInt(3, amount);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setTokens(String playerName, int amount) {
        String sql = "INSERT INTO igshop_players (user_id, tokens) VALUES ((SELECT id FROM users WHERE name=?), ?) ON DUPLICATE KEY UPDATE tokens=?";
        updateMoney(playerName, amount, sql);
    }

    public void addTokens(String playerName, int amount) {
        String sql = "INSERT INTO igshop_players (user_id, tokens) VALUES ((SELECT id FROM users WHERE name=?), ?) ON DUPLICATE KEY UPDATE tokens=tokens+?";
        updateMoney(playerName, amount, sql);
    }

    public void removeTokens(String playerName, int amount) {
        String sql = "UPDATE igshop_players SET tokens=tokens-? WHERE user_id=(SELECT id FROM users WHERE name=?);";
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, amount);
                ps.setString(2, playerName);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addPurchaseHistory(String playerName, ShopItem item) {
        String sql = "INSERT INTO `igshop_history`(`user_id`, `product_name`, `price`, `created_at`) VALUES ((SELECT id FROM users WHERE name=?), ?, ?, NOW());";
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, playerName);
                ps.setString(2, ChatColor.stripColor(item.getDisplay().getItemStack().getItemMeta().getDisplayName()));
                ps.setInt(3, item.getPrice());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
