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

    /** Première API où une commande de module ouvre un menu ({@code PlayerCommandDeclaration.Builder.opensMenu}). */
    public static final String MIN_API_VERSION = "2.11.0";
    /** Dossier du module sous plugins/Artisan/modules/, l'id de project enregistré avant. */
    public static final String PROJECT_ID = "boutique_addon";
    /** Ancienne racine, sous plugins/Boutique/, déplacée une fois dans le dossier du module. */
    static final String LEGACY_PROJECT_DIR = "editor";

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
            new ShopPlaceHolders(this).register();
        }

        var reg = getServer().getServicesManager().getRegistration(net.artisanmc.modules.api.ArtisanAPI.class);
        if (reg == null || !reg.getProvider().isCompatibleWith(MIN_API_VERSION)) {
            getLogger().severe("Boutique a besoin d'Artisan avec l'API " + MIN_API_VERSION + " ou plus récente (trouvé : "
                    + (reg == null ? "pas d'Artisan" : reg.getProvider().getApiVersion()) + ") — Boutique désactivée.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        var api = reg.getProvider();
        this.artisanApi = api;

        // Le contenu de la boutique vit dans plugins/Artisan/modules/boutique_addon/,
        // le dossier du module (design « un dossier = un module »). Le nom du dossier
        // reste l'ancien id de project : ses Versions et brouillons (clé
        // `addon:boutique_addon`) et les références qualifiées suivent. Un ancien
        // plugins/Boutique/editor/ y est déplacé une fois par Artisan ; tant que les
        // deux dossiers ont des fichiers, l'ancien reste lu et le log le dit.
        // AVANT hasContent pour que le check voie un project déjà bootstrappé.
        File legacyDir = new File(getDataFolder(), LEGACY_PROJECT_DIR);
        File projectDir = api.getProjectRoots().moduleFolder(
                PROJECT_ID, fr.iban.boutique.artisan.BoutiqueModule.ID, legacyDir.isDirectory() ? legacyDir : null);

        // Ce qui suit ÉCRIT dans ce dossier. Le core reprend son instantané au
        // moment de l'enregistrement ci-dessus — donc sur un dossier encore vide
        // — et ne peut pas deviner nos écritures ensuite : sans la déclaration en
        // fin de méthode, l'éditeur montre un project vide jusqu'à un
        // `/artisan reload` tapé à la main (constaté en prod le 2026-08-17).
        boolean wroteContent = false;

        // Contenu par défaut, ou catalogue repris du config.yml MenuAPI. AVANT
        // les migrateurs : c'est ce bootstrap qui pose le boutique/shop.yaml
        // migré, que la conversion ci-dessous bascule dans la Table au même
        // boot (l'add-on ne sert jamais un catalogue à moitié migré).
        if (!api.getProject().hasContent("boutique")) {
            try {
                // Migration depuis le texte brut capturé AVANT assainissement (le
                // config.yml sur disque n'a plus son bloc categories à ce stade).
                Optional<java.util.Map<String, String>> migrated = (hadLegacyConfig && legacyRaw != null)
                        ? LegacyConfigMigrator.migrate(legacyRaw)
                        : Optional.empty();
                if (Bootstrapper.bootstrapIfAbsent(projectDir, migrated)) {
                    wroteContent = true;
                    getLogger().warning("Contenu Boutique bootstrappé dans " + projectDir.getPath()
                            + (migrated.isPresent() ? " (catalogue migré depuis config.yml)" : " (catalogue exemple)")
                            + ".");
                }
            } catch (IOException e) {
                getLogger().severe("Erreur lors du bootstrap du contenu Boutique : " + e.getMessage());
            }
        }

        // One-shot (ADR composable-data-models §8): un project pré-migration
        // (boutique/shop.yaml) bascule vers la Table de models déclarés
        // (data/categories/). AVANT le register du module, pour que le premier
        // reloadRepo lise déjà le nouveau format ; la sync files-first
        // d'Artisan auto-pushe ensuite le nouvel arbre (LOCAL_ONLY).
        if (fr.iban.boutique.artisan.ShopDataMigrator.migrateIfNeeded(projectDir, getDataFolder())) {
            wroteContent = true;
            getLogger().warning("Catalogue migré vers data/categories/ (models déclarés) — "
                    + "l'ancien shop.yaml est archivé dans plugins/Boutique/shop.yaml.migrated.");
        }

        if (fr.iban.boutique.artisan.CatalogIconMigrator.migrateIfNeeded(projectDir, getDataFolder())) {
            wroteContent = true;
            getLogger().info("Lore du catalogue déplacé dans les icônes (sauvegarde : catalog-icon-backup).");
        }

        try {
            switch (fr.iban.boutique.artisan.ShopCommandMigrator.migrate(projectDir)) {
                case REMOVED -> {
                    wroteContent = true;
                    getLogger().info("commands/boutique.yaml retiré : /boutique est maintenant une commande du module (section Système de l'éditeur).");
                }
                case KEPT_CUSTOM -> getLogger().warning("commands/boutique.yaml a été modifié : il est gardé et garde le nom /boutique, "
                        + "la commande du module reste joignable en /boutique:boutique. Pour basculer, reporte tes changements "
                        + "dans un override de boutique:open (section Système de l'éditeur) puis supprime ce fichier.");
                case NONE -> { }
            }
        } catch (IOException e) {
            getLogger().warning("Migration de commands/boutique.yaml impossible : " + e.getMessage());
        }

        try {
            if (fr.iban.boutique.artisan.ManifestCleaner.removeLegacyProjectId(projectDir)) {
                wroteContent = true;
                getLogger().info("project_id retiré du manifest : l'identité du module est son dossier.");
            }
        } catch (IOException e) {
            getLogger().warning("Nettoyage du manifest impossible : " + e.getMessage());
        }

        // One-shot (ADR yaml-assisted-editing) : les formats de prix vivaient
        // dans config.yml où plus aucun code ne les lisait — ils deviennent des
        // réglages éditables du project, typés par le model déclaré.
        if (fr.iban.boutique.artisan.SettingsMigrator.ensureSettings(
                projectDir,
                getConfig().getString("placeholders.price-display"),
                getConfig().getString("placeholders.discount-price-display"))) {
            wroteContent = true;
            getLogger().info("Formats de prix repris dans data/shop_settings.yaml (éditables depuis l'éditeur).");
        }

        // On DÉCLARE ce qu'on vient d'écrire. Le core sert un instantané pris au
        // chargement (sémantique de reload) : sans cet appel, le contenu qu'on
        // vient de poser reste invisible en jeu comme dans l'éditeur, et l'admin
        // croit son installation ratée.
        if (wroteContent) {
            api.getProject().reload();
        }

        this.shopRepo = new ShopRepo();
        api.getModules().register(new BoutiqueModule(this, shopRepo));
        getCommand("boutiqueadmin").setExecutor(new fr.iban.boutique.artisan.BoutiqueAdminCommand(api, projectDir));
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
