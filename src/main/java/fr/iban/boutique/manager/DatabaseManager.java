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

    public int getTokens(String playerName) {
        return storage.getTokens(playerName);
    }

    public CompletableFuture<Integer> getTokensAsync(String playerName){
        return future(() -> getTokens(playerName));
    }

    public CompletableFuture<Void> addPurchaseHistory(String playerName, ShopItem item){
        return future(() -> {
            storage.addPurchaseHistory(playerName, item);
        });
    }

    public CompletableFuture<Void> removeTokens(String playerName, int amount){
        return future(() -> {
            storage.removeTokens(playerName, amount);
        });
    }

    public CompletableFuture<Void> addTokens(String playerName, int amount){
        return future(() -> {
            storage.addTokens(playerName, amount);
        });
    }

    public CompletableFuture<Void> setTokens(String playerName, int amount){
        return future(() -> {
            storage.setTokens(playerName, amount);
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
