package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BootstrapperTest {

    private static final List<String> EXPECTED_FILES = List.of(
            "manifest.yaml", "menus/shop_main.yaml", "menus/shop_category.yaml",
            "dialogs/confirm_purchase.yaml", "commands/boutique.yaml", "boutique/shop.yaml",
            "lang/fr.yaml");

    @Test
    void happyPath_writesAllBundledFiles(@TempDir File projectsDir) throws IOException {
        boolean wrote = Bootstrapper.bootstrapIfAbsent(projectsDir, Optional.empty());

        assertTrue(wrote);
        File projectDir = new File(projectsDir, "boutique_shop");
        assertTrue(projectDir.isDirectory());
        for (String res : EXPECTED_FILES) {
            File f = new File(projectDir, res);
            assertTrue(f.isFile(), () -> res + " should exist");
        }
        // no leftover temp dir
        assertFalse(new File(projectsDir, ".boutique_shop.tmp").exists());
    }

    @Test
    void preExistingProjectDir_returnsFalseAndWritesNothing(@TempDir File projectsDir) throws IOException {
        File projectDir = new File(projectsDir, "boutique_shop");
        Files.createDirectories(projectDir.toPath());
        Files.writeString(new File(projectDir, "marker.txt").toPath(), "already there");

        boolean wrote = Bootstrapper.bootstrapIfAbsent(projectsDir, Optional.empty());

        assertFalse(wrote);
        // untouched: only the marker we wrote is present, nothing bootstrapped
        assertTrue(new File(projectDir, "marker.txt").isFile());
        assertFalse(new File(projectDir, "manifest.yaml").exists());
    }

    @Test
    void staleTempDir_isCleanedUpAndDoesNotBlockBootstrap(@TempDir File projectsDir) throws IOException {
        File tmpDir = new File(projectsDir, ".boutique_shop.tmp");
        Files.createDirectories(tmpDir.toPath());
        Files.writeString(new File(tmpDir, "leftover.yaml").toPath(), "junk from a crashed previous attempt");

        boolean wrote = Bootstrapper.bootstrapIfAbsent(projectsDir, Optional.empty());

        assertTrue(wrote);
        File projectDir = new File(projectsDir, "boutique_shop");
        assertTrue(projectDir.isDirectory());
        for (String res : EXPECTED_FILES) {
            assertTrue(new File(projectDir, res).isFile());
        }
        assertFalse(new File(projectDir, "leftover.yaml").exists());
        assertFalse(tmpDir.exists());
    }

    @Test
    void migratedFiles_overrideBundledAndAddCapturedItems(@TempDir File projectsDir) throws IOException {
        String shop = "categories: []\n";
        String stack = "item:\n  '==': org.bukkit.inventory.ItemStack\n  type: DIAMOND_SWORD\n";
        Bootstrapper.bootstrapIfAbsent(projectsDir,
                Optional.of(java.util.Map.of("boutique/shop.yaml", shop, "items/ci_sword.yml", stack)));

        File projectDir = new File(projectsDir, "boutique_shop");
        assertEquals(shop, Files.readString(new File(projectDir, "boutique/shop.yaml").toPath()));
        // Les fichiers migrés hors ressources bundlées (items capturés) sont écrits aussi.
        assertEquals(stack, Files.readString(new File(projectDir, "items/ci_sword.yml").toPath()));
    }
}
