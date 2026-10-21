package fr.iban.boutique.artisan;

import net.artisanmc.modules.api.template.TemplateDeclaration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SystemMenusTest {

    @Test
    void menusAreSystemElements() {
        TemplateDeclaration menus = BoutiqueModule.systemMenus();

        assertTrue(menus.isSystem());
        assertNull(menus.getSeedProject());
        assertEquals(BoutiqueModule.SYSTEM_MENU_FILES, menus.getPaths());
    }

    /** Artisan sees a fresh install's files as the shipped ones: no "modified" right after bootstrap. */
    @Test
    void bootstrapWritesTheSameBytes(@TempDir File parent) throws IOException {
        File projectDir = new File(parent, "boutique_addon");
        Bootstrapper.bootstrapIfAbsent(projectDir, Optional.empty());

        TemplateDeclaration menus = BoutiqueModule.systemMenus();
        for (String path : menus.getPaths()) {
            byte[] written = Files.readAllBytes(new File(projectDir, path).toPath());
            assertEquals(menus.getHash(path), TemplateDeclaration.sha256(written), path);
        }
    }
}
