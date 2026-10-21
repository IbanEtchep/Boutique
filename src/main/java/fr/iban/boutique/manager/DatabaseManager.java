package fr.iban.boutique.manager;

import fr.iban.boutique.ShopItem;
import fr.iban.boutique.ShopPlugin;
import fr.iban.boutique.sql.AbstractStorage;
import fr.iban.boutique.sql.SqlStorage;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class DatabaseManager {

    private final ShopPlugin plugin;
    public AbstractStorage storage;

    /** JDBC blocks: its own threads, not the JVM's common pool every plugin shares. */
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Boutique-db");
        t.setDaemon(true);
        return t;
    });

    public DatabaseManager(ShopPlugin plugin) {
        this.storage = new SqlStorage();
        this.plugin = plugin;
        // The tables, off onEnable: an unreachable database costs nothing on the main thread.
        executor.execute(fr.iban.boutique.sql.DbTables::createTables);
    }

    public void shutdown() {
        executor.shutdown();
    }

    public int getTokens(UUID uuid) {
        return storage.getTokens(uuid);
    }

    public CompletableFuture<Integer> getTokensAsync(UUID uuid){
        return future(() -> getTokens(uuid));
    }

    public CompletableFuture<Void> addPurchaseHistory(UUID uuid, ShopItem item){
        return future(() -> {
            storage.addPurchaseHistory(uuid, item);
        });
    }

    public CompletableFuture<Void> removeTokens(UUID uuid, int amount){
        return future(() -> {
            storage.removeTokens(uuid, amount);
        });
    }

    /** Debits [amount] if the balance covers it: the check and the debit are one statement, so two purchases cannot both pass. */
    public CompletableFuture<Boolean> removeTokensIfEnough(UUID uuid, int amount){
        return future(() -> storage.removeTokensIfEnough(uuid, amount));
    }

    public CompletableFuture<Void> addTokens(UUID uuid, int amount){
        return future(() -> {
            storage.addTokens(uuid, amount);
        });
    }

    public CompletableFuture<Void> setTokens(UUID uuid, int amount){
        return future(() -> {
            storage.setTokens(uuid, amount);
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
        }, executor);
    }

    public CompletableFuture<Void> future(Runnable runnable) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw e;
            }
        }, executor);
    }
}
