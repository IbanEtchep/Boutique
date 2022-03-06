package fr.iban.boutique.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import fr.iban.boutique.ShopPlugin;
import fr.iban.boutique.manager.ShopManager;
import fr.iban.boutique.menus.ShopCategoryListMenu;
import fr.iban.menuapi.utils.HexColor;

public class BoutiqueCMD implements CommandExecutor {

	private final ShopPlugin plugin;
	private ShopManager manager;
	private String currency;

	public BoutiqueCMD(ShopPlugin plugin) {
		this.manager = plugin.getShopManager();
		this.plugin = plugin;
		currency = plugin.getConfig().getString("messages.currency-name");
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
			@NotNull String[] args) {
		if(sender instanceof Player) {
			Player player = (Player)sender;
			if(args.length == 0) {
				new ShopCategoryListMenu(player, plugin).open();
			}else if(args.length >= 1) {
				if(args[0].equalsIgnoreCase("createCategory") && player.hasPermission("boutique.manage") && args.length == 2) {
					String name = args[1];
					manager.addCategory(name);
					player.sendMessage("Une catégorie au nom de " + HexColor.translateColorCodes(name) + "§r a été crée, veuillez l'éditer dans la config.");
				}
				if(args.length == 3){
					if(args[0].equalsIgnoreCase("give"+currency)){
						try{
							int amount = Integer.parseInt(args[2]);
							Player to = Bukkit.getPlayer(args[1]);
							if(to != null){
								plugin.getDatabaseManager().getTokensAsync(player.getName()).thenAccept(tokens -> {
									if(tokens > amount){
										plugin.getDatabaseManager().removeTokens(player.getName(), amount);
										plugin.getDatabaseManager().addTokens(to.getName(), amount);
										player.sendMessage("§aVous avez envoyé " + amount + " " + currency + " à " + to.getName() + ".");
										to.sendMessage("§a"+ player.getName() + " vous a envoyé " + amount + " " + currency + ".");
										plugin.getLogger().info(player.getName() + " a envoyé " + amount + " " + currency + " à " + to.getName() + "." );
										Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
											plugin.getTokensCache().put(player.getUniqueId(), plugin.getDatabaseManager().getTokens(player.getName()));
											plugin.getTokensCache().put(to.getUniqueId(), plugin.getDatabaseManager().getTokens(to.getName()));
										});
									}else{
										player.sendMessage("§cIl vous manque " + (amount-tokens) + " " + currency + ".");
									}
								});
							}else{
								player.sendMessage("§c"+args[1]+" n'est pas en ligne.");
							}
						}catch (NumberFormatException e){
							player.sendMessage("§cLe nombre doit être un nombre entier !");
						}
					}
				}
			}
		}
		return false;
	}
}
