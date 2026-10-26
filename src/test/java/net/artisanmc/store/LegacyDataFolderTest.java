package net.artisanmc.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class LegacyDataFolderTest {

    @Test
    void movesTheBoutiqueFolderWithItsContent(@TempDir File plugins) throws IOException {
        File legacy = new File(plugins, "Boutique");
        File current = new File(plugins, "ArtisanStore");
        Files.createDirectories(new File(legacy, "catalog-icon-backup").toPath());
        Files.writeString(new File(legacy, "config.yml").toPath(), "database:\n  host: db\n");
        Files.writeString(new File(legacy, "migrations.done").toPath(), "catalog-icons\n");

        assertEquals(LegacyDataFolder.Result.MOVED, LegacyDataFolder.migrate(legacy, current));

        assertFalse(legacy.exists());
        assertEquals("database:\n  host: db\n", Files.readString(new File(current, "config.yml").toPath()));
        assertTrue(new File(current, "migrations.done").isFile());
        assertTrue(new File(current, "catalog-icon-backup").isDirectory());
        assertEquals(LegacyDataFolder.Result.NONE, LegacyDataFolder.migrate(legacy, current), "second start changes nothing");
    }

    @Test
    void anEmptyNewFolderIsReplaced(@TempDir File plugins) throws IOException {
        File legacy = new File(plugins, "Boutique");
        File current = new File(plugins, "ArtisanStore");
        Files.createDirectories(legacy.toPath());
        Files.writeString(new File(legacy, "config.yml").toPath(), "x: 1\n");
        Files.createDirectories(current.toPath());

        assertEquals(LegacyDataFolder.Result.MOVED, LegacyDataFolder.migrate(legacy, current));
        assertTrue(new File(current, "config.yml").isFile());
    }

    @Test
    void neverOverwritesANewFolderThatHoldsFiles(@TempDir File plugins) throws IOException {
        File legacy = new File(plugins, "Boutique");
        File current = new File(plugins, "ArtisanStore");
        Files.createDirectories(legacy.toPath());
        Files.writeString(new File(legacy, "config.yml").toPath(), "old: 1\n");
        Files.createDirectories(current.toPath());
        Files.writeString(new File(current, "config.yml").toPath(), "new: 1\n");

        assertEquals(LegacyDataFolder.Result.BOTH, LegacyDataFolder.migrate(legacy, current));
        assertEquals("old: 1\n", Files.readString(new File(legacy, "config.yml").toPath()));
        assertEquals("new: 1\n", Files.readString(new File(current, "config.yml").toPath()));
    }

    @Test
    void freshInstall_isANoOp(@TempDir File plugins) throws IOException {
        assertEquals(LegacyDataFolder.Result.NONE,
                LegacyDataFolder.migrate(new File(plugins, "Boutique"), new File(plugins, "ArtisanStore")));
        assertFalse(new File(plugins, "ArtisanStore").exists());
    }
}
