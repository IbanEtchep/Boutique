package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShopSettingsTest {

    @Test
    void bareFileIsRead() {
        var s = ShopSettings.parse("""
                whole_shop_discount: 10
                placeholders:
                  price_display: "%price% jetons"
                  discount_price_display: "&m%old_price% &f%price% jetons (-%discount_percent%%)"
                """);
        assertEquals(10, s.wholeShopDiscount());
        assertEquals("%price% jetons", s.priceDisplay());
    }

    @Test
    void legacyDataV1FormIsStillRead() {
        var s = ShopSettings.parse("schema: data/v1\nshape: config\nvalue:\n  whole_shop_discount: 7\n");
        assertEquals(7, s.wholeShopDiscount());
    }

    /** Non-hermétique : rien de ce qui manque ou casse ne doit lever. */
    @Test
    void missingBrokenOrPartialFallsBackToDefaults() {
        assertEquals(ShopSettings.defaults(), ShopSettings.parse(null));
        assertEquals(ShopSettings.defaults(), ShopSettings.parse("  "));
        assertEquals(ShopSettings.defaults(), ShopSettings.parse("whole_shop_discount: [unclosed"));
        var partial = ShopSettings.parse("whole_shop_discount: 5\n");
        assertEquals(5, partial.wholeShopDiscount());
        assertEquals(ShopModels.DEFAULT_PRICE_DISPLAY, partial.priceDisplay());
    }

    @Test
    void formatsPlainPriceWhenNoDiscountApplies() {
        var s = ShopSettings.defaults();
        assertEquals("100 tokens", s.formatPrice(100, 100));
    }

    @Test
    void formatsPromoPriceWhenTheFinalPriceIsLower() {
        var s = ShopSettings.defaults();
        // -20% : 100 → 80
        assertEquals("&m100 &f80 tokens (-20%)", s.formatPrice(100, 80));
    }

    /** Un prix nul ne doit pas produire de division par zéro. */
    @Test
    void zeroBasePriceIsSafe() {
        assertEquals("0 tokens", ShopSettings.defaults().formatPrice(0, 0));
    }
}
