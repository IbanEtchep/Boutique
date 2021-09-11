package fr.iban.boutique.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import fr.iban.boutique.BoutiquePlugin;
import fr.iban.boutique.ShopManager;
import fr.iban.boutique.menus.ShopCategoryListMenu;
import fr.iban.menuapi.utils.HexColor;

public class BoutiqueCMD implements CommandExecutor {

	private final BoutiquePlugin plugin;
	private ShopManager manager;

	public BoutiqueCMD(BoutiquePlugin plugin) {
		this.manager = plugin.getShopManager();
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
			@NotNull String[] args) {
		if(sender instanceof Player) {
			Player player = (Player)sender;
			if(args.length == 0) {
				new ShopCategoryListMenu(player, plugin).open();
			}else if(args.length >= 1 && player.hasPermission("boutique.manage")) {
				if(args[0].equalsIgnoreCase("createCategory") && args.length == 2) {
					String name = args[1];
					manager.addCategory(name);
					player.sendMessage("Une catégorie au nom de " + HexColor.translateColorCodes(name) + "§r a été crée, veuillez l'éditer dans la config.");
				}
			}
		}
		return false;
	}

}
