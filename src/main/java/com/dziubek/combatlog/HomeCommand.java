package com.dziubek.combatlog;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomeCommand implements CommandExecutor {

    private final CombatLogPlugin plugin;

    public HomeCommand(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tej komendy może użyć tylko gracz.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length >= 1 && args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                player.sendMessage("§cUżycie: /home set <numer 1-" + HomeManager.MAX_HOMES + ">");
                return true;
            }

            int slot;
            try {
                slot = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cNumer musi być liczbą.");
                return true;
            }

            if (slot < 1 || slot > HomeManager.MAX_HOMES) {
                player.sendMessage("§cNumer musi być od 1 do " + HomeManager.MAX_HOMES + ".");
                return true;
            }

            plugin.getHomes().setHome(player.getUniqueId(), slot, player.getLocation());
            player.sendMessage("§aUstawiono home #" + slot + " w tym miejscu.");
            return true;
        }

        // brak argumentów - otwórz menu z guzikami
        plugin.getHomeGui().open(player);
        return true;
    }
}
