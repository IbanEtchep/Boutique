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
        String out = LegacyConfigMigrator.migrate(LEGACY).orElseThrow();
        assertTrue(out.contains("schema: boutique/v1"));
        assertTrue(out.contains("whole_shop_discount: 5"));
        assertTrue(out.contains("icon: DIAMOND_SWORD"));
        assertTrue(out.contains("price: 100"));
        assertTrue(out.contains("give %player% diamond_sword 1"));
        // Les noms gardent leurs codes couleur legacy (& → rendu MiniMessage viendra plus tard) :
        assertTrue(out.contains("Armes"));
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
        String out = LegacyConfigMigrator.migrate(legacy).orElseThrow();
        assertTrue(out.contains("id: potion"), out);
        assertTrue(out.contains("id: potion_2"), out);
    }

    @Test void emptyOrUnparseableConfigYieldsEmpty() {
        assertTrue(LegacyConfigMigrator.migrate("").isEmpty());
        assertTrue(LegacyConfigMigrator.migrate("{{{").isEmpty());
        assertTrue(LegacyConfigMigrator.migrate("database: {}").isEmpty()); // pas de categories
    }
}
