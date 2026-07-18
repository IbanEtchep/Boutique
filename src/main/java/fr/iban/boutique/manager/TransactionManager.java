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
        if (item.getActions().isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-buy-commands")));
            return;
        }
        databaseManager.getTokensAsync(player.getUniqueId()).thenAccept(money -> Bukkit.getScheduler().runTask(plugin, () -> {
            int price = (int) Math.round(item.finalPrice(wholeShopDiscount));
            if (money >= price) {
                // L'arbre de steps de l'item (conditions, messages, commandes…)
                // s'exécute via le moteur d'actions Artisan, main-thread.
                plugin.getArtisanApi().getActions().execute(player, item.getActions(), java.util.Map.of());
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
