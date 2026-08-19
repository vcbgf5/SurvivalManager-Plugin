package com.dziubek.combatlog;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TpaDenyCommand implements CommandExecutor {

    private final CombatLogPlugin plugin;

    public TpaDenyCommand(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tej komendy może użyć tylko gracz.");
            return true;
        }

        Player player = (Player) sender;
        UUID requesterUuid = plugin.getTpa().getRequester(player.getUniqueId());
        plugin.getTpa().clear(player.getUniqueId());

        if (requesterUuid == null) {
            player.sendMessage("§cNie masz żadnej oczekującej prośby o teleportację.");
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester != null && requester.isOnline()) {
            requester.sendMessage("§c" + player.getName() + " odrzucił Twoją prośbę o teleportację.");
        }
        player.sendMessage("§aOdrzucono prośbę o teleportację.");
        return true;
    }
}
