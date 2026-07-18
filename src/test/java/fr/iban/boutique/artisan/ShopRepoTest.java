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

    @Test void migratesLegacyBuyCommandsIntoActionSteps() {
        ShopRepo repo = new ShopRepo();
        repo.load(path -> YAML, List.of("shop.yaml"));
        var actions = repo.findItem("diamond_sword").orElseThrow().getActions();
        assertEquals(1, actions.size());
        assertEquals("action", actions.get(0).get("kind"));
        @SuppressWarnings("unchecked")
        var action = (java.util.Map<String, Object>) actions.get(0).get("action");
        assertEquals("run_command", action.get("type"));
        assertEquals("give {player} diamond_sword 1", action.get("command"));
        assertEquals("console", action.get("as"));
    }

    @Test void nativeActionsWinOverLegacyBuyCommands() {
        String yaml = YAML.replace(
            "        buy_commands: [\"give %player% diamond_sword 1\"]",
            String.join("\n",
                "        buy_commands: [\"ignored\"]",
                "        actions:",
                "          - kind: action",
                "            action: {type: send_message, text: \"Merci !\"}"));
        ShopRepo repo = new ShopRepo();
        repo.load(path -> yaml, List.of("shop.yaml"));
        var actions = repo.findItem("diamond_sword").orElseThrow().getActions();
        assertEquals(1, actions.size());
        @SuppressWarnings("unchecked")
        var action = (java.util.Map<String, Object>) actions.get(0).get("action");
        assertEquals("send_message", action.get("type"));
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
