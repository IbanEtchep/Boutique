package fr.iban.boutique.artisan;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ShopRepoTest {
    private static final String YAML = String.join("\n",
        "schema: boutique/v1",
        "whole_shop_discount: 5",
        "categories:",
        "  - id: weapons",
        "    name: Armes",
        "    icon: DIAMOND_SWORD",
        "    discount: 10",
        "    items:",
        "      - id: diamond_sword",
        "        name: Épée",
        "        icon: DIAMOND_SWORD",
        "        price: 100",
        "        discount: 5",
        "        buy_commands: [\"give %player% diamond_sword 1\"]",
        "");

    @Test void parsesCategoriesItemsAndDiscounts() {
        ShopRepo repo = new ShopRepo();
        repo.load(path -> YAML, List.of("shop.yaml"));
        assertEquals(1, repo.categories().size());
        var item = repo.findItem("diamond_sword").orElseThrow();
        assertEquals("Épée", item.getName());
        assertEquals(80.0, item.finalPrice(repo.wholeShopDiscount()), 1e-9); // 100*(1-0.20)
    }

    @Test void migratesLegacyBuyCommandsIntoActionDsl() {
        ShopRepo repo = new ShopRepo();
        repo.load(path -> YAML, List.of("shop.yaml"));
        String actions = repo.findItem("diamond_sword").orElseThrow().getActions();
        assertEquals("run_command \"give {player} diamond_sword 1\" as console", actions);
    }

    @Test void nativeActionsWinOverLegacyBuyCommands() {
        String yaml = YAML.replace(
            "        buy_commands: [\"give %player% diamond_sword 1\"]",
            String.join("\n",
                "        buy_commands: [\"ignored\"]",
                "        actions: 'send_message \"Merci !\"'"));
        ShopRepo repo = new ShopRepo();
        repo.load(path -> yaml, List.of("shop.yaml"));
        String actions = repo.findItem("diamond_sword").orElseThrow().getActions();
        assertEquals("send_message \"Merci !\"", actions);
    }

    @Test void unknownSchemaOrMalformedYamlYieldsEmptyState() {
        ShopRepo repo = new ShopRepo();
        repo.load(path -> "schema: nope/v9", List.of("shop.yaml"));
        assertTrue(repo.categories().isEmpty());
        repo.load(path -> "{{{not yaml", List.of("shop.yaml"));
        assertTrue(repo.categories().isEmpty());
    }

    @Test void findItemAcrossMultipleFiles() {
        ShopRepo repo = new ShopRepo();
        repo.load(path -> YAML, List.of("a.yaml", "b.yaml")); // ids dupliqués: dernier gagne, pas de crash
        assertTrue(repo.findItem("diamond_sword").isPresent());
    }
}
