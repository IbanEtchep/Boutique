package fr.iban.boutique.artisan;

import net.artisanmc.modules.api.ArtisanAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.Map;

/**
 * /boutiqueadmin additem &lt;id&gt; &lt;prix&gt; [catégorie] — capture l'item TENU EN MAIN
 * comme article de boutique.
 *
 * Le stack exact (NBT/enchants/CMD) et l'article partaient autrefois au backend
 * via le message module `boutique_additem` ; le backend mutait sa copie du
 * project et rebroadcastait. Le backend ne stocke plus aucun contenu (ADR
 * Artisan `server-disk-only-storage`) : on écrit désormais **directement** dans
 * la racine que ce module possède, hors du thread principal, puis demande la relecture à Artisan.
 *
 * Le fichier écrit EST la vérité — il n'y a rien à pousser derrière. Reste à le
 * DÉCLARER : le core sert un instantané pris au chargement (sémantique de
 * reload), donc une écriture non annoncée resterait invisible au serveur qui
 * tourne comme à l'éditeur jusqu'à un `/artisan reload` tapé à la main. On est
 * le seul à savoir qu'on a écrit, c'est donc à nous de le dire.
 */
public final class BoutiqueAdminCommand implements CommandExecutor {
    private final ArtisanAPI api;
    private final File projectDir;

    public BoutiqueAdminCommand(ArtisanAPI api, File projectDir) {
        this.api = api;
        this.projectDir = projectDir;
    }

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

        // The item is read on the main thread; the YAML files are read, parsed and
        // written off it, then Artisan re-reads the projects (its own thread).
        Map<String, Object> display = api.getItems().describe(held);
        String serialized = api.getItems().serialize(held);
        String id = args[1];
        String category = args.length >= 4 ? args[3] : null;
        org.bukkit.plugin.Plugin plugin = org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass());
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            CapturedItemWriter.Result res = CapturedItemWriter.addItem(projectDir, id, price, category, serialized, display);
            if (res.ok()) api.getProject().reload();
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (!res.ok()) {
                    player.sendMessage("§cCapture impossible : " + res.error());
                    return;
                }
                player.sendMessage("§a« " + id + " » (" + price + ") ajouté à la boutique. "
                        + "Complète les commandes d'achat dans l'éditeur.");
            });
        });
        return true;
    }
}
