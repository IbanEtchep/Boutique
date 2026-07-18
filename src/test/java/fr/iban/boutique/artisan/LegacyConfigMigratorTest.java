package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyConfigMigratorTest {
    /**
     * Extrait représentatif du vrai config.yml MenuAPI. Le fichier ressource shipé
     * (src/main/resources/config.yml, template) écrit la clé item sous "display", mais c'est un artefact
     * de template obsolète : le vrai code runtime pré-refactor (ShopManager.loadShops/saveShopItem,
     * commit c72f586, `manager/ShopManager.java` lignes 45/104) lit et écrit systématiquement
     * "menuitem" pour catégories ET items — donc un config.yml réel (produit par le plugin, pas
     * recopié à la main depuis le template) a toujours "menuitem" aux deux niveaux. Le migrateur
     * garde un fallback "display" côté item par robustesse (cf. LegacyConfigMigrator#display).
     */
    private static final String LEGACY = String.join("\n",
        "whole-shop-discount: 5",
        "categories:",
        "  '1':",
        "    discount: 10",
        "    menuitem:",
        "      ==: menuitem",
        "      slot: 0",
        "      item:",
        "        ==: org.bukkit.inventory.ItemStack",
        "        type: DIAMOND_SWORD",
        "      name: '&bArmes'",
        "    items:",
        "      '1':",
        "        price: 100",
        "        discount: 5",
        "        buycommands:",
        "        - give %player% diamond_sword 1",
        "        menuitem:",
        "          ==: menuitem",
        "          slot: 0",
        "          item:",
        "            ==: org.bukkit.inventory.ItemStack",
        "            type: DIAMOND_SWORD",
        "          name: '&fÉpée en diamant'",
        "          lore:",
        "          - '&7Une épée tranchante'",
        "");

    @Test void migratesCategoriesItemsPricesAndStripsColorCodes() {
        var files = LegacyConfigMigrator.migrate(LEGACY).orElseThrow();
        String out = files.get("boutique/shop.yaml");
        assertTrue(out.contains("schema: boutique/v1"));
        assertTrue(out.contains("whole_shop_discount: 5"));
        assertTrue(out.contains("price: 100"));
        // buycommands → steps `actions` (run_command console, %player% → {player})
        assertTrue(out.contains("actions:"));
        assertTrue(out.contains("type: run_command"));
        assertTrue(out.contains("give {player} diamond_sword 1"));
        // Les noms gardent leurs codes couleur legacy (& → rendu MiniMessage viendra plus tard) :
        assertTrue(out.contains("Armes"));
    }

    @Test void capturesSerializedStacksAsItemFilesAndItemRefs() {
        var files = LegacyConfigMigrator.migrate(LEGACY).orElseThrow();
        String out = files.get("boutique/shop.yaml");
        // Le stack sérialisé devient un item capturé, référencé en icon universel.
        assertTrue(out.contains("icon: item:ci_armes"), out);
        assertTrue(out.contains("icon: item:ci_epee_en_diamant"), out);
        String captured = files.get("items/ci_epee_en_diamant.yml");
        assertNotNull(captured, String.valueOf(files.keySet()));
        assertTrue(captured.contains("item:"), captured);
        assertTrue(captured.contains("==: org.bukkit.inventory.ItemStack"), captured);
        assertTrue(captured.contains("type: DIAMOND_SWORD"), captured);
        // Les flags legacy que le stack seul ne dit pas voyagent en bloc display:.
        assertTrue(captured.contains("name: Épée en diamant"), captured);
    }

    @Test void identicallyNamedEntriesGetDistinctIds() {
        String legacy = String.join("\n",
            "categories:",
            "  '1':",
            "    menuitem: { name: 'Divers' }",
            "    items:",
            "      '1':",
            "        price: 10",
            "        menuitem: { name: '&fPotion' }",
            "      '2':",
            "        price: 20",
            "        menuitem: { name: '&fPotion' }",
            "");
        String out = LegacyConfigMigrator.migrate(legacy).orElseThrow().get("boutique/shop.yaml");
        assertTrue(out.contains("id: potion"), out);
        assertTrue(out.contains("id: potion_2"), out);
        // Pas de stack sérialisé → pas de capture, icônes fallback.
        assertTrue(out.contains("icon: BARRIER"), out);
    }

    @Test void stripCategoriesBlockRemovesOnlyThatBlock() {
        String yaml = String.join("\n",
            "#Azuriom database",
            "database:",
            "  host: localhost",
            "  port: 3306",
            "categories:",
            "  '1':",
            "    menuitem:",
            "      ==: menuitem",
            "      name: '&cname'",
            "",
            "  '2':",
            "    discount: 0",
            "whole-shop-discount: 5",
            "messages:",
            "  currency-name: tokens",
            "");
        String out = LegacyConfigMigrator.stripCategoriesBlock(yaml);
        assertFalse(out.contains("categories:"), out);
        assertFalse(out.contains("==:"), out);
        assertTrue(out.contains("host: localhost"), out);
        assertTrue(out.contains("whole-shop-discount: 5"), out);
        assertTrue(out.contains("currency-name: tokens"), out);
    }

    @Test void stripCategoriesBlockAtEndOfFile() {
        String yaml = "database:\n  host: h\ncategories:\n  '1':\n    menuitem:\n      ==: menuitem\n";
        String out = LegacyConfigMigrator.stripCategoriesBlock(yaml);
        assertFalse(out.contains("categories:"));
        assertTrue(out.contains("host: h"));
    }

    @Test void emptyOrUnparseableConfigYieldsEmpty() {
        assertTrue(LegacyConfigMigrator.migrate("").isEmpty());
        assertTrue(LegacyConfigMigrator.migrate("{{{").isEmpty());
        assertTrue(LegacyConfigMigrator.migrate("database: {}").isEmpty()); // pas de categories
    }
}
