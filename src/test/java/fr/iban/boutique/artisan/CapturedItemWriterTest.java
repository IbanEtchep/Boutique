package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * `/boutiqueadmin additem` écrit désormais **sur le disque du module**.
 *
 * Avant, la capture partait au backend qui mutait sa copie du project. Le
 * backend ne stocke plus rien (ADR Artisan `server-disk-only-storage`) : le
 * module possède sa racine, il y écrit lui-même, et le scan du plugin recharge
 * à chaud — exactement comme une édition faite à la main.
 */
class CapturedItemWriterTest {

    private static final Map<String, Object> DISPLAY = Map.of(
            "material", "DIAMOND_SWORD", "name", "Épée du roi", "enchanted", true);

    private static File seedProject(Path tmp) throws Exception {
        File dir = tmp.resolve("editor").toFile();
        new File(dir, "data/categories").mkdirs();
        Files.writeString(new File(dir, "manifest.yaml").toPath(), "default_lang: fr\n");
        Files.writeString(new File(dir, "data/categories/_source.yaml").toPath(),
                "order:\n- weapons\n");
        Files.writeString(new File(dir, "data/categories/weapons.yaml").toPath(),
                "id: weapons\nname: Armes\nicon: CHEST\ndiscount: 0\nitems: []\n");
        return dir;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readYaml(File f) throws Exception {
        return new Yaml().load(Files.readString(f.toPath()));
    }

    @Test
    void ajoute_l_article_dans_la_categorie_demandee(@TempDir Path tmp) throws Exception {
        File dir = seedProject(tmp);

        CapturedItemWriter.Result res = CapturedItemWriter.addItem(
                dir, "epee_roi", 250, "Armes", "item:\n  type: DIAMOND_SWORD\n", DISPLAY);

        assertTrue(res.ok(), res.error());
        Map<String, Object> row = readYaml(new File(dir, "data/categories/weapons.yaml"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) row.get("items");
        assertEquals(1, items.size());
        assertEquals("epee_roi", items.get(0).get("id"));
        assertEquals(250, items.get(0).get("price"));
        assertEquals("Épée du roi", items.get(0).get("name"));
        // L'icône pointe vers l'item capturé, pas vers un material.
        assertEquals("item:" + res.capturedItemId(), items.get(0).get("icon"));
    }

    @Test
    void ecrit_l_item_capture_et_sa_metadonnee_editeur(@TempDir Path tmp) throws Exception {
        File dir = seedProject(tmp);

        CapturedItemWriter.Result res = CapturedItemWriter.addItem(
                dir, "epee_roi", 250, "Armes", "item:\n  type: DIAMOND_SWORD\n", DISPLAY);

        // Le stack exact, verbatim : c'est lui qui porte NBT/enchants/CMD.
        File captured = new File(dir, "items/" + res.capturedItemId() + ".yml");
        assertTrue(captured.isFile());
        assertEquals("item:\n  type: DIAMOND_SWORD\n", Files.readString(captured.toPath()));

        // Le descripteur d'affichage vit dans l'état éditeur, hors hash.
        Map<String, Object> editorState = readYaml(new File(dir, ".artisan/editor.yaml"));
        assertEquals("editor-state/v1", editorState.get("schema"));
        Map<String, Object> items = (Map<String, Object>) editorState.get("items");
        Map<String, Object> meta = (Map<String, Object>) items.get(res.capturedItemId());
        assertEquals("Épée du roi", meta.get("name"));
        Map<String, Object> display = (Map<String, Object>) meta.get("display");
        assertEquals("DIAMOND_SWORD", display.get("material"));
        assertEquals(true, display.get("enchanted"));
    }

    @Test
    void un_id_deja_present_est_mis_a_jour_pas_duplique(@TempDir Path tmp) throws Exception {
        File dir = seedProject(tmp);
        CapturedItemWriter.addItem(dir, "epee_roi", 250, "Armes", "item: v1\n", DISPLAY);

        CapturedItemWriter.Result res = CapturedItemWriter.addItem(
                dir, "epee_roi", 999, "Armes", "item: v2\n", DISPLAY);

        assertTrue(res.ok());
        Map<String, Object> row = readYaml(new File(dir, "data/categories/weapons.yaml"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) row.get("items");
        assertEquals(1, items.size(), "l'article doit être mis à jour, pas dupliqué");
        assertEquals(999, items.get(0).get("price"));
        assertEquals("item:" + res.capturedItemId(), items.get(0).get("icon"));
    }

    @Test
    void une_categorie_inconnue_est_creee_et_ajoutee_a_l_ordre(@TempDir Path tmp) throws Exception {
        File dir = seedProject(tmp);

        CapturedItemWriter.Result res = CapturedItemWriter.addItem(
                dir, "potion", 10, "Consommables", "item: p\n", Map.of("material", "POTION"));

        assertTrue(res.ok());
        File rowFile = new File(dir, "data/categories/consommables.yaml");
        assertTrue(rowFile.isFile(), "la catégorie doit être créée");
        Map<String, Object> row = readYaml(rowFile);
        assertEquals("Consommables", row.get("name"));

        Map<String, Object> meta = readYaml(new File(dir, "data/categories/_source.yaml"));
        assertEquals(List.of("weapons", "consommables"), meta.get("order"));
    }

    @Test
    void sans_categorie_l_article_va_dans_la_premiere(@TempDir Path tmp) throws Exception {
        File dir = seedProject(tmp);

        CapturedItemWriter.Result res = CapturedItemWriter.addItem(
                dir, "truc", 5, null, "item: t\n", Map.of("material", "STONE"));

        assertTrue(res.ok());
        Map<String, Object> row = readYaml(new File(dir, "data/categories/weapons.yaml"));
        assertEquals(1, ((List<?>) row.get("items")).size());
    }

    @Test
    void la_categorie_se_retrouve_par_id_autant_que_par_nom(@TempDir Path tmp) throws Exception {
        File dir = seedProject(tmp);

        CapturedItemWriter.addItem(dir, "a", 1, "weapons", "item: a\n", Map.of());

        Map<String, Object> row = readYaml(new File(dir, "data/categories/weapons.yaml"));
        assertEquals(1, ((List<?>) row.get("items")).size(), "matché par id");
        // Aucune catégorie parasite n'a été créée.
        assertFalse(new File(dir, "data/categories/weapons_1.yaml").exists());
    }

    @Test
    void sans_table_declaree_l_ecriture_est_refusee_proprement(@TempDir Path tmp) throws Exception {
        File dir = tmp.resolve("vide").toFile();
        dir.mkdirs();

        CapturedItemWriter.Result res = CapturedItemWriter.addItem(
                dir, "x", 1, null, "item: x\n", Map.of());

        // Pas de catalogue = rien à muter. On le dit, on n'invente pas un project.
        assertFalse(res.ok());
        assertNotNull(res.error());
    }
}
