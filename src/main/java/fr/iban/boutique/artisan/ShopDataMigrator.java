package fr.iban.boutique.artisan;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One-shot migration to the declared-model catalog (ADR composable-data-models
 * §8): converts {@code boutique/shop.yaml} into the Artisan Table
 * {@code data/categories/} (per-row files, model {@code boutique:category},
 * version stamped) plus the {@code data/shop_settings.yaml} config source,
 * minting i18n keys for name/lore literals into {@code lang/<defaultLang>.yaml}
 * (key-site convention {@code sources.categories.<rowKey>.<fieldPath>} with the
 * {@code _n} list suffix). The legacy file is archived OUTSIDE the project dir
 * so it never travels in the bundle. Artisan's files-first sync then auto-pushes
 * the new tree (LOCAL_ONLY) — no manual command needed.
 */
public final class ShopDataMigrator {
    private ShopDataMigrator() {}

    /** @return true when a migration ran. */
    public static boolean migrateIfNeeded(File projectDir) {
        File shopYaml = new File(projectDir, "boutique/shop.yaml");
        File sourceMeta = new File(projectDir, "data/categories/_source.yaml");
        if (!shopYaml.isFile() || sourceMeta.exists()) return false;
        try {
            return migrate(projectDir, shopYaml);
        } catch (Exception e) {
            // Never block plugin boot on a migration failure — the legacy
            // reader still works; a WARN suffices for post-mortem.
            System.err.println("[Boutique] shop.yaml migration failed: " + e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean migrate(File projectDir, File shopYamlFile) throws IOException {
        Yaml yaml = plainYaml();
        Map<String, Object> shop = yaml.load(Files.readString(shopYamlFile.toPath()));
        if (shop == null || !"boutique/v1".equals(shop.get("schema"))) return false;

        String lang = defaultLang(projectDir, yaml);
        Map<String, Object> translations = new LinkedHashMap<>();
        List<Map<String, Object>> categories = (List<Map<String, Object>>) shop.getOrDefault("categories", List.of());

        // ── per-row category files ──────────────────────────────────────────
        File catDir = new File(projectDir, "data/categories");
        if (!catDir.mkdirs() && !catDir.isDirectory()) throw new IOException("cannot create " + catDir);
        List<String> order = new ArrayList<>();
        for (Map<String, Object> cat : categories) {
            String catId = String.valueOf(cat.getOrDefault("id", "cat_" + (order.size() + 1)));
            String rowKey = slug(catId);
            order.add(rowKey);
            String base = "sources.categories." + rowKey;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", catId);
            row.put("name", mint(translations, base + ".name", cat.get("name"), catId));
            row.put("icon", String.valueOf(cat.getOrDefault("icon", "CHEST")));
            row.put("discount", intOf(cat.get("discount")));

            List<Map<String, Object>> items = new ArrayList<>();
            List<Map<String, Object>> srcItems = (List<Map<String, Object>>) cat.getOrDefault("items", List.of());
            for (int i = 0; i < srcItems.size(); i++) {
                Map<String, Object> src = srcItems.get(i);
                String itemBase = base + ".items" + (i == 0 ? "" : "_" + (i + 1));
                Map<String, Object> item = new LinkedHashMap<>();
                String itemId = String.valueOf(src.getOrDefault("id", "item_" + (i + 1)));
                item.put("id", itemId);
                item.put("name", mint(translations, itemBase + ".name", src.get("name"), itemId));
                item.put("icon", String.valueOf(src.getOrDefault("icon", "BARRIER")));
                item.put("price", src.getOrDefault("price", 0));
                item.put("discount", intOf(src.get("discount")));
                List<String> loreKeys = new ArrayList<>();
                List<Object> lore = (List<Object>) src.getOrDefault("lore", List.of());
                for (int j = 0; j < lore.size(); j++) {
                    String loreKey = itemBase + ".lore" + (j == 0 ? "" : "_" + (j + 1));
                    loreKeys.add(mint(translations, loreKey, lore.get(j), ""));
                }
                item.put("lore", loreKeys);
                item.put("actions", actionsOf(src));
                items.add(item);
            }
            row.put("items", items);
            Files.writeString(new File(catDir, rowKey + ".yaml").toPath(), yaml.dump(row));
        }

        // ── _source.yaml (meta + flattened self-contained fields) ───────────
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("schema", "data/v1");
        meta.put("id", "categories");
        meta.put("name", "Catégories");
        meta.put("shape", "table");
        meta.put("model", "boutique:category");
        meta.put("model_version", ShopModels.VERSION);
        meta.put("key_field", "id");
        meta.put("granularity", "row");
        meta.put("order", order);
        meta.put("fields", flattenedCategoryFields());
        Files.writeString(sourceMetaFile(projectDir).toPath(), yaml.dump(meta));

        // ── shop settings config source ─────────────────────────────────────
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("schema", "data/v1");
        settings.put("id", "shop_settings");
        settings.put("name", "Réglages boutique");
        settings.put("shape", "config");
        settings.put("fields", List.of(dataField("whole_shop_discount", "integer")));
        settings.put("value", Map.of("whole_shop_discount", intOf(shop.get("whole_shop_discount"))));
        Files.writeString(new File(projectDir, "data/shop_settings.yaml").toPath(), yaml.dump(settings));

        // ── lang merge (existing keys preserved) ────────────────────────────
        File langFile = new File(projectDir, "lang/" + lang + ".yaml");
        Map<String, Object> existing = new LinkedHashMap<>();
        if (langFile.isFile()) {
            Map<String, Object> parsed = yaml.load(Files.readString(langFile.toPath()));
            if (parsed != null) existing.putAll(parsed);
        }
        existing.putAll(translations);
        langFile.getParentFile().mkdirs();
        Files.writeString(langFile.toPath(), yaml.dump(existing));

        // ── archive the legacy file OUTSIDE the project (never bundled) ─────
        File archive = new File(projectDir.getParentFile(), "shop.yaml.migrated");
        Files.move(shopYamlFile.toPath(), archive.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private static File sourceMetaFile(File projectDir) {
        return new File(projectDir, "data/categories/_source.yaml");
    }

    /** Store the literal under the key; the row carries the KEY (ltext value). */
    private static String mint(Map<String, Object> translations, String key, Object literal, String fallback) {
        translations.put(key, literal != null ? String.valueOf(literal) : fallback);
        return key;
    }

    private static String actionsOf(Map<String, Object> item) {
        Object actions = item.get("actions");
        if (actions instanceof String s) return s;
        Object legacy = item.get("buy_commands");
        if (legacy instanceof List<?> cmds) {
            List<String> lines = new ArrayList<>();
            for (Object cmd : cmds) {
                lines.add("run_command \"" + String.valueOf(cmd).replace("%player%", "{player}") + "\" as console");
            }
            return String.join("\n", lines);
        }
        return "";
    }

    private static int intOf(Object v) {
        return v instanceof Number n ? n.intValue() : 0;
    }

    private static String slug(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "_");
        return s.isEmpty() ? "cat" : s;
    }

    private static Map<String, Object> dataField(String name, String kind) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", name);
        f.put("kind", kind);
        return f;
    }

    /** data/v1 flattened view of boutique:category (self-contained for the runtime). */
    private static List<Map<String, Object>> flattenedCategoryFields() {
        Map<String, Object> loreField = dataField("lore", "list");
        loreField.put("of", Map.of("kind", "ltext"));
        Map<String, Object> itemFields = new LinkedHashMap<>();
        itemFields.put("kind", "object");
        itemFields.put("fields", List.of(
                dataField("id", "string"),
                dataField("name", "ltext"),
                dataField("icon", "item"),
                dataField("price", "number"),
                dataField("discount", "integer"),
                loreField,
                dataField("actions", "actions")));
        Map<String, Object> itemsField = dataField("items", "list");
        itemsField.put("of", itemFields);
        return List.of(
                dataField("id", "string"),
                dataField("name", "ltext"),
                dataField("icon", "item"),
                dataField("discount", "integer"),
                itemsField);
    }

    private static String defaultLang(File projectDir, Yaml yaml) {
        try {
            File manifest = new File(projectDir, "manifest.yaml");
            if (manifest.isFile()) {
                Map<String, Object> m = yaml.load(Files.readString(manifest.toPath()));
                Object lang = m != null ? m.get("default_lang") : null;
                if (lang instanceof String s && !s.isBlank()) return s;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "fr";
    }

    private static Yaml plainYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(options);
    }
}
