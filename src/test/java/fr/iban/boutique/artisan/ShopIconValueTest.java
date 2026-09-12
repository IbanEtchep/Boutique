package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ShopIconValueTest {
    @Test void translatesNestedIconTextWithoutChangingItemReferences() {
        var yaml = """
            id: weapons
            items:
              - id: sword
                icon:
                  extends: {material: 'item:original', title: Original}
                  lore: ['{base.lore}', Offer]
                price: 10
            """;
        var repo = new ShopRepo();
        repo.loadFromDataTable(path -> yaml, List.of("weapons.yaml"), Map.of(
            "sources.categories.weapons.items.icon.extends.title", "Original traduit",
            "sources.categories.weapons.items.icon.lore_2", "Offre"), 0);
        var icon = (Map<?, ?>) repo.findItem("sword").orElseThrow().getIcon();
        assertEquals(Map.of("material", "item:original", "title", "Original traduit"), icon.get("extends"));
        assertEquals(List.of("{base.lore}", "Offre"), icon.get("lore"));
    }

    @Test void preservesStructuredIconsForCategoriesAndProducts() {
        var yaml = """
            id: weapons
            name: Weapons
            icon: {material: CHEST, count: 2}
            items:
              - id: sword
                name: Sword
                icon: {material: DIAMOND_SWORD, count: 3, enchants: {sharpness: 5}}
                price: 10
            """;
        var repo = new ShopRepo();
        repo.loadFromDataTable(path -> yaml, List.of("weapons.yaml"), Map.of(), 0);
        assertEquals(Map.of("material", "CHEST", "count", 2), repo.categories().getFirst().getIcon());
        assertEquals(Map.of("material", "DIAMOND_SWORD", "count", 3, "enchants", Map.of("sharpness", 5)),
            repo.findItem("sword").orElseThrow().getIcon());
    }
}
