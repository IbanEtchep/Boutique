package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Un dossier de module passé en {@code .yml} ({@code artisan-module.yml}, lignes en {@code .yml}) se lit et s'écrit en place. */
class YmlModuleTest {

    private static File ymlProject(Path tmp) throws Exception {
        File dir = tmp.resolve("boutique").toFile();
        new File(dir, "data/categories").mkdirs();
        Files.writeString(new File(dir, "artisan-module.yml").toPath(), "schema: module/v1\ndefault_lang: fr\n");
        Files.writeString(new File(dir, "data/categories/_source.yml").toPath(), "order:\n- weapons\n");
        Files.writeString(new File(dir, "data/categories/weapons.yml").toPath(),
                "id: weapons\nname: Armes\nicon: CHEST\ndiscount: 0\nitems: []\n");
        return dir;
    }

    @Test
    void la_capture_ecrit_dans_la_ligne_yml_existante(@TempDir Path tmp) throws Exception {
        File dir = ymlProject(tmp);
        CapturedItemWriter.Result res = CapturedItemWriter.addItem(dir, "epee", 10, "Armes", "item: {}\n", java.util.Map.of());
        assertTrue(res.ok(), res.error());
        assertTrue(Files.readString(new File(dir, "data/categories/weapons.yml").toPath()).contains("epee"));
        assertFalse(new File(dir, "data/categories/weapons.yaml").exists());

        CapturedItemWriter.Result other = CapturedItemWriter.addItem(dir, "pain", 1, "Nourriture", "item: {}\n", java.util.Map.of());
        assertTrue(other.ok(), other.error());
        assertTrue(new File(dir, "data/categories/nourriture.yml").isFile(), "une nouvelle ligne suit l'extension du dossier");
    }

    @Test
    void un_module_en_artisan_module_yml_n_est_pas_re_bootstrappe(@TempDir Path tmp) throws Exception {
        File dir = ymlProject(tmp);
        assertFalse(Bootstrapper.bootstrapIfAbsent(dir, Optional.empty()));
        assertFalse(new File(dir, "manifest.yaml").exists());
    }

    @Test
    void les_reglages_vont_dans_le_fichier_yml_existant(@TempDir Path tmp) throws Exception {
        File dir = ymlProject(tmp);
        Files.writeString(new File(dir, "data/shop_settings.yml").toPath(), "whole_shop_discount: 5\n");
        Files.createDirectories(new File(dir, "models").toPath());
        Files.writeString(new File(dir, "models/shop_settings.yml").toPath(), "model: boutique:shop_settings\n");
        SettingsMigrator.ensureSettings(dir, null, null, false);
        assertFalse(new File(dir, "data/shop_settings.yaml").exists());
        assertFalse(new File(dir, "models/shop_settings.yaml").exists());
        assertTrue(Files.readString(new File(dir, "data/shop_settings.yml").toPath()).contains("whole_shop_discount: 5"));
    }

    @Test
    void le_project_id_legacy_part_aussi_d_artisan_module_yml(@TempDir Path tmp) throws Exception {
        File dir = ymlProject(tmp);
        Files.writeString(new File(dir, "artisan-module.yml").toPath(), "schema: module/v1\nproject_id: boutique_shop\n");
        assertTrue(ManifestCleaner.removeLegacyProjectId(dir));
        assertEquals("schema: module/v1\n", Files.readString(new File(dir, "artisan-module.yml").toPath()));
    }
}
