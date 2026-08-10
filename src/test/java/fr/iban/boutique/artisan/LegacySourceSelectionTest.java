package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * D'où vient le texte donné au migrateur.
 *
 * L'assainissement du config.yml (retrait du bloc `categories:`, indispensable
 * avant le premier getConfig() : Bukkit ne sait plus désérialiser les blocs
 * `==: menuitem`) est DESTRUCTIF et se produit avant la migration. Si le plugin
 * meurt entre les deux — ce qui est arrivé le 2026-08-08, LinkageError juste
 * après le WARN d'assainissement — alors au boot suivant `config.yml` n'a plus
 * de catalogue, la migration ne trouve rien, et le bootstrap pose le catalogue
 * exemple par-dessus une boutique de 7 catégories.
 *
 * D'où la règle : `config.yml.legacy` est la copie d'avant assainissement, donc
 * la source de vérité tant qu'il existe.
 */
class LegacySourceSelectionTest {

    private static final String LEGACY_WITH_CATEGORIES = String.join("\n",
            "database:",
            "  host: 172.18.0.1",
            "categories:",
            "  '1':",
            "    menuitem:",
            "      ==: menuitem",
            "      item:",
            "        ==: org.bukkit.inventory.ItemStack",
            "        type: TRIPWIRE_HOOK",
            "      name: '&bLes Cles'",
            "    items:",
            "      '1':",
            "        price: 500",
            "        buycommands:",
            "        - crate key give %player% legendary 1",
            "        menuitem:",
            "          ==: menuitem",
            "          item:",
            "            ==: org.bukkit.inventory.ItemStack",
            "            type: TRIPWIRE_HOOK",
            "          name: '&cCle Legendaire'",
            "");

    @Test
    void prefersTheUntouchedLegacyCopyOverTheSanitizedConfig(@TempDir File dataFolder) throws Exception {
        // L'état exact d'un serveur qui a crashé après l'assainissement :
        // config.yml amputé de son catalogue, config.yml.legacy intact.
        Files.writeString(new File(dataFolder, "config.yml").toPath(),
                LegacyConfigMigrator.stripCategoriesBlock(LEGACY_WITH_CATEGORIES));
        Files.writeString(new File(dataFolder, "config.yml.legacy").toPath(), LEGACY_WITH_CATEGORIES);

        String source = LegacyConfigMigrator.readLegacySource(dataFolder);
        Optional<Map<String, String>> migrated = LegacyConfigMigrator.migrate(source);

        assertTrue(migrated.isPresent(), "le catalogue doit être repris depuis config.yml.legacy");
        assertTrue(migrated.get().get("boutique/shop.yaml").contains("price: 500"));
    }

    @Test
    void fallsBackToConfigYamlWhenNoLegacyCopyExists(@TempDir File dataFolder) throws Exception {
        Files.writeString(new File(dataFolder, "config.yml").toPath(), LEGACY_WITH_CATEGORIES);

        String source = LegacyConfigMigrator.readLegacySource(dataFolder);

        assertTrue(LegacyConfigMigrator.migrate(source).isPresent());
    }

    @Test
    void returnsNullWhenThereIsNothingToRead(@TempDir File dataFolder) {
        assertNull(LegacyConfigMigrator.readLegacySource(dataFolder));
    }
}
