package fr.iban.boutique.artisan;

import net.artisanmc.modules.api.playercommand.PlayerCommandDeclaration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ShopCommandMigratorTest {

    private static final String SHIPPED = """
            schema: command/v1
            id: boutique
            name: boutique
            description: "Ouvre la boutique"
            actions: 'open_menu shop_main'
            """;

    private static final String MANIFEST = """
            schema: manifest/v1
            module: boutique
            default_lang: fr
            files:
              menus: [shop_main, shop_category]
              commands: [boutique]
              dialogs: [confirm_purchase]
            """;

    private static File folder(Path dir, String command, String manifest) throws IOException {
        Files.createDirectories(dir.resolve("commands"));
        Files.writeString(dir.resolve("commands/boutique.yaml"), command);
        Files.writeString(dir.resolve("manifest.yaml"), manifest);
        return dir.toFile();
    }

    @Test
    void theShippedFileGoesWithItsManifestEntry(@TempDir Path dir) throws IOException {
        File projectDir = folder(dir, SHIPPED, MANIFEST);
        assertEquals(ShopCommandMigrator.Result.REMOVED, ShopCommandMigrator.migrate(projectDir));
        assertFalse(Files.exists(dir.resolve("commands/boutique.yaml")));
        assertEquals(MANIFEST.replace("  commands: [boutique]\n", ""), Files.readString(dir.resolve("manifest.yaml")));
        assertEquals(ShopCommandMigrator.Result.NONE, ShopCommandMigrator.migrate(projectDir), "idempotent");
    }

    @Test
    void anotherCommandOfTheManifestStays(@TempDir Path dir) throws IOException {
        folder(dir, SHIPPED, MANIFEST.replace("[boutique]", "[boutique, vip]"));
        ShopCommandMigrator.migrate(dir.toFile());
        assertTrue(Files.readString(dir.resolve("manifest.yaml")).contains("  commands: [vip]\n"));
    }

    @Test
    void aChangedFileIsKeptAsWritten(@TempDir Path dir) throws IOException {
        String custom = SHIPPED.replace("open_menu shop_main", "open_menu vip_shop");
        folder(dir, custom, MANIFEST);
        assertEquals(ShopCommandMigrator.Result.KEPT_CUSTOM, ShopCommandMigrator.migrate(dir.toFile()));
        assertEquals(custom, Files.readString(dir.resolve("commands/boutique.yaml")));
        assertEquals(MANIFEST, Files.readString(dir.resolve("manifest.yaml")));
    }

    @Test
    void artisansOwnCommandFileIsLeftToArtisan(@TempDir Path dir) throws IOException {
        String artisan = """
                schema: command/v1
                name: boutique
                actions:
                  - open_menu: boutique_addon:shop_main
                description:
                  fr: Ouvre la boutique
                  en: Opens the shop
                """;
        folder(dir, artisan, MANIFEST);
        assertEquals(ShopCommandMigrator.Result.NONE, ShopCommandMigrator.migrate(dir.toFile()));
        assertEquals(artisan, Files.readString(dir.resolve("commands/boutique.yaml")));
        assertEquals(MANIFEST, Files.readString(dir.resolve("manifest.yaml")));
    }

    @Test
    void anAddedPermissionOrAliasCountsAsAChange() {
        assertFalse(ShopCommandMigrator.isShipped(SHIPPED + "permission: boutique.use\n"));
        assertFalse(ShopCommandMigrator.isShipped(SHIPPED.replace("Ouvre la boutique", "La boutique")));
        assertTrue(ShopCommandMigrator.isShipped(SHIPPED.replace("'open_menu shop_main'", "'open_menu  shop_main '")));
    }

    @Test
    void theModuleCommandOpensTheShopMenuOfTheModuleFolder() {
        PlayerCommandDeclaration command = BoutiqueModule.openCommand();
        assertEquals("boutique:open", command.getId());
        assertEquals("boutique", command.getName());
        assertEquals("boutique_addon:shop_main", command.getOpensMenu());
        assertNull(command.getExecutor());
        assertFalse(command.isConsoleAllowed());
    }
}
