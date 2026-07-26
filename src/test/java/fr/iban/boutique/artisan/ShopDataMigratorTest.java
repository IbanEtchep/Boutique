package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * One-shot migration (ADR composable-data-models §8): boutique/shop.yaml →
 * the declared-model Table (data/categories/, per-row files) + shop settings
 * config + localized name/lore keys in lang/. The legacy file is archived
 * OUTSIDE the project dir (it must not travel in the bundle).
 */
class ShopDataMigratorTest {

    private static final String SHOP_YAML = """
            schema: boutique/v1
            whole_shop_discount: 5
            categories:
              - id: armes
                name: Armes
                icon: DIAMOND_SWORD
                discount: 10
                items:
                  - id: epee
                    name: Épée légendaire
                    icon: "item:ci_epee"
                    price: 100
                    discount: 0
                    lore: ["Tranchante", "Rare"]
                    actions: 'give_item material=DIAMOND_SWORD count=1'
              - id: blocs
                name: Blocs
                icon: STONE
                discount: 0
                items: []
            """;

    private File seedProject(File root) throws Exception {
        File projectDir = new File(root, "editor");
        new File(projectDir, "boutique").mkdirs();
        new File(projectDir, "lang").mkdirs();
        Files.writeString(new File(projectDir, "manifest.yaml").toPath(),
                "schema: manifest/v1\nproject_id: boutique_shop\nmodule: boutique\ndefault_lang: fr\nlanguages: [fr]\nfiles:\n  menus: []\n  commands: []\n");
        Files.writeString(new File(projectDir, "boutique/shop.yaml").toPath(), SHOP_YAML);
        Files.writeString(new File(projectDir, "lang/fr.yaml").toPath(),
                "boutique.shop_main.title: Boutique\n");
        return projectDir;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> yaml(File f) throws Exception {
        return new Yaml().load(Files.readString(f.toPath()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void migratesShopYamlToDeclaredModelTable(@TempDir File root) throws Exception {
        File projectDir = seedProject(root);
        assertTrue(ShopDataMigrator.migrateIfNeeded(projectDir));

        // _source.yaml: declared-model Table, per-row granularity, version stamped.
        Map<String, Object> meta = yaml(new File(projectDir, "data/categories/_source.yaml"));
        assertEquals("data/v1", meta.get("schema"));
        assertEquals("categories", meta.get("id"));
        assertEquals("table", meta.get("shape"));
        assertEquals("boutique:category", meta.get("model"));
        assertEquals(ShopModels.VERSION, meta.get("model_version"));
        assertEquals("row", meta.get("granularity"));
        assertEquals("id", meta.get("key_field"));
        assertEquals(List.of("armes", "blocs"), meta.get("order"));
        // Flattened inline fields keep the file self-contained (ltext typed).
        List<Map<String, Object>> fields = (List<Map<String, Object>>) meta.get("fields");
        assertEquals("ltext", fields.stream().filter(f -> f.get("name").equals("name")).findFirst().orElseThrow().get("kind"));

        // Per-row file: name/lore are i18n KEYS following the key-site convention.
        Map<String, Object> armes = yaml(new File(projectDir, "data/categories/armes.yaml"));
        assertEquals("armes", armes.get("id"));
        assertEquals("sources.categories.armes.name", armes.get("name"));
        assertEquals("DIAMOND_SWORD", armes.get("icon"));
        assertEquals(10, armes.get("discount"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) armes.get("items");
        assertEquals(1, items.size());
        assertEquals("sources.categories.armes.items.name", items.get(0).get("name"));
        assertEquals("item:ci_epee", items.get(0).get("icon"));
        assertEquals(List.of("sources.categories.armes.items.lore", "sources.categories.armes.items.lore_2"),
                items.get(0).get("lore"));
        assertEquals("give_item material=DIAMOND_SWORD count=1", items.get(0).get("actions"));

        // Settings config source.
        Map<String, Object> settings = yaml(new File(projectDir, "data/shop_settings.yaml"));
        assertEquals("config", settings.get("shape"));
        assertEquals(5, ((Map<String, Object>) settings.get("value")).get("whole_shop_discount"));

        // Lang: literals landed as translations; existing keys preserved.
        Map<String, Object> fr = yaml(new File(projectDir, "lang/fr.yaml"));
        assertEquals("Boutique", fr.get("boutique.shop_main.title"));
        assertEquals("Armes", fr.get("sources.categories.armes.name"));
        assertEquals("Épée légendaire", fr.get("sources.categories.armes.items.name"));
        assertEquals("Tranchante", fr.get("sources.categories.armes.items.lore"));
        assertEquals("Rare", fr.get("sources.categories.armes.items.lore_2"));

        // Legacy file archived OUTSIDE the project dir (never bundled).
        assertFalse(new File(projectDir, "boutique/shop.yaml").exists());
        assertTrue(new File(root, "shop.yaml.migrated").exists());
    }

    @Test
    void noOpWhenAlreadyMigratedOrNothingToMigrate(@TempDir File root) throws Exception {
        File projectDir = seedProject(root);
        assertTrue(ShopDataMigrator.migrateIfNeeded(projectDir));
        // Second run: nothing left to do.
        assertFalse(ShopDataMigrator.migrateIfNeeded(projectDir));
        // Fresh dir without shop.yaml: no-op.
        File empty = new File(root, "empty");
        empty.mkdirs();
        assertFalse(ShopDataMigrator.migrateIfNeeded(empty));
    }
}
