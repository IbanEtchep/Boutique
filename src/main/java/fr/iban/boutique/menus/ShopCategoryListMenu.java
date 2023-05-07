package fr.iban.boutique.menus;

import fr.iban.boutique.ShopCategory;
import fr.iban.boutique.ShopPlugin;
import fr.iban.boutique.manager.ShopManager;
import fr.iban.menuapi.MenuAPI;
import fr.iban.menuapi.menu.ConfigurableMenu;
import fr.iban.menuapi.menuitem.ConfigurableItem;
import fr.iban.menuapi.menuitem.MenuItem;
import fr.iban.menuapi.utils.ItemBuilder;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ShopCategoryListMenu extends ConfigurableMenu<ShopCategory> {

	private final ShopPlugin plugin;
	private ShopManager manager;

	public ShopCategoryListMenu(Player player, ShopPlugin plugin) {
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
		return new ConfigurableItem(category.getDisplay());
	}

	@Override
	protected void setItemDisplay(ShopCategory category, ConfigurableItem display) {
		category.setDisplay(display);
		manager.saveShopCategory(category);
	}

	@Override
	protected MenuItem getMenuItem(ShopCategory category) {
		return getConfigurableItem(category).setClickCallback(e -> {
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
	public void setMenuItems() {
		for(Map.Entry<Integer, ConfigurableItem> entry : MenuAPI.getInstance().getTemplateManager().getTemplate("boutique").getDisplays().entrySet()){
			setMenuTemplateItem(entry.getValue());
		}
		addItemAsync(new MenuItem(4, new ItemBuilder(Material.RAW_GOLD).setDisplayName("§cChargement...").build()), getTokensItem());
		super.setMenuItems();
	}

	private CompletableFuture<MenuItem> getTokensItem() {
		return CompletableFuture.supplyAsync(() -> new MenuItem(4,
						new ItemBuilder(Material.RAW_GOLD)
								.setDisplayName("§5§lInformations")
								.addLore("")
								.addLore("§7Cette boutique est à votre disposition")
								.addLore("§7pour vous permettre de soutenir le serveur")
								.addLore("§7dans le but de couvrir les frais engendrés")
								.addLore("§7par le serveur et nous aider à le développer.")
								.addLore("")
								.addLore(String.format("§d§lVos %s : §f§l" + plugin.getDatabaseManager().getTokens(player.getUniqueId()), plugin.getConfig().getString("messages.currency-name")))
								.addLore("")
								.addLore(String.format("§7§lCliquez-ici pour acheter des %s.", plugin.getConfig().getString("messages.currency-name")))
								.build()).setClickCallback(e -> {
					e.getWhoClicked().sendMessage(String.format("§5§l> §dCliquez sur le lien : %s", plugin.getConfig().getString("messages.buy-link")));
				})
		);
	}
}
