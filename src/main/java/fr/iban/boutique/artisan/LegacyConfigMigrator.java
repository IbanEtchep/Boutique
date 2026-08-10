package fr.iban.boutique.artisan;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/** One-shot : ancien config.yml MenuAPI → boutique/v1. Lecture en maps brutes (jamais YamlConfiguration : blocs `==: menuitem`). */
public final class LegacyConfigMigrator {
    private LegacyConfigMigrator() {}

    /**
     * Le texte à donner à {@link #migrate} : {@code config.yml.legacy} s'il
     * existe, sinon {@code config.yml}, sinon null.
     *
     * L'assainissement ({@link #stripCategoriesBlock}) est destructif et tourne
     * AVANT la migration — il le doit, Bukkit ne sait plus désérialiser les
     * blocs `==: menuitem` et le moindre getConfig() planterait. Un plugin qui
     * meurt entre les deux (LinkageError du 2026-08-08) laisse donc un
     * config.yml sans catalogue, et la migration du boot suivant ne trouve plus
     * rien : le serveur se réveille avec le catalogue d'exemple à la place de
     * ses 7 catégories. La copie d'avant assainissement est la source de vérité
     * tant qu'elle est là.
     */
    public static String readLegacySource(File dataFolder) {
        for (String name : new String[]{"config.yml.legacy", "config.yml"}) {
            File f = new File(dataFolder, name);
            if (!f.isFile()) continue;
            try {
                String content = Files.readString(f.toPath());
                if (content.contains("categories:")) return content;
            } catch (IOException e) {
                // Fichier illisible : on tente le suivant plutôt que d'échouer.
            }
        }
        return null;
    }

    /**
     * Retire le bloc top-level `categories:` d'un config.yml legacy. Nécessaire AVANT le premier
     * {@code getConfig()} Bukkit : les blocs sérialisés `==: menuitem` ne se désérialisent plus
     * (MenuAPI absent) et font échouer YamlConfiguration — le catalogue est migré vers Artisan,
     * le reste du config (database, messages…) doit rester lisible.
     */
    public static String stripCategoriesBlock(String yaml) {
        StringBuilder out = new StringBuilder();
        boolean skipping = false;
        for (String line : yaml.split("\n", -1)) {
            if (line.startsWith("categories:")) { skipping = true; continue; }
            if (skipping) {
                boolean topLevel = !line.isEmpty() && !Character.isWhitespace(line.charAt(0)) && !line.startsWith("#");
                if (!topLevel) continue;
                skipping = false;
            }
            out.append(line).append('\n');
        }
        // split(-1) + append('\n') ajoute un \n final de trop si l'entrée n'en avait pas — sans conséquence YAML.
        return out.toString();
    }

