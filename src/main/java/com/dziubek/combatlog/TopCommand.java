package com.dziubek.combatlog;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class TopCommand implements CommandExecutor {

    private static final List<String> STATS = List.of("crates", "daily", "money");

    private final CombatLogPlugin plugin;

    public TopCommand(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String stat = args.length >= 1 ? args[0].toLowerCase() : "crates";
        if (!STATS.contains(stat)) {
            sender.sendMessage("§cNieznana statystyka. Dostępne: " + String.join(", ", STATS));
            return true;
        }

        int count = 10;
        if (args.length >= 2) {
            try {
                count = Math.max(1, Math.min(25, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {
                // zostaje domyślne 10
            }
        }

        List<StatsManager.TopEntry> top = plugin.getStats().topN(stat, count);

        sender.sendMessage("§6§l--- TOP (" + statLabel(stat) + ") ---");
        if (top.isEmpty()) {
            sender.sendMessage("§7Brak jeszcze danych.");
            return true;
        }

        int place = 1;
        for (StatsManager.TopEntry entry : top) {
            String value = stat.equals("money")
                    ? String.format("%.2f", entry.value())
                    : String.valueOf((int) entry.value());
            sender.sendMessage("§e#" + place + " §f" + entry.name() + " §7- §f" + value);
            place++;
        }
        return true;
    }

    private String statLabel(String stat) {
        switch (stat) {
            case "daily":
                return "odebrane daily";
            case "money":
                return "wydane w sklepie";
            default:
                return "otwarte skrzynie";
        }
    }
}
