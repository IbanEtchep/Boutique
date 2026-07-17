package fr.iban.boutique.artisan;

import net.artisanmc.modules.api.ArtisanAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * /boutiqueadmin additem <id> <prix> [catégorie] — capture l'item TENU EN MAIN
 * comme article de boutique. Le stack exact (NBT/enchants/CMD) part au backend
 * via le WS (message module `boutique_additem`) ; le backend mute le projet
 * boutique (item capturé + article icon=item:<id>), commit et rebroadcast — la
 * boutique se recharge en jeu en quelques secondes. Le plugin n'écrit JAMAIS
 * les fichiers projet lui-même (ADR captured-items-and-module-field-kit §3).
 */
public final class BoutiqueAdminCommand implements CommandExecutor {
    private final ArtisanAPI api;

    public BoutiqueAdminCommand(ArtisanAPI api) { this.api = api; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande en jeu uniquement.");
            return true;
        }
        if (!player.hasPermission("boutique.admin")) {
            player.sendMessage("§cPermission manquante (boutique.admin).");
            return true;
        }
        if (args.length < 3 || !args[0].equalsIgnoreCase("additem")) {
            player.sendMessage("§eUsage: /" + label + " additem <id> <prix> [catégorie]");
            return true;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            player.sendMessage("§cTiens l'item à vendre en main principale.");
            return true;
        }
        int price;
        try {
            price = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cPrix invalide: " + args[2]);
            return true;
        }
        if (price < 0) {
            player.sendMessage("§cLe prix doit être positif.");
            return true;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("request_id", UUID.randomUUID().toString());
        payload.put("item_id", args[1]);
        payload.put("price", price);
        if (args.length >= 4) payload.put("category", args[3]);
        payload.put("stack_yaml", api.getItems().serialize(held));
        payload.put("display", api.getItems().describe(held));
        api.getMessages().send("boutique_additem", payload);

        player.sendMessage("§aCapture envoyée — « " + args[1] + " » (" + price
                + ") arrive en boutique dans quelques secondes. Complète les commandes d'achat dans l'éditeur.");
        return true;
    }
}
