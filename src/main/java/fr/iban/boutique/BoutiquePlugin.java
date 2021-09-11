package fr.iban.boutique;

import fr.iban.boutique.sql.DbAccess;
import fr.iban.boutique.sql.DbCredentials;
import fr.iban.boutique.sql.SqlStorage;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import fr.iban.boutique.commands.BoutiqueCMD;

public final class BoutiquePlugin extends JavaPlugin {
	
    private ShopManager shopManager;
    private SqlStorage sqlStorage;
	
    @Override
    public void onEnable() {
    	saveDefaultConfig();
        try {
            DbAccess.initPool(new DbCredentials(getConfig().getString("database.host"), getConfig().getString("database.user"), getConfig().getString("database.password"), getConfig().getString("database.dbname"), getConfig().getInt("database.port")));
        }catch (Exception e) {
            getLogger().severe("Erreur lors de l'initialisation de la connexion sql.");
            //Bukkit.shutdown();
        }        this.shopManager = new ShopManager(this);
        this.sqlStorage = new SqlStorage(this);
        getCommand("boutique").setExecutor(new BoutiqueCMD(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void registerListeners(Listener... listeners) {
        PluginManager pm = Bukkit.getPluginManager();
        for (Listener listener : listeners) {
            pm.registerEvents(listener, this);
        }
    }
    
    public ShopManager getShopManager() {
		return shopManager;
	}

    public SqlStorage getSqlStorage() {
        return sqlStorage;
    }
}
