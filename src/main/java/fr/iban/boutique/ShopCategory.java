package fr.iban.boutique;

import java.util.List;

public final class ShopCategory {

    private final String id;
    private final String name;
    private final Object icon;
    private final int discount;
    private final List<ShopItem> items;

    public ShopCategory(String id, String name, Object icon, int discount, List<ShopItem> items) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.discount = discount;
        this.items = items;
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

    public int getDiscount() {
        return discount;
    }

    public List<ShopItem> getItems() {
        return items;
    }
}
