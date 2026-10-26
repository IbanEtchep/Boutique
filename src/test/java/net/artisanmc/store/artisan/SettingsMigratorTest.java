package net.artisanmc.store.artisan;

import org.bukkit.configuration.file.YamlConfiguration;
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
        assertTrue(SettingsMigrator.ensureSettings(projectDir,
                new SettingsMigrator.Legacy("&e%price% jetons", null, Map.of())));
        assertEquals("&e%price% jetons", placeholders(file).get("price_display"));
        assertEquals(ShopModels.DEFAULT_DISCOUNT_PRICE_DISPLAY, placeholders(file).get("discount_price_display"));
    }

    @Test
    void afterThatAnAdminWhoSetsTheDefaultsAgainKeepsThem() throws Exception {
        File file = settings();
        SettingsMigrator.ensureSettings(projectDir, SettingsMigrator.Legacy.NONE);
        assertFalse(SettingsMigrator.ensureSettings(projectDir, SettingsMigrator.Legacy.NONE),
                "nothing to write once the legacy formats were taken");
        assertEquals(ShopModels.DEFAULT_PRICE_DISPLAY, placeholders(file).get("price_display"));
    }

    @Test
    void aMissingBlockIsStillWritten() throws Exception {
        new File(projectDir, "data").mkdirs();
        File file = new File(projectDir, "data/shop_settings.yaml");
        Files.writeString(file.toPath(), "whole_shop_discount: 5\n");
        assertTrue(SettingsMigrator.ensureSettings(projectDir, SettingsMigrator.Legacy.NONE));
        assertEquals(ShopModels.DEFAULT_PRICE_DISPLAY, placeholders(file).get("price_display"));
        assertEquals(ShopModels.DEFAULT_BUY_SUCCESS, block(file, "messages").get("buy_success"));
        assertEquals(5, ShopSettings.parse(Files.readString(file.toPath())).wholeShopDiscount());
    }

    @Test
    void theMessagesOfConfigYmlReplaceTheUntouchedDefaults() throws Exception {
        File file = settings();
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                messages:
                  currency-name: jetons
                  buy-success: "&aMerci !"
                  buy-link: "https://lien.fr/"
                placeholders:
                  price-display: "%price% jetons"
                """);
        assertTrue(SettingsMigrator.ensureSettings(projectDir, SettingsMigrator.legacy(config, false, true)));
        Map<String, Object> messages = block(file, "messages");
        assertEquals("jetons", messages.get("currency_name"));
        assertEquals("&aMerci !", messages.get("buy_success"));
        assertEquals(ShopModels.DEFAULT_NOT_ENOUGH_MONEY, messages.get("not_enough_money"));
        assertFalse(messages.containsKey("buy_link"), "buy-link was read by nothing");
        assertEquals(ShopModels.DEFAULT_PRICE_DISPLAY, placeholders(file).get("price_display"),
                "the formats were taken by their own migration");
        assertEquals("jetons", ShopSettings.parse(Files.readString(file.toPath())).messages().currencyName());
        assertTrue(SettingsMigrator.holdsContent(projectDir));
    }

    @Test
    void messagesTheAdminChoseAreNeverReplaced() throws Exception {
        File file = settings();
        Files.writeString(file.toPath(), Files.readString(file.toPath())
                + "messages:\n  currency_name: crédits\n");
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("messages:\n  currency-name: jetons\n");
        assertFalse(SettingsMigrator.ensureSettings(projectDir, SettingsMigrator.legacy(config, false, true)));
        assertEquals(Map.of("currency_name", "crédits"), block(file, "messages"));
        // The parse fills what the block lacks.
        assertEquals(ShopModels.DEFAULT_BUY_SUCCESS, ShopSettings.parse(Files.readString(file.toPath())).messages().buySuccess());
    }

    @Test
    void configYmlKeepsTheDatabaseOnly() throws Exception {
        File config = new File(projectDir, "config.yml");
        String before = """
                #Azuriom database
                database:
                  host: db
                  port: 3306
                whole-shop-discount: 0
                messages:
                  currency-name: jetons
                placeholders:
                  price-display: "%price% jetons"
                """;
        Files.writeString(config.toPath(), before);
        assertTrue(SettingsMigrator.slimConfig(config));
        YamlConfiguration slim = YamlConfiguration.loadConfiguration(config);
        assertEquals(java.util.Set.of("database"), slim.getKeys(false));
        assertEquals(3306, slim.getInt("database.port"));
        assertTrue(Files.readString(config.toPath()).contains("Azuriom database"), "the kept keys keep their comments");
        assertEquals(before, Files.readString(new File(projectDir, "config.yml.before-settings").toPath()));
        assertFalse(SettingsMigrator.slimConfig(config), "nothing left to remove");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> block(File file, String key) throws Exception {
        Map<String, Object> root = new Yaml().load(Files.readString(file.toPath()));
        return (Map<String, Object>) root.get(key);
    }
}
