package fr.iban.boutique.artisan;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Écrit un article capturé (`/boutiqueadmin additem`) dans le catalogue, **sur
 * le disque du module**.
 *
 * Cette mutation partait autrefois au backend, qui patchait sa copie du project
 * puis rebroadcastait. Le backend ne stocke plus aucun contenu (ADR Artisan
 * `server-disk-only-storage`) : le module possède sa racine
 * (`plugins/Boutique/editor/`, enregistrée comme `boutique_shop`), il y écrit
 * lui-même, et le scan disque d'Artisan recharge à chaud — exactement comme une
 * édition faite à la main ou par un agent.
 *
 * Forme écrite : la **Table déclarée** `data/categories/` en fichiers nus (ADR
 * `bare-data-files`) — `name` porte le littéral, aucune clé n'est mintée et
 * `lang/` n'est pas touché. Un catalogue legacy `boutique/shop.yaml` est déjà
 * converti au boot par {@link ShopDataMigrator}, donc il n'a pas à être géré
 * ici : sans Table, on refuse plutôt que d'inventer un format.
 */
public final class CapturedItemWriter {

    /** Résultat d'une capture — `error` non nul dit pourquoi rien n'a été écrit. */
    public record Result(boolean ok, String capturedItemId, String error) {
        static Result failed(String why) { return new Result(false, null, why); }
        static Result written(String ciId) { return new Result(true, ciId, null); }
    }

    private CapturedItemWriter() {}

    public static Result addItem(
            File projectDir,
            String itemId,
            int price,
            String category,
            String stackYaml,
            Map<String, Object> display) {
        File catDir = new File(projectDir, "data/categories");
        File metaFile = new File(catDir, "_source.yaml");
        if (!metaFile.isFile()) {
            return Result.failed("aucun catalogue (data/categories/_source.yaml) dans " + projectDir.getName());
        }
        try {
            Yaml yaml = plainYaml();
            Map<String, Object> meta = load(yaml, metaFile);
            List<String> order = new ArrayList<>();
            Object rawOrder = meta.get("order");
            if (rawOrder instanceof List<?> l) for (Object o : l) order.add(String.valueOf(o));

            String ciId = writeCapturedItem(projectDir, yaml, stackYaml, display, itemId);

            // La ligne cible : par id, puis par nom, sinon la première du
            // catalogue quand l'admin n'a pas précisé de catégorie.
            String wanted = category == null ? null : category.trim().toLowerCase(Locale.ROOT);
            String rowKey = null;
            Map<String, Object> row = null;
            for (String key : order) {
                Map<String, Object> parsed = load(yaml, new File(catDir, key + ".yaml"));
                if (parsed.isEmpty()) continue;
                String id = String.valueOf(parsed.getOrDefault("id", key)).toLowerCase(Locale.ROOT);
                String name = String.valueOf(parsed.getOrDefault("name", "")).toLowerCase(Locale.ROOT);
                if (wanted == null || id.equals(wanted) || name.equals(wanted)) {
                    rowKey = key;
                    row = parsed;
                    break;
                }
            }
            if (row == null) {
                // Catégorie inconnue : on la crée. Le nom est le littéral tel que
                // tapé — pas de clé de traduction (fichiers data nus).
                rowKey = slug(wanted == null ? "divers" : wanted);
                row = new LinkedHashMap<>();
                row.put("id", rowKey);
                row.put("name", category == null || category.isBlank() ? "Divers" : category.trim());
                row.put("icon", "CHEST");
                row.put("discount", 0);
                row.put("items", new ArrayList<Map<String, Object>>());
                if (!order.contains(rowKey)) {
                    order.add(rowKey);
                    meta.put("order", order);
                    Files.writeString(metaFile.toPath(), yaml.dump(meta));
                }
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = row.get("items") instanceof List<?>
                    ? new ArrayList<>((List<Map<String, Object>>) row.get("items"))
                    : new ArrayList<>();
            String displayName = display.get("name") instanceof String s && !s.isBlank() ? s : itemId;
            Map<String, Object> existing = null;
            for (Map<String, Object> i : items) {
                if (itemId.equals(String.valueOf(i.get("id")))) { existing = i; break; }
            }
            if (existing != null) {
                // Re-capturer un id existant le met à jour : l'admin corrige un
                // prix ou remplace le stack, il ne crée pas un doublon.
                existing.put("name", displayName);
                existing.put("icon", "item:" + ciId);
                existing.put("price", price);
            } else {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", itemId);
                entry.put("name", displayName);
                entry.put("icon", "item:" + ciId);
                entry.put("price", price);
                entry.put("discount", 0);
                entry.put("lore", new ArrayList<String>());
                entry.put("actions", "");
                items.add(entry);
            }
            row.put("items", items);
            Files.writeString(new File(catDir, rowKey + ".yaml").toPath(), yaml.dump(row));
            return Result.written(ciId);
        } catch (IOException | RuntimeException e) {
            return Result.failed(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    /**
     * Le stack sérialisé va dans `items/<ciId>.yml` **verbatim** (c'est lui qui
     * porte NBT / enchants / CMD), et son descripteur d'affichage dans
     * `.artisan/editor.yaml` — état éditeur, donc hors hash et hors runtime.
     */
    private static String writeCapturedItem(
            File projectDir, Yaml yaml, String stackYaml,
            Map<String, Object> display, String itemId) throws IOException {
        String ciId = "ci_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        File itemFile = new File(projectDir, "items/" + ciId + ".yml");
        Files.createDirectories(itemFile.getParentFile().toPath());
        Files.writeString(itemFile.toPath(), stackYaml);

        File stateFile = new File(projectDir, ".artisan/editor.yaml");
        Map<String, Object> state = load(yaml, stateFile);
        if (!(state.get("schema") instanceof String)) state.put("schema", "editor-state/v1");
        @SuppressWarnings("unchecked")
        Map<String, Object> itemsMeta = state.get("items") instanceof Map<?, ?> m
                ? new LinkedHashMap<>((Map<String, Object>) m)
                : new LinkedHashMap<>();
        Map<String, Object> descriptor = new LinkedHashMap<>();
        copyIfPresent(display, descriptor, "material", "material");
        copyIfPresent(display, descriptor, "head_texture", "headTexture");
        copyIfPresent(display, descriptor, "enchanted", "enchanted");
        copyIfPresent(display, descriptor, "count", "count");
        copyIfPresent(display, descriptor, "name", "name");
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", display.get("name") instanceof String s && !s.isBlank() ? s : itemId);
        entry.put("display", descriptor);
        itemsMeta.put(ciId, entry);
        state.put("items", itemsMeta);
        Files.createDirectories(stateFile.getParentFile().toPath());
        Files.writeString(stateFile.toPath(), yaml.dump(state));
        return ciId;
    }

    private static void copyIfPresent(Map<String, Object> from, Map<String, Object> to, String key, String as) {
        Object v = from.get(key);
        if (v != null && !(v instanceof String s && s.isBlank())) to.put(as, v);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> load(Yaml yaml, File f) {
        if (!f.isFile()) return new LinkedHashMap<>();
        try {
            Object parsed = yaml.load(Files.readString(f.toPath()));
            return parsed instanceof Map<?, ?> m ? new LinkedHashMap<>((Map<String, Object>) m) : new LinkedHashMap<>();
        } catch (IOException | RuntimeException e) {
            return new LinkedHashMap<>();
        }
    }

    private static String slug(String raw) {
        String s = raw.replaceAll("[^a-z0-9_-]+", "_");
        return s.isBlank() ? "divers" : s;
    }

    private static Yaml plainYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(options);
    }
}
