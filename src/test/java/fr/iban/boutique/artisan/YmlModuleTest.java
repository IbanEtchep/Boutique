package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Un dossier de module passé en {@code .yml} ({@code artisan-module.yml}, catégories en {@code .yml}) se lit et s'écrit en place. */
class YmlModuleTest {

    private static File ymlProject(Path tmp) throws Exception {
        File dir = tmp.resolve("boutique").toFile();
        new File(dir, CategoryKindMigrator.DIR).mkdirs();
        Files.writeString(new File(dir, "artisan-module.yml").toPath(), "schema: module/v1\ndefault_lang: fr\n");
        Files.writeString(new File(dir, CategoryKindMigrator.DIR + "/weapons.yml").toPath(),
                "schema: boutique_category/v1\nname: Armes\nicon: CHEST\ndiscount: 0\norder: 1\nitems: []\n");
        return dir;
    }

    @Test
    void la_capture_ecrit_dans_la_ligne_yml_existante(@TempDir Path tmp) throws Exception {
        File dir = ymlProject(tmp);
        CapturedItemWriter.Result res = CapturedItemWriter.addItem(dir, "epee", 10, "Armes", "item: {}\n", java.util.Map.of());
        assertTrue(res.ok(), res.error());
        assertTrue(Files.readString(new File(dir, CategoryKindMigrator.DIR + "/weapons.yml").toPath()).contains("epee"));
        assertFalse(new File(dir, CategoryKindMigrator.DIR + "/weapons.yaml").exists());

        CapturedItemWriter.Result other = CapturedItemWriter.addItem(dir, "pain", 1, "Nourriture", "item: {}\n", java.util.Map.of());
        assertTrue(other.ok(), other.error());
        String[] created = new File(dir, CategoryKindMigrator.DIR).list((d, n) -> !n.startsWith("weapons"));
        assertNotNull(created);
        assertEquals(1, created.length);
        assertTrue(created[0].endsWith(".yml"), "une nouvelle catégorie suit l'extension du dossier : " + created[0]);
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
        new File(dir, "data").mkdirs();
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

    @Test
    void la_table_yml_devient_des_documents_yml(@TempDir Path tmp) throws Exception {
        File dir = tmp.resolve("boutique").toFile();
        new File(dir, "data/categories").mkdirs();
        new File(dir, "lang").mkdirs();
        new File(dir, CategoryKindMigrator.DIR).mkdirs();
        Files.writeString(new File(dir, "artisan-module.yml").toPath(), "schema: module/v1\ndefault_lang: en\n");
        Files.writeString(new File(dir, "lang/en.yml").toPath(), "cat:\n  weapons: Weapons\n");
        Files.writeString(new File(dir, "data/categories/_source.yml").toPath(), "order:\n- food\n- weapons\n");
        Files.writeString(new File(dir, "data/categories/weapons.yml").toPath(), "id: weapons\nname: cat.weapons\nitems: []\n");
        Files.writeString(new File(dir, CategoryKindMigrator.DIR + "/other.yml").toPath(), "schema: boutique_category/v1\nname: Other\n");

        CategoryKindMigrator.migrate(dir.toPath(), tmp.resolve("backup"));

        String doc = Files.readString(new File(dir, CategoryKindMigrator.DIR + "/weapons.yml").toPath());
        assertTrue(doc.contains("name: Weapons"), doc);
        assertTrue(doc.contains("order: 2"), doc);
        assertFalse(new File(dir, CategoryKindMigrator.DIR + "/weapons.yaml").exists());
    }
}
