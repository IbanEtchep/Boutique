package fr.iban.boutique.artisan;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Moves the catalogue from the {@code data/categories/} Table (one file per category, edited by
 * the former « Catalogue » screen) to {@code boutique_category} Documents
 * ({@code boutique_categories/<id>.yaml}, or {@code .yml}), before Artisan reads the folder.
 *
 * <p>Each category keeps its id, name, icon, discount, articles and any key the shop does not
 * know; its rank in {@code _source.yaml}'s {@code order} becomes {@code order}. A text takes its
 * languages along (ADR field-local-translations): the default language's value as the shop
 * resolved it, and those other languages kept in {@code lang/} at the Table's addresses
 * ({@code sources.categories.<id>.name}…), as {@code {fr: …, en: …}}. {@code lang/} itself is
 * not touched.
 *
 * <p>Idempotent and safe to interrupt: every original goes to {@code backupRoot/
 * category-kind-backup/} before anything moves, a category whose Document already exists is
 * left where it is (never overwritten), and the Table's files are removed only once their
 * Document is written.
 */
public final class CategoryKindMigrator {
    public static final String DIR = "boutique_categories";
    public static final String SCHEMA = "boutique_category/v1";
    static final String TABLE = "data/categories";
    static final String BACKUP = "category-kind-backup";

    private CategoryKindMigrator() {}

    /**
     * Category files the shop does not read, among the folder's paths ({@code list(pid, "")}):
     * a {@code boutique_categories/} under a themed folder ({@code catalogue/boutique_categories/
     * kits.yml}) or a sub-folder of it. Only {@code boutique_categories/<id>.yml} at the root is
     * the catalogue; the editor moves the others back there.
     */
    public static List<String> misplaced(List<String> paths) {
        List<String> out = new ArrayList<>();
        for (String rel : paths) {
            if (!YamlFiles.isYaml(rel)) continue;
            boolean underThemed = rel.contains("/" + DIR + "/");
            boolean nested = rel.startsWith(DIR + "/") && rel.indexOf('/', DIR.length() + 1) >= 0;
            if (underThemed || nested) out.add(rel);
        }
        return out;
    }

    /** What a run did: categories moved, and those left in the Table (their Document exists). */
    public record Result(List<String> moved, List<String> skipped) {
        public boolean changed() { return !moved.isEmpty(); }
    }

