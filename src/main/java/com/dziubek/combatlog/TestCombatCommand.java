package com.dziubek.combatlog;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TestCombatCommand implements CommandExecutor {

    private final CombatLogPlugin plugin;

    public TestCombatCommand(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tej komendy może użyć tylko gracz.");
            return true;
        }

        Player player = (Player) sender;
        long duration = plugin.getCombatDurationSeconds();
        plugin.getCombatManager().tag(player.getUniqueId(), duration);

        player.sendMessage("§e[TEST] Nałożono na Ciebie combat tag na " + duration + "s.");
        player.sendMessage("§7Spróbuj teraz wejść do regionu '" + plugin.getProtectedRegionName()
                + "' albo wyjść z serwera (/quit, alt+f4) żeby przetestować karę.");
        return true;
    }
}
