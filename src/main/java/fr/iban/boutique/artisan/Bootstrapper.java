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
            "dialogs/confirm_purchase.yaml", "commands/boutique.yaml", "boutique/shop.yaml");

    private Bootstrapper() {}

    /**
     * @return true si le bootstrap a écrit quelque chose.
     *
     * Écrit dans un répertoire temporaire sibling ({@code .boutique_shop.tmp}) puis rename
     * atomique vers {@code boutique_shop} : une écriture partielle qui échoue ne laisse jamais
     * un {@code boutique_shop/} à moitié rempli qui bloquerait tout retry futur (guard
     * {@code projectDir.exists()}).
     */
    public static boolean bootstrapIfAbsent(File projectsDir, Optional<String> migratedShopYaml) throws IOException {
        File projectDir = new File(projectsDir, "boutique_shop");
        if (projectDir.exists()) return false;

        File tmpDir = new File(projectsDir, ".boutique_shop.tmp");
        if (tmpDir.exists()) deleteRecursively(tmpDir);

        try {
            for (String res : RESOURCES) {
                File target = new File(tmpDir, res);
                Files.createDirectories(target.getParentFile().toPath());
                if (res.equals("boutique/shop.yaml") && migratedShopYaml.isPresent()) {
                    Files.writeString(target.toPath(), migratedShopYaml.get());
                } else {
                    try (InputStream in = Bootstrapper.class.getResourceAsStream("/bootstrap/" + res)) {
                        if (in == null) throw new IOException("Missing bundled resource /bootstrap/" + res);
                        Files.write(target.toPath(), in.readAllBytes());
                    }
                }
            }
            Files.createDirectories(projectsDir.toPath());
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
