package net.artisanmc.store;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Le plugin s'appelait Boutique jusqu'à la 2.x : son dossier était {@code plugins/Boutique/}
 * (base Azuriom dans config.yml, migrations.done, sauvegardes des migrations). Au premier
 * démarrage d'ArtisanStore, ce dossier devient {@code plugins/ArtisanStore/}, renommé d'un
 * bloc. Un dossier ArtisanStore qui a déjà du contenu n'est jamais écrasé : l'ancien reste en
 * place, le log le dit, et l'admin choisit lequel garder.
 */
public final class LegacyDataFolder {
    /** Le nom du plugin, donc de son dossier, avant Store. */
    public static final String LEGACY_NAME = "Boutique";

    public enum Result { NONE, MOVED, BOTH }

    private LegacyDataFolder() {}

    /** Renomme {@code legacy} en {@code current} quand {@code current} est absent ou vide. */
    public static Result migrate(File legacy, File current) throws IOException {
        if (!legacy.isDirectory()) return Result.NONE;
        if (current.isDirectory()) {
            String[] content = current.list();
            if (content == null || content.length > 0) return Result.BOTH;
            Files.delete(current.toPath());
        } else if (current.exists()) {
            return Result.BOTH;
        }
        // Même parent (plugins/) : un renommage, jamais une copie à moitié faite.
        Files.move(legacy.toPath(), current.toPath(), StandardCopyOption.ATOMIC_MOVE);
        return Result.MOVED;
    }
}
