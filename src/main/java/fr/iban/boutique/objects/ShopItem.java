package fr.iban.boutique.objects;

import java.util.List;

import fr.iban.menuapi.ConfigurableItem;

public class ShopItem {
	
	private int id;
	private int price;
	private int discount;
	private ConfigurableItem display;
	private List<String> buyCommands;
	
	public ShopItem(int id) {
		this.id = id;
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
}
