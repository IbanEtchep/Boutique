package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Project lang files are YAML TREES (each `.` of the key is a level); the
 * runtime addresses them by dotted key. Mirror of Artisan's
 * `plugin/…/yaml/LangFlattener.kt` / `editor/src/lib/langTree.ts`, tolerant to
 * the legacy flat form and to any mix of the two.
 */
class LangFlattenerTest {

    private Map<String, String> flattenYaml(String src) {
        return LangFlattener.flatten(new Yaml().load(src));
    }

    @Test
    void readsANestedTreeAsDottedKeys() {
        assertEquals(
                Map.of("boutique.shop_main.title", "Boutique",
                        "sources.categories.armes.name", "Armes"),
                flattenYaml("""
                        boutique:
                          shop_main:
                            title: Boutique
                        sources:
                          categories:
                            armes:
                              name: Armes
                        """));
    }

    @Test
    void stillReadsTheLegacyFlatFormAndAMixOfBoth() {
        assertEquals(
                Map.of("boutique.a.title", "A", "boutique.b.title", "B", "other.k", "v"),
                flattenYaml("""
                        boutique.a.title: A
                        boutique:
                          b.title: B
                        other.k: v
                        """));
    }

    @Test
    void coercesNonStringScalarsAndTreatsNullAsTheEmptySentinel() {
        assertEquals(
                Map.of("a.n", "42", "a.b", "true", "a.empty", ""),
                flattenYaml("""
                        a:
                          n: 42
                          b: true
                          empty:
                        """));
    }

    @Test
    void skipsListsAndEmptyMaps() {
        assertEquals(
                Map.of("keep", "yes"),
                flattenYaml("""
                        a:
                          list: [x, y]
                          empty: {}
                        keep: "yes"
                        """));
    }

    @Test
    void toleratesNullAndNonMapRoots() {
        assertEquals(Map.of(), LangFlattener.flatten(null));
        assertEquals(Map.of(), LangFlattener.flatten("not a map"));
    }
}
