package fr.iban.boutique.artisan;

import fr.iban.boutique.ShopPlugin;
import net.artisanmc.modules.api.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class BoutiqueModule implements ArtisanModule {
    private final ShopPlugin plugin;
    private final ShopRepo repo;
    private ArtisanAPI api;

    public BoutiqueModule(ShopPlugin plugin, ShopRepo repo) { this.plugin = plugin; this.repo = repo; }

    @Override public String getId() { return "boutique"; }
    @Override public String getSchemaVersion() { return "boutique/v1"; }

    @Override public void onEnable(ArtisanAPI api) {
        this.api = api;
        // Declared Data models (ADR composable-data-models §4): the editor
        // renders the catalog form from these — shape versioned with this jar.
        api.getModels().declare(ShopModels.item());
        api.getModels().declare(ShopModels.category());
        reloadRepo();
        // Both sources are STATIC: a stable, enumerable key-set (categories/items
        // keyed by `id`), so they're placeable in the editor's placement mode.
        // Field values (e.g. final_price) may still resolve per-player at runtime.
        api.getDataSources().register(new DataSourceDeclaration(
                "boutique:categories",
                (player, params) -> repo.categories().stream()
                        .map(this::categoryRow)
                        .collect(Collectors.toList()),
                List.of(
                        new DataSourceField("id", FieldKind.STRING, null),
                        new DataSourceField("name", FieldKind.STRING, null),
                        new DataSourceField("icon", FieldKind.STRING, null),
                        new DataSourceField("items", FieldKind.LIST, null)),
                null,
                Stability.STATIC,
                "id"));
        api.getDataSources().register(new DataSourceDeclaration(
                "boutique:items",
                (player, params) -> repo.categories().stream()
                        .flatMap(c -> c.getItems().stream())
                        .map(this::itemRow)
                        .collect(Collectors.toList()),
                List.of(
                        new DataSourceField("id", FieldKind.STRING, null),
                        new DataSourceField("name", FieldKind.STRING, null),
                        new DataSourceField("icon", FieldKind.STRING, null),
                        new DataSourceField("category", FieldKind.STRING, null),
                        new DataSourceField("price", FieldKind.NUMBER, null),
                        new DataSourceField("final_price", FieldKind.INTEGER, null),
                        new DataSourceField("lore", FieldKind.STRING, null)),
                null,
                Stability.STATIC,
                "id"));
        api.getCommands().register(new CommandDeclaration("boutique:buy", (player, args) -> {
            handleBuy(player, args);
            return kotlin.Unit.INSTANCE;
        }));
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
        row.put("final_price", (int) Math.round(i.finalPrice(repo.wholeShopDiscount())));
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
        // Declared-model Table first (ADR composable-data-models §8): the
        // migrated/bootstrapped format. Legacy boutique/shop.yaml stays as the
        // transition fallback for not-yet-migrated projects.
        for (String pid : api.getProject().projectIds()) {
            List<String> rows = api.getProject().list(pid, "data/categories").stream()
                    .filter(rel -> rel.endsWith(".yaml") && !rel.equals("_source.yaml"))
                    .collect(Collectors.toList());
            if (rows.isEmpty()) continue;
            repo.loadFromDataTable(
                    rel -> api.getProject().read(pid, "data/categories/" + rel),
                    rows,
                    readLangMap(pid),
                    readWholeShopDiscount(pid));
            return;
        }
        List<String> files = new ArrayList<>();
        Map<String, String> contents = new HashMap<>();
        for (String pid : api.getProject().projectIds()) {
            for (String rel : api.getProject().list(pid, "boutique")) {
                String content = api.getProject().read(pid, "boutique/" + rel);
                if (content != null) { files.add(pid + "/" + rel); contents.put(pid + "/" + rel, content); }
            }
        }
        repo.load(contents::get, files);
    }

    /** Default-lang translations of the project (mono-lang runtime resolution). */
    @SuppressWarnings("unchecked")
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
        Object parsed = new org.yaml.snakeyaml.Yaml().load(content);
        if (!(parsed instanceof Map)) return Map.of();
        Map<String, String> out = new HashMap<>();
        ((Map<String, Object>) parsed).forEach((k, v) -> { if (v != null) out.put(k, v.toString()); });
        return out;
    }

    @SuppressWarnings("unchecked")
    private int readWholeShopDiscount(String pid) {
        String content = api.getProject().read(pid, "data/shop_settings.yaml");
        if (content == null) return 0;
        Object parsed = new org.yaml.snakeyaml.Yaml().load(content);
        if (parsed instanceof Map<?, ?> m && m.get("value") instanceof Map<?, ?> v
                && v.get("whole_shop_discount") instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }
}
