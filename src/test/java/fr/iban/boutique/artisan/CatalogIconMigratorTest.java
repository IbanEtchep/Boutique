package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CatalogIconMigratorTest {
 @TempDir Path root;
 @Test void migratesDiskAndTranslationsOnlyOnce() throws Exception {
  Path project = root.resolve("editor");
  Files.createDirectories(project.resolve("data/categories"));
  Files.createDirectories(project.resolve("lang"));
  Path row = project.resolve("data/categories/weapons.yaml");
  String original = "id: weapons\nitems:\n- id: sword\n  icon: {material: 'item:sword', title: Original}\n  lore: [Rare]\n  price: 10\n- id: plain\n  icon: STONE\n  lore: []\n";
  Files.writeString(row, original);
  Files.writeString(project.resolve("lang/fr.yaml"), "sources.categories.weapons.items.lore: Rare traduit\nsources.categories.weapons.items.icon.title: Original traduit\n");
  assertTrue(CatalogIconMigrator.migrateIfNeeded(project.toFile()));
  Map<?,?> parsed = new Yaml().load(Files.readString(row));
  List<?> items = (List<?>)parsed.get("items");
  Map<?,?> item = (Map<?,?>)items.get(0);
  assertFalse(item.containsKey("lore"));
  Map<?,?> icon = (Map<?,?>)item.get("icon");
  assertEquals(List.of("Rare"), icon.get("lore"));
  assertFalse(icon.containsKey("lore_mode"));
  assertEquals(Map.of("material", "item:sword", "title", "Original"), icon.get("extends"));
  assertEquals(Map.of("id", "plain", "icon", "STONE"), items.get(1));
  Map<?,?> lang = new Yaml().load(Files.readString(project.resolve("lang/fr.yaml")));
  assertEquals("Rare traduit", lang.get("sources.categories.weapons.items.icon.lore"));
  assertEquals("Original traduit", lang.get("sources.categories.weapons.items.icon.extends.title"));
  String once = Files.readString(row);
  assertFalse(CatalogIconMigrator.migrateIfNeeded(project.toFile()));
  assertEquals(once, Files.readString(row));
  assertEquals(original, Files.readString(root.resolve("catalog-icon-backup/data/categories/weapons.yaml")));
 }
}