    /**
     * @return relPath → contenu : {@code boutique/shop.yaml} + un
     * {@code items/ci_<id>.yml} par stack sérialisé capturé (le stack legacy
     * est la vérité runtime — émis verbatim sous la clé racine {@code item:},
     * avec un bloc {@code display:} portant les flags legacy comme
     * {@code enchanted} que le stack seul ne dit pas). Les icônes deviennent
     * des refs universelles {@code item:ci_<id>}.
     */
    @SuppressWarnings("unchecked")
    public static Optional<Map<String, String>> migrate(String legacyYaml) {
        Object parsed;
        try { parsed = new Yaml().load(legacyYaml); } catch (RuntimeException e) { return Optional.empty(); }
        if (!(parsed instanceof Map)) return Optional.empty();
        Map<String, Object> root = (Map<String, Object>) parsed;
        if (!(root.get("categories") instanceof Map)) return Optional.empty();

        Map<String, String> files = new LinkedHashMap<>();
        List<Map<String, Object>> categories = new ArrayList<>();
        Set<String> usedCatIds = new HashSet<>();
        Set<String> usedItemIds = new HashSet<>();
        Map<String, Object> rawCats = (Map<String, Object>) root.get("categories");
        for (Map.Entry<String, Object> catEntry : rawCats.entrySet()) {
            if (!(catEntry.getValue() instanceof Map)) continue;
            Map<String, Object> c = (Map<String, Object>) catEntry.getValue();
            Map<String, Object> catDisplay = display(c.get("menuitem"));
            String catId = unique(slug((String) catDisplay.get("name"), "cat_" + catEntry.getKey()), usedCatIds);
            List<Map<String, Object>> items = new ArrayList<>();
            Object rawItems = c.get("items");
            if (rawItems instanceof Map) {
                for (Map.Entry<String, Object> itemEntry : ((Map<String, Object>) rawItems).entrySet()) {
                    if (!(itemEntry.getValue() instanceof Map)) continue;
                    Map<String, Object> i = (Map<String, Object>) itemEntry.getValue();
                    // Le vrai runtime pré-refactor lit/écrit toujours "menuitem" pour l'item (cf. ShopManager
                    // c72f586 lignes 45/104) ; "display" reste un fallback pour un config.yml recopié à la
                    // main depuis le template ressource (qui, lui, utilisait "display").
                    Object rawItemDisplay = i.containsKey("menuitem") ? i.get("menuitem") : i.get("display");
                    Map<String, Object> itemDisplay = display(rawItemDisplay);
                    String itemId = unique(slug((String) itemDisplay.get("name"), "item_" + itemEntry.getKey()), usedItemIds);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", itemId);
                    item.put("name", stripAmp((String) itemDisplay.getOrDefault("name", "item " + itemEntry.getKey())));
                    item.put("icon", captureStack(rawItemDisplay, "ci_" + itemId,
                            (String) itemDisplay.getOrDefault("icon", "BARRIER"), files));
                    item.put("price", i.getOrDefault("price", 0));
                    item.put("discount", i.getOrDefault("discount", 0));
                    item.put("lore", dropPricePlaceholders(itemDisplay.get("lore")));
                    item.put("actions", migrateCommands(i.getOrDefault("buycommands", List.of())));
                    putPlacement(item, rawItemDisplay);
                    items.add(item);
                }
            }
            Map<String, Object> cat = new LinkedHashMap<>();
            cat.put("id", catId);
            cat.put("name", stripAmp((String) catDisplay.getOrDefault("name", "Catégorie " + catEntry.getKey())));
            cat.put("icon", captureStack(c.get("menuitem"), "ci_" + catId,
                    (String) catDisplay.getOrDefault("icon", "CHEST"), files));
            cat.put("discount", c.getOrDefault("discount", 0));
            putPlacement(cat, c.get("menuitem"));
            cat.put("items", items);
            categories.add(cat);
        }
        if (categories.isEmpty()) return Optional.empty();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("schema", "boutique/v1");
        out.put("whole_shop_discount", root.getOrDefault("whole-shop-discount", 0));
        out.put("categories", categories);
        files.put("boutique/shop.yaml", blockYaml().dump(out));
        return Optional.of(files);
    }

    /**
     * Capture le stack sérialisé d'un bloc menuitem legacy dans
     * {@code items/ci_<id>.yml} (racine canonique {@code item:} + bloc
     * {@code display:} avec le nom nettoyé et le flag {@code enchanted}
     * legacy) et renvoie la ref {@code item:ci_<id>}. Sans stack sérialisé,
     * renvoie {@code fallbackIcon} (comportement historique).
     */
    @SuppressWarnings("unchecked")
    private static String captureStack(Object rawMenuItem, String ciId, String fallbackIcon,
                                       Map<String, String> files) {
        if (!(rawMenuItem instanceof Map)) return fallbackIcon;
        Map<String, Object> m = (Map<String, Object>) rawMenuItem;
        if (!(m.get("item") instanceof Map)) return fallbackIcon;
        Map<String, Object> stack = (Map<String, Object>) m.get("item");
        // Ancien format (item: {type: DIAMOND_SWORD}) : pas un vrai stack
        // sérialisé — garder le material simple, pas de capture.
        if (!stack.containsKey("==")) return fallbackIcon;

        Map<String, Object> displayBlock = new LinkedHashMap<>();
        if (m.get("name") != null) displayBlock.put("name", stripAmp(m.get("name").toString()));
        if (Boolean.TRUE.equals(m.get("enchanted"))) displayBlock.put("enchanted", true);

        Map<String, Object> file = new LinkedHashMap<>();
        file.put("item", stack);
        if (!displayBlock.isEmpty()) file.put("display", displayBlock);
        files.put("items/" + ciId + ".yml", blockYaml().dump(file));
        return "item:" + ciId;
    }

    private static Yaml blockYaml() {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(opts);
    }

