package fr.iban.boutique.artisan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Écrit le project par défaut editor dans plugins/Boutique/ s'il n'existe pas. */
public final class Bootstrapper {
    /**
     * Catalogue EXEMPLE (ADR composable-data-models §8) — la Table déclarée
     * qu'une install vierge doit trouver. Écarté quand un catalogue legacy est
     * migré : il masquerait le vrai contenu au chargement ({@code ShopRepo}
     * lit la Table en priorité) et bloquerait sa conversion (garde
     * {@code _source.yaml} de {@link ShopDataMigrator}).
     */
    static final List<String> EXAMPLE_CATALOG = List.of(
            "data/categories/_source.yaml", "data/categories/weapons.yaml");

    private static final List<String> RESOURCES = List.of(
            "manifest.yaml", "menus/shop_main.yaml", "menus/shop_category.yaml",
            "dialogs/confirm_purchase.yaml", "commands/boutique.yaml",
            "data/categories/_source.yaml", "data/categories/weapons.yaml",
            "data/shop_settings.yaml",
            // Ref d'une ligne vers le model DÉCLARÉ : le schéma vit dans ce JAR,
            // il n'est jamais recopié sur le disque (ADR yaml-assisted-editing).
            "models/shop_settings.yaml",
            "lang/fr.yaml");

    private Bootstrapper() {}

    /**
     * @return true si le bootstrap a écrit quelque chose.
     *
     * Écrit dans un répertoire temporaire sibling ({@code .editor.tmp}) puis rename
     * atomique vers {@code editor} : une écriture partielle qui échoue ne laisse jamais
     * un {@code editor/} à moitié rempli qui bloquerait tout retry futur (guard
     * {@code projectDir.exists()}).
     */
    public static boolean bootstrapIfAbsent(File projectDir, Optional<java.util.Map<String, String>> migratedFiles) throws IOException {
        if (projectDir.exists()) return false;

        File parent = projectDir.getParentFile();
        File tmpDir = new File(parent, "." + projectDir.getName() + ".tmp");
        if (tmpDir.exists()) deleteRecursively(tmpDir);

        java.util.Map<String, String> migrated = migratedFiles.orElse(java.util.Map.of());
        // Un catalogue migré remplace l'exemple : c'est ShopDataMigrator qui
        // écrira data/categories/ depuis le boutique/shop.yaml posé plus bas.
        boolean migratedCatalog = migrated.containsKey("boutique/shop.yaml");
        try {
            for (String res : RESOURCES) {
                if (migratedCatalog && EXAMPLE_CATALOG.contains(res)) continue;
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
