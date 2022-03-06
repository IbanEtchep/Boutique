package fr.iban.boutique.listener;

import fr.iban.boutique.ShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinQuitListener implements Listener {

    private ShopPlugin plugin;

    public JoinQuitListener(ShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        Player player = e.getPlayer();
        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getTokensCache().put(player.getUniqueId(), plugin.getDatabaseManager().getTokens(player.getName()));
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e){
        Player player = e.getPlayer();
        plugin.getTokensCache().remove(player.getUniqueId());
    }

}
