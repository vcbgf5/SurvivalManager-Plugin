package com.dziubek.combatlog;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TestCombatOffCommand implements CommandExecutor {

    private final CombatLogPlugin plugin;

    public TestCombatOffCommand(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tej komendy może użyć tylko gracz.");
            return true;
        }

        Player player = (Player) sender;
        plugin.getCombatManager().clear(player.getUniqueId());
        player.sendMessage("§a[TEST] Zdjęto combat tag.");
        return true;
    }
}
