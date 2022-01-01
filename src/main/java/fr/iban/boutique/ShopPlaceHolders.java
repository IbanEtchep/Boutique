package fr.iban.boutique;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShopPlaceHolders extends PlaceholderExpansion {

    private final ShopPlugin plugin;

    public ShopPlaceHolders(ShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "boutique";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier){

        if(player == null){
            return "";
        }

        // %someplugin_placeholder1%
        if(identifier.equals("tokens")){
            return plugin.getTokensCache().getOrDefault(player.getUniqueId(), 0)+"";
        }

        return null;
    }
}
