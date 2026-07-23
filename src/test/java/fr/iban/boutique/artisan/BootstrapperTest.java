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

    // bootstrapIfAbsent writes directly INTO the given project dir (the Add-on
    // registers e.g. plugins/Boutique/editor as its root), using a sibling .<name>.tmp.

    @Test
    void happyPath_writesAllBundledFiles(@TempDir File parent) throws IOException {
        File projectDir = new File(parent, "editor");
        boolean wrote = Bootstrapper.bootstrapIfAbsent(projectDir, Optional.empty());

        assertTrue(wrote);
        assertTrue(projectDir.isDirectory());
        for (String res : EXPECTED_FILES) {
            File f = new File(projectDir, res);
            assertTrue(f.isFile(), () -> res + " should exist");
        }
        // no leftover temp dir
        assertFalse(new File(parent, ".editor.tmp").exists());
    }

    @Test
    void preExistingProjectDir_returnsFalseAndWritesNothing(@TempDir File parent) throws IOException {
        File projectDir = new File(parent, "editor");
        Files.createDirectories(projectDir.toPath());
        Files.writeString(new File(projectDir, "marker.txt").toPath(), "already there");

        boolean wrote = Bootstrapper.bootstrapIfAbsent(projectDir, Optional.empty());

        assertFalse(wrote);
        // untouched: only the marker we wrote is present, nothing bootstrapped
        assertTrue(new File(projectDir, "marker.txt").isFile());
        assertFalse(new File(projectDir, "manifest.yaml").exists());
    }

    @Test
    void staleTempDir_isCleanedUpAndDoesNotBlockBootstrap(@TempDir File parent) throws IOException {
        File tmpDir = new File(parent, ".editor.tmp");
        Files.createDirectories(tmpDir.toPath());
        Files.writeString(new File(tmpDir, "leftover.yaml").toPath(), "junk from a crashed previous attempt");

        File projectDir = new File(parent, "editor");
        boolean wrote = Bootstrapper.bootstrapIfAbsent(projectDir, Optional.empty());

        assertTrue(wrote);
        assertTrue(projectDir.isDirectory());
        for (String res : EXPECTED_FILES) {
            assertTrue(new File(projectDir, res).isFile());
        }
        assertFalse(new File(projectDir, "leftover.yaml").exists());
        assertFalse(tmpDir.exists());
    }

    @Test
    void migratedFiles_overrideBundledAndAddCapturedItems(@TempDir File parent) throws IOException {
        String shop = "categories: []\n";
        String stack = "item:\n  '==': org.bukkit.inventory.ItemStack\n  type: DIAMOND_SWORD\n";
        File projectDir = new File(parent, "editor");
        Bootstrapper.bootstrapIfAbsent(projectDir,
                Optional.of(java.util.Map.of("boutique/shop.yaml", shop, "items/ci_sword.yml", stack)));

        assertEquals(shop, Files.readString(new File(projectDir, "boutique/shop.yaml").toPath()));
        // Les fichiers migrés hors ressources bundlées (items capturés) sont écrits aussi.
        assertEquals(stack, Files.readString(new File(projectDir, "items/ci_sword.yml").toPath()));
    }
}
