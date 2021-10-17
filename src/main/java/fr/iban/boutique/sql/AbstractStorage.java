package fr.iban.boutique.sql;

import fr.iban.boutique.ShopItem;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractStorage {

    public abstract int getTokens(Player player);

    public abstract void setTokens(Player player, int amount);

    public abstract void addTokens(Player player, int amount);

    public abstract void removeTokens(Player player, int amount);

    public abstract void addPurchaseHistory(Player player, ShopItem item);
}
