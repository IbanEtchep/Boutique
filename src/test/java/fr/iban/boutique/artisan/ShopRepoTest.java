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

    // ── declared-model Table format (ADR composable-data-models §8) ─────────

    private static final String ROW_ARMES = """
            id: armes
            name: sources.categories.armes.name
            icon: DIAMOND_SWORD
            discount: 10
            items:
              - id: epee
                name: sources.categories.armes.items.name
                icon: "item:ci_epee"
                price: 100
                discount: 0
                lore: [sources.categories.armes.items.lore, sources.categories.armes.items.lore_2]
                actions: 'give_item material=DIAMOND_SWORD count=1'
            """;

    @Test void loadFromDataTableResolvesLtextKeysAgainstTheLangMap() {
        ShopRepo repo = new ShopRepo();
        repo.loadFromDataTable(
                path -> path.equals("armes.yaml") ? ROW_ARMES : null,
                java.util.List.of("armes.yaml"),
                java.util.Map.of(
                        "sources.categories.armes.name", "Armes",
                        "sources.categories.armes.items.name", "Épée légendaire",
                        "sources.categories.armes.items.lore", "Tranchante"),
                5);
        assertEquals(5, repo.wholeShopDiscount());
        assertEquals(1, repo.categories().size());
        var cat = repo.categories().get(0);
        assertEquals("Armes", cat.getName());
        assertEquals(10, cat.getDiscount());
        var item = repo.findItem("epee").orElseThrow();
        assertEquals("Épée légendaire", item.getName());
        assertEquals("item:ci_epee", item.getIcon());
        assertEquals(100.0, item.getPrice());
        // Missing translation falls back to the raw key (non-hermetic).
        assertEquals(java.util.List.of("Tranchante", "sources.categories.armes.items.lore_2"), item.getLore());
        assertEquals("give_item material=DIAMOND_SWORD count=1", item.getActions());
    }
}
