package fr.iban.boutique.manager;

import fr.iban.boutique.ShopItem;
import fr.iban.boutique.ShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class TransactionManager {

    private final ShopPlugin plugin;
    private final DatabaseManager databaseManager;

    public TransactionManager(ShopPlugin shopPlugin) {
        this.plugin = shopPlugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    public void buy(Player player, ShopItem item, int wholeShopDiscount) {
        if (item.getBuyCommands().isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-buy-commands")));
            return;
        }
        databaseManager.getTokensAsync(player.getUniqueId()).thenAccept(money -> Bukkit.getScheduler().runTask(plugin, () -> {
            int price = (int) Math.round(item.finalPrice(wholeShopDiscount));
            if (money >= price) {
                for (String command : item.getBuyCommands()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
                }
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.buy-success")));
                databaseManager.addPurchaseHistory(player.getUniqueId(), item);
                databaseManager.removeTokens(player.getUniqueId(), price);
                Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    plugin.getTokensCache().put(player.getUniqueId(), plugin.getDatabaseManager().getTokens(player.getUniqueId()));
                });
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.not-enough-money")));
            }
        }));
    }

}
