package fr.iban.boutique.artisan;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fait entrer les réglages de contenu de {@code config.yml} — formats de prix et messages — dans les réglages éditables du dossier du module ({@code data/shop_settings.yaml},
 * model déclaré {@code boutique:shop_settings}). Le config.yml ne garde que ce qui est propre
 * au serveur : la base de données.
 *
 * Idempotent et non destructif : n'écrit que ce qui manque ou ce qui est resté aux défauts du
 * bootstrap, ne touche jamais une valeur choisie, et n'échoue jamais le boot.
 */
public final class SettingsMigrator {
    private SettingsMigrator() {}

    /** Les clés {@code messages.*} du config.yml, sous leur nom dans {@code data/shop_settings}. */
    static final Map<String, String> MESSAGE_KEYS = messageKeys();

    /**
     * Ce que le config.yml portait : chaque valeur {@code null} (et {@code messages} vide) quand
     * il ne l'a pas, ou quand sa migration a déjà été faite.
     */
    public record Legacy(String priceDisplay, String discountPriceDisplay, Map<String, String> messages) {
        public static final Legacy NONE = new Legacy(null, null, Map.of());
    }

    /**
     * Les valeurs d'un config.yml. Le caller passe ensuite {@code null} pour un bloc déjà migré :
     * un admin qui remet exactement les défauts les garde.
     */
    public static Legacy legacy(ConfigurationSection config,
                                boolean formats, boolean messages) {
        Map<String, String> texts = new LinkedHashMap<>();
        if (messages) {
            for (Map.Entry<String, String> e : MESSAGE_KEYS.entrySet()) {
                String value = config.getString("messages." + e.getKey());
                if (value != null) texts.put(e.getValue(), value);
            }
        }
        return new Legacy(
                formats ? config.getString("placeholders.price-display") : null,
                formats ? config.getString("placeholders.discount-price-display") : null,
                texts);
    }

    /** @return true si un fichier a été écrit */
    public static boolean ensureSettings(File projectDir, Legacy legacy) {
        try {
            return apply(projectDir, legacy);
        } catch (Exception e) {
            System.err.println("[Boutique] settings migration failed: " + e.getMessage());
            return false;
        }
    }

