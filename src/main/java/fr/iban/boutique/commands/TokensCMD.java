package fr.iban.boutique.commands;

import fr.iban.boutique.ShopPlugin;
import fr.iban.boutique.manager.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TokensCMD implements CommandExecutor {

    private ShopPlugin plugin;
    private DatabaseManager dbManager;
    private String currency;


    public TokensCMD(ShopPlugin shopPlugin) {
        this.plugin = shopPlugin;
        dbManager = plugin.getDatabaseManager();
        currency = plugin.getConfig().getString("messages.currency-name");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender.hasPermission("tokens.admin")){
            if(args.length >= 2){
                OfflinePlayer to = Bukkit.getOfflinePlayer(args[1]);

                if(to.hasPlayedBefore() || to.isOnline()){
                    switch (args[0]){

                        case "get":
                            dbManager.getTokensAsync(to.getName()).thenAccept(tokens -> {
                                sender.sendMessage("§c"+ to.getName()+ " a " + tokens + " " + currency +".");
                            });
                            break;

                        case "set":
                            if(args.length == 3){
                                try{
                                    int amount = Integer.parseInt(args[2]);
                                    plugin.getDatabaseManager().setTokens(to.getName(), amount);
                                    if(to.getPlayer() != null){
                                        to.getPlayer().sendMessage("§aVotre solde a été défini à " + amount + " " + currency + ".");
                                    }
                                    String msg = "§eLe solde de " + to.getName() + " a été défini à " + amount + " " + currency + ".";
                                    sender.sendMessage(msg);
                                    plugin.getLogger().info(msg);
                                    Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                                        plugin.getTokensCache().put(to.getUniqueId(), plugin.getDatabaseManager().getTokens(to.getName()));
                                    });
                                }catch (NumberFormatException e){
                                    sender.sendMessage("§cLe nombre doit être un nombre entier !");
                                }
                            }
                            break;
                        case "add":
                            if(args.length == 3){
                                try{
                                    int amount = Integer.parseInt(args[2]);
                                    plugin.getDatabaseManager().addTokens(to.getName(), amount);
                                    if(to.getPlayer() != null){
                                        to.getPlayer().sendMessage("§aVous avez reçu " + amount + " " + currency + ".");
                                    }
                                    String msg = "§e" + amount + " " + currency + " ont été ajoutés au solde de " + to.getName() + ".";
                                    sender.sendMessage(msg);
                                    plugin.getLogger().info(msg);
                                    Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                                        plugin.getTokensCache().put(to.getUniqueId(), plugin.getDatabaseManager().getTokens(to.getName()));
                                    });

                                }catch (NumberFormatException e){
                                    sender.sendMessage("§cLe nombre doit être un nombre entier !");
                                }
                            }
                            break;

                        case "remove":
                            if(args.length == 3){
                                try{
                                    dbManager.getTokensAsync(to.getName()).thenAccept(tokens -> {
                                        int amount = Integer.parseInt(args[2]);
                                        if(tokens < amount){
                                            amount = tokens;
                                        }
                                        plugin.getDatabaseManager().removeTokens(to.getName(), amount);
                                        if(to.getPlayer() != null){
                                            to.getPlayer().sendMessage("§a" + amount + " " + currency + " ont été retirés de vote solde.");
                                        }
                                        String msg = "§e" + amount + " " + currency + " ont été retirés du solde de " + to.getName() + ".";
                                        sender.sendMessage(msg);
                                        plugin.getLogger().info(msg);
                                        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                                            plugin.getTokensCache().put(to.getUniqueId(), plugin.getDatabaseManager().getTokens(to.getName()));
                                        });
                                    });

                                }catch (NumberFormatException e){
                                    sender.sendMessage("§cLe nombre doit être un nombre entier !");
                                }
                            }
                            break;

                    }
                }else{
                    sender.sendMessage("§cCe joueur n'a jamais joué sur le serveur.");
                }
            }
        }
        return false;
    }
}
