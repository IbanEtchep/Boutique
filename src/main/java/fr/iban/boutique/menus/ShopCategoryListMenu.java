package fr.iban.boutique.menus;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import fr.iban.boutique.BoutiquePlugin;
import fr.iban.menuapi.MenuAPI;
import fr.iban.menuapi.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import fr.iban.boutique.ShopManager;
import fr.iban.boutique.objects.ShopCategory;
import fr.iban.menuapi.MenuItem;
import fr.iban.menuapi.menu.ConfigurableMenu;
import fr.iban.menuapi.ConfigurableItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.inventory.ItemStack;

public class ShopCategoryListMenu extends ConfigurableMenu<ShopCategory> {

	private final BoutiquePlugin plugin;
	private ShopManager manager;

	public ShopCategoryListMenu(Player player, BoutiquePlugin plugin) {
		super(player);
		this.manager = plugin.getShopManager();
		this.plugin = plugin;
	}

	@Override
	public String getMenuName() {
		return centerTitle(ChatColor.of("#be2edd") + "§lBoutique");
	}

	@Override
	public int getRows() {
		return 6;
	}

	@Override
	protected Collection<ShopCategory> getItems() {
		return manager.getCategories().values();
	}

	@Override
	protected ConfigurableItem getConfigurableItem(ShopCategory category) {
		return category.getDisplay();
	}

	@Override
	protected void setItemDisplay(ShopCategory category, ConfigurableItem display) {
		category.setDisplay(display);
		manager.saveShopCategory(category);
	}

	@Override
	protected MenuItem getMenuItem(ShopCategory category) {
		return new MenuItem(getConfigurableItem(category), event -> {
			new ShopCategoryMenu(player, plugin, category, this).open();
		});
	}
	
	@Override
	protected void addItem(ConfigurableItem display) {
		manager.addCategory(display);
	}

	@Override
	protected void removeItem(ShopCategory item) {
		manager.deleteShopCategory(item);
	}

	@Override
	public void setMenuTemplateItems() {
		for(Map.Entry<Integer, ConfigurableItem> entry : MenuAPI.getInstance().getTemplateManager().getTemplate("boutique").getDisplays().entrySet()){
			setMenuTemplateItem(new MenuItem(entry.getValue()));
		}
		addItemAsync(new MenuItem(4, new ItemBuilder(Material.RAW_GOLD).setDisplayName("§cChargement...").build()), getTokensItem());
	}

	private CompletableFuture<MenuItem> getTokensItem(){
		return CompletableFuture.supplyAsync(() -> new MenuItem(4, new ItemBuilder(Material.RAW_GOLD).setDisplayName("Primals : " + plugin.getSqlStorage().getTokens(player)).build()));
	}
}