    @SuppressWarnings("unchecked")
    public static Result migrate(Path project, Path backupRoot) throws IOException {
        Path table = project.resolve(TABLE);
        if (!Files.isDirectory(table)) return new Result(List.of(), List.of());
        Yaml yaml = yaml();

        List<Path> rows;
        try (Stream<Path> files = Files.list(table)) {
            rows = files.filter(p -> isYaml(p) && !p.getFileName().toString().startsWith("_")).sorted().toList();
        }
        File sourceFile = YamlFiles.find(table.toFile(), "_source");
        Path source = sourceFile != null ? sourceFile.toPath() : table.resolve("_source.yaml");
        List<String> order = new ArrayList<>();
        if (Files.isRegularFile(source)) {
            Object parsed = yaml.load(Files.readString(source));
            if (parsed instanceof Map<?, ?> m && m.get("order") instanceof List<?> ids) {
                for (Object id : ids) order.add(String.valueOf(id));
            }
        }
        Texts texts = Texts.read(project, yaml);

        Path documents = project.resolve(DIR);
        Path backups = backupRoot.resolve(BACKUP);
        List<String> moved = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (Path row : rows) {
            Object parsed = yaml.load(Files.readString(row));
            if (!(parsed instanceof Map<?, ?> raw)) continue;
            Map<String, Object> category = (Map<String, Object>) raw;
            String fileId = row.getFileName().toString().replaceFirst("\\.ya?ml$", "");
            String id = category.get("id") == null ? fileId : String.valueOf(category.get("id"));
            Path target = YamlFiles.target(documents.toFile(), id).toPath();
            backup(project, backups, row);
            if (Files.exists(target)) {
                skipped.add(id);
                continue;
            }
            int rank = order.indexOf(id);
            String doc = yaml.dump(toDocument(category, id, rank < 0 ? null : rank + 1, texts));
            Files.createDirectories(documents);
            writeAtomically(target, doc);
            Files.delete(row);
            moved.add(id);
        }
        // The Table is gone once its rows are: its `_source.yaml` and folder go too.
        try (Stream<Path> left = Files.list(table)) {
            if (left.noneMatch(p -> isYaml(p) && !p.getFileName().toString().startsWith("_"))) {
                if (Files.isRegularFile(source)) {
                    backup(project, backups, source);
                    Files.delete(source);
                }
            }
        }
        try (Stream<Path> left = Files.list(table)) {
            if (left.findAny().isEmpty()) Files.delete(table);
        }
        return new Result(List.copyOf(moved), List.copyOf(skipped));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> toDocument(Map<String, Object> row, String id, Integer order, Texts texts) {
        String base = "sources.categories." + id.toLowerCase(Locale.ROOT);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("schema", SCHEMA);
        out.put("name", texts.keyed(row.get("name"), base + ".name"));
        if (row.get("icon") != null) out.put("icon", texts.icon(row.get("icon"), base + ".icon"));
        out.put("discount", row.get("discount") instanceof Number n ? n : 0);
        if (order != null) out.put("order", order);
        for (var entry : row.entrySet()) {
            if (!Set.of("id", "schema", "name", "icon", "discount", "items").contains(entry.getKey())) {
                out.put(entry.getKey(), entry.getValue());
            }
        }
        List<Object> items = new ArrayList<>();
        List<?> rawItems = row.get("items") instanceof List<?> l ? l : List.of();
        for (int n = 0; n < rawItems.size(); n++) {
            if (!(rawItems.get(n) instanceof Map<?, ?> rawItem)) continue;
            Map<String, Object> item = (Map<String, Object>) rawItem;
            String itemBase = base + ".items" + (n == 0 ? "" : "_" + (n + 1));
            Map<String, Object> next = new LinkedHashMap<>();
            next.put("id", item.get("id"));
            next.put("name", texts.keyed(item.get("name"), itemBase + ".name"));
            if (item.get("icon") != null) next.put("icon", texts.icon(item.get("icon"), itemBase + ".icon"));
            next.put("price", item.get("price") instanceof Number p ? p : 0);
            if (item.get("discount") instanceof Number d && d.doubleValue() != 0) next.put("discount", d);
            for (var entry : item.entrySet()) {
                if (!Set.of("id", "name", "icon", "price", "discount").contains(entry.getKey())) {
                    next.put(entry.getKey(), entry.getValue());
                }
            }
            items.add(next);
        }
        out.put("items", items);
        return out;
    }

    /**
     * The folder's translations, read the way the shop resolved the Table: a name is a key of
     * the default language or itself; an icon text is first looked up at its address.
     */
    static final class Texts {
        final String defaultLang;
        /** Language → flattened {@code lang/<lang>.yaml}, the default language first. */
        final Map<String, Map<String, String>> langs;

        Texts(String defaultLang, Map<String, Map<String, String>> langs) {
            this.defaultLang = defaultLang;
            this.langs = langs;
        }

        static Texts read(Path project, Yaml yaml) throws IOException {
            String defaultLang = "fr";
            File moduleFile = YamlFiles.moduleFile(project.toFile());
            Path manifest = moduleFile != null ? moduleFile.toPath() : project.resolve("manifest.yaml");
            if (Files.isRegularFile(manifest)
                    && yaml.load(Files.readString(manifest)) instanceof Map<?, ?> m
                    && m.get("default_lang") instanceof String s && !s.isBlank()) {
                defaultLang = s;
            }
            Map<String, Map<String, String>> langs = new LinkedHashMap<>();
            langs.put(defaultLang, Map.of());
            Path dir = project.resolve("lang");
            if (Files.isDirectory(dir)) {
                try (Stream<Path> files = Files.list(dir)) {
                    for (Path file : files.filter(CategoryKindMigrator::isYaml).sorted().toList()) {
                        String lang = file.getFileName().toString().replaceFirst("\\.ya?ml$", "");
                        langs.put(lang, LangFlattener.flatten(yaml.load(Files.readString(file))));
                    }
                }
            }
            return new Texts(defaultLang, langs);
        }

        /** A name: {@code lang.getOrDefault(value, value)} in the default language, like {@code ShopRepo}. */
        Object keyed(Object value, String address) {
            if (value instanceof Map<?, ?>) return value;
            String raw = value == null ? "" : value.toString();
            String own = langs.get(defaultLang).getOrDefault(raw, raw);
            return withOthers(own, address, raw);
        }

        /** An icon's texts: the address first, then the text as a key, then the text. */
        @SuppressWarnings("unchecked")
        Object icon(Object value, String address) {
            if (!(value instanceof Map<?, ?> raw)) return value;
            Map<String, Object> out = new LinkedHashMap<>();
            for (var entry : ((Map<String, Object>) raw).entrySet()) {
                String name = entry.getKey();
                Object v = entry.getValue();
                if (name.equals("extends")) out.put(name, icon(v, address + ".extends"));
                else if (name.equals("title") && v instanceof String text) out.put(name, addressed(text, address + ".title"));
                else if (name.equals("lore") && v instanceof List<?> lines) out.put(name, lore(lines, address + ".lore"));
                else out.put(name, v);
            }
            return out;
        }

        private Object addressed(String text, String address) {
            Map<String, String> own = langs.get(defaultLang);
            return withOthers(own.getOrDefault(address, own.getOrDefault(text, text)), address, text);
        }

        /** Lines: one list, or one list per language when another language has its own lines. */
        private Object lore(List<?> lines, String address) {
            Map<String, List<String>> perLang = new LinkedHashMap<>();
            boolean others = false;
            for (String lang : langs.keySet()) {
                List<String> out = new ArrayList<>();
                boolean any = false;
                for (int n = 0; n < lines.size(); n++) {
                    String text = String.valueOf(lines.get(n));
                    String at = address + (n == 0 ? "" : "_" + (n + 1));
                    Map<String, String> tr = langs.get(lang);
                    String found = tr.get(at);
                    if (found == null) found = tr.get(text);
                    if (found != null) any = true;
                    out.add(found != null ? found : text);
                }
                if (lang.equals(defaultLang)) perLang.put(lang, out);
                else if (any) {
                    perLang.put(lang, out);
                    others = true;
                }
            }
            if (!others) return perLang.get(defaultLang);
            return new LinkedHashMap<>(perLang);
        }

        /** {@code own}, or {@code {default: own, other: …}} when another language translates it. */
        private Object withOthers(String own, String address, String raw) {
            Map<String, String> out = new LinkedHashMap<>();
            out.put(defaultLang, own);
            for (var entry : langs.entrySet()) {
                if (entry.getKey().equals(defaultLang)) continue;
                String found = entry.getValue().get(address);
                if (found == null) found = entry.getValue().get(raw);
                if (found != null) out.put(entry.getKey(), found);
            }
            return out.size() == 1 ? own : out;
        }
    }

    private static Yaml yaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowUnicode(true);
        return new Yaml(options);
    }

    private static boolean isYaml(Path path) {
        return path.getFileName().toString().matches(".*\\.ya?ml$");
    }

    /** Keeps the first copy of {@code file}: a retry never replaces the original with a migrated one. */
    private static void backup(Path project, Path backups, Path file) throws IOException {
        Path copy = backups.resolve(project.relativize(file).toString());
        Files.createDirectories(copy.getParent());
        if (!Files.exists(copy)) Files.copy(file, copy);
    }

    private static void writeAtomically(Path file, String content) throws IOException {
        Path tmp = Files.createTempFile(file.getParent(), ".category-migration-", ".tmp");
        try {
            Files.writeString(tmp, content);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
