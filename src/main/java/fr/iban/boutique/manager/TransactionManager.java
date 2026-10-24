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
        if (!item.hasActions()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getSettings().messages().noBuyCommands()));
            return;
        }
        int price = (int) Math.round(item.finalPrice(wholeShopDiscount));
        java.util.UUID playerId = player.getUniqueId();
        // Le débit vient en premier, conditionné au solde dans la même requête : deux
        // achats lancés avant que le premier débit n'arrive ne passent plus tous les deux.
        databaseManager.removeTokensIfEnough(playerId, price).thenAccept(paid -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!paid) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getSettings().messages().notEnoughMoney()));
                return;
            }
            if (!player.isOnline()) {
                // Parti entre le clic et le débit : rien n'a été livré, le solde revient.
                databaseManager.addTokens(playerId, price);
                return;
            }
            // La chaîne de l'article (conditions, messages, commandes…) s'exécute via le
            // moteur d'actions Artisan, main-thread : liste keyed écrite par l'éditeur ou
            // Action DSL d'un catalogue d'avant, `run` lit les deux. Project en portée :
            // une action peut porter `@clé`, résolue dans les lang/ du dossier boutique.
            plugin.getArtisanApi().getActions().run(
                    player, item.getActions(), java.util.Map.of(), ShopPlugin.PROJECT_ID);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getSettings().messages().buySuccess()));
            databaseManager.addPurchaseHistory(playerId, item);
            databaseManager.getTokensAsync(playerId).thenAccept(tokens -> plugin.getTokensCache().put(playerId, tokens));
        }));
    }

}
