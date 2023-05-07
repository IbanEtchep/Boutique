package fr.iban.boutique.sql;

import fr.iban.boutique.ShopItem;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractStorage {

    public abstract int getTokens(UUID uuid);

    public abstract void setTokens(UUID uuid, int amount);

    public abstract void addTokens(UUID uuid, int amount);

    public abstract void removeTokens(UUID uuid, int amount);

    public abstract void addPurchaseHistory(UUID uuid, ShopItem item);
}
