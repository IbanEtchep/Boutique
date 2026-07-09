package fr.iban.boutique;

import fr.iban.boutique.artisan.BoutiqueModule;
import fr.iban.boutique.artisan.Bootstrapper;
import fr.iban.boutique.artisan.LegacyConfigMigrator;
import fr.iban.boutique.artisan.ShopRepo;
import fr.iban.boutique.commands.TokensCMD;
import fr.iban.boutique.listener.JoinQuitListener;
import fr.iban.boutique.manager.DatabaseManager;
import fr.iban.boutique.manager.TransactionManager;
import fr.iban.boutique.sql.DbAccess;
import fr.iban.boutique.sql.DbCredentials;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ShopPlugin extends JavaPlugin {

    private TransactionManager transactionManager;
    private DatabaseManager databaseManager;
    private ShopRepo shopRepo;
    private static ShopPlugin instance;
    private final Map<UUID, Integer> tokensCache = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        boolean hadLegacyConfig = new File(getDataFolder(), "config.yml").exists();
        saveDefaultConfig();
        try {
            DbAccess.initPool(new DbCredentials(getConfig().getString("database.host"), getConfig().getString("database.user"), getConfig().getString("database.password"), getConfig().getString("database.dbname"), getConfig().getInt("database.port")));
        }catch (Exception e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de l'initialisation de la connexion sql.", e);
            //Bukkit.shutdown();
        }
        this.databaseManager = new DatabaseManager(this);
        this.transactionManager = new TransactionManager(this);

        getCommand("tokens").setExecutor(new TokensCMD(this));

        registerListeners(new JoinQuitListener(this));

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ShopPlaceHolders(this).register();
        }

        var reg = getServer().getServicesManager().getRegistration(net.artisanmc.modules.api.ArtisanAPI.class);
        if (reg == null) {
            getLogger().severe("Artisan introuvable — Boutique désactivée.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        var api = reg.getProvider();
        this.shopRepo = new ShopRepo();
        api.getModules().register(new BoutiqueModule(this, shopRepo));

        if (!api.getProject().hasContent("boutique")) {
            try {
                Optional<String> migrated = Optional.empty();
                if (hadLegacyConfig) {
                    File legacy = new File(getDataFolder(), "config.yml");
                    migrated = LegacyConfigMigrator.migrate(Files.readString(legacy.toPath()));
                }
                File projectsDir = new File(api.getPlugin().getDataFolder(), "projects");
                if (Bootstrapper.bootstrapIfAbsent(projectsDir, migrated)) {
                    getLogger().warning("Contenu Boutique bootstrappé dans projects/boutique_shop"
                            + (migrated.isPresent() ? " (catalogue migré depuis config.yml)" : " (catalogue exemple)")
                            + " — /artisan reload puis /artisan pushLocal boutique_shop pour synchroniser.");
                }
            } catch (IOException e) {
                getLogger().severe("Erreur lors du bootstrap du contenu Boutique : " + e.getMessage());
            }
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
