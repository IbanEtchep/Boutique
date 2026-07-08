package fr.iban.boutique;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopItemTest {
    private ShopItem item(int catDiscount, int itemDiscount, double price) {
        ShopCategory cat = new ShopCategory("weapons", "Armes", "DIAMOND_SWORD", catDiscount, new java.util.ArrayList<>());
        return new ShopItem("sword", "Épée", "DIAMOND_SWORD", price, itemDiscount,
                List.of(), List.of("give %player% diamond_sword 1"), cat);
    }

    @Test void finalPriceStacksWholeShopCategoryAndItemDiscounts() {
        assertEquals(60.0, item(20, 10, 100).finalPrice(10), 1e-9); // 100 * (1 - 0.40)
    }

    @Test void finalPriceNeverNegative() {
        assertEquals(0.0, item(80, 80, 100).finalPrice(80), 1e-9);
    }
}
