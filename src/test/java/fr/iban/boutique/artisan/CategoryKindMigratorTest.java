package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The {@code data/categories} Table becomes {@code boutique_category} Documents, losing nothing. */
class CategoryKindMigratorTest {

    private static final String WEAPONS = """
            id: weapons
            name: Armes
            icon: {extends: DIAMOND_SWORD, title: Armes, lore: [Tranchant, Rapide]}
            discount: 10
            color: red
            items:
            - id: sword
              name: Épée
              icon: DIAMOND_SWORD
              price: 100
              discount: 0
              actions: run_command "give {player} diamond_sword 1" as console
            - id: bow
              name: Arc
              icon: BOW
              price: 50
              discount: 5
            """;

    private static Path seed(Path root) throws Exception {
        Path project = root.resolve("boutique_addon");
        Files.createDirectories(project.resolve("data/categories"));
        Files.createDirectories(project.resolve("lang"));
        Files.writeString(project.resolve("manifest.yaml"), "default_lang: fr\n");
        Files.writeString(project.resolve("data/categories/_source.yaml"), "order:\n- tools\n- weapons\n");
        Files.writeString(project.resolve("data/categories/weapons.yaml"), WEAPONS);
        Files.writeString(project.resolve("data/categories/tools.yaml"),
                "id: tools\nname: shop.tools\nicon: IRON_PICKAXE\ndiscount: 0\nitems: []\n");
        Files.writeString(project.resolve("lang/fr.yaml"), "shop.tools: Outils\n");
        return project;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> read(Path file) throws Exception {
        return new Yaml().load(Files.readString(file));
    }

    @Test
    void namesTheCategoryFilesTheShopDoesNotRead() {
        assertEquals(
                List.of("boutique_categories/keys.yml", "catalogue/old/kits.yaml",
                        "divers/boutique_categories/packs.yml"),
                CategoryKindMigrator.misplaced(List.of(
                        "catalogue/divers.yml",
                        "boutique_categories/keys.yml",
                        "catalogue/old/kits.yaml",
                        "divers/boutique_categories/packs.yml",
                        "divers/menus/shop_main.yml",
                        "catalogue/notes.txt")));
    }

    @Test
    void categoriesMoveToTheCatalogueFolderWithTheirBytes(@TempDir Path root) throws Exception {
        Path project = root.resolve("boutique_addon");
        Files.createDirectories(project.resolve("boutique_categories"));
        Files.createDirectories(project.resolve("catalogue/boutique_categories"));
        Files.createDirectories(project.resolve("menus"));
        Files.writeString(project.resolve("boutique_categories/divers.yml"), "name: Divers\n");
        Files.writeString(project.resolve("catalogue/boutique_categories/les_cles.yaml"), "name: Clés\n");
        Files.writeString(project.resolve("menus/shop_main.yml"), "title: Boutique\n");

        CategoryKindMigrator.Result result = CategoryKindMigrator.relocate(project);

        assertEquals(List.of("boutique_categories/divers.yml", "catalogue/boutique_categories/les_cles.yaml"),
                result.moved());
        assertEquals("name: Divers\n", Files.readString(project.resolve("catalogue/divers.yml")));
        assertEquals("name: Clés\n", Files.readString(project.resolve("catalogue/les_cles.yml")));
        assertFalse(Files.exists(project.resolve("boutique_categories")), "the emptied former folder goes");
        assertFalse(Files.exists(project.resolve("catalogue/boutique_categories")));
        assertEquals("title: Boutique\n", Files.readString(project.resolve("menus/shop_main.yml")));
        assertFalse(CategoryKindMigrator.relocate(project).changed(), "a second run does nothing");
    }

    @Test
    void anIdAlreadyInTheCatalogueIsNeverOverwritten(@TempDir Path root) throws Exception {
        Path project = root.resolve("boutique_addon");
        Files.createDirectories(project.resolve("boutique_categories"));
        Files.createDirectories(project.resolve("catalogue/boutique_categories"));
        Files.writeString(project.resolve("boutique_categories/divers.yml"), "name: Racine\n");
        Files.writeString(project.resolve("catalogue/boutique_categories/divers.yaml"), "name: Rangée\n");

        CategoryKindMigrator.Result result = CategoryKindMigrator.relocate(project);

        assertEquals(List.of("boutique_categories/divers.yml"), result.moved());
        assertEquals(List.of("catalogue/boutique_categories/divers.yaml"), result.skipped());
        assertEquals("name: Racine\n", Files.readString(project.resolve("catalogue/divers.yml")));
        assertEquals("name: Rangée\n",
                Files.readString(project.resolve("catalogue/boutique_categories/divers.yaml")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void eachRowBecomesADocumentAndTheTableGoes(@TempDir Path root) throws Exception {
        Path project = seed(root);

        CategoryKindMigrator.Result result = CategoryKindMigrator.migrate(project, root);

        assertEquals(List.of("tools", "weapons"), result.moved());
        assertTrue(result.skipped().isEmpty());
        assertFalse(Files.exists(project.resolve("data/categories")), "the Table is gone once moved");

        Map<String, Object> weapons = read(project.resolve("catalogue/weapons.yaml"));
        assertEquals("boutique_category/v1", weapons.get("schema"));
        assertNull(weapons.get("id"), "the file name is the id");
        assertEquals("Armes", weapons.get("name"));
        assertEquals(10, weapons.get("discount"));
        assertEquals(2, weapons.get("order"), "rank in _source.yaml, from 1");
        assertEquals("red", weapons.get("color"), "a key the shop does not know is kept");
        Map<String, Object> icon = (Map<String, Object>) weapons.get("icon");
        assertEquals("DIAMOND_SWORD", icon.get("extends"));
        assertEquals(List.of("Tranchant", "Rapide"), icon.get("lore"));

        List<Map<String, Object>> items = (List<Map<String, Object>>) weapons.get("items");
        assertEquals("sword", items.get(0).get("id"));
        assertFalse(items.get(0).containsKey("discount"), "no discount, no key");
        assertEquals("run_command \"give {player} diamond_sword 1\" as console", items.get(0).get("actions"));
        assertEquals(5, items.get(1).get("discount"));

        // A name that was a key of the default language takes its text.
        Map<String, Object> tools = read(project.resolve("catalogue/tools.yaml"));
        assertEquals("Outils", tools.get("name"));
        assertEquals(1, tools.get("order"));

        // Every original is kept, lang/ is not touched.
        assertEquals(WEAPONS, Files.readString(root.resolve("category-kind-backup/data/categories/weapons.yaml")));
        assertTrue(Files.isRegularFile(root.resolve("category-kind-backup/data/categories/_source.yaml")));
        assertEquals("shop.tools: Outils\n", Files.readString(project.resolve("lang/fr.yaml")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void anotherLanguageTravelsWithItsText(@TempDir Path root) throws Exception {
        Path project = seed(root);
        Files.writeString(project.resolve("lang/en.yaml"), String.join("\n",
                "sources.categories.weapons.name: Weapons",
                "sources.categories.weapons.items.name: Sword",
                "sources.categories.weapons.icon.lore_2: Fast",
                "shop.tools: Tools", ""));

        CategoryKindMigrator.migrate(project, root);

        Map<String, Object> weapons = read(project.resolve("catalogue/weapons.yaml"));
        assertEquals(Map.of("fr", "Armes", "en", "Weapons"), weapons.get("name"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) weapons.get("items");
        assertEquals(Map.of("fr", "Épée", "en", "Sword"), items.get(0).get("name"));
        assertEquals("Arc", items.get(1).get("name"), "untranslated: the text alone");
        Map<String, Object> icon = (Map<String, Object>) weapons.get("icon");
        assertEquals(Map.of("fr", List.of("Tranchant", "Rapide"), "en", List.of("Tranchant", "Fast")), icon.get("lore"));
        assertEquals(Map.of("fr", "Outils", "en", "Tools"),
                read(project.resolve("catalogue/tools.yaml")).get("name"));
    }

    @Test
    void anExistingDocumentIsNeverOverwritten(@TempDir Path root) throws Exception {
        Path project = seed(root);
        Files.createDirectories(project.resolve("catalogue"));
        String mine = "schema: boutique_category/v1\nname: Mes armes\nitems: []\n";
        Files.writeString(project.resolve("catalogue/weapons.yaml"), mine);

        CategoryKindMigrator.Result result = CategoryKindMigrator.migrate(project, root);

        assertEquals(List.of("tools"), result.moved());
        assertEquals(List.of("weapons"), result.skipped());
        assertEquals(mine, Files.readString(project.resolve("catalogue/weapons.yaml")));
        // The row stays where it was, its Table with it.
        assertTrue(Files.isRegularFile(project.resolve("data/categories/weapons.yaml")));
        assertTrue(Files.isRegularFile(project.resolve("data/categories/_source.yaml")));
    }

    @Test
    void aSecondRunDoesNothing(@TempDir Path root) throws Exception {
        Path project = seed(root);
        CategoryKindMigrator.migrate(project, root);
        String after = Files.readString(project.resolve("catalogue/weapons.yaml"));

        CategoryKindMigrator.Result again = CategoryKindMigrator.migrate(project, root);

        assertFalse(again.changed());
        assertEquals(after, Files.readString(project.resolve("catalogue/weapons.yaml")));
    }

    @Test
    void theMigratedDocumentsServeTheSameShop(@TempDir Path root) throws Exception {
        Path project = seed(root);
        CategoryKindMigrator.migrate(project, root);

        ShopRepo repo = new ShopRepo();
        repo.loadCategories(List.of(
                withId(read(project.resolve("catalogue/weapons.yaml")), "weapons"),
                withId(read(project.resolve("catalogue/tools.yaml")), "tools")), "fr", 0);

        assertEquals(List.of("tools", "weapons"), repo.categories().stream().map(c -> c.getId()).toList());
        var sword = repo.findItem("sword").orElseThrow();
        assertEquals("Épée", sword.getName());
        assertEquals(90.0, sword.finalPrice(0), 1e-9); // 100 - 10 % (category)
        assertTrue(sword.hasActions());
    }

    private static Map<String, Object> withId(Map<String, Object> doc, String id) {
        var out = new java.util.LinkedHashMap<>(doc);
        out.put("id", id);
        return out;
    }
}
