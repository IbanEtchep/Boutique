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
 * Le legacy ne pagine pas : chaque catégorie et chaque article porte un
 * {@code page}/{@code slot} explicite. La migration doit rendre cette mise en
 * page — donc du placement — mais dans UN SEUL menu paramétré, pas un menu par
 * catégorie (ADR `parameterised-menu-over-menu-per-category`).
 *
 * Un menu par catégorie rendait la même chose et se lisait plus simplement,
 * mais faisait payer quatre artefacts corrélés à chaque nouvelle catégorie :
 * une ligne, un menu, un lien, un filtre. Avec un menu paramétré, ajouter une
 * catégorie est un geste — créer la ligne — et tout le reste suit.
 */
class PlacedMenuGenerationTest {

    private static final String LEGACY = String.join("\n",
            "categories:",
            "  '1':",
            "    menuitem:",
            "      ==: menuitem",
            "      page: 0",
            "      slot: 20",
            "      item: {==: org.bukkit.inventory.ItemStack, type: TRIPWIRE_HOOK}",
            "      name: '&bLes Cles'",
            "    items:",
            "      '1':",
            "        price: 500",
            "        buycommands: ['crate key give %player% legendary 1']",
            "        menuitem:",
            "          ==: menuitem",
            "          page: 0",
            "          slot: 20",
            "          item: {==: org.bukkit.inventory.ItemStack, type: TRIPWIRE_HOOK}",
            "          name: '&cCle Legendaire'",
            "      '2':",
            "        price: 2250",
            "        buycommands: ['crate key give %player% legendary 5']",
            "        menuitem:",
            "          ==: menuitem",
            "          page: 1",
            "          slot: 29",
            "          item: {==: org.bukkit.inventory.ItemStack, type: TRIPWIRE_HOOK}",
            "          name: '&cCle Rare'",
            "  '2':",
            "    menuitem:",
            "      ==: menuitem",
            "      page: 0",
            "      slot: 28",
            "      item: {==: org.bukkit.inventory.ItemStack, type: IRON_PICKAXE}",
            "      name: '&bLes Kits'",
            "    items:",
            "      '1':",
            "        price: 300",
            "        buycommands: ['kit mineur %player%']",
            "        menuitem:",
            "          ==: menuitem",
            "          page: 0",
            "          slot: 20",
            "          item: {==: org.bukkit.inventory.ItemStack, type: IRON_PICKAXE}",
            "          name: '&bKit Mineur'",
            "");

    private File migrate(File root) throws Exception {
        File projectDir = new File(root, "editor");
        Bootstrapper.bootstrapIfAbsent(projectDir, LegacyConfigMigrator.migrate(LEGACY));
        assertTrue(ShopDataMigrator.migrateIfNeeded(projectDir));
        return projectDir;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> yaml(File f) throws Exception {
        assertTrue(f.isFile(), () -> f + " devrait exister");
        return new Yaml().load(Files.readString(f.toPath()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> placesOf(Map<String, Object> menu, int elementIndex) {
        List<Map<String, Object>> elements = (List<Map<String, Object>>) menu.get("elements");
        return (Map<String, Object>) elements.get(elementIndex).get("places");
    }

    @Test
    @SuppressWarnings("unchecked")
    void aSingleParameterisedMenuServesEveryCategory(@TempDir File root) throws Exception {
        File projectDir = migrate(root);

        assertFalse(new File(projectDir, "menus/shop_les_cles.yaml").exists(),
                "un menu par catégorie ferait payer quatre artefacts à chaque ajout");

        Map<String, Object> menu = yaml(new File(projectDir, "menus/shop_category.yaml"));
        List<Map<String, Object>> inputs = (List<Map<String, Object>>) menu.get("inputs");

        // La catégorie arrive par le contexte, pas en dur : c'est ce qui rend le
        // menu réutilisable — et ce qui peuple le sélecteur de Preview context.
        Map<String, Object> items = inputs.stream()
                .filter(i -> "items".equals(i.get("name"))).findFirst().orElseThrow();
        Map<String, Object> source = (Map<String, Object>) items.get("source");
        assertEquals("boutique:items", source.get("ref"));
        assertEquals(Map.of("category", "{category}"), source.get("filter"));
    }

    /**
     * LE test de la décision : les articles de TOUTES les catégories vivent dans
     * le même élément, et deux d'entre eux visent le slot 20. Ce n'est pas une
     * collision — le filtre fait qu'un seul existe à la fois — mais c'est ce qui
     * rend ces placements illisibles sans Preview context.
     */
    @Test
    void everyCategoryPlacesItsItemsInTheSameElement(@TempDir File root) throws Exception {
        File projectDir = migrate(root);

        Map<String, Object> places = placesOf(yaml(new File(projectDir, "menus/shop_category.yaml")), 0);
        assertEquals("items", places.get("source"));
        assertEquals("id", places.get("key_field"));
        assertEquals(List.of(
                Map.of("key", "cle_legendaire", "page", 0, "position", 20),
                Map.of("key", "cle_rare", "page", 1, "position", 29),
                Map.of("key", "kit_mineur", "page", 0, "position", 20)),
                places.get("placements"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void theMainMenuPlacesCategoriesAndPassesTheContext(@TempDir File root) throws Exception {
        File projectDir = migrate(root);

        Map<String, Object> menu = yaml(new File(projectDir, "menus/shop_main.yaml"));
        List<Map<String, Object>> elements = (List<Map<String, Object>>) menu.get("elements");
        assertEquals(1, elements.size(), "un seul élément suffit : le lien est dynamique");

        assertEquals(List.of(
                Map.of("key", "les_cles", "page", 0, "position", 20),
                Map.of("key", "les_kits", "page", 0, "position", 28)),
                placesOf(menu, 0).get("placements"));

        // Le template tient dans une chaîne entre guillemets — contrairement à un
        // identifiant de menu, que le lexer du DSL refuse d'interpoler.
        Map<String, Object> appearance = ((List<Map<String, Object>>) elements.get(0).get("appearances")).get(0);
        Map<String, Object> click = (Map<String, Object>) appearance.get("click");
        assertEquals("open_menu shop_category with category=\"{cat.id}\" category_name=\"{cat.name}\"",
                click.get("left"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void theManifestListsBothMenus(@TempDir File root) throws Exception {
        File projectDir = migrate(root);

        Map<String, Object> manifest = yaml(new File(projectDir, "manifest.yaml"));
        Map<String, Object> files = (Map<String, Object>) manifest.get("files");
        assertEquals(List.of("shop_main", "shop_category"), files.get("menus"));
    }

    /** La position est une affaire de MENU : elle ne pollue pas la donnée. */
    @Test
    @SuppressWarnings("unchecked")
    void slotsNeverLeakIntoTheCatalogueRows(@TempDir File root) throws Exception {
        File projectDir = migrate(root);

        Map<String, Object> row = yaml(new File(projectDir, "data/categories/les_cles.yaml"));
        assertNull(row.get("slot"));
        assertNull(row.get("page"));
        Map<String, Object> item = ((List<Map<String, Object>>) row.get("items")).get(0);
        assertNull(item.get("slot"));
        assertNull(item.get("page"));
    }
}
