package fr.iban.boutique.sql;

import fr.iban.boutique.ShopItem;
import fr.iban.boutique.ShopPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class SqlStorage extends AbstractStorage {

    private final DataSource dataSource;

    public SqlStorage(){
        this.dataSource = DbAccess.getDataSource();
        DbTables.createTables();
    }

    public int getTokens(Player player){
        String sql = "SELECT tokens FROM igshop_players WHERE user_id=(SELECT id FROM users WHERE name=?);";
        int tokens = 0;
        try(Connection connection = dataSource.getConnection()){
            try(PreparedStatement ps = connection.prepareStatement(sql)){
                ps.setString(1, player.getName());
                try(ResultSet rs = ps.executeQuery()){
                    while(rs.next()) {
                        tokens = rs.getInt("tokens");
                    }
                }
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return tokens;
    }

    private void updateMoney(Player player, int amount, String sql){
        try (Connection connection = dataSource.getConnection()) {
            try(PreparedStatement ps = connection.prepareStatement(sql)){
                ps.setString(1, player.getName());
                ps.setInt(2, amount);
                ps.setInt(3, amount);
                ps.executeUpdate();
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setTokens(Player player, int amount){
        String sql = "INSERT INTO igshop_players (user_id, tokens) VALUES ((SELECT id FROM users WHERE name=?), ?) ON DUPLICATE KEY UPDATE tokens=?";
        updateMoney(player, amount, sql);
    }

    public void addTokens(Player player, int amount){
        String sql = "INSERT INTO igshop_players (user_id, tokens) VALUES ((SELECT id FROM users WHERE name=?), ?) ON DUPLICATE KEY UPDATE tokens=tokens+?";
        updateMoney(player, amount, sql);
    }

    public void removeTokens(Player player, int amount){
        String sql = "UPDATE igshop_players SET tokens=tokens-? WHERE user_id=(SELECT id FROM users WHERE name=?);";
        try (Connection connection = dataSource.getConnection()) {
            try(PreparedStatement ps = connection.prepareStatement(sql)){
                ps.setInt(1, amount);
                ps.setString(2, player.getName());
                ps.executeUpdate();
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void addPurchaseHistory(Player player, ShopItem item){
        String sql = "INSERT INTO `igshop_history`(`user_id`, `product_name`, `price`, `created_at`) VALUES ((SELECT id FROM users WHERE name=?), ?, ?, NOW());";
        try (Connection connection = dataSource.getConnection()) {
            try(PreparedStatement ps = connection.prepareStatement(sql)){
                ps.setString(1, player.getName());
                ps.setString(2, ChatColor.stripColor(item.getDisplay().getBuiltItemStack().getItemMeta().getDisplayName()));
                ps.setInt(3, item.getPrice());
                ps.executeUpdate();
            }

        }catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
