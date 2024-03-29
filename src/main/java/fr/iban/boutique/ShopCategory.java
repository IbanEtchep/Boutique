package fr.iban.boutique;

import java.util.HashMap;

import fr.iban.menuapi.menuitem.ConfigurableItem;

public class ShopCategory {
	
	private final int id;
	private final HashMap<Integer, ShopItem> shopitems = new HashMap<>();
	private ConfigurableItem display;
	private int discount = 0;
	
	public ShopCategory(int id) {
		this.id = id;
	}
	
	public HashMap<Integer, ShopItem> getShopitems() {
		return shopitems;
	}
	
	public int getId() {
		return id;
	}
	
	public ConfigurableItem getDisplay() {
		return display;
	}
	
	public void setDisplay(ConfigurableItem display) {
		this.display = display;
	}

	public int getDiscount() {
		return discount;
	}
	
	public void setDiscount(int discount) {
		this.discount = discount;
	}

}
