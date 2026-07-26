package fr.iban.boutique.artisan;

import net.artisanmc.modules.api.ModelRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The declared model/v1 documents (ADR composable-data-models §4): namespaced
 * ids, version, master-detail catalog hint, ltext name/lore, editable icon —
 * and they must be accepted verbatim by the core ModelRegistry.
 */
class ShopModelsTest {

    @Test
    void documentsDeclareCleanlyInTheCoreRegistry() {
        ModelRegistry registry = new ModelRegistry();
        registry.declare(ShopModels.item());
        registry.declare(ShopModels.category());
        assertTrue(registry.has("boutique:item"));
        assertTrue(registry.has("boutique:category"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void categoryIsAMasterDetailTableOfItems() {
        Map<String, Object> cat = ShopModels.category();
        assertEquals("model/v1", cat.get("schema"));
        assertEquals("boutique:category", cat.get("id"));
        assertEquals(ShopModels.VERSION, cat.get("version"));
        assertEquals(Boolean.TRUE, cat.get("master_detail"));
        assertEquals("id", cat.get("key_field"));

        List<Map<String, Object>> fields = (List<Map<String, Object>>) cat.get("fields");
        Map<String, Object> items = fields.stream()
                .filter(f -> f.get("name").equals("items")).findFirst().orElseThrow();
        Map<String, Object> itemsType = (Map<String, Object>) items.get("type");
        assertEquals("list", itemsType.get("kind"));
        assertEquals(Map.of("kind", "model", "ref", "boutique:item"), itemsType.get("of"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void itemHasLocalizedNameLoreAndLockedIcon() {
        Map<String, Object> item = ShopModels.item();
        List<Map<String, Object>> fields = (List<Map<String, Object>>) item.get("fields");

        Map<String, Object> name = fields.stream().filter(f -> f.get("name").equals("name")).findFirst().orElseThrow();
        assertEquals(Map.of("kind", "ltext"), name.get("type"));

        Map<String, Object> lore = fields.stream().filter(f -> f.get("name").equals("lore")).findFirst().orElseThrow();
        assertEquals(Map.of("kind", "list", "of", Map.of("kind", "ltext")), lore.get("type"));

        Map<String, Object> icon = fields.stream().filter(f -> f.get("name").equals("icon")).findFirst().orElseThrow();
        // Icon stays EDITABLE in the form (user feedback 2026-07-26) — additem
        // sets it, the admin may override it.
        assertNull(icon.get("locked"));

        Map<String, Object> price = fields.stream().filter(f -> f.get("name").equals("price")).findFirst().orElseThrow();
        assertEquals(0, price.get("min"));
    }
}
