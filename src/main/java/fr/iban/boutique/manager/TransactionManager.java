package fr.iban.boutique.manager;

import fr.iban.boutique.ShopItem;
import fr.iban.boutique.ShopPlugin;
import fr.iban.menuapi.utils.HexColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TransactionManager {

    private final ShopPlugin plugin;
    private DatabaseManager databaseManager;

    public TransactionManager(ShopPlugin shopPlugin) {
        this.plugin = shopPlugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    public void buy(Player player, ShopItem item){
        databaseManager.getTokensAsync(player).thenAccept(money -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                int price = (int)(item.getPrice() * item.getPriceModifier());
                if (money >= price) {
                    if (!item.getBuyCommands().isEmpty()) {
                        for (String command : item.getBuyCommands()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
                        }
                        player.sendMessage(HexColor.translateColorCodes(plugin.getConfig().getString("messages.buy-success")));
                        databaseManager.addPurchaseHistory(player, item);
                        databaseManager.removeTokens(player, price);
                    }
                } else {
                    player.sendMessage(HexColor.translateColorCodes(plugin.getConfig().getString("messages.not-enough-money")));
                }
            });
        });
    }

}
