package fr.iban.boutique.artisan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

/** Écrit le project par défaut boutique_shop dans plugins/Artisan/projects/ s'il n'existe pas. */
public final class Bootstrapper {
    private static final List<String> RESOURCES = List.of(
            "manifest.yaml", "menus/shop_main.yaml", "menus/shop_category.yaml",
            "dialogs/confirm_purchase.yaml", "commands/boutique.yaml", "boutique/shop.yaml");

    private Bootstrapper() {}

    /** @return true si le bootstrap a écrit quelque chose. */
    public static boolean bootstrapIfAbsent(File projectsDir, Optional<String> migratedShopYaml) throws IOException {
        File projectDir = new File(projectsDir, "boutique_shop");
        if (projectDir.exists()) return false;
        for (String res : RESOURCES) {
            File target = new File(projectDir, res);
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
        return true;
    }
}
