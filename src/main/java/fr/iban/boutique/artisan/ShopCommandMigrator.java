package fr.iban.boutique.artisan;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code /boutique} est devenue une commande du module ({@code boutique:open}, API 2.11.0), qu'Artisan
 * écrit lui-même en {@code commands/boutique.yaml} du dossier et remplace à chaque version tant
 * qu'elle n'est pas modifiée. Le fichier que les versions précédentes posaient au même chemin (il
 * porte {@code id: boutique}, celui d'Artisan non) compterait comme modifié et resterait pour
 * toujours : il part, avec son entrée du manifest, tant qu'il est resté celui livré, et Artisan
 * écrit le sien. Modifié par l'admin, il est gardé tel quel : c'est désormais le fichier de la
 * commande du module.
 */
public final class ShopCommandMigrator {
    public enum Result { NONE, REMOVED, KEPT_CUSTOM }

    static final String FILE = "commands/boutique.yaml";
    private static final Set<String> SHIPPED_KEYS = Set.of("schema", "id", "name", "description", "actions");
    private static final Pattern COMMANDS_LINE = Pattern.compile("(?m)^(\\s+commands:\\s*\\[)([^\\]\\r\\n]*)(\\][^\\r\\n]*)(\\r?\\n|\\z)");

    private ShopCommandMigrator() {}

    public static Result migrate(File projectDir) throws IOException {
        Path file = projectDir.toPath().resolve(FILE);
        if (!Files.isRegularFile(file)) return Result.NONE;
        String content = Files.readString(file);
        if (!isLegacy(content)) return Result.NONE;
        if (!isShipped(content)) return Result.KEPT_CUSTOM;
        Files.delete(file);
        removeFromManifest(projectDir.toPath().resolve("manifest.yaml"));
        return Result.REMOVED;
    }

    /** Written by an older Boutique (with an {@code id}), where Artisan's command file has none. */
    static boolean isLegacy(String content) {
        return load(content) instanceof Map<?, ?> map && map.containsKey("id");
    }

    private static Object load(String content) {
        try {
            return new Yaml().load(content);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The file the bootstrap wrote: {@code open_menu shop_main}, the shipped name and description. */
    static boolean isShipped(String content) {
        if (!(load(content) instanceof Map<?, ?> map)) return false;
        for (Object key : map.keySet()) if (!SHIPPED_KEYS.contains(String.valueOf(key))) return false;
        Object description = map.get("description");
        return "command/v1".equals(map.get("schema"))
                && "boutique".equals(map.get("id"))
                && "boutique".equals(Objects.requireNonNullElse(map.get("name"), "boutique"))
                && (description == null || "Ouvre la boutique".equals(description))
                && map.get("actions") instanceof String actions
                && actions.trim().replaceAll("\\s+", " ").equals("open_menu shop_main");
    }

    /** Drops {@code boutique} from {@code files.commands}, the rest of the manifest kept as written. */
    static void removeFromManifest(Path manifest) throws IOException {
        if (!Files.isRegularFile(manifest)) return;
        String before = Files.readString(manifest);
        Matcher m = COMMANDS_LINE.matcher(before);
        if (!m.find()) return;
        List<String> kept = java.util.Arrays.stream(m.group(2).split(","))
                .map(String::trim).filter(s -> !s.isEmpty() && !s.equals("boutique")).toList();
        String line = kept.isEmpty() ? "" : m.group(1) + String.join(", ", kept) + m.group(3) + m.group(4);
        String after = before.substring(0, m.start()) + line + before.substring(m.end());
        if (after.equals(before)) return;
        Path tmp = Files.createTempFile(manifest.getParent(), ".manifest-", ".tmp");
        try {
            Files.writeString(tmp, after);
            Files.move(tmp, manifest, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