    /** name/lore/icon depuis un bloc menuitem sérialisé ({==: menuitem, name, lore, item: {type}}). */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> display(Object rawMenuItem) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (rawMenuItem instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) rawMenuItem;
            if (m.get("name") != null) out.put("name", m.get("name").toString());
            if (m.get("lore") instanceof List) {
                List<String> lore = new ArrayList<>();
                for (Object l : (List<Object>) m.get("lore")) lore.add(stripAmp(String.valueOf(l)));
                out.put("lore", lore);
            }
            if (m.get("item") instanceof Map && ((Map<String, Object>) m.get("item")).get("type") != null) {
                out.put("icon", ((Map<String, Object>) m.get("item")).get("type").toString());
            }
        }
        return out;
    }

    /**
     * Le lore sans ses lignes de placeholder de prix. `%price_display%` (et sa
     * variante promo) était résolu par l'ancien moteur de rendu ; côté Artisan
     * le prix est affiché par le menu, et la ligne resterait littérale dans
     * l'infobulle.
     */
    @SuppressWarnings("unchecked")
    private static List<String> dropPricePlaceholders(Object rawLore) {
        if (!(rawLore instanceof List)) return List.of();
        List<String> out = new ArrayList<>();
        for (Object line : (List<Object>) rawLore) {
            String s = String.valueOf(line);
            if (s.trim().matches("(?i)%(price|promo_price|discount_price)_display%")) continue;
            out.add(s);
        }
        return List.copyOf(out);
    }

    /** buycommands legacy → chaîne Action DSL `actions` : une ligne
     *  `run_command "…" as console` par commande, `%player%` → `{player}`
     *  (placeholder du moteur de steps Artisan), jointes par des retours ligne. */
    private static String migrateCommands(Object raw) {
        StringBuilder sb = new StringBuilder();
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o == null) continue;
                if (sb.length() > 0) sb.append('\n');
                sb.append("run_command \"")
                  .append(o.toString().replace("%player%", "{player}"))
                  .append("\" as console");
            }
        }
        return sb.toString();
    }

    /**
     * Reporte `page`/`slot` du bloc menuitem legacy. Ces deux champs décrivent
     * la MISE EN PAGE, pas l'article : ShopDataMigrator s'en sert pour générer
     * des menus en mode placement, puis les jette — ils n'ont rien à faire dans
     * la Table, où ils ne sont pas déclarés par le model.
     */
    @SuppressWarnings("unchecked")
    private static void putPlacement(Map<String, Object> target, Object rawMenuItem) {
        if (!(rawMenuItem instanceof Map)) return;
        Map<String, Object> m = (Map<String, Object>) rawMenuItem;
        if (m.get("slot") instanceof Number slot) {
            target.put("slot", slot.intValue());
            target.put("page", m.get("page") instanceof Number p ? p.intValue() : 0);
        }
    }

    /** Slugs identiques (deux entrées de même nom) → suffixe _2, _3… pour garder des ids uniques. */
    private static String unique(String id, Set<String> used) {
        String candidate = id;
        for (int n = 2; !used.add(candidate); n++) candidate = id + "_" + n;
        return candidate;
    }

    /**
     * Retire les codes couleur d'un texte legacy. Le serveur en mélange DEUX
     * syntaxes, et les deux doivent sauter :
     *
     *   `&x&D&2&A&6&A&6&l` — legacy Bukkit, écrit en MAJUSCULES (d'où le (?i) :
     *                        ne traiter que les minuscules laissait « &D&A&A »)
     *   `#3399FF` / `&#3399FF` — hex moderne, qui sinon collait au nom
     *                        (« #3399FFSpawner à poule ») et faisait tomber
     *                        l'id en repli, un slug ne pouvant pas commencer
     *                        par le chiffre du code couleur.
     */
    static String stripColours(String s) {
        return s == null ? "" : s.replaceAll("(?i)(&?#[0-9a-f]{6}|[&§][0-9a-fk-orx])", "").trim();
    }

    private static String stripAmp(String s) {
        return stripColours(s);
    }

    private static String slug(String name, String fallback) {
        if (name == null) return fallback;
        String s = stripAmp(name).toLowerCase(Locale.ROOT)
                .replaceAll("[àâä]", "a").replaceAll("[éèêë]", "e").replaceAll("[îï]", "i")
                .replaceAll("[ôö]", "o").replaceAll("[ùûü]", "u").replaceAll("ç", "c")
                .replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return s.isEmpty() || !s.matches("^[a-z].*") ? fallback : s;
    }
}
