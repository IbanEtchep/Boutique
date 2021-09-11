package fr.iban.boutique.menus;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import fr.iban.boutique.BoutiquePlugin;
import fr.iban.boutique.objects.ShopCategory;
import fr.iban.menuapi.MenuAPI;
import fr.iban.menuapi.menu.ConfirmMenu;
import fr.iban.menuapi.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import fr.iban.boutique.ShopManager;
import fr.iban.boutique.objects.ShopItem;
import fr.iban.menuapi.MenuItem;
import fr.iban.menuapi.menu.ConfigurableMenu;
import fr.iban.menuapi.ConfigurableItem;
import fr.iban.menuapi.utils.HexColor;

public class ShopCategoryMenu extends ConfigurableMenu<ShopItem> {

	private final BoutiquePlugin plugin;
	private ShopCategory category;
	private ShopManager manager;

	public ShopCategoryMenu(Player player, BoutiquePlugin plugin, ShopCategory category, ShopCategoryListMenu previousMenu) {
		super(player);
		this.manager = plugin.getShopManager();
		this.plugin = plugin;
		this.category = category;
		this.previousMenu = previousMenu;
	}

	@Override
	public String getMenuName() {
		return centerTitle(HexColor.translateColorCodes(category.getDisplay().getName()));
	}

	@Override
	public int getRows() {
		return 6;
	}

	@Override
	protected Collection<ShopItem> getItems() {
		return category.getShopitems().values();
	}

	@Override
	protected ConfigurableItem getConfigurableItem(ShopItem object) {
		return object.getDisplay();
	}

	@Override
	protected void setItemDisplay(ShopItem item, ConfigurableItem display) {
		item.setDisplay(display);
		manager.saveShopItem(item, category);
	}

	@Override
	protected MenuItem getMenuItem(ShopItem item) {
		return new MenuItem(getConfigurableItem(item), event -> {
			new ConfirmMenu(player, "§2§lConfirmer", "§aVoulez-vous vraiment acheter " + item.getDisplay() + " §apour " + item.getPrice() + " Primals ?" ,
					confirmed -> {
				if(confirmed){
					if(!item.getBuyCommands().isEmpty()){
						for(String command : item.getBuyCommands()){
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
						}
					}
				}else{
					this.open();
				}
			}).open();
		});
	}

	@Override
	public void setMenuTemplateItems() {
		for(Map.Entry<Integer, ConfigurableItem> entry : MenuAPI.getInstance().getTemplateManager().getTemplate("boutique").getDisplays().entrySet()){
			setMenuTemplateItem(new MenuItem(entry.getValue()));
		}
		addMenuBottons();
		addItemAsync(new MenuItem(4, new ItemBuilder(Material.RAW_GOLD).setDisplayName("§cChargement...").build()), getTokensItem());
	}

	@Override
	protected void addItem(ConfigurableItem display) {
		manager.addShopItem(category, display);
	}

	@Override
	protected void removeItem(ShopItem item) {
		manager.deleteShopItem(item, category);
	}

	private CompletableFuture<MenuItem> getTokensItem(){
		return CompletableFuture.supplyAsync(() -> new MenuItem(4, new ItemBuilder(Material.RAW_GOLD).setDisplayName("Primals : " + plugin.getSqlStorage().getTokens(player)).build()));
	}
}
