package fr.iban.boutique.artisan;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Moves legacy product lore into its icon before Artisan loads the project. */
public final class CatalogIconMigrator {
    private CatalogIconMigrator() {}

    public static boolean migrateItem(Map<String, Object> item) {
        if (!item.containsKey("lore")) return false;
        Object raw = item.get("lore");
        List<?> lines = raw instanceof List<?> list ? list : raw instanceof String s ? List.of(s) : List.of();
        if (!lines.isEmpty()) {
            Map<String, Object> icon = new LinkedHashMap<>();
            icon.put("extends", item.getOrDefault("icon", "BARRIER"));
            // Artisan garde le lore de l'item de base et ajoute celui-ci dessous :
            // ni placeholder ni `lore_mode`, la forme nue dit déjà ce qu'il faut.
            icon.put("lore", new ArrayList<Object>(lines));
            item.put("icon", icon);
        }
        item.remove("lore");
        return true;
    }

    @SuppressWarnings("unchecked")
    public static boolean migrateIfNeeded(File projectDir) {
        Path project = projectDir.toPath();
        Path categories = project.resolve("data/categories");
        if (!Files.isDirectory(categories)) return false;
        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            Map<Path, String> writes = new LinkedHashMap<>();
            Map<String, String> renames = new LinkedHashMap<>();
            try (var files = Files.list(categories)) {
                for (Path path : files.filter(p -> !p.getFileName().toString().startsWith("_") && p.toString().matches(".*\\.ya?ml$")).toList()) {
                    Object parsed = yaml.load(Files.readString(path));
                    if (!(parsed instanceof Map<?, ?> row) || !(row.get("items") instanceof List<?> items)) continue;
                    boolean changed = false;
                    for (int n = 0; n < items.size(); n++) {
                        if (!(items.get(n) instanceof Map<?, ?> raw)) continue;
                        Map<String, Object> item = (Map<String, Object>)raw;
                        Object lore = item.get("lore");
                        int count = lore instanceof List<?> list ? list.size() : lore instanceof String ? 1 : 0;
                        if (count > 0) {
                            String base = "sources.categories." + String.valueOf(row.get("id")).toLowerCase(Locale.ROOT) + ".items" + (n == 0 ? "" : "_" + (n + 1));
                            iconTranslations(item.get("icon"), base + ".icon", base + ".icon.extends", renames);
                            // Les lignes ne sont plus décalées par un `{base.lore}` de tête :
                            // l'adresse d'une ligne garde son rang sous `icon`.
                            for (int line = 0; line < count; line++) {
                                String suffix = ".lore" + (line == 0 ? "" : "_" + (line + 1));
                                renames.put(base + suffix, base + ".icon" + suffix);
                            }
                        }
                        changed |= migrateItem(item);
                    }
                    if (changed) writes.put(path, yaml.dump(row));
                }
            }
            // Translations first: a retry after interruption must not move them twice.
            Path langDir = project.resolve("lang");
            if (!renames.isEmpty() && Files.isDirectory(langDir)) {
                try (var langs = Files.list(langDir)) {
                    for (Path path : langs.filter(p -> p.toString().matches(".*\\.ya?ml$")).toList()) {
                        Map<String, String> before = LangFlattener.flatten(yaml.load(Files.readString(path)));
                        Map<String, String> after = new LinkedHashMap<>(before);
                        Path backup = project.resolveSibling("catalog-icon-backup").resolve(project.relativize(path));
                        Map<String, String> source = Files.exists(backup)
                                ? LangFlattener.flatten(yaml.load(Files.readString(backup))) : before;
                        for (var entry : source.entrySet()) for (var rename : renames.entrySet()) {
                            String from = rename.getKey();
                            if (entry.getKey().equals(from)) {
                                String to = rename.getValue();
                                after.put(to, entry.getValue());
                                break;
                            }
                        }
                        if (!after.equals(before)) write(project, path, yaml.dump(after));
                    }
                }
            }
            for (var entry : writes.entrySet()) write(project, entry.getKey(), entry.getValue());
            return !writes.isEmpty();
        } catch (IOException | RuntimeException e) {
            System.err.println("[Boutique] Icon migration failed: " + e.getMessage());
            return false;
        }
    }

    private static void iconTranslations(Object value, String from, String to, Map<String, String> renames) {
        if (!(value instanceof Map<?, ?> icon)) return;
        for (String field : List.of("title", "lore")) {
            Object text = icon.get(field);
            int count = text instanceof List<?> lines ? lines.size() : text instanceof String ? 1 : 0;
            for (int n = 0; n < count; n++) {
                String suffix = "." + field + (n == 0 ? "" : "_" + (n + 1));
                renames.put(from + suffix, to + suffix);
            }
        }
        iconTranslations(icon.get("extends"), from + ".extends", to + ".extends", renames);
    }

    private static void write(Path project, Path file, String content) throws IOException {
        Path backup = project.resolveSibling("catalog-icon-backup").resolve(project.relativize(file));
        Files.createDirectories(backup.getParent());
        if (!Files.exists(backup)) Files.copy(file, backup);
        Path tmp = Files.createTempFile(file.getParent(), ".icon-migration-", ".tmp");
        try {
            Files.writeString(tmp, content);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(tmp); }
    }
}
