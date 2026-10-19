package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class ManifestCleanerTest {

    @Test
    void removesTheLegacyProjectIdLine(@TempDir File dir) throws IOException {
        File manifest = new File(dir, "manifest.yaml");
        Files.writeString(manifest.toPath(),
                "schema: manifest/v1\nproject_id: boutique_shop\nmodule: boutique\ndefault_lang: fr\n");

        assertTrue(ManifestCleaner.removeLegacyProjectId(dir));

        assertEquals("schema: manifest/v1\nmodule: boutique\ndefault_lang: fr\n", Files.readString(manifest.toPath()));
        assertFalse(ManifestCleaner.removeLegacyProjectId(dir), "second run changes nothing");
    }

    @Test
    void keepsNestedKeysAndLastLineWithoutNewline(@TempDir File dir) throws IOException {
        File manifest = new File(dir, "manifest.yaml");
        Files.writeString(manifest.toPath(), "schema: manifest/v1\nfiles:\n  project_id: x\nproject_id: boutique_shop");

        assertTrue(ManifestCleaner.removeLegacyProjectId(dir));

        assertEquals("schema: manifest/v1\nfiles:\n  project_id: x\n", Files.readString(manifest.toPath()));
    }

    @Test
    void noManifest_isANoOp(@TempDir File dir) throws IOException {
        assertFalse(ManifestCleaner.removeLegacyProjectId(dir));
    }
}
