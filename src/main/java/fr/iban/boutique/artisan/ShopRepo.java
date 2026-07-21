package fr.iban.boutique.artisan;

import fr.iban.boutique.ShopCategory;
import fr.iban.boutique.ShopItem;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.function.Function;

/** État shop en mémoire, rechargé depuis boutique/*.yaml (api.project). Thread-safe par remplacement atomique. */
public final class ShopRepo {
    private volatile List<ShopCategory> categories = List.of();
    private volatile Map<String, ShopItem> itemsById = Map.of();
    private volatile int wholeShopDiscount = 0;

    public List<ShopCategory> categories() { return categories; }
    public Optional<ShopItem> findItem(String id) { return Optional.ofNullable(itemsById.get(id)); }
    public int wholeShopDiscount() { return wholeShopDiscount; }

    @SuppressWarnings("unchecked")
    public void load(Function<String, String> readFile, List<String> shopFiles) {
        List<ShopCategory> cats = new ArrayList<>();
        Map<String, ShopItem> byId = new HashMap<>();
        int shopDiscount = 0;
        for (String file : shopFiles) {
            String content = readFile.apply(file);
            if (content == null) continue;
            Map<String, Object> root;
            try {
                Object parsed = new Yaml().load(content);
                if (!(parsed instanceof Map)) continue;
                root = (Map<String, Object>) parsed;
            } catch (RuntimeException e) { continue; }
            if (!"boutique/v1".equals(root.get("schema"))) continue;
            shopDiscount = asInt(root.get("whole_shop_discount"), shopDiscount);
            for (Object rawCat : asList(root.get("categories"))) {
                if (!(rawCat instanceof Map)) continue;
                Map<String, Object> c = (Map<String, Object>) rawCat;
                List<ShopItem> items = new ArrayList<>();
                ShopCategory cat = new ShopCategory(
                        str(c, "id"), str(c, "name"), str(c, "icon"), asInt(c.get("discount"), 0), items);
                for (Object rawItem : asList(c.get("items"))) {
                    if (!(rawItem instanceof Map)) continue;
                    Map<String, Object> i = (Map<String, Object>) rawItem;
                    ShopItem item = new ShopItem(
                            str(i, "id"), str(i, "name"), str(i, "icon"),
                            asDouble(i.get("price")), asInt(i.get("discount"), 0),
                            strList(i.get("lore")), actionsOf(i), cat);
                    items.add(item);
                    byId.put(item.getId(), item);
                }
                cats.add(cat);
            }
        }
        this.categories = List.copyOf(cats);
        this.itemsById = Map.copyOf(byId);
        this.wholeShopDiscount = shopDiscount;
    }

    /** `actions` (chaîne Action DSL) si présent, sinon migration mécanique des
     *  `buy_commands` legacy : une ligne `run_command "…" as console` par commande,
     *  `%player%` → `{player}` (placeholder du moteur de steps Artisan), jointes par
     *  des retours ligne. */
    private static String actionsOf(Map<String, Object> i) {
        Object raw = i.get("actions");
        if (raw instanceof String s && !s.isBlank()) return s;
        StringBuilder sb = new StringBuilder();
        for (String cmd : strList(i.get("buy_commands"))) {
            if (sb.length() > 0) sb.append('\n');
            sb.append("run_command \"").append(cmd.replace("%player%", "{player}")).append("\" as console");
        }
        return sb.toString();
    }

    private static String str(Map<String, Object> m, String k) { Object v = m.get(k); return v == null ? "" : v.toString(); }
    private static int asInt(Object v, int dflt) { return v instanceof Number n ? n.intValue() : dflt; }
    private static double asDouble(Object v) { return v instanceof Number n ? n.doubleValue() : 0.0; }
    private static List<Object> asList(Object v) { return v instanceof List ? (List<Object>) v : List.of(); }
    private static List<String> strList(Object v) {
        List<String> out = new ArrayList<>();
        for (Object o : asList(v)) if (o != null) out.add(o.toString());
        return List.copyOf(out);
    }
}
