package com.dziubek.combatlog;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TestCombatStatusCommand implements CommandExecutor {

    private final CombatLogPlugin plugin;

    public TestCombatStatusCommand(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tej komendy może użyć tylko gracz.");
            return true;
        }

        Player player = (Player) sender;
        boolean tagged = plugin.getCombatManager().isTagged(player.getUniqueId());

        if (tagged) {
            long left = plugin.getCombatManager().secondsLeft(player.getUniqueId());
            player.sendMessage("§e[TEST] Jesteś w walce. Zostało: " + left + "s.");
        } else {
            player.sendMessage("§a[TEST] Nie jesteś w walce.");
        }
        return true;
    }
}
