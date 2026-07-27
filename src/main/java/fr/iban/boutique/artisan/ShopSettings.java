package fr.iban.boutique.artisan;

import java.util.Map;

/**
 * Les réglages de la boutique, lus depuis {@code data/shop_settings.yaml}
 * (model déclaré {@code boutique:shop_settings}). Tolérant par construction :
 * un fichier absent, nu, legacy {@code data/v1} ou incomplet donne toujours des
 * réglages utilisables — jamais d'erreur (philosophie non-hermétique).
 */
public record ShopSettings(int wholeShopDiscount, String priceDisplay, String discountPriceDisplay) {

    public static ShopSettings defaults() {
        return new ShopSettings(0, ShopModels.DEFAULT_PRICE_DISPLAY, ShopModels.DEFAULT_DISCOUNT_PRICE_DISPLAY);
    }

    /** Parse le contenu YAML du fichier de réglages ; {@code null} ⇒ défauts. */
    public static ShopSettings parse(String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) return defaults();
        Object parsed;
        try {
            parsed = new org.yaml.snakeyaml.Yaml().load(yamlContent);
        } catch (RuntimeException e) {
            return defaults();
        }
        if (!(parsed instanceof Map<?, ?> m)) return defaults();
        // Forme legacy data/v1 : les valeurs vivent sous `value:`.
        Map<?, ?> root = m.get("value") instanceof Map<?, ?> v ? v : m;

        int discount = root.get("whole_shop_discount") instanceof Number n ? n.intValue() : 0;
        String price = ShopModels.DEFAULT_PRICE_DISPLAY;
        String promo = ShopModels.DEFAULT_DISCOUNT_PRICE_DISPLAY;
        if (root.get("placeholders") instanceof Map<?, ?> p) {
            if (p.get("price_display") instanceof String s && !s.isBlank()) price = s;
            if (p.get("discount_price_display") instanceof String s && !s.isBlank()) promo = s;
        }
        return new ShopSettings(discount, price, promo);
    }

    /**
     * Prix mis en forme pour l'affichage : le format « promotion » s'applique
     * dès qu'une remise (article ou boutique entière) s'applique réellement.
     */
    public String formatPrice(double basePrice, int finalPrice) {
        int base = (int) Math.round(basePrice);
        String template = finalPrice < base ? discountPriceDisplay : priceDisplay;
        int percent = base <= 0 ? 0 : (int) Math.round((base - finalPrice) * 100.0 / base);
        return template
                .replace("%old_price%", String.valueOf(base))
                .replace("%discount_percent%", String.valueOf(percent))
                .replace("%price%", String.valueOf(finalPrice));
    }
}
