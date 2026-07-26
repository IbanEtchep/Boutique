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
 * §8, BARE form per ADR bare-data-files): converts {@code boutique/shop.yaml}
 * into the Artisan Table {@code data/categories/} — per-row files of PURE
 * VALUES (name/lore stay literal default-language text; the schema is the JAR
 * declaration {@code boutique:category}, linked by the minimal
 * {@code _source.yaml}) — plus the bare {@code data/shop_settings.yaml}
 * mapping. No i18n keys are minted (mono-language catalog; other languages
 * would live in lang/ under the canonical key-site address). The legacy file
 * is archived OUTSIDE the project dir so it never travels in the bundle.
 * Artisan's files-first sync then auto-pushes the new tree (LOCAL_ONLY).
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

        List<Map<String, Object>> categories = (List<Map<String, Object>>) shop.getOrDefault("categories", List.of());

        // ── per-row category files ──────────────────────────────────────────
        File catDir = new File(projectDir, "data/categories");
        if (!catDir.mkdirs() && !catDir.isDirectory()) throw new IOException("cannot create " + catDir);
        List<String> order = new ArrayList<>();
        for (Map<String, Object> cat : categories) {
            String catId = String.valueOf(cat.getOrDefault("id", "cat_" + (order.size() + 1)));
            String rowKey = slug(catId);
            order.add(rowKey);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", catId);
            row.put("name", literal(cat.get("name"), catId));
            row.put("icon", String.valueOf(cat.getOrDefault("icon", "CHEST")));
            row.put("discount", intOf(cat.get("discount")));

            List<Map<String, Object>> items = new ArrayList<>();
            List<Map<String, Object>> srcItems = (List<Map<String, Object>>) cat.getOrDefault("items", List.of());
            for (int i = 0; i < srcItems.size(); i++) {
                Map<String, Object> src = srcItems.get(i);
                Map<String, Object> item = new LinkedHashMap<>();
                String itemId = String.valueOf(src.getOrDefault("id", "item_" + (i + 1)));
                item.put("id", itemId);
                item.put("name", literal(src.get("name"), itemId));
                item.put("icon", String.valueOf(src.getOrDefault("icon", "BARRIER")));
                item.put("price", src.getOrDefault("price", 0));
                item.put("discount", intOf(src.get("discount")));
                List<String> loreLines = new ArrayList<>();
                for (Object l : (List<Object>) src.getOrDefault("lore", List.of())) {
                    loreLines.add(String.valueOf(l));
                }
                item.put("lore", loreLines);
                item.put("actions", actionsOf(src));
                items.add(item);
            }
            row.put("items", items);
            Files.writeString(new File(catDir, rowKey + ".yaml").toPath(), yaml.dump(row));
        }

        // ── minimal _source.yaml (ADR bare-data-files) : le schéma est la
        // déclaration JAR — le méta ne porte que le lien model + l'ordre.
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("name", "Catégories");
        meta.put("model", "boutique:category");
        meta.put("model_version", ShopModels.VERSION);
        meta.put("order", order);
        Files.writeString(sourceMetaFile(projectDir).toPath(), yaml.dump(meta));

        // ── réglages : un mapping NU — rien que la valeur ──
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("whole_shop_discount", intOf(shop.get("whole_shop_discount")));
        Files.writeString(new File(projectDir, "data/shop_settings.yaml").toPath(), yaml.dump(settings));

        // ── archive the legacy file OUTSIDE the project (never bundled) ─────
        File archive = new File(projectDir.getParentFile(), "shop.yaml.migrated");
        Files.move(shopYamlFile.toPath(), archive.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private static File sourceMetaFile(File projectDir) {
        return new File(projectDir, "data/categories/_source.yaml");
    }

    /** ltext values stay LITERAL default-language text (ADR bare-data-files ④). */
    private static String literal(Object value, String fallback) {
        return value != null ? String.valueOf(value) : fallback;
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




    private static Yaml plainYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(options);
    }
}
