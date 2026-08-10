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
 * page, donc produire UN MENU PAR CATÉGORIE, chacun plaçant ses propres
 * articles.
 *
 * Pourquoi un menu par catégorie plutôt qu'un menu paramétré unique : le mode
 * placement épingle des lignes d'une source à des slots fixes. Avec un seul
 * menu réutilisé pour les 7 catégories, un même slot porterait 7 occupants
 * selon le contexte d'aperçu — invisible à l'édition, et l'article de la
 * catégorie « Clés » et celui des « Kits » visent tous deux le slot 20. Un menu
 * par catégorie rend au placement sa sémantique : une source, un écran, des
 * slots. Le filtre littéral (`filter: {category: "<id>"}`) suffit — la source
 * reste STATIC, donc plaçable.
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

    @Test
    @SuppressWarnings("unchecked")
    void oneMenuPerCategoryFilteredOnItsOwnId(@TempDir File root) throws Exception {
        File projectDir = migrate(root);

        Map<String, Object> menu = yaml(new File(projectDir, "menus/shop_les_cles.yaml"));
        assertEquals("shop_les_cles", menu.get("id"));

        List<Map<String, Object>> inputs = (List<Map<String, Object>>) menu.get("inputs");
        Map<String, Object> source = (Map<String, Object>) inputs.get(0).get("source");
        assertEquals("boutique:items", source.get("ref"));
        // Filtre LITTÉRAL : la source reste statique, donc plaçable.
        assertEquals(Map.of("category", "les_cles"), source.get("filter"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void itemsKeepTheirSlotAndPage(@TempDir File root) throws Exception {
        File projectDir = migrate(root);

        Map<String, Object> menu = yaml(new File(projectDir, "menus/shop_les_cles.yaml"));
        List<Map<String, Object>> elements = (List<Map<String, Object>>) menu.get("elements");
        Map<String, Object> places = (Map<String, Object>) elements.get(0).get("places");

        assertEquals("items", places.get("source"));
        assertEquals("id", places.get("key_field"));
        assertEquals(List.of(
                Map.of("key", "cle_legendaire", "page", 0, "position", 20),
                Map.of("key", "cle_rare", "page", 1, "position", 29)),
                places.get("placements"));
    }

    /**
     * Le menu principal : UN élément par catégorie, chacun ne plaçant que la
     * sienne. Un seul élément pour les 7 aurait été plus court, mais le clic
     * serait commun — et `open_menu shop_{cat.id}` ne parse pas : le lexer du
     * DSL n'accepte que [A-Za-z_] dans un identifiant, pas un template. Un
     * élément par catégorie donne à chacune son clic littéral tout en gardant
     * nom et icône liés à la donnée.
     */
    @Test
    @SuppressWarnings("unchecked")
    void theMainMenuPlacesCategoriesAndOpensTheirMenu(@TempDir File root) throws Exception {
        File projectDir = migrate(root);

        Map<String, Object> menu = yaml(new File(projectDir, "menus/shop_main.yaml"));
        List<Map<String, Object>> elements = (List<Map<String, Object>>) menu.get("elements");
        assertEquals(2, elements.size());

        Map<String, Object> first = elements.get(0);
        Map<String, Object> places = (Map<String, Object>) first.get("places");
        assertEquals("cats", places.get("source"));
        assertEquals(List.of(Map.of("key", "les_cles", "page", 0, "position", 20)),
                places.get("placements"));

        Map<String, Object> appearance = ((List<Map<String, Object>>) first.get("appearances")).get(0);
        assertEquals("{cat.name}", appearance.get("title"));
        Map<String, Object> click = (Map<String, Object>) appearance.get("click");
        assertEquals("open_menu shop_les_cles", click.get("left"));

        Map<String, Object> second = (Map<String, Object>) ((List<Map<String, Object>>) elements.get(1).get("appearances")).get(0);
        assertEquals("open_menu shop_les_kits",
                ((Map<String, Object>) second.get("click")).get("left"));
    }

    /** Le manifeste doit lister les menus générés, sinon le plugin ne les lit pas. */
    @Test
    @SuppressWarnings("unchecked")
    void theManifestListsEveryGeneratedMenu(@TempDir File root) throws Exception {
        File projectDir = migrate(root);

        Map<String, Object> manifest = yaml(new File(projectDir, "manifest.yaml"));
        Map<String, Object> files = (Map<String, Object>) manifest.get("files");
        assertEquals(List.of("shop_main", "shop_les_cles", "shop_les_kits"), files.get("menus"));
        // Le menu paginé bundlé n'a plus de raison d'être.
        assertFalse(new File(projectDir, "menus/shop_category.yaml").exists());
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
