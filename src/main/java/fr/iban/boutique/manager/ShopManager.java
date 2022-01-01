package fr.iban.boutique.manager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import fr.iban.boutique.ShopCategory;
import fr.iban.boutique.ShopItem;
import fr.iban.boutique.ShopPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import fr.iban.menuapi.menuitem.ConfigurableItem;

public class ShopManager {
	
	private ShopPlugin plugin;
	private FileConfiguration config;
	private Map<Integer, ShopCategory> categories = new HashMap<>();

	public ShopManager(ShopPlugin plugin) {
		this.plugin = plugin;
		config = plugin.getConfig();
		loadShops();
	}
	
	public void loadShops() {
		plugin.getLogger().log(Level.INFO, "Chargement de la boutique...");
		for(String categoryID : config.getConfigurationSection("categories").getKeys(false)) {
			String catPath = "categories."+categoryID;
			ShopCategory category = new ShopCategory(Integer.parseInt(categoryID));
			category.setDiscount(config.getInt(catPath+".discount"));
			category.setDisplay((ConfigurableItem) config.get(catPath+".menuitem"));
			
			ConfigurationSection section = config.getConfigurationSection(catPath+".items");
			if(section != null) {
				for(String itemID : config.getConfigurationSection(catPath+".items").getKeys(false)) {
					String itemPath = catPath+".items."+itemID;
					ShopItem shopitem = new ShopItem(Integer.parseInt(itemID), category);
					shopitem.setDiscount(config.getInt(itemPath+".discount"));
					shopitem.setPrice(config.getInt(itemPath+".price"));
					shopitem.setBuyCommands(config.getStringList(itemPath+".buycommands"));
					shopitem.setDisplay((ConfigurableItem) config.get(itemPath+".menuitem"));
					shopitem.setCategory(category);
					category.getShopitems().put(shopitem.getId(), shopitem);
				}
			}
			categories.put(Integer.parseInt(categoryID), category);
		}
		plugin.getLogger().log(Level.INFO, "Chargement de la boutique terminé (" + categories.size() + " catégories).");
	}

	public void addCategory(String name) {
		ShopCategory category = new ShopCategory(generateID(categories.keySet()));
		ConfigurableItem display = new ConfigurableItem();
		display.setName(name);
		category.setDisplay(display);
		categories.put(category.getId(), category);
		saveShopCategory(category);
	}

	public void addCategory(ConfigurableItem display){
		ShopCategory category = new ShopCategory(generateID(categories.keySet()));
		category.setDisplay(display);
		categories.put(category.getId(), category);
		saveShopCategory(category);
	}
	
    public void saveShopCategory(ShopCategory category) {
    	String path = new StringBuilder("categories.").append(category.getId()).append(".").toString();
    	config.set(path+"menuitem", category.getDisplay());
    	config.set(path+"discount", category.getDiscount());
		plugin.saveConfig();
    }
    
    public void deleteShopCategory(ShopCategory category) {
		String path = new StringBuilder("categories.").append(category.getId()).toString();
		config.set(path, null);
		categories.remove(category);
		plugin.saveConfig();
    }


	public void addShopItem(ShopCategory category, ConfigurableItem display) {
		ShopItem item = new ShopItem(generateID(category.getShopitems().keySet()), category);
		item.setDisplay(display);
		category.getShopitems().put(item.getId(), item);
		saveShopItem(item, category);
	}

    public void addShopItem(ShopCategory category, String name) {
    	ShopItem item = new ShopItem(generateID(category.getShopitems().keySet()), category);
		ConfigurableItem display = new ConfigurableItem();
		display.setName(name);
		item.setDisplay(display);
		category.getShopitems().put(item.getId(), item);
		saveShopItem(item, category);
    }
    
    public void saveShopItem(ShopItem item, ShopCategory category) {
    	String path = new StringBuilder("categories.").append(category.getId()).append(".").append("items.").append(item.getId()).append(".").toString();
    	config.set(path+"menuitem", item.getDisplay());
    	config.set(path+"price", item.getPrice());
    	config.set(path+"discount", item.getDiscount());
    	config.set(path+"buycommands", item.getBuyCommands());
		plugin.saveConfig();
    }
    
    public void deleteShopItem(ShopItem item, ShopCategory category) {
    	config.set(new StringBuilder("categories.").append(category.getId()).append(".").append("items.").append(item.getId()).toString(), null);
    	category.getShopitems().remove(item);
		plugin.saveConfig();
	}
    
    public Map<Integer, ShopCategory> getCategories() {
		return categories;
	}

	public String getCurrencyName(){
		return plugin.getConfig().getString("messages.currency-name");
	}
    
    public int generateID(Collection<Integer> collection) {
    	return collection.isEmpty() ? 1 : Collections.max(collection) + 1;
    }
	
}
