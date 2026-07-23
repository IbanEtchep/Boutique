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
    private net.artisanmc.modules.api.ArtisanAPI artisanApi;
    private static ShopPlugin instance;
    private final Map<UUID, Integer> tokensCache = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        boolean hadLegacyConfig = new File(getDataFolder(), "config.yml").exists();
        // Un config legacy contient des blocs sérialisés MenuAPI (`==: menuitem`) que
        // YamlConfiguration ne peut plus désérialiser — il faut capturer le texte brut
        // (pour le migrateur) et assainir le fichier AVANT le premier getConfig().
        String legacyRaw = null;
        if (hadLegacyConfig) {
            try {
                File configFile = new File(getDataFolder(), "config.yml");
                legacyRaw = Files.readString(configFile.toPath());
                if (legacyRaw.contains("==:")) {
                    Files.writeString(new File(getDataFolder(), "config.yml.legacy").toPath(), legacyRaw);
                    Files.writeString(configFile.toPath(), LegacyConfigMigrator.stripCategoriesBlock(legacyRaw));
                    getLogger().warning("config.yml contenait des blocs MenuAPI sérialisés — original sauvegardé dans config.yml.legacy, bloc categories retiré (le catalogue est migré vers le project Artisan).");
                }
            } catch (IOException e) {
                getLogger().log(java.util.logging.Level.SEVERE, "Lecture/assainissement de config.yml impossible.", e);
            }
        }
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
        this.artisanApi = api;

        // Le project boutique_shop vit dans le dossier plugin de CET add-on, pas dans
        // le core plugins/Artisan/projects/ — le core le lit/synchronise via le root
        // enregistré (ADR addon-owned-project-roots). Le dossier plugins/Boutique/UI/
        // contient directement le contenu du project. Enregistrer AVANT hasContent pour
        // que le check voie un project déjà bootstrappé aux démarrages suivants.
        File projectDir = new File(getDataFolder(), "UI");
        api.getProjectRoots().register("boutique_shop", projectDir);

        this.shopRepo = new ShopRepo();
        api.getModules().register(new BoutiqueModule(this, shopRepo));
        getCommand("boutiqueadmin").setExecutor(new fr.iban.boutique.artisan.BoutiqueAdminCommand(api));

        if (!api.getProject().hasContent("boutique")) {
            try {
                // Migration depuis le texte brut capturé AVANT assainissement (le
                // config.yml sur disque n'a plus son bloc categories à ce stade).
                Optional<java.util.Map<String, String>> migrated = (hadLegacyConfig && legacyRaw != null)
                        ? LegacyConfigMigrator.migrate(legacyRaw)
                        : Optional.empty();
                if (Bootstrapper.bootstrapIfAbsent(projectDir, migrated)) {
                    getLogger().warning("Contenu Boutique bootstrappé dans plugins/Boutique/UI"
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

    public net.artisanmc.modules.api.ArtisanAPI getArtisanApi() {
        return artisanApi;
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
