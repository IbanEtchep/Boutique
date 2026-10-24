package fr.iban.boutique.artisan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Écrit le contenu par défaut dans le dossier du module s'il n'en a pas encore. */
public final class Bootstrapper {
    /**
     * Catalogue EXEMPLE : une catégorie {@code boutique_category} qu'une install vierge doit
     * trouver. Écarté quand un catalogue legacy est migré : il masquerait le vrai contenu au
     * chargement ({@code ShopRepo} lit les catégories en priorité).
     */
    static final List<String> EXAMPLE_CATALOG = List.of("boutique_categories/weapons.yaml");

    private static final List<String> RESOURCES = List.of(
            "manifest.yaml", "menus/shop_main.yaml", "menus/shop_category.yaml",
            "dialogs/confirm_purchase.yaml",
            "boutique_categories/weapons.yaml",
            "data/shop_settings.yaml",
            // Ref d'une ligne vers le model DÉCLARÉ : le schéma vit dans ce JAR,
            // il n'est jamais recopié sur le disque (ADR yaml-assisted-editing).
            "models/shop_settings.yaml",
            "lang/fr.yaml");

    private Bootstrapper() {}

    /**
     * @return true si le bootstrap a écrit quelque chose.
     *
     * Le dossier du module ({@code plugins/Artisan/modules/boutique_addon/}) existe
     * avant nous : Artisan le crée en le réservant. Le contenu est donc « absent »
     * tant que {@code manifest.yaml} manque, pas tant que le dossier manque.
     *
     * Écrit dans un répertoire temporaire sibling ({@code .<nom>.tmp}, caché du scan
     * d'Artisan) puis le déplace : d'un bloc si le dossier n'existe pas, fichier par
     * fichier sinon, {@code manifest.yaml} en dernier. Une écriture interrompue ne
     * laisse donc jamais de manifest, et le démarrage suivant recommence.
     */
    public static boolean bootstrapIfAbsent(File projectDir, Optional<java.util.Map<String, String>> migratedFiles) throws IOException {
        if (YamlFiles.moduleFile(projectDir) != null) return false;

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
            if (!projectDir.exists()) {
                Files.move(tmpDir.toPath(), projectDir.toPath());
            } else {
                moveInto(tmpDir.toPath(), projectDir.toPath());
                deleteRecursively(tmpDir);
            }
        } catch (IOException e) {
            deleteRecursively(tmpDir);
            throw e;
        }
        return true;
    }

    /** Moves every file of {@code from} into {@code to}, {@code manifest.yaml} last. */
    private static void moveInto(java.nio.file.Path from, java.nio.file.Path to) throws IOException {
        java.nio.file.Path manifest = from.resolve("manifest.yaml");
        List<java.nio.file.Path> files;
        try (Stream<java.nio.file.Path> walk = Files.walk(from)) {
            files = walk.filter(Files::isRegularFile).filter(p -> !p.equals(manifest)).toList();
        }
        for (java.nio.file.Path file : files) moveFile(file, to.resolve(from.relativize(file)));
        if (Files.isRegularFile(manifest)) moveFile(manifest, to.resolve("manifest.yaml"));
    }

    private static void moveFile(java.nio.file.Path file, java.nio.file.Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Files.move(file, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
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
