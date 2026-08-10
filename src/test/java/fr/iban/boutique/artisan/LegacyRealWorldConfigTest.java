package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extrait fidèle du config.yml de production (PrimalCraft) : noms en codes hex
 * `&x&R&R&G&G&B&B`, lore portant `%price_display%`, items à meta sérialisée.
 */
class LegacyRealWorldConfigTest {

    /** "Les Clés" et "Clé Légendaire" écrits en hex, comme le fait le vrai serveur. */
    private static final String LEGACY = String.join("\n",
            "categories:",
            "  '1':",
            "    menuitem:",
            "      ==: menuitem",
            "      item:",
            "        ==: org.bukkit.inventory.ItemStack",
            "        type: TRIPWIRE_HOOK",
            "      name: '&x&D&2&A&6&A&6&lL&x&C&7&9&6&9&6&le&x&B&C&8&5&8&5&ls &x&A&7&6&5&6&5&lC&x&9&C&5&5&5&5&ll&x&9&1&4&4&4&4&lé&x&8&6&3&4&3&4&ls'",
            "      enchanted: true",
            "    discount: 0",
            "    items:",
            "      '1':",
            "        menuitem:",
            "          ==: menuitem",
            "          item:",
            "            ==: org.bukkit.inventory.ItemStack",
            "            type: TRIPWIRE_HOOK",
            "          name: '&x&B&C&2&6&2&6C&x&C&6&3&7&2&8l&x&C&F&4&8&2&9é'",
            "          lore:",
            "          - '&f>> 1 clé qui vous donne la chance de gagner'",
            "          - '%price_display%'",
            "          enchanted: true",
            "        price: 500",
            "        discount: 0",
            "        buycommands:",
            "        - crate key give %player% legendary 1",
            "");

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstCategory() {
        String shop = LegacyConfigMigrator.migrate(LEGACY).orElseThrow().get("boutique/shop.yaml");
        Map<String, Object> root = new Yaml().load(shop);
        return ((List<Map<String, Object>>) root.get("categories")).get(0);
    }

    /**
     * Les codes hex s'écrivent en MAJUSCULES (`&x&D&2&A&6…`). Un nettoyage qui ne
     * retirait que les minuscules laissait « &D&A&A » dans le nom, et donnait des
     * ids illisibles comme `d_a_al` au lieu de `les_cles`.
     */
    @Test
    void hexColourCodesAreStrippedWhateverTheirCase() {
        Map<String, Object> cat = firstCategory();
        assertEquals("Les Clés", cat.get("name"));
        assertEquals("les_cles", cat.get("id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void itemNamesAreReadableToo() {
        Map<String, Object> item = ((List<Map<String, Object>>) firstCategory().get("items")).get(0);
        assertEquals("Clé", item.get("name"));
        assertEquals(500, item.get("price"));
        assertEquals("run_command \"crate key give {player} legendary 1\" as console", item.get("actions"));
    }

    /**
     * Le serveur mélange DEUX syntaxes de couleur : `&x&R&R&G&G&B&B` (legacy
     * Bukkit) et `#RRGGBB` (moderne). Ne nettoyer que la première laissait
     * « #3399FFSpawner à poule » comme nom, et un id de repli `item_1_2` —
     * le slug ne peut pas commencer par le chiffre du code couleur.
     */
    @Test
    void modernHexColourCodesAreStrippedToo() {
        assertEquals("Kit Mineur", LegacyConfigMigrator.stripColours("#3399FF&lKit Mineur"));
        assertEquals("Pack fer", LegacyConfigMigrator.stripColours(
                "&l#A5B2B7P#9AA5A9a#8E999Cc#838C8Ek #6C7372f#606765e#555A57r"));
        assertEquals("Spawner à poule", LegacyConfigMigrator.stripColours("#3399FF&lSpawner à poule"));
    }

    /**
     * `%price_display%` était résolu par l'ancien moteur de rendu. Côté Artisan le
     * prix est affiché par le menu ; garder la ligne afficherait le placeholder
     * brut dans le lore.
     */
    @SuppressWarnings("unchecked")
    @Test
    void thePriceDisplayPlaceholderLineIsDropped() {
        Map<String, Object> item = ((List<Map<String, Object>>) firstCategory().get("items")).get(0);
        List<String> lore = (List<String>) item.get("lore");
        assertEquals(1, lore.size(), () -> "le placeholder de prix doit sauter, reste: " + lore);
        assertTrue(lore.get(0).contains("1 clé qui vous donne la chance"));
    }
}
