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
 * One-shot migration (ADR composable-data-models §8, BARE form per ADR
 * bare-data-files): boutique/shop.yaml → the declared-model Table
 * (data/categories/, per-row PURE-VALUE files, minimal _source.yaml) + the
 * bare shop settings mapping. Name/lore stay literal default-language text —
 * no keys minted, lang/ untouched. The legacy file is archived OUTSIDE the
 * project dir (it must not travel in the bundle).
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

        // Minimal _source.yaml (ADR bare-data-files ③): model link + order only
        // — the schema is the JAR declaration, never inline.
        Map<String, Object> meta = yaml(new File(projectDir, "data/categories/_source.yaml"));
        assertEquals("boutique:category", meta.get("model"));
        assertEquals(ShopModels.VERSION, meta.get("model_version"));
        assertEquals(List.of("armes", "blocs"), meta.get("order"));
        assertNull(meta.get("fields"));
        assertNull(meta.get("schema"));

        // Per-row file: PURE VALUES — name/lore are literal default-language text.
        Map<String, Object> armes = yaml(new File(projectDir, "data/categories/armes.yaml"));
        assertEquals("armes", armes.get("id"));
        assertEquals("Armes", armes.get("name"));
        assertEquals("DIAMOND_SWORD", armes.get("icon"));
        assertEquals(10, armes.get("discount"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) armes.get("items");
        assertEquals(1, items.size());
        assertEquals("Épée légendaire", items.get(0).get("name"));
        assertEquals("item:ci_epee", items.get(0).get("icon"));
        assertEquals(List.of("Tranchante", "Rare"), items.get(0).get("lore"));
        assertEquals("give_item material=DIAMOND_SWORD count=1", items.get(0).get("actions"));

        // Settings: a bare mapping — the file IS the value.
        Map<String, Object> settings = yaml(new File(projectDir, "data/shop_settings.yaml"));
        assertEquals(Map.of("whole_shop_discount", 5), settings);

        // Lang untouched: no keys minted (mono-language catalog).
        Map<String, Object> fr = yaml(new File(projectDir, "lang/fr.yaml"));
        assertEquals("Boutique", fr.get("boutique.shop_main.title"));
        assertNull(fr.get("sources.categories.armes.name"));

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
