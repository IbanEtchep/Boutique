package fr.iban.boutique.menus;

import fr.iban.boutique.ShopCategory;
import fr.iban.boutique.ShopItem;
import fr.iban.boutique.ShopPlugin;
import fr.iban.boutique.manager.ShopManager;
import fr.iban.menuapi.MenuAPI;
import fr.iban.menuapi.menu.ConfigurableMenu;
import fr.iban.menuapi.menu.ConfirmMenu;
import fr.iban.menuapi.menuitem.ConfigurableItem;
import fr.iban.menuapi.menuitem.MenuItem;
import fr.iban.menuapi.utils.HexColor;
import fr.iban.menuapi.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ShopCategoryMenu extends ConfigurableMenu<ShopItem> {

	private final ShopPlugin plugin;
	private ShopCategory category;
	private ShopManager manager;

	public ShopCategoryMenu(Player player, ShopPlugin plugin, ShopCategory category, ShopCategoryListMenu previousMenu) {
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
		return new ConfigurableItem(object.getDisplay());
	}

	@Override
	protected void setItemDisplay(ShopItem item, ConfigurableItem display) {
		item.setDisplay(display);
		manager.saveShopItem(item, category);
	}

	@Override
	protected MenuItem getMenuItem(ShopItem item) {
		ConfigurableItem configurableItem = new ConfigurableItem(getConfigurableItem(item));
		for (int i = 0; i < configurableItem.getLore().size() ; i++) {
			String line = configurableItem.getLore().get(i);
			if(line.contains("%price_display%")){
				if(item.getPriceModifier() >= 1){
					line = line.replace("%price_display%", plugin.getConfig().getString("placeholders.price-display"));
				}else{
					line = line.replace("%price_display%", plugin.getConfig().getString("placeholders.promo-price-display"));
					line = line.replace("%old_price%", item.getPrice()+"");
					line = line.replace("%discount_percent%", (int)(100-item.getPriceModifier()*100.0)+"");
				}
				line = line.replace("%price%", (int)(item.getPrice()*item.getPriceModifier())+"");
				configurableItem.getLore().set(i, line);
			}
		}
		return configurableItem.setClickCallback(e -> {
			new ConfirmMenu(player, String.format("§2§lConfirmer", "§aVoulez-vous vraiment acheter " + item.getDisplay().getName() + " §apour " + item.getPrice() + " %s ?",plugin.getConfig().getString("messages.currency-name")) ,
					confirmed -> {
						if(confirmed){
							player.closeInventory();
							plugin.getTransactionManager().buy(player, item);
						}else{
							this.open();
						}
					}).open();
		});
	}

	@Override
	public void setMenuItems() {
		for(Map.Entry<Integer, ConfigurableItem> entry : MenuAPI.getInstance().getTemplateManager().getTemplate("boutique").getDisplays().entrySet()){
			setMenuTemplateItem(entry.getValue());
		}
		addMenuBottons();
		addItemAsync(new MenuItem(4, new ItemBuilder(Material.RAW_GOLD).setDisplayName("§cChargement...").build()), getTokensItem());
		super.setMenuItems();
	}

	@Override
	protected void addItem(ConfigurableItem display) {
		manager.addShopItem(category, display);
	}

	@Override
	protected void removeItem(ShopItem item) {
		manager.deleteShopItem(item, category);
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
								.addLore(String.format("§d§lVos %s : §f§l" + plugin.getDatabaseManager().getTokens(player.getName()), plugin.getConfig().getString("messages.currency-name")))
								.addLore("")
								.addLore(String.format("§7§lCliquez-ici pour acheter des %s.", plugin.getConfig().getString("messages.currency-name")))
								.build()).setClickCallback(e -> {
					e.getWhoClicked().sendMessage(String.format("§5§l> §dCliquez sur le lien : %s", plugin.getConfig().getString("messages.buy-link")));
				})
		);
	}
}
