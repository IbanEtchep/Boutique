package fr.iban.boutique.artisan;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

/** One-shot : ancien config.yml MenuAPI → boutique/v1. Lecture en maps brutes (jamais YamlConfiguration : blocs `==: menuitem`). */
public final class LegacyConfigMigrator {
    private LegacyConfigMigrator() {}

    @SuppressWarnings("unchecked")
    public static Optional<String> migrate(String legacyYaml) {
        Object parsed;
        try { parsed = new Yaml().load(legacyYaml); } catch (RuntimeException e) { return Optional.empty(); }
        if (!(parsed instanceof Map)) return Optional.empty();
        Map<String, Object> root = (Map<String, Object>) parsed;
        if (!(root.get("categories") instanceof Map)) return Optional.empty();

        List<Map<String, Object>> categories = new ArrayList<>();
        Map<String, Object> rawCats = (Map<String, Object>) root.get("categories");
        for (Map.Entry<String, Object> catEntry : rawCats.entrySet()) {
            if (!(catEntry.getValue() instanceof Map)) continue;
            Map<String, Object> c = (Map<String, Object>) catEntry.getValue();
            Map<String, Object> catDisplay = display(c.get("menuitem"));
            List<Map<String, Object>> items = new ArrayList<>();
            Object rawItems = c.get("items");
            if (rawItems instanceof Map) {
                for (Map.Entry<String, Object> itemEntry : ((Map<String, Object>) rawItems).entrySet()) {
                    if (!(itemEntry.getValue() instanceof Map)) continue;
                    Map<String, Object> i = (Map<String, Object>) itemEntry.getValue();
                    // Le vrai runtime pré-refactor lit/écrit toujours "menuitem" pour l'item (cf. ShopManager
                    // c72f586 lignes 45/104) ; "display" reste un fallback pour un config.yml recopié à la
                    // main depuis le template ressource (qui, lui, utilisait "display").
                    Object rawItemDisplay = i.containsKey("menuitem") ? i.get("menuitem") : i.get("display");
                    Map<String, Object> itemDisplay = display(rawItemDisplay);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", slug((String) itemDisplay.get("name"), "item_" + itemEntry.getKey()));
                    item.put("name", stripAmp((String) itemDisplay.getOrDefault("name", "item " + itemEntry.getKey())));
                    item.put("icon", itemDisplay.getOrDefault("icon", "BARRIER"));
                    item.put("price", i.getOrDefault("price", 0));
                    item.put("discount", i.getOrDefault("discount", 0));
                    item.put("lore", itemDisplay.getOrDefault("lore", List.of()));
                    item.put("buy_commands", i.getOrDefault("buycommands", List.of()));
                    items.add(item);
                }
            }
            Map<String, Object> cat = new LinkedHashMap<>();
            cat.put("id", slug((String) catDisplay.get("name"), "cat_" + catEntry.getKey()));
            cat.put("name", stripAmp((String) catDisplay.getOrDefault("name", "Catégorie " + catEntry.getKey())));
            cat.put("icon", catDisplay.getOrDefault("icon", "CHEST"));
            cat.put("discount", c.getOrDefault("discount", 0));
            cat.put("items", items);
            categories.add(cat);
        }
        if (categories.isEmpty()) return Optional.empty();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("schema", "boutique/v1");
        out.put("whole_shop_discount", root.getOrDefault("whole-shop-discount", 0));
        out.put("categories", categories);
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return Optional.of(new Yaml(opts).dump(out));
    }

    /** name/lore/icon depuis un bloc menuitem sérialisé ({==: menuitem, name, lore, item: {type}}). */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> display(Object rawMenuItem) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (rawMenuItem instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) rawMenuItem;
            if (m.get("name") != null) out.put("name", m.get("name").toString());
            if (m.get("lore") instanceof List) {
                List<String> lore = new ArrayList<>();
                for (Object l : (List<Object>) m.get("lore")) lore.add(stripAmp(String.valueOf(l)));
                out.put("lore", lore);
            }
            if (m.get("item") instanceof Map && ((Map<String, Object>) m.get("item")).get("type") != null) {
                out.put("icon", ((Map<String, Object>) m.get("item")).get("type").toString());
            }
        }
        return out;
    }

    private static String stripAmp(String s) { return s == null ? "" : s.replaceAll("[&§][0-9a-fk-orx]", "").trim(); }

    private static String slug(String name, String fallback) {
        if (name == null) return fallback;
        String s = stripAmp(name).toLowerCase(Locale.ROOT)
                .replaceAll("[àâä]", "a").replaceAll("[éèêë]", "e").replaceAll("[îï]", "i")
                .replaceAll("[ôö]", "o").replaceAll("[ùûü]", "u").replaceAll("ç", "c")
                .replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return s.isEmpty() || !s.matches("^[a-z].*") ? fallback : s;
    }
}
