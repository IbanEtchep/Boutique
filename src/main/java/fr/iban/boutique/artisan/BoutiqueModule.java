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
        reloadRepo();
        // Both sources are STATIC: a stable, enumerable key-set (categories/items
        // keyed by `id`), so they're placeable in the editor's placement mode.
        // Field values (e.g. final_price) may still resolve per-player at runtime.
        api.getDataSources().register(new DataSourceDeclaration(
                "boutique:categories",
                (player, params) -> repo.categories().stream()
                        .map(c -> Map.<String, Object>of("id", c.getId(), "name", c.getName(), "icon", c.getIcon()))
                        .collect(Collectors.toList()),
                List.of(
                        new DataSourceField("id", FieldKind.STRING, null),
                        new DataSourceField("name", FieldKind.STRING, null),
                        new DataSourceField("icon", FieldKind.STRING, null)),
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
}
