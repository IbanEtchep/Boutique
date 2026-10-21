package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SettingsMigratorTest {
    @TempDir File projectDir;

    private File settings() throws Exception {
        File data = new File(projectDir, "data");
        data.mkdirs();
        new File(projectDir, "models").mkdirs();
        Files.writeString(new File(projectDir, "models/shop_settings.yaml").toPath(), "model: boutique:shop_settings\n");
        File file = new File(data, "shop_settings.yaml");
        Files.writeString(file.toPath(), "whole_shop_discount: 0\nplaceholders:\n  price_display: '"
                + ShopModels.DEFAULT_PRICE_DISPLAY + "'\n  discount_price_display: '" + ShopModels.DEFAULT_DISCOUNT_PRICE_DISPLAY + "'\n");
        return file;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> placeholders(File file) throws Exception {
        Map<String, Object> root = new Yaml().load(Files.readString(file.toPath()));
        return (Map<String, Object>) root.get("placeholders");
    }

    @Test
    void theLegacyFormatsReplaceTheUntouchedDefaultsTheFirstTime() throws Exception {
        File file = settings();
        assertTrue(SettingsMigrator.ensureSettings(projectDir, "&e%price% jetons", null, true));
        assertEquals("&e%price% jetons", placeholders(file).get("price_display"));
        assertEquals(ShopModels.DEFAULT_DISCOUNT_PRICE_DISPLAY, placeholders(file).get("discount_price_display"));
    }

    @Test
    void afterThatAnAdminWhoSetsTheDefaultsAgainKeepsThem() throws Exception {
        File file = settings();
        assertFalse(SettingsMigrator.ensureSettings(projectDir, "&e%price% jetons", null, false),
                "nothing to write once the legacy formats were taken");
        assertEquals(ShopModels.DEFAULT_PRICE_DISPLAY, placeholders(file).get("price_display"));
    }

    @Test
    void aMissingBlockIsStillWritten() throws Exception {
        new File(projectDir, "data").mkdirs();
        File file = new File(projectDir, "data/shop_settings.yaml");
        Files.writeString(file.toPath(), "whole_shop_discount: 5\n");
        assertTrue(SettingsMigrator.ensureSettings(projectDir, null, null, false));
        assertEquals(ShopModels.DEFAULT_PRICE_DISPLAY, placeholders(file).get("price_display"));
    }
}
