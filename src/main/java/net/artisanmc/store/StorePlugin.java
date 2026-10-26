package net.artisanmc.store;

import net.artisanmc.store.artisan.StoreModule;
import net.artisanmc.store.artisan.Bootstrapper;
import net.artisanmc.store.artisan.LegacyConfigMigrator;
import net.artisanmc.store.artisan.Migrations;
import net.artisanmc.store.artisan.ShopRepo;
import net.artisanmc.store.commands.TokensCMD;
import net.artisanmc.store.listener.JoinQuitListener;
import net.artisanmc.store.manager.DatabaseManager;
import net.artisanmc.store.manager.TransactionManager;
import net.artisanmc.store.sql.DbAccess;
import net.artisanmc.store.sql.DbCredentials;
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

public final class StorePlugin extends JavaPlugin {

    /** Première API où une commande de module ouvre un menu ({@code PlayerCommandDeclaration.Builder.opensMenu}). */
    public static final String MIN_API_VERSION = "2.12.0";
    /** Dossier du module sous plugins/Artisan/modules/, l'id de project enregistré avant. */
    public static final String PROJECT_ID = "boutique_addon";
    /** Ancienne racine, sous le dossier du plugin, déplacée une fois dans le dossier du module. */
    static final String LEGACY_PROJECT_DIR = "editor";

