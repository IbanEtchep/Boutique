package fr.iban.boutique;

import java.util.List;

public final class ShopItem {

    private final String id;
    private final String name;
    private final String icon;
    private final double price;
    private final int discount;
    private final List<String> lore;
    private final List<String> buyCommands;
    private final ShopCategory category;

    public ShopItem(String id, String name, String icon, double price, int discount,
                     List<String> lore, List<String> buyCommands, ShopCategory category) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.price = price;
        this.discount = discount;
        this.lore = lore;
        this.buyCommands = buyCommands;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
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

    public List<String> getBuyCommands() {
        return buyCommands;
    }

    public ShopCategory getCategory() {
        return category;
    }

    public double finalPrice(int wholeShopDiscount) {
        double mod = 1.0 - (wholeShopDiscount + category.getDiscount() + discount) / 100.0;
        return Math.max(0, price * mod);
    }
}
