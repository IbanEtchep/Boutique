package fr.iban.boutique.artisan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Écrit le project par défaut boutique_shop dans plugins/Artisan/projects/ s'il n'existe pas. */
public final class Bootstrapper {
    private static final List<String> RESOURCES = List.of(
            "manifest.yaml", "menus/shop_main.yaml", "menus/shop_category.yaml",
            "dialogs/confirm_purchase.yaml", "commands/boutique.yaml", "boutique/shop.yaml",
            "lang/fr.yaml");

    private Bootstrapper() {}

    /**
     * @return true si le bootstrap a écrit quelque chose.
     *
     * Écrit dans un répertoire temporaire sibling ({@code .boutique_shop.tmp}) puis rename
     * atomique vers {@code boutique_shop} : une écriture partielle qui échoue ne laisse jamais
     * un {@code boutique_shop/} à moitié rempli qui bloquerait tout retry futur (guard
     * {@code projectDir.exists()}).
     */
    public static boolean bootstrapIfAbsent(File projectDir, Optional<java.util.Map<String, String>> migratedFiles) throws IOException {
        if (projectDir.exists()) return false;

        File parent = projectDir.getParentFile();
        File tmpDir = new File(parent, "." + projectDir.getName() + ".tmp");
        if (tmpDir.exists()) deleteRecursively(tmpDir);

        java.util.Map<String, String> migrated = migratedFiles.orElse(java.util.Map.of());
        try {
            for (String res : RESOURCES) {
                File target = new File(tmpDir, res);
                Files.createDirectories(target.getParentFile().toPath());
                if (migrated.containsKey(res)) {
                    Files.writeString(target.toPath(), migrated.get(res));
                } else {
                    try (InputStream in = Bootstrapper.class.getResourceAsStream("/bootstrap/" + res)) {
                        if (in == null) throw new IOException("Missing bundled resource /bootstrap/" + res);
                        Files.write(target.toPath(), in.readAllBytes());
                    }
                }
            }
            // Fichiers migrés hors ressources bundlées (items/ci_*.yml des stacks capturés).
            for (var entry : migrated.entrySet()) {
                if (RESOURCES.contains(entry.getKey())) continue;
                File target = new File(tmpDir, entry.getKey());
                Files.createDirectories(target.getParentFile().toPath());
                Files.writeString(target.toPath(), entry.getValue());
            }
            Files.createDirectories(parent.toPath());
            Files.move(tmpDir.toPath(), projectDir.toPath());
        } catch (IOException e) {
            deleteRecursively(tmpDir);
            throw e;
        }
        return true;
    }

    private static void deleteRecursively(File dir) throws IOException {
        if (!dir.exists()) return;
        try (Stream<java.nio.file.Path> walk = Files.walk(dir.toPath())) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                if (!p.toFile().delete()) {
                    p.toFile().deleteOnExit();
                }
            });
        }
    }
}
