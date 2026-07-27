package fr.iban.boutique.artisan;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fait entrer les formats de prix — historiquement dans {@code config.yml}, où
 * plus aucun code ne les lisait — dans les réglages éditables du project
 * ({@code data/shop_settings.yaml}, model déclaré {@code boutique:shop_settings}).
 *
 * Idempotent et non destructif : n'écrit que ce qui manque, ne touche jamais une
 * valeur déjà présente, et n'échoue jamais le boot.
 */
public final class SettingsMigrator {
    private SettingsMigrator() {}

    /**
     * @param legacyPriceDisplay valeur de {@code placeholders.price-display} du
     *                           config.yml, ou {@code null} si absente
     * @return true si un fichier a été écrit
     */
    public static boolean ensureSettings(File projectDir, String legacyPriceDisplay, String legacyDiscountDisplay) {
        try {
            return apply(projectDir, legacyPriceDisplay, legacyDiscountDisplay);
        } catch (Exception e) {
            System.err.println("[Boutique] settings migration failed: " + e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean apply(File projectDir, String legacyPrice, String legacyDiscount) throws IOException {
        File settingsFile = new File(projectDir, "data/shop_settings.yaml");
        if (!settingsFile.getParentFile().isDirectory()) return false;

        Map<String, Object> root = new LinkedHashMap<>();
        if (settingsFile.isFile()) {
            Object parsed = plainYaml().load(Files.readString(settingsFile.toPath()));
            // Forme legacy data/v1 : on ne touche pas, l'éditeur la
            // canonicalisera à la première sauvegarde.
            if (parsed instanceof Map<?, ?> m && m.get("value") != null) return ensureBesideRef(projectDir);
            if (parsed instanceof Map<?, ?> m) root.putAll((Map<String, Object>) m);
        }

        boolean changed = false;
        if (!(root.get("whole_shop_discount") instanceof Number)) {
            root.put("whole_shop_discount", 0);
            changed = true;
        }
        if (!(root.get("placeholders") instanceof Map)) {
            Map<String, Object> ph = new LinkedHashMap<>();
            ph.put("price_display", blankTo(legacyPrice, ShopModels.DEFAULT_PRICE_DISPLAY));
            ph.put("discount_price_display", blankTo(legacyDiscount, ShopModels.DEFAULT_DISCOUNT_PRICE_DISPLAY));
            root.put("placeholders", ph);
            changed = true;
        }
        if (changed) Files.writeString(settingsFile.toPath(), plainYaml().dump(root));
        return ensureBesideRef(projectDir) || changed;
    }

    /** `models/<id>.yaml` = ref d'une ligne : le schéma déclaré vit dans le JAR. */
    private static boolean ensureBesideRef(File projectDir) throws IOException {
        File models = new File(projectDir, "models");
        File ref = new File(models, "shop_settings.yaml");
        if (ref.isFile()) return false;
        if (!models.isDirectory() && !models.mkdirs()) return false;
        Files.writeString(ref.toPath(), "model: boutique:shop_settings\n");
        return true;
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Yaml plainYaml() {
        DumperOptions o = new DumperOptions();
        o.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(o);
    }
}
