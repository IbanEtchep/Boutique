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
            "dialogs/confirm_purchase.yaml", "commands/boutique.yaml",
            // Declared-model catalog (ADR composable-data-models §8).
            "data/categories/_source.yaml", "data/categories/weapons.yaml",
            "data/shop_settings.yaml",
            "lang/fr.yaml");

    // bootstrapIfAbsent writes directly INTO the given project dir (the module folder
    // plugins/Artisan/modules/boutique_addon), using a sibling .<name>.tmp.

    @Test
    void happyPath_writesAllBundledFiles(@TempDir File parent) throws IOException {
        File projectDir = new File(parent, "editor");
        boolean wrote = Bootstrapper.bootstrapIfAbsent(projectDir, Optional.empty());

        assertTrue(wrote);
        assertTrue(projectDir.isDirectory());
        assertFalse(Files.readString(new File(projectDir, "manifest.yaml").toPath()).contains("project_id:"));
        for (String res : EXPECTED_FILES) {
            File f = new File(projectDir, res);
            assertTrue(f.isFile(), () -> res + " should exist");
        }
        // no leftover temp dir
        assertFalse(new File(parent, ".editor.tmp").exists());
    }

    @Test
    void projectWithManifest_returnsFalseAndWritesNothing(@TempDir File parent) throws IOException {
        File projectDir = new File(parent, "editor");
        Files.createDirectories(projectDir.toPath());
        Files.writeString(new File(projectDir, "manifest.yaml").toPath(), "schema: manifest/v1\n");

        boolean wrote = Bootstrapper.bootstrapIfAbsent(projectDir, Optional.empty());

        assertFalse(wrote);
        assertEquals("schema: manifest/v1\n", Files.readString(new File(projectDir, "manifest.yaml").toPath()));
        assertFalse(new File(projectDir, "menus").exists());
    }

    @Test
    void emptyModuleFolder_isFilled(@TempDir File parent) throws IOException {
        // Artisan creates plugins/Artisan/modules/<folder>/ before the add-on writes its defaults.
        File projectDir = new File(parent, "boutique_addon");
        Files.createDirectories(new File(projectDir, ".artisan").toPath());
        Files.writeString(new File(projectDir, ".artisan/project-id").toPath(), "kept\n");

        assertTrue(Bootstrapper.bootstrapIfAbsent(projectDir, Optional.empty()));

        for (String res : EXPECTED_FILES) {
            assertTrue(new File(projectDir, res).isFile(), () -> res + " should exist");
        }
        assertEquals("kept\n", Files.readString(new File(projectDir, ".artisan/project-id").toPath()));
        assertFalse(new File(parent, ".boutique_addon.tmp").exists());
    }

    @Test
    void interruptedFill_withoutManifest_isRetried(@TempDir File parent) throws IOException {
        File projectDir = new File(parent, "boutique_addon");
        Files.createDirectories(new File(projectDir, "menus").toPath());
        Files.writeString(new File(projectDir, "menus/shop_main.yaml").toPath(), "half written");

        assertTrue(Bootstrapper.bootstrapIfAbsent(projectDir, Optional.empty()));

        assertNotEquals("half written", Files.readString(new File(projectDir, "menus/shop_main.yaml").toPath()));
        assertTrue(new File(projectDir, "manifest.yaml").isFile());
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
        assertFalse(Files.readString(new File(projectDir, "manifest.yaml").toPath()).contains("project_id:"));
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
