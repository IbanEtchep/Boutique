package fr.iban.boutique.artisan;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
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
        if (!shopYaml.isFile()) return false;
        if (sourceMetaFile(projectDir).exists()) {
            // Une Table existe déjà. L'exemple bundlé INTACT n'est pas du
            // contenu — il cède la place (rattrapage des installs bootstrappées
            // avant que le catalogue exemple ne soit écarté en mode migration).
            // Une Table éditée, elle, est la vérité : on n'y touche pas.
            if (!isUntouchedExampleCatalog(projectDir)) {
                System.err.println("[Boutique] boutique/shop.yaml en attente de conversion, mais "
                        + "data/categories/ contient déjà un catalogue — conversion ignorée. "
                        + "Vider data/categories/ pour forcer la reprise du legacy.");
                return false;
            }
            deleteExampleCatalog(projectDir);
        }
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

        // ── mise en page d'origine : un menu par catégorie ──────────────────
        writePlacedMenus(projectDir, categories, yaml);

        // ── archive the legacy file OUTSIDE the project (never bundled) ─────
        File archive = new File(projectDir.getParentFile(), "shop.yaml.migrated");
        Files.move(shopYamlFile.toPath(), archive.toPath(), StandardCopyOption.REPLACE_EXISTING);
        // Le dossier `boutique/` n'a plus lieu d'être : vide, il voyagerait dans
        // le bundle et dans les exports de project comme un fantôme du legacy.
        File legacyDir = shopYamlFile.getParentFile();
        String[] left = legacyDir.list();
        if (left != null && left.length == 0) legacyDir.delete();
        return true;
    }

    /**
     * Rend la mise en page du legacy, où rien n'est paginé : chaque catégorie et
     * chaque article portent leur slot. On produit donc UN MENU PAR CATÉGORIE,
     * dont la source est filtrée sur cet id — le placement retrouve sa
     * sémantique (une source, un écran, des slots fixes) au lieu d'un menu
     * unique où un même slot porterait sept occupants selon le contexte.
     *
     * Sans aucun slot dans le legacy, on ne touche à rien : les menus paginés
     * bundlés restent.
     */
    @SuppressWarnings("unchecked")
    private static void writePlacedMenus(File projectDir, List<Map<String, Object>> categories, Yaml yaml)
            throws IOException {
        if (categories.stream().noneMatch(c -> c.get("slot") instanceof Number)) return;

        File menusDir = new File(projectDir, "menus");
        if (!menusDir.mkdirs() && !menusDir.isDirectory()) throw new IOException("cannot create " + menusDir);

        List<String> menuIds = new ArrayList<>();
        menuIds.add("shop_main");
        List<Map<String, Object>> mainElements = new ArrayList<>();

        for (Map<String, Object> cat : categories) {
            String catId = String.valueOf(cat.get("id"));
            String menuId = "shop_" + slug(catId);
            menuIds.add(menuId);

            // Menu principal : un élément par catégorie. Le clic doit être
            // littéral (`open_menu shop_les_cles`) — le lexer du DSL n'accepte
            // pas de template dans un identifiant — mais le placement garde nom
            // et icône liés à la donnée.
            Map<String, Object> el = new LinkedHashMap<>();
            el.put("id", "cat_" + slug(catId));
            el.put("places", places("cats", "cat", List.of(placement(catId, cat))));
            el.put("appearances", List.of(appearance("{cat.icon}", "{cat.name}",
                    List.of(), "open_menu " + menuId)));
            mainElements.add(el);

            // Menu de la catégorie : ses articles, à leurs slots.
            List<Map<String, Object>> placements = new ArrayList<>();
            for (Map<String, Object> item : (List<Map<String, Object>>) cat.getOrDefault("items", List.of())) {
                if (item.get("slot") instanceof Number) {
                    placements.add(placement(String.valueOf(item.get("id")), item));
                }
            }
            Map<String, Object> grid = new LinkedHashMap<>();
            grid.put("id", "grid");
            grid.put("places", places("items", "item", placements));
            grid.put("appearances", List.of(appearance("{item.icon}", "{item.name}",
                    List.of("{item.lore}", "{item.price_display}"),
                    "open_dialog confirm_purchase with item_id=\"{item.id}\" "
                            + "item_name=\"{item.name}\" item_price=\"{item.final_price}\"")));

            Map<String, Object> back = new LinkedHashMap<>();
            back.put("id", "back");
            back.put("positions", 49);
            back.put("appearances", List.of(appearance("ARROW", "§7Retour", List.of(), "open_menu shop_main")));

            Map<String, Object> menu = new LinkedHashMap<>();
            menu.put("schema", "menu/v1");
            menu.put("id", menuId);
            menu.put("title", String.valueOf(cat.get("name")));
            menu.put("size", 54);
            menu.put("inputs", List.of(Map.of(
                    "name", "items",
                    "type", "list",
                    "source", Map.of("ref", "boutique:items", "filter", Map.of("category", catId)))));
            menu.put("elements", List.of(grid, back));
            Files.writeString(new File(menusDir, menuId + ".yaml").toPath(), yaml.dump(menu));
        }

        Map<String, Object> main = new LinkedHashMap<>();
        main.put("schema", "menu/v1");
        main.put("id", "shop_main");
        main.put("title", Map.of("key", "boutique.shop_main.title"));
        main.put("size", 54);
        main.put("inputs", List.of(Map.of(
                "name", "cats", "type", "list", "source", Map.of("ref", "boutique:categories"))));
        main.put("elements", mainElements);
        Files.writeString(new File(menusDir, "shop_main.yaml").toPath(), yaml.dump(main));

        // Le menu paginé bundlé n'a plus de raison d'être.
        new File(menusDir, "shop_category.yaml").delete();
        rewriteManifestMenus(projectDir, menuIds, yaml);
    }

    private static Map<String, Object> placement(String key, Map<String, Object> src) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("key", key);
        p.put("page", intOf(src.get("page")));
        p.put("position", intOf(src.get("slot")));
        return p;
    }

    private static Map<String, Object> places(String source, String alias, List<Map<String, Object>> placements) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("source", source);
        p.put("alias", alias);
        p.put("key_field", "id");
        p.put("placements", placements);
        return p;
    }

    private static Map<String, Object> appearance(String material, String title, List<String> lore, String click) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("material", material);
        a.put("title", title);
        if (!lore.isEmpty()) a.put("lore", lore);
        a.put("click", Map.of("left", click));
        return a;
    }

    /** Un menu absent du manifeste n'est pas lu par le plugin. */
    @SuppressWarnings("unchecked")
    private static void rewriteManifestMenus(File projectDir, List<String> menuIds, Yaml yaml) throws IOException {
        File manifestFile = new File(projectDir, "manifest.yaml");
        if (!manifestFile.isFile()) return;
        Map<String, Object> manifest = yaml.load(Files.readString(manifestFile.toPath()));
        if (manifest == null) return;
        Object rawFiles = manifest.get("files");
        Map<String, Object> files = rawFiles instanceof Map
                ? new LinkedHashMap<>((Map<String, Object>) rawFiles)
                : new LinkedHashMap<>();
        files.put("menus", menuIds);
        manifest.put("files", files);
        Files.writeString(manifestFile.toPath(), yaml.dump(manifest));
    }

    private static File sourceMetaFile(File projectDir) {
        return new File(projectDir, "data/categories/_source.yaml");
    }

    /**
     * La Table est-elle exactement le catalogue exemple bundlé, octet pour
     * octet ? Comparaison au contenu du JAR plutôt qu'à une heuristique de nom :
     * une catégorie ajoutée, renommée ou retouchée fait échouer la comparaison
     * et protège le contenu de l'admin.
     */
    private static boolean isUntouchedExampleCatalog(File projectDir) {
        File catDir = new File(projectDir, "data/categories");
        String[] present = catDir.list((d, n) -> n.endsWith(".yaml") || n.endsWith(".yml"));
        if (present == null) return false;
        List<String> expected = new ArrayList<>();
        for (String res : Bootstrapper.EXAMPLE_CATALOG) expected.add(res.substring(res.lastIndexOf('/') + 1));
        if (present.length != expected.size()) return false;
        for (String res : Bootstrapper.EXAMPLE_CATALOG) {
            try (InputStream in = Bootstrapper.class.getResourceAsStream("/bootstrap/" + res)) {
                if (in == null) return false;
                File onDisk = new File(projectDir, res);
                if (!onDisk.isFile()) return false;
                if (!Arrays.equals(in.readAllBytes(), Files.readAllBytes(onDisk.toPath()))) return false;
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    private static void deleteExampleCatalog(File projectDir) {
        for (String res : Bootstrapper.EXAMPLE_CATALOG) {
            File f = new File(projectDir, res);
            if (f.isFile() && !f.delete()) f.deleteOnExit();
        }
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
