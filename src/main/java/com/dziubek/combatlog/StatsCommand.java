package com.dziubek.combatlog;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class StatsCommand implements CommandExecutor {

    private static final String ADMIN_PERMISSION = "combatlog.stats.admin";

    private final CombatLogPlugin plugin;

    public StatsCommand(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        UUID target;
        String targetName;

        if (args.length >= 1) {
            if (!sender.hasPermission(ADMIN_PERMISSION)) {
                sender.sendMessage("§cNie masz uprawnień, by sprawdzać statystyki innych graczy.");
                return true;
            }
            OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
            target = offline.getUniqueId();
            targetName = offline.getName() != null ? offline.getName() : args[0];
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cUżycie: /stats <gracz>");
                return true;
            }
            Player player = (Player) sender;
            target = player.getUniqueId();
            targetName = player.getName();
        }

        int cratesOpened = plugin.getStats().getCratesOpened(target);
        int dailyClaims = plugin.getStats().getDailyClaims(target);
        double moneySpent = plugin.getStats().getMoneySpent(target);

        sender.sendMessage("§6§l--- Statystyki: §f" + targetName + " §6§l---");
        sender.sendMessage("§7Otwarte skrzynie: §f" + cratesOpened);
        sender.sendMessage("§7Odebrane daily: §f" + dailyClaims);
        sender.sendMessage("§7Wydane w sklepie: §f" + String.format("%.2f", moneySpent));
        return true;
    }
}
