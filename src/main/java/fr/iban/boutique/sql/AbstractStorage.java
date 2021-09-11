package fr.iban.boutique.sql;

import org.bukkit.entity.Player;

public abstract class AbstractStorage {

    public abstract int getTokens(Player player);

    public abstract void setTokens(Player player, int amount);

    public abstract void addTokens(Player player, int amount);

    public abstract void removeTokens(Player player, int amount);

}
