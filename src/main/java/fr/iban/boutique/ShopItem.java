package fr.iban.boutique;

import java.util.List;

public final class ShopItem {

    private final String id;
    private final String name;
    private final Object icon;
    private final double price;
    private final int discount;
    private final List<String> lore;
    /** Actions d'achat en **Action DSL** (le wire du champ `actions`, texte littéral — cf. ADR action-dsl). */
    private final String actions;
    private final ShopCategory category;

    public ShopItem(String id, String name, Object icon, double price, int discount,
                     List<String> lore, String actions, ShopCategory category) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.price = price;
        this.discount = discount;
        this.lore = lore;
        this.actions = actions;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Object getIcon() {
        return icon;
    }

    public double getPrice() {
        return price;
    }

    public int getDiscount() {
        return discount;
    }

    public List<String> getLore() {
        return lore;
    }

    public String getActions() {
        return actions;
    }

    public ShopCategory getCategory() {
        return category;
    }

    public double finalPrice(int wholeShopDiscount) {
        double mod = 1.0 - (wholeShopDiscount + category.getDiscount() + discount) / 100.0;
        return Math.max(0, price * mod);
    }
}
