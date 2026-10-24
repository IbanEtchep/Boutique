package fr.iban.boutique.artisan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

/**
 * Retire le {@code project_id} que les premières versions écrivaient dans le manifest
 * ({@code boutique_shop}). Artisan l'ignore et avertit à chaque chargement : l'identité du
 * module est le nom de son dossier. Seule la ligne de premier niveau part, le reste du
 * fichier est gardé tel quel.
 */
public final class ManifestCleaner {
    private static final Pattern PROJECT_ID = Pattern.compile("(?m)^project_id:[^\\r\\n]*(\\r?\\n|\\z)");

    private ManifestCleaner() {}

    /** @return true when the manifest was rewritten. */
    public static boolean removeLegacyProjectId(File projectDir) throws IOException {
        File moduleFile = YamlFiles.moduleFile(projectDir);
        if (moduleFile == null) return false;
        Path manifest = moduleFile.toPath();
        String before = Files.readString(manifest);
        String after = PROJECT_ID.matcher(before).replaceAll("");
        if (after.equals(before)) return false;
        Path tmp = Files.createTempFile(projectDir.toPath(), ".manifest-", ".tmp");
        try {
            Files.writeString(tmp, after);
            Files.move(tmp, manifest, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
        return true;
    }
}