    private TransactionManager transactionManager;
    private DatabaseManager databaseManager;
    private ShopRepo shopRepo;
    private StoreModule module;
    private net.artisanmc.modules.api.ArtisanAPI artisanApi;
    private static StorePlugin instance;
    /** Écrit depuis les tâches async (join, achat), lu sur le thread principal (placeholders). */
    private final Map<UUID, Integer> tokensCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        // Boutique.jar et ArtisanStore.jar ensemble déclareraient deux fois le module boutique
        // et se partageraient la même base : un seul des deux tourne.
        if (getServer().getPluginManager().getPlugin(LegacyDataFolder.LEGACY_NAME) != null) {
            getLogger().severe(LegacyDataFolder.LEGACY_NAME + ".jar est encore dans plugins/ : ArtisanStore le remplace."
                    + " Retirez-le puis redémarrez. ArtisanStore désactivé.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // AVANT toute lecture de getDataFolder() : config.yml (la base) et migrations.done
        // sont ceux de plugins/Boutique/ tant qu'il n'a pas été renommé.
        File legacyFolder = new File(getDataFolder().getParentFile(), LegacyDataFolder.LEGACY_NAME);
        try {
            switch (LegacyDataFolder.migrate(legacyFolder, getDataFolder())) {
                case MOVED -> getLogger().warning(legacyFolder.getPath() + " renommé en " + getDataFolder().getPath()
                        + " (config.yml, migrations.done et sauvegardes inchangés).");
                case BOTH -> getLogger().warning(legacyFolder.getPath() + " et " + getDataFolder().getPath()
                        + " existent tous les deux : seul le second est lu. Supprimez celui que vous ne gardez pas.");
                case NONE -> { }
            }
        } catch (IOException e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Renommage de " + legacyFolder.getPath() + " en "
                    + getDataFolder().getPath() + " impossible : ArtisanStore désactivé pour ne pas repartir d'une base vide.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        boolean hadLegacyConfig = new File(getDataFolder(), "config.yml").exists();
        // Un config legacy contient des blocs sérialisés MenuAPI (`==: menuitem`) que
        // YamlConfiguration ne peut plus désérialiser — il faut capturer le texte brut
        // (pour le migrateur) et assainir le fichier AVANT le premier getConfig().
        // Source du catalogue : config.yml.legacy s'il existe (copie d'avant
        // assainissement), sinon config.yml. Sans ça, un plugin qui meurt entre
        // l'assainissement et le bootstrap perd le catalogue pour de bon — le
        // boot suivant ne voit plus qu'un config.yml amputé et pose l'exemple.
        String legacyRaw = LegacyConfigMigrator.readLegacySource(getDataFolder());
        if (hadLegacyConfig) {
            try {
                File configFile = new File(getDataFolder(), "config.yml");
                String onDisk = Files.readString(configFile.toPath());
                if (onDisk.contains("==:")) {
                    if (legacyRaw == null) legacyRaw = onDisk;
                    // Ne JAMAIS écraser une copie de référence existante : elle
                    // porte le catalogue, celle-ci pourrait déjà en être amputée.
                    File legacyCopy = new File(getDataFolder(), "config.yml.legacy");
                    if (!legacyCopy.isFile()) Files.writeString(legacyCopy.toPath(), onDisk);
                    // On assainit le fichier VIVANT, pas la copie : le config.yml
                    // porte aussi database/messages, éventuellement édités depuis.
                    Files.writeString(configFile.toPath(), LegacyConfigMigrator.stripCategoriesBlock(onDisk));
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
            new ShopPlaceHolders(this, "store").register();
            new ShopPlaceHolders(this, "boutique").register();
        }

        var reg = getServer().getServicesManager().getRegistration(net.artisanmc.modules.api.ArtisanAPI.class);
        if (reg == null || !reg.getProvider().isCompatibleWith(MIN_API_VERSION)) {
            getLogger().severe("ArtisanStore a besoin d'Artisan avec l'API " + MIN_API_VERSION + " ou plus récente (trouvé : "
                    + (reg == null ? "pas d'Artisan" : reg.getProvider().getApiVersion()) + ") — ArtisanStore désactivé.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        var api = reg.getProvider();
        this.artisanApi = api;

        // Le contenu de la boutique vit dans plugins/Artisan/modules/boutique_addon/,
        // le dossier du module (design « un dossier = un module »). Le nom du dossier
        // reste l'ancien id de project : ses Versions et brouillons (clé
        // `addon:boutique_addon`) et les références qualifiées suivent. Un ancien
        // plugins/ArtisanStore/editor/ y est déplacé une fois par Artisan ; tant que les
        // deux dossiers ont des fichiers, l'ancien reste lu et le log le dit.
        // AVANT hasContent pour que le check voie un project déjà bootstrappé.
        File legacyDir = new File(getDataFolder(), LEGACY_PROJECT_DIR);
        File projectDir = api.getProjectRoots().moduleFolder(
                PROJECT_ID, net.artisanmc.store.artisan.StoreModule.ID, legacyDir.isDirectory() ? legacyDir : null);

        // Ce qui suit ÉCRIT dans ce dossier. Artisan lit le dossier enregistré
        // ci-dessus au tick suivant, une fois ce onEnable terminé : il voit ces
        // écritures, et appelle le onReload() du module. Aucune relecture à
        // demander ici (elle relisait tous les projects et faisait attendre le
        // module sur le verrou de cette relecture).
        Migrations migrations = new Migrations(getDataFolder());

        // Contenu par défaut, ou catalogue repris du config.yml MenuAPI. AVANT
        // les migrateurs : c'est ce bootstrap qui pose le boutique/shop.yaml
        // migré, que la conversion ci-dessous bascule dans la Table au même
        // boot (l'add-on ne sert jamais un catalogue à moitié migré). Le
        // bootstrap regarde lui-même le manifest du dossier : rien à parcourir.
        try {
            // Migration depuis le texte brut capturé AVANT assainissement (le
            // config.yml sur disque n'a plus son bloc categories à ce stade).
            Optional<java.util.Map<String, String>> migrated = (hadLegacyConfig && legacyRaw != null)
                    ? LegacyConfigMigrator.migrate(legacyRaw)
                    : Optional.empty();
            if (Bootstrapper.bootstrapIfAbsent(projectDir, migrated)) {
                getLogger().warning("Contenu du Store bootstrappé dans " + projectDir.getPath()
                        + (migrated.isPresent() ? " (catalogue migré depuis config.yml)" : " (catalogue exemple)")
                        + ".");
            }
        } catch (IOException e) {
            getLogger().severe("Erreur lors du bootstrap du contenu du Store : " + e.getMessage());
        }

        // One-shot (ADR composable-data-models §8): un project pré-migration
        // (boutique/shop.yaml) bascule vers la Table de models déclarés
        // (data/categories/). AVANT le register du module, pour que le premier
        // reloadRepo lise déjà le nouveau format ; la sync files-first
        // d'Artisan auto-pushe ensuite le nouvel arbre (LOCAL_ONLY).
        if (net.artisanmc.store.artisan.ShopDataMigrator.migrateIfNeeded(projectDir, getDataFolder())) {
            getLogger().warning("Catalogue migré vers data/categories/ (models déclarés) — "
                    + "l'ancien shop.yaml est archivé dans " + new File(getDataFolder(), "shop.yaml.migrated").getPath() + ".");
        }

        // Reparse chaque catégorie pour savoir s'il y a quelque chose à faire : une fois suffit.
        if (!migrations.isDone("catalog-icons")) {
            if (net.artisanmc.store.artisan.CatalogIconMigrator.migrateIfNeeded(projectDir, getDataFolder())) {
                getLogger().info("Lore du catalogue déplacé dans les icônes (sauvegarde : catalog-icon-backup).");
            }
            migrations.markDone("catalog-icons");
        }

        // Les catégories vivent dans catalogue/<id>.yml (2026-10-24) : celles de
        // l'ancien boutique_categories/, et celles qu'un brouillon a rangées dans
        // un dossier thématique (catalogue/boutique_categories/), y sont déplacées.
        try {
            net.artisanmc.store.artisan.CategoryKindMigrator.Result relocated =
                    net.artisanmc.store.artisan.CategoryKindMigrator.relocate(projectDir.toPath());
            if (relocated.changed()) {
                getLogger().info("Catégories déplacées vers catalogue/ : " + String.join(", ", relocated.moved()));
            }
            if (!relocated.skipped().isEmpty()) {
                getLogger().warning("Catégories laissées en place (même id déjà dans catalogue/) : "
                        + String.join(", ", relocated.skipped()));
            }
        } catch (IOException e) {
            getLogger().warning("Déplacement des catégories vers catalogue/ impossible : " + e.getMessage());
        }

        // La Table data/categories/ devient des Documents boutique_category
        // (catalogue/<id>.yml), éditables dans le Catalogue de l'éditeur. À
        // chaque démarrage (idempotent) : une catégorie dont le Document existe
        // déjà reste dans la Table, jamais écrasée.
        try {
            net.artisanmc.store.artisan.CategoryKindMigrator.Result moved =
                    net.artisanmc.store.artisan.CategoryKindMigrator.migrate(projectDir.toPath(), getDataFolder().toPath());
            if (moved.changed()) {
                getLogger().info("Catalogue déplacé vers catalogue/ : " + String.join(", ", moved.moved())
                        + " (sauvegarde : category-kind-backup).");
            }
            if (!moved.skipped().isEmpty()) {
                getLogger().warning("Catégories laissées dans data/categories/ (leur Document existe déjà) : "
                        + String.join(", ", moved.skipped()));
            }
        } catch (IOException e) {
            getLogger().warning("Migration du catalogue vers catalogue/ impossible : " + e.getMessage());
        }

        try {
            switch (net.artisanmc.store.artisan.ShopCommandMigrator.migrate(projectDir)) {
                case REMOVED ->
                    getLogger().info("Ancien commands/boutique.yaml retiré : Artisan écrit la commande du module /store (alias /boutique).");
                case KEPT_CUSTOM -> {
                    if (!migrations.isDone("shop-command-kept")) {
                        getLogger().info("commands/boutique.yaml a été modifié : il est gardé et /boutique reste la sienne. La commande "
                                + "du module est /store ; son alias /boutique s'efface devant ce fichier.");
                        migrations.markDone("shop-command-kept");
                    }
                }
                case NONE -> { }
            }
        } catch (IOException e) {
            getLogger().warning("Migration de commands/boutique.yaml impossible : " + e.getMessage());
        }

        try {
            if (net.artisanmc.store.artisan.ManifestCleaner.removeLegacyProjectId(projectDir)) {
                getLogger().info("project_id retiré du manifest : l'identité du module est son dossier.");
            }
        } catch (IOException e) {
            getLogger().warning("Nettoyage du manifest impossible : " + e.getMessage());
        }

        // One-shot (ADR yaml-assisted-editing) : les formats de prix et les messages vivaient
        // dans config.yml ; ils deviennent des réglages éditables du dossier du module, typés
        // par le model déclaré. Les valeurs du config.yml ne priment qu'une fois chacune :
        // ensuite, un admin qui remet exactement les défauts les garde. Une fois les réglages
        // en place, config.yml ne garde que la base de données, propre à ce serveur.
        boolean legacyFormats = !migrations.isDone("settings-placeholders");
        boolean legacyMessages = !migrations.isDone("settings-messages");
        if (net.artisanmc.store.artisan.SettingsMigrator.ensureSettings(projectDir,
                net.artisanmc.store.artisan.SettingsMigrator.legacy(getConfig(), legacyFormats, legacyMessages))) {
            getLogger().info("Formats de prix et messages repris dans data/shop_settings (éditables depuis l'éditeur, Réglages).");
        }
        migrations.markDone("settings-placeholders");
        if (net.artisanmc.store.artisan.SettingsMigrator.holdsContent(projectDir)) {
            migrations.markDone("settings-messages");
            try {
                if (net.artisanmc.store.artisan.SettingsMigrator.slimConfig(new File(getDataFolder(), "config.yml"))) {
                    reloadConfig();
                    getLogger().info("config.yml ne garde que la base de données : le reste est dans "
                            + projectDir.getPath() + "/data/shop_settings (copie d'avant : config.yml.before-settings).");
                }
            } catch (IOException e) {
                getLogger().warning("Nettoyage de config.yml impossible : " + e.getMessage());
            }
        }

        this.shopRepo = new ShopRepo();
        this.module = new StoreModule(this, shopRepo);
        api.getModules().register(module);
        getCommand("storeadmin").setExecutor(new net.artisanmc.store.artisan.StoreAdminCommand(api, projectDir));
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.shutdown();
        DbAccess.closePool();
    }

    private void registerListeners(Listener... listeners) {
        PluginManager pm = Bukkit.getPluginManager();
        for (Listener listener : listeners) {
            pm.registerEvents(listener, this);
        }
    }

    public static StorePlugin getInstance() {
        return instance;
    }

    public net.artisanmc.modules.api.ArtisanAPI getArtisanApi() {
        return artisanApi;
    }

    /** Les réglages du dossier du module ({@code data/shop_settings}) : messages, formats de prix. */
    public net.artisanmc.store.artisan.ShopSettings getSettings() {
        return module != null ? module.settings() : net.artisanmc.store.artisan.ShopSettings.defaults();
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
