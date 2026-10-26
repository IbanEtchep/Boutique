package net.artisanmc.store.artisan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class MigrationsTest {
    @TempDir File dataFolder;

    @Test
    void aMigrationNotedIsDoneAtTheNextStart() throws Exception {
        Migrations first = new Migrations(dataFolder);
        assertFalse(first.isDone("catalog-icons"));
        first.markDone("catalog-icons");
        first.markDone("settings-placeholders");
        assertTrue(first.isDone("catalog-icons"));
        assertEquals("catalog-icons\nsettings-placeholders\n", Files.readString(new File(dataFolder, "migrations.done").toPath()));

        Migrations next = new Migrations(dataFolder);
        assertTrue(next.isDone("catalog-icons"));
        assertTrue(next.isDone("settings-placeholders"));
        assertFalse(next.isDone("something-else"));
    }

    @Test
    void anUnreadableFileMeansNothingDone() throws Exception {
        new File(dataFolder, "migrations.done").mkdir();
        assertFalse(new Migrations(dataFolder).isDone("catalog-icons"));
    }
}
