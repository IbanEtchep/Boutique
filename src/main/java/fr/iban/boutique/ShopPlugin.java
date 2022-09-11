package fr.iban.boutique;

import fr.iban.boutique.commands.TokensCMD;
import fr.iban.boutique.listener.JoinQuitListener;
import fr.iban.boutique.manager.ShopManager;
import fr.iban.boutique.manager.DatabaseManager;
import fr.iban.boutique.manager.TransactionManager;
import fr.iban.boutique.sql.DbAccess;
import fr.iban.boutique.sql.DbCredentials;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import fr.iban.boutique.commands.BoutiqueCMD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ShopPlugin extends JavaPlugin {
	
    private ShopManager shopManager;
    private TransactionManager transactionManager;
    private DatabaseManager databaseManager;
    private static ShopPlugin instance;
    private final Map<UUID, Integer> tokensCache = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
    	saveDefaultConfig();
        try {
            DbAccess.initPool(new DbCredentials(getConfig().getString("database.host"), getConfig().getString("database.user"), getConfig().getString("database.password"), getConfig().getString("database.dbname"), getConfig().getInt("database.port")));
        }catch (Exception e) {
            getLogger().severe("Erreur lors de l'initialisation de la connexion sql.");
            //Bukkit.shutdown();
        }
        this.shopManager = new ShopManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.transactionManager = new TransactionManager(this);

        getCommand("boutique").setExecutor(new BoutiqueCMD(this));
        getCommand("tokens").setExecutor(new TokensCMD(this));

        registerListeners(new JoinQuitListener(this));

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ShopPlaceHolders(this).register();
        }
    }

    @Override
    public void onDisable() {
        DbAccess.closePool();
    }

    private void registerListeners(Listener... listeners) {
        PluginManager pm = Bukkit.getPluginManager();
        for (Listener listener : listeners) {
            pm.registerEvents(listener, this);
        }
    }

    public static ShopPlugin getInstance() {
        return instance;
    }

    public ShopManager getShopManager() {
		return shopManager;
	}

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public Map<UUID, Integer> getTokensCache() {
        return tokensCache;
    }
}