    /** True quand {@code data/shop_settings} porte les trois blocs : le config.yml peut s'en défaire. */
    @SuppressWarnings("unchecked")
    public static boolean holdsContent(File projectDir) {
        File file = YamlFiles.find(new File(projectDir, "data"), "shop_settings");
        if (file == null) return false;
        try {
            Object parsed = plainYaml().load(Files.readString(file.toPath()));
            if (!(parsed instanceof Map<?, ?> m)) return false;
            Map<?, ?> root = m.get("value") instanceof Map<?, ?> v ? v : m;
            return root.get("whole_shop_discount") instanceof Number
                    && root.get("placeholders") instanceof Map
                    && root.get("messages") instanceof Map;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /** Les clés de contenu que config.yml portait avant {@code data/shop_settings}. */
    static final List<String> CONTENT_KEYS = List.of("whole-shop-discount", "messages", "placeholders");

    /**
     * Retire de {@code config} les {@link #CONTENT_KEYS}, à appeler une fois
     * {@link #holdsContent} vrai. La version d'avant reste dans {@code config.yml.before-settings}
     * (jamais écrasée) ; les commentaires des clés gardées restent. Retourne true quand le fichier
     * a changé.
     */
    public static boolean slimConfig(File config) throws IOException {
        if (!config.isFile()) return false;
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(config);
        } catch (InvalidConfigurationException e) {
            throw new IOException(config.getName() + " cannot be read: " + e.getMessage(), e);
        }
        boolean held = false;
        for (String key : CONTENT_KEYS) {
            if (!yaml.contains(key, true)) continue;
            yaml.set(key, null);
            held = true;
        }
        if (!held) return false;
        File backup = new File(config.getParentFile(), config.getName() + ".before-settings");
        if (!backup.exists()) Files.copy(config.toPath(), backup.toPath());
        yaml.save(config);
        return true;
    }

    @SuppressWarnings("unchecked")
    private static boolean apply(File projectDir, Legacy legacy) throws IOException {
        File dataDir = new File(projectDir, "data");
        if (!dataDir.isDirectory()) return false;
        File settingsFile = YamlFiles.target(dataDir, "shop_settings");

        Map<String, Object> root = new LinkedHashMap<>();
        if (settingsFile.isFile()) {
            Object parsed = plainYaml().load(Files.readString(settingsFile.toPath()));
            // Forme legacy data/v1 : on ne touche pas, l'éditeur la
            // canonicalisera à la première sauvegarde.
            if (parsed instanceof Map<?, ?> m && m.get("value") != null) return ensureBesideRef(projectDir);
            if (parsed instanceof Map<?, ?> m) root.putAll((Map<String, Object>) m);
        }

        boolean changed = false;
        // La remise globale du config.yml a déjà fait le chemin du catalogue (LegacyConfigMigrator
        // puis ShopDataMigrator) : la reprendre ici écraserait celle choisie depuis.
        if (!(root.get("whole_shop_discount") instanceof Number)) {
            root.put("whole_shop_discount", 0);
            changed = true;
        }
        // Le bootstrap pose déjà les blocs aux valeurs du modèle : tant qu'elles sont les
        // défauts intacts, les valeurs du config.yml legacy priment (sinon elles seraient
        // perdues sur une install fraîche). Une valeur choisie par l'admin, elle, n'est jamais
        // écrasée.
        Map<String, Object> ph = new LinkedHashMap<>();
        ph.put("price_display", blankTo(legacy.priceDisplay(), ShopModels.DEFAULT_PRICE_DISPLAY));
        ph.put("discount_price_display", blankTo(legacy.discountPriceDisplay(), ShopModels.DEFAULT_DISCOUNT_PRICE_DISPLAY));
        changed |= replaceDefaults(root, "placeholders", ph, defaultPlaceholders());

        Map<String, Object> messages = new LinkedHashMap<>();
        defaultMessages().forEach((key, value) -> messages.put(key, blankTo(legacy.messages().get(key), (String) value)));
        changed |= replaceDefaults(root, "messages", messages, defaultMessages());

        if (changed) Files.writeString(settingsFile.toPath(), plainYaml().dump(root));
        return ensureBesideRef(projectDir) || changed;
    }

    /** Pose {@code block} sous {@code key} quand il manque ou n'a que les défauts intacts. */
    private static boolean replaceDefaults(Map<String, Object> root, String key, Map<String, Object> block,
                                           Map<String, Object> defaults) {
        Object current = root.get(key);
        if (current instanceof Map && !defaults.equals(current)) return false;
        if (block.equals(current)) return false;
        root.put(key, block);
        return true;
    }

    private static Map<String, Object> defaultPlaceholders() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("price_display", ShopModels.DEFAULT_PRICE_DISPLAY);
        m.put("discount_price_display", ShopModels.DEFAULT_DISCOUNT_PRICE_DISPLAY);
        return m;
    }

    private static Map<String, Object> defaultMessages() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("currency_name", ShopModels.DEFAULT_CURRENCY_NAME);
        m.put("buy_success", ShopModels.DEFAULT_BUY_SUCCESS);
        m.put("not_enough_money", ShopModels.DEFAULT_NOT_ENOUGH_MONEY);
        m.put("no_buy_commands", ShopModels.DEFAULT_NO_BUY_COMMANDS);
        return m;
    }

    private static Map<String, String> messageKeys() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("currency-name", "currency_name");
        m.put("buy-success", "buy_success");
        m.put("not-enough-money", "not_enough_money");
        m.put("no-buy-commands", "no_buy_commands");
        return java.util.Collections.unmodifiableMap(m);
    }

    /** `models/<id>.yaml` = ref d'une ligne : le schéma déclaré vit dans le JAR. */
    private static boolean ensureBesideRef(File projectDir) throws IOException {
        File models = new File(projectDir, "models");
        if (YamlFiles.find(models, "shop_settings") != null) return false;
        if (!models.isDirectory() && !models.mkdirs()) return false;
        Files.writeString(YamlFiles.target(models, "shop_settings").toPath(), "model: boutique:shop_settings\n");
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
