package fr.iban.boutique.artisan;

import fr.iban.boutique.ShopPlugin;
import net.artisanmc.modules.api.*;
import net.artisanmc.modules.api.action.*;
import net.artisanmc.modules.api.command.*;
import net.artisanmc.modules.api.datasource.*;
import net.artisanmc.modules.api.model.*;
import net.artisanmc.modules.api.playercommand.PlayerCommandDeclaration;
import net.artisanmc.modules.api.template.TemplateDeclaration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class BoutiqueModule implements ArtisanModule {
    private final ShopPlugin plugin;
    private final ShopRepo repo;
    private ArtisanAPI api;

    public BoutiqueModule(ShopPlugin plugin, ShopRepo repo) { this.plugin = plugin; this.repo = repo; }

    public static final String ID = "boutique";
    public static final String OPEN_COMMAND = "boutique:open";

    @Override public String getId() { return ID; }
    @Override public String getSchemaVersion() { return "boutique/v1"; }

    /** Réglages courants (rechargés avec le repo). */
    private volatile ShopSettings settings = ShopSettings.defaults();

    @Override public void onEnable(ArtisanAPI api) {
        this.api = api;
        // Declared Data models (ADR composable-data-models §4): the editor
        // renders the catalog form from these — shape versioned with this jar.
        api.getModels().declare(ShopModels.item());
        api.getModels().declare(ShopModels.category());
        api.getModels().declare(ShopModels.shopSettings());
        // Config screens (ADR module-config-screens): sidebar entries whose
        // core-generated form edits these tables live over the WS relay.
        api.getScreens().declare("boutique:catalog", "Catalogue", "data/categories", ShopPlugin.PROJECT_ID);
        api.getScreens().declare("boutique:settings", "Réglages", "data/shop_settings", ShopPlugin.PROJECT_ID);
        // Le catalogue se charge au onReload() qu'Artisan appelle au tick suivant,
        // une fois le dossier du module lu : rien ne le consulte avant.
        // Both sources are STATIC: a stable, enumerable key-set (categories/items
        // keyed by `id`), so they're placeable in the editor's placement mode.
        // Field values (e.g. final_price) may still resolve per-player at runtime.
        api.getDataSources().register(new DataSourceDeclaration(
                "boutique:categories",
                (player, params) -> repo.categories().stream()
                        .map(this::categoryRow)
                        .collect(Collectors.toList()),
                List.of(
                        new DataSourceField("id", ModelFieldType.string(), null),
                        new DataSourceField("name", ModelFieldType.string(), null),
                        new DataSourceField("icon", ModelFieldType.item(), null),
                        new DataSourceField("items", ModelFieldType.list(ModelFieldType.string()), null)),
                Stability.STATIC,
                "id"));
        api.getDataSources().register(new DataSourceDeclaration(
                "boutique:items",
                (player, params) -> repo.categories().stream()
                        .flatMap(c -> c.getItems().stream())
                        .map(this::itemRow)
                        .collect(Collectors.toList()),
                List.of(
                        new DataSourceField("id", ModelFieldType.string(), null),
                        new DataSourceField("name", ModelFieldType.string(), null),
                        new DataSourceField("icon", ModelFieldType.item(), null),
                        new DataSourceField("category", ModelFieldType.string(), null),
                        new DataSourceField("price", ModelFieldType.number(), null),
                        new DataSourceField("final_price", ModelFieldType.integer(), null),
                        // Prix déjà mis en forme par les réglages — un menu binde
                        // `{item.price_display}` au lieu de recomposer le texte.
                        new DataSourceField("price_display", ModelFieldType.string(), null),
                        new DataSourceField("lore", ModelFieldType.string(), null)),
                Stability.STATIC,
                "id"));
        api.getCommands().register(new CommandDeclaration("boutique:buy", this::handleBuy));
        api.getPlayerCommands().register(openCommand());
        api.getTemplates().register(systemMenus());
    }

    /**
     * Les menus et le dialogue que le code ouvre par leur id (API 2.12.0) : des éléments
     * système du dossier. Artisan les écrit depuis ce JAR, les met à jour tant que l'admin n'y
     * a pas touché, et l'éditeur les garde (ni suppression ni déplacement, un « Réinitialiser »).
     * Mêmes octets que le bootstrap : une install vierge les a déjà, à l'identique.
     * {@link #SYSTEM_MENUS_VERSION} monte à chaque changement de ces fichiers.
     */
    static TemplateDeclaration systemMenus() {
        return TemplateDeclaration.fromResources(BoutiqueModule.class.getClassLoader(), "bootstrap",
                        "menus", SYSTEM_MENUS_VERSION, SYSTEM_MENU_FILES)
                .title("Menus de la boutique")
                .system()
                .build();
    }

    static final int SYSTEM_MENUS_VERSION = 1;
    static final List<String> SYSTEM_MENU_FILES = List.of(
            "menus/shop_main.yaml", "menus/shop_category.yaml", "dialogs/confirm_purchase.yaml");

    /**
     * {@code /boutique} : une commande du module (API 2.11.0) qui ouvre le menu principal du
     * dossier. L'admin la renomme, change sa permission ou ce qu'elle fait par un override
     * (section Système de l'éditeur) ; elle reste joignable en {@code /boutique:boutique}.
     */
    static PlayerCommandDeclaration openCommand() {
        return PlayerCommandDeclaration.builder(OPEN_COMMAND)
                .name("boutique")
                .description(Map.of("fr", "Ouvre la boutique", "en", "Opens the shop"))
                .opensMenu(ShopPlugin.PROJECT_ID + ":shop_main")
                .build();
    }

    /** Ligne categorie avec ses items IMBRIQUES - permet aussi bien le join
     *  plat (boutique:items + filtre {category.id}) que la pagination pointee
     *  (contexte `category: {cat}` -> `iterates.source: category.items`). */
    private Map<String, Object> categoryRow(fr.iban.boutique.ShopCategory c) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", c.getId());
        row.put("name", c.getName());
        row.put("icon", c.getIcon());
        row.put("items", c.getItems().stream().map(this::itemRow).collect(Collectors.toList()));
        return row;
    }

    private Map<String, Object> itemRow(fr.iban.boutique.ShopItem i) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", i.getId());
        row.put("name", i.getName());
        row.put("icon", i.getIcon());
        row.put("category", i.getCategory().getId());
        row.put("price", i.getPrice());
        int finalPrice = (int) Math.round(i.finalPrice(repo.wholeShopDiscount()));
        row.put("final_price", finalPrice);
        row.put("price_display", settings.formatPrice(i.getPrice(), finalPrice));
        row.put("lore", String.join("\n", i.getLore()));
        return row;
    }

    @SuppressWarnings("unchecked")
    private void handleBuy(Player player, Map<String, ?> args) {
        List<String> argv = args.get("argv") instanceof List<?> l
                ? l.stream().map(String::valueOf).collect(Collectors.toList()) : List.of();
        if (argv.isEmpty()) { player.sendMessage("§cUsage interne: boutique:buy <item_id>"); return; }
        var item = repo.findItem(argv.get(0)).orElse(null);
        if (item == null) { player.sendMessage("§cCet article n'existe plus."); return; }
        plugin.getTransactionManager().buy(player, item, repo.wholeShopDiscount());
    }

    @Override public void onReload() { reloadRepo(); }
    @Override public void onDisable() {}

    private void reloadRepo() {
        // The module's own folder only: the catalog lives there and nowhere else.
        // Reading every project made the first one with a data/categories win,
        // so another module's table could replace the catalog.
        String pid = ShopPlugin.PROJECT_ID;
        // Declared-model Table first (ADR composable-data-models §8): the
        // migrated/bootstrapped format. Legacy boutique/shop.yaml stays as the
        // transition fallback for a not-yet-migrated folder.
        List<String> rows = api.getProject().list(pid, "data/categories").stream()
                .filter(rel -> rel.endsWith(".yaml") && !rel.equals("_source.yaml"))
                .collect(Collectors.toList());
        if (!rows.isEmpty()) {
            this.settings = ShopSettings.parse(api.getProject().read(pid, "data/shop_settings.yaml"));
            repo.loadFromDataTable(
                    rel -> api.getProject().read(pid, "data/categories/" + rel),
                    rows,
                    readLangMap(pid),
                    settings.wholeShopDiscount());
            return;
        }
        List<String> files = new ArrayList<>();
        Map<String, String> contents = new HashMap<>();
        for (String rel : api.getProject().list(pid, "boutique")) {
            String content = api.getProject().read(pid, "boutique/" + rel);
            if (content != null) { files.add(pid + "/" + rel); contents.put(pid + "/" + rel, content); }
        }
        repo.load(contents::get, files);
    }

    /** Default-lang translations of the project (mono-lang runtime resolution).
     *  The file is a YAML tree — `LangFlattener` folds it back to dotted keys. */
    private Map<String, String> readLangMap(String pid) {
        String lang = "fr";
        String manifest = api.getProject().read(pid, "manifest.yaml");
        if (manifest != null) {
            Object parsed = new org.yaml.snakeyaml.Yaml().load(manifest);
            if (parsed instanceof Map<?, ?> m && m.get("default_lang") instanceof String s && !s.isBlank()) {
                lang = s;
            }
        }
        String content = api.getProject().read(pid, "lang/" + lang + ".yaml");
        if (content == null) return Map.of();
        return LangFlattener.flatten(new org.yaml.snakeyaml.Yaml().load(content));
    }

}
