package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class SettingsMigratorTest {

    private File project(File tmp) throws IOException {
        File dir = new File(tmp, "boutique_shop");
        assertTrue(new File(dir, "data").mkdirs());
        return dir;
    }

    @Test
    void seedsPlaceholdersFromTheLegacyConfig(@TempDir File tmp) throws Exception {
        File dir = project(tmp);
        Files.writeString(new File(dir, "data/shop_settings.yaml").toPath(), "whole_shop_discount: 10\n");

        assertTrue(SettingsMigrator.ensureSettings(dir, "%price% jetons", "&m%old_price% &f%price% jetons"));

        var s = ShopSettings.parse(Files.readString(new File(dir, "data/shop_settings.yaml").toPath()));
        assertEquals(10, s.wholeShopDiscount(), "la valeur existante est conservée");
        assertEquals("%price% jetons", s.priceDisplay());
        // La ref beside rend le formulaire typé par la déclaration du JAR.
        assertEquals("model: boutique:shop_settings\n",
                Files.readString(new File(dir, "models/shop_settings.yaml").toPath()));
    }

    @Test
    void fallsBackToDefaultsWhenTheLegacyConfigHasNothing(@TempDir File tmp) throws Exception {
        File dir = project(tmp);
        Files.writeString(new File(dir, "data/shop_settings.yaml").toPath(), "whole_shop_discount: 0\n");

        SettingsMigrator.ensureSettings(dir, null, "  ");

        var s = ShopSettings.parse(Files.readString(new File(dir, "data/shop_settings.yaml").toPath()));
        assertEquals(ShopModels.DEFAULT_PRICE_DISPLAY, s.priceDisplay());
        assertEquals(ShopModels.DEFAULT_DISCOUNT_PRICE_DISPLAY, s.discountPriceDisplay());
    }

    @Test
    void neverOverwritesValuesAlreadyThere(@TempDir File tmp) throws Exception {
        File dir = project(tmp);
        String existing = """
                whole_shop_discount: 25
                placeholders:
                  price_display: "%price% pièces"
                  discount_price_display: "promo %price%"
                """;
        Files.writeString(new File(dir, "data/shop_settings.yaml").toPath(), existing);

        SettingsMigrator.ensureSettings(dir, "%price% jetons", "autre");
        SettingsMigrator.ensureSettings(dir, "%price% jetons", "autre");

        var s = ShopSettings.parse(Files.readString(new File(dir, "data/shop_settings.yaml").toPath()));
        assertEquals(25, s.wholeShopDiscount());
        assertEquals("%price% pièces", s.priceDisplay());
        assertEquals("promo %price%", s.discountPriceDisplay());
    }

    /** Un projet sans dossier data/ n'est pas un projet boutique — ne rien faire. */
    @Test
    void doesNothingOutsideAProject(@TempDir File tmp) {
        assertFalse(SettingsMigrator.ensureSettings(new File(tmp, "absent"), null, null));
    }
}
