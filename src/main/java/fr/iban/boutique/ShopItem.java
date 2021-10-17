package fr.iban.boutique;

import java.util.List;

import fr.iban.menuapi.ConfigurableItem;

public class ShopItem {
	
	private int id;
	private int price;
	private int discount;
	private ConfigurableItem display;
	private List<String> buyCommands;
	private ShopCategory category;
	
	public ShopItem(int id, ShopCategory category) {
		this.id = id;
		this.category = category;
	}
	
	public int getId() {
		return id;
	}
	
	public int getPrice() {
		return price;
	}
	
	public void setPrice(int price) {
		this.price = price;
	}
	
	public int getDiscount() {
		return discount;
	}
	
	public void setDiscount(int discount) {
		this.discount = discount;
	}
	
	public ConfigurableItem getDisplay() {
		return display;
	}
	
	public void setDisplay(ConfigurableItem display) {
		this.display = display;
	}
	
	public List<String> getBuyCommands() {
		return buyCommands;
	}
	
	public void setBuyCommands(List<String> buyCommands) {
		this.buyCommands = buyCommands;
	}

	public void setCategory(ShopCategory category) {
		this.category = category;
	}

	public ShopCategory getCategory() {
		return category;
	}

	public double getPriceModifier() {
		int wholeShopDiscount = ShopPlugin.getInstance().getConfig().getInt("whole-shop-discount");
		return 1.0-((wholeShopDiscount + getCategory().getDiscount() + getDiscount())/100.0);
	}
}
