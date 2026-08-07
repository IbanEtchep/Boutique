package fr.iban.boutique.artisan;

import fr.iban.boutique.ShopCategory;
import fr.iban.boutique.ShopItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bout en bout de l'install legacy — la séquence exacte de {@code ShopPlugin.onEnable} :
 * config.yml MenuAPI → {@link LegacyConfigMigrator} → {@link Bootstrapper} →
 * {@link ShopDataMigrator} → {@link ShopRepo}. Les tests unitaires par étage
 * passaient tous alors que la chaîne complète était morte : le bootstrap posait
 * le catalogue exemple, ce qui bloquait la conversion (garde `_source.yaml`) ET
 * masquait le legacy au chargement (la Table gagne sur `boutique/shop.yaml`).
 */
class LegacyInstallTest {

    private static final String LEGACY = String.join("\n",
            "whole-shop-discount: 5",
            "placeholders:",
            "  price-display: '%price% jetons'",
            "categories:",
            "  '1':",
            "    discount: 10",
            "    menuitem:",
            "      ==: menuitem",
            "      item:",
            "        ==: org.bukkit.inventory.ItemStack",
            "        type: CHEST",
            "      name: '&bArmes'",
            "    items:",
            "      '1':",
            "        price: 100",
            "        discount: 5",
            "        buycommands:",
            "        - give %player% diamond_sword 1",
            "        menuitem:",
            "          ==: menuitem",
            "          item:",
            "            ==: org.bukkit.inventory.ItemStack",
            "            type: DIAMOND_SWORD",
            "          name: '&fÉpée en diamant'",
            "");

    /** Les étapes de {@code onEnable} qui touchent le disque, dans l'ordre. */
    private static File install(File parent, Optional<Map<String, String>> migrated) throws IOException {
        File projectDir = new File(parent, "editor");
        Bootstrapper.bootstrapIfAbsent(projectDir, migrated);
        ShopDataMigrator.migrateIfNeeded(projectDir);
        return projectDir;
    }

    private static ShopRepo loadRepo(File projectDir) {
        File catDir = new File(projectDir, "data/categories");
        List<String> rows = Arrays.stream(Optional.ofNullable(catDir.list()).orElse(new String[0]))
                .filter(n -> n.endsWith(".yaml") && !n.equals("_source.yaml"))
                .sorted()
                .collect(Collectors.toList());
        ShopRepo repo = new ShopRepo();
        repo.loadFromDataTable(rel -> read(new File(catDir, rel)), rows, Map.of(), 5);
        return repo;
    }

    private static String read(File f) {
        try { return f.isFile() ? Files.readString(f.toPath()) : null; }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    @Test
    void freshInstallFromALegacyConfigServesTheLegacyCatalog(@TempDir File parent) throws Exception {
        Optional<Map<String, String>> migrated = LegacyConfigMigrator.migrate(LEGACY);
        File projectDir = install(parent, migrated);

        // Le catalogue exemple ne doit pas concurrencer le catalogue migré.
        assertFalse(new File(projectDir, "data/categories/weapons.yaml").exists(),
                "le catalogue exemple masquerait le catalogue migré");
        // …et la conversion a bien eu lieu, dès ce boot.
        assertFalse(new File(projectDir, "boutique/shop.yaml").exists(), "shop.yaml doit être archivé");
        assertTrue(new File(parent, "shop.yaml.migrated").isFile());

        ShopRepo repo = loadRepo(projectDir);
        List<ShopCategory> cats = repo.categories();
        assertEquals(List.of("armes"), cats.stream().map(ShopCategory::getId).collect(Collectors.toList()));
        assertEquals("Armes", cats.get(0).getName());
        assertEquals(10, cats.get(0).getDiscount());

        ShopItem item = repo.findItem("epee_en_diamant").orElseThrow();
        assertEquals("Épée en diamant", item.getName());
        assertEquals(100.0, item.getPrice());
        assertEquals("run_command \"give {player} diamond_sword 1\" as console", item.getActions());
        // L'icône reste une ref universelle vers le stack capturé, lui aussi posé.
        assertEquals("item:ci_epee_en_diamant", item.getIcon());
        assertTrue(new File(projectDir, "items/ci_epee_en_diamant.yml").isFile());

        // Les autres ressources bundlées sont là (menus, commande, dialogue, lang).
        for (String res : List.of("manifest.yaml", "menus/shop_main.yaml", "commands/boutique.yaml",
                "dialogs/confirm_purchase.yaml", "lang/fr.yaml", "models/shop_settings.yaml")) {
            assertTrue(new File(projectDir, res).isFile(), res);
        }
    }

    @Test
    void freshInstallWithoutLegacyConfigServesTheExampleCatalog(@TempDir File parent) throws Exception {
        File projectDir = install(parent, Optional.empty());

        assertTrue(new File(projectDir, "data/categories/weapons.yaml").isFile());
        ShopRepo repo = loadRepo(projectDir);
        assertEquals(List.of("weapons"), repo.categories().stream()
                .map(ShopCategory::getId).collect(Collectors.toList()));
    }

    /**
     * Rattrapage des serveurs déjà cassés : bootstrappés avec l'exemple ET
     * porteurs d'un shop.yaml migré jamais converti. L'exemple INTACT n'est pas
     * du contenu — il cède la place.
     */
    @Test
    void anUntouchedExampleTableDoesNotBlockAPendingMigration(@TempDir File parent) throws Exception {
        File projectDir = new File(parent, "editor");
        Bootstrapper.bootstrapIfAbsent(projectDir, Optional.empty());       // exemple posé
        Map<String, String> migrated = LegacyConfigMigrator.migrate(LEGACY).orElseThrow();
        Files.createDirectories(new File(projectDir, "boutique").toPath());
        Files.writeString(new File(projectDir, "boutique/shop.yaml").toPath(),
                migrated.get("boutique/shop.yaml"));                        // shop.yaml orphelin

        assertTrue(ShopDataMigrator.migrateIfNeeded(projectDir));

        assertFalse(new File(projectDir, "data/categories/weapons.yaml").exists());
        assertEquals(List.of("armes"), loadRepo(projectDir).categories().stream()
                .map(ShopCategory::getId).collect(Collectors.toList()));
    }

    /** …mais une Table éditée par l'admin est du contenu : on n'y touche jamais. */
    @Test
    void anEditedTableIsNeverClobberedByAnOrphanShopYaml(@TempDir File parent) throws Exception {
        File projectDir = new File(parent, "editor");
        Bootstrapper.bootstrapIfAbsent(projectDir, Optional.empty());
        File weapons = new File(projectDir, "data/categories/weapons.yaml");
        Files.writeString(weapons.toPath(), Files.readString(weapons.toPath())
                .replace("name: \"Armes\"", "name: \"Mes armes à moi\""));
        Files.createDirectories(new File(projectDir, "boutique").toPath());
        Files.writeString(new File(projectDir, "boutique/shop.yaml").toPath(),
                LegacyConfigMigrator.migrate(LEGACY).orElseThrow().get("boutique/shop.yaml"));

        assertFalse(ShopDataMigrator.migrateIfNeeded(projectDir));

        assertTrue(Files.readString(weapons.toPath()).contains("Mes armes à moi"));
        assertTrue(new File(projectDir, "boutique/shop.yaml").isFile(), "rien n'est archivé sans conversion");
    }
}
