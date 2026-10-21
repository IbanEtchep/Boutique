package fr.iban.boutique.artisan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Les migrations faites une fois, notées dans {@code plugins/Boutique/migrations.done}
 * (une par ligne) : au démarrage suivant, un migrateur noté ne relit ni ne reparse
 * rien. Un fichier absent ou illisible vaut « rien de fait » : chaque migrateur reste
 * idempotent, il refait au pire une lecture.
 */
public final class Migrations {
    private final File file;
    private final Set<String> done = new LinkedHashSet<>();

    public Migrations(File dataFolder) {
        this.file = new File(dataFolder, "migrations.done");
        if (file.isFile()) {
            try {
                for (String line : Files.readAllLines(file.toPath())) {
                    String name = line.trim();
                    if (!name.isEmpty()) done.add(name);
                }
            } catch (IOException ignored) {
                // Rien de noté : les migrateurs revérifient eux-mêmes.
            }
        }
    }

    public boolean isDone(String name) { return done.contains(name); }

    /** Note [name] faite ; une écriture ratée ne bloque rien, la migration sera revérifiée. */
    public void markDone(String name) {
        if (!done.add(name)) return;
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.writeString(file.toPath(), String.join("\n", done) + "\n");
        } catch (IOException ignored) {
            // Idem : revérifié au prochain démarrage.
        }
    }
}
