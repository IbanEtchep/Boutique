package fr.iban.boutique.sql;

import fr.iban.boutique.BoutiquePlugin;
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
    private BoutiquePlugin plugin;

    public SqlStorage(BoutiquePlugin plugin){
        this.plugin = plugin;
        this.dataSource = DbAccess.getDataSource();
    }

    public int getTokens(Player player){
        String sql = "SELECT money FROM users WHERE name LIKE ?;";
        int tokens = 0;
        try(Connection connection = dataSource.getConnection()){
            try(PreparedStatement ps = connection.prepareStatement(sql)){
                ps.setString(1, player.getName());
                try(ResultSet rs = ps.executeQuery()){
                    while(rs.next()) {
                        tokens = rs.getInt("money");
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
                ps.setInt(1, amount);
                ps.setString(2, player.getName());
                ps.executeUpdate();
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setTokens(Player player, int amount){
        String sql = "UPDATE users SET money=? WHERE name LIKE ?;";
        updateMoney(player, amount, sql);
    }

    public void addTokens(Player player, int amount){
        String sql = "UPDATE users SET money=money+? WHERE name LIKE ?;";
        updateMoney(player, amount, sql);
    }

    public void removeTokens(Player player, int amount){
        String sql = "UPDATE users SET money=money-? WHERE name LIKE ?;";
        updateMoney(player, amount, sql);
    }

    public CompletableFuture<Integer> getTokensAsync(Player player){
        return future(() -> getTokens(player));
    }

    public <T> CompletableFuture<T> future(Callable<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.call();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new CompletionException(e);
            }
        });
    }

    public CompletableFuture<Void> future(Runnable runnable) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new CompletionException(e);
            }
        });
    }

}
