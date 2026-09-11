package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ShopIconValueTest {
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
