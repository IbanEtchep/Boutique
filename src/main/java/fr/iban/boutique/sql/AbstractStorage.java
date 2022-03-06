package fr.iban.boutique.sql;

import fr.iban.boutique.ShopItem;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractStorage {

    public abstract int getTokens(String playerName);

    public abstract void setTokens(String playerName, int amount);

    public abstract void addTokens(String playerName, int amount);

    public abstract void removeTokens(String playerName, int amount);

    public abstract void addPurchaseHistory(String playerName, ShopItem item);
}
