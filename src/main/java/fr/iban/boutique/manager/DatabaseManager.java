package fr.iban.boutique.manager;

import fr.iban.boutique.ShopItem;
import fr.iban.boutique.ShopPlugin;
import fr.iban.boutique.sql.AbstractStorage;
import fr.iban.boutique.sql.SqlStorage;
import org.bukkit.entity.Player;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class DatabaseManager {

    private final ShopPlugin plugin;
    public AbstractStorage storage;

    public DatabaseManager(ShopPlugin plugin) {
        this.storage = new SqlStorage();
        this.plugin = plugin;
    }

    public int getTokens(Player player) {
        return storage.getTokens(player);
    }

    public CompletableFuture<Integer> getTokensAsync(Player player){
        return future(() -> getTokens(player));
    }

    public CompletableFuture<Void> addPurchaseHistory(Player player, ShopItem item){
        return future(() -> {
            storage.addPurchaseHistory(player, item);
        });
    }

    public CompletableFuture<Void> removeTokens(Player player, int amount){
        return future(() -> {
            storage.removeTokens(player, amount);
        });
    }

    public CompletableFuture<Void> addTokens(Player player, int amount){
        return future(() -> {
            storage.addTokens(player, amount);
        });
    }

    public CompletableFuture<Void> setTokens(Player player, int amount){
        return future(() -> {
            storage.setTokens(player, amount);
        });
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
