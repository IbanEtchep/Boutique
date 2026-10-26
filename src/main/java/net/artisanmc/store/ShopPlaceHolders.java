package net.artisanmc.store;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShopPlaceHolders extends PlaceholderExpansion {

    private final StorePlugin plugin;
    private final String identifier;

    /** {@code store} ({@code %store_tokens%}), et {@code boutique} pour les textes écrits avant Store. */
    public ShopPlaceHolders(StorePlugin plugin, String identifier) {
        this.plugin = plugin;
        this.identifier = identifier;
    }

    @Override
    public @NotNull String getIdentifier() {
        return identifier;
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
            return String.valueOf(plugin.getTokensCache().getOrDefault(player.getUniqueId(), 0));
        }

        return null;
    }
}
