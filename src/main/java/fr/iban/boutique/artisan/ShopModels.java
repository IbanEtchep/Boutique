package fr.iban.boutique.artisan;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The boutique's declared Data models (ADR composable-data-models §4):
 * model/v1 documents passed verbatim to {@code api.getModels().declare(...)}.
 * The editor renders the schema-driven form from these (master-detail catalog,
 * localized name/lore, locked icon) — the shape is read-only for the admin and
 * versioned with this plugin. Version 1; ship declarative migrations alongside
 * any future shape change.
 */
public final class ShopModels {
    private ShopModels() {}

    /** Current declared version of both models. */
    public static final int VERSION = 1;

    private static Map<String, Object> kind(String kind) {
        return Map.of("kind", kind);
    }

    private static Map<String, Object> field(String name, Map<String, Object> type, Map<String, Object> extra) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", name);
        f.put("type", type);
        f.putAll(extra);
        return f;
    }

    /** `boutique:item` — one purchasable article. */
    public static Map<String, Object> item() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("schema", "model/v1");
        doc.put("id", "boutique:item");
        doc.put("name", "Article");
        doc.put("key_field", "id");
        doc.put("version", VERSION);
        doc.put("fields", List.of(
                field("id", kind("string"), Map.of("required", true)),
                field("name", kind("ltext"), Map.of("required", true)),
                // Managed by /boutiqueadmin additem (captured item refs) — read-only in the form.
                field("icon", kind("item"), Map.of("locked", true)),
                field("price", kind("number"), Map.of("required", true, "min", 0)),
                field("discount", kind("integer"), Map.of("default", 0, "min", 0, "max", 100)),
                field("lore", Map.of("kind", "list", "of", kind("ltext")), Map.of()),
                field("actions", kind("actions"), Map.of("help", "Executed after payment (Action DSL)."))
        ));
        return doc;
    }

    /** `boutique:category` — a curated group of articles (master-detail catalog). */
    public static Map<String, Object> category() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("schema", "model/v1");
        doc.put("id", "boutique:category");
        doc.put("name", "Catégorie");
        doc.put("key_field", "id");
        doc.put("version", VERSION);
        doc.put("master_detail", true);
        doc.put("fields", List.of(
                field("id", kind("string"), Map.of("required", true)),
                field("name", kind("ltext"), Map.of("required", true)),
                field("icon", kind("item"), Map.of()),
                field("discount", kind("integer"), Map.of("default", 0, "min", 0, "max", 100)),
                field("items", Map.of("kind", "list", "of", Map.of("kind", "model", "ref", "boutique:item")), Map.of())
        ));
        return doc;
    }
}
