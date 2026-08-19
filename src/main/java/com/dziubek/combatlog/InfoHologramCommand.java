package com.dziubek.combatlog;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class InfoHologramCommand implements CommandExecutor {

    private static final String ADMIN_PERMISSION = "combatlog.infoholo.admin";
    private static final List<String> TYPES = List.of("crates", "kits", "daily", "shop");

    private final CombatLogPlugin plugin;

    public InfoHologramCommand(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tej komendy może użyć tylko gracz.");
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission(ADMIN_PERMISSION)) {
            player.sendMessage("§cNie masz uprawnień.");
            return true;
        }

        if (!plugin.getDecentHolograms().isAvailable()) {
            player.sendMessage("§cDecentHolograms nie jest zainstalowany na tym serwerze.");
            return true;
        }

        if (args.length < 2) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("remove")) {
            plugin.getDecentHolograms().removeHologram(args[1]);
            player.sendMessage("§aUsunięto hologram '" + args[1] + "'.");
            return true;
        }

        if (!sub.equals("create")) {
            sendUsage(player);
            return true;
        }

        if (args.length < 3) {
            sendUsage(player);
            return true;
        }

        String type = args[1].toLowerCase();
        if (!TYPES.contains(type)) {
            player.sendMessage("§cNieznany typ. Dostępne: " + String.join(", ", TYPES));
            return true;
        }

        String id = args[2];
        List<String> lines = buildLines(type);
        plugin.getDecentHolograms().createInfoHologram(id, player.getLocation(), lines);

        // klik na hologram -> wykonuje komendę powiązaną z danym typem funkcji.
        plugin.getServer().dispatchCommand(
                plugin.getServer().getConsoleSender(),
                "dh p addaction " + id + " 0 left_click COMMAND:" + commandForType(type)
        );

        player.sendMessage("§aStworzono hologram '" + type + "' (id: " + id + ") w tym miejscu.");
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage("§cUżycie: /infoholo create <typ> <id> §7| §c/infoholo remove <id>");
        player.sendMessage("§7Dostępne typy: §f" + String.join(", ", TYPES));
        player.sendMessage("§7Każdy typ to osobny, mały hologram - postaw ich tyle ile chcesz, gdzie chcesz.");
    }

    private String commandForType(String type) {
        switch (type) {
            case "crates":
                return "/crate";
            case "kits":
                return "/kit";
            case "daily":
                return "/daily";
            case "shop":
                return "/sklep";
            default:
                return "/info-serwer";
        }
    }

    private List<String> buildLines(String type) {
        switch (type) {
            case "crates":
                return buildCratesLines();
            case "kits":
                return buildKitsLines();
            case "daily":
                return buildDailyLines();
            case "shop":
                return buildShopLines();
            default:
                return new ArrayList<>();
        }
    }

    private List<String> buildCratesLines() {
        List<String> lines = new ArrayList<>();
        lines.add("&6&l✦ Skrzynie ✦");
        lines.add("&7Komenda: &f/crate");
        lines.add("&8&m--------------------");
        lines.add("&7Kliknij, aby otworzyć &f/crate");
        return lines;
    }

    private List<String> buildKitsLines() {
        List<String> lines = new ArrayList<>();
        lines.add("&b&l✦ Kity ✦");
        lines.add("&7Komenda: &f/kit <nazwa>");
        lines.add("&8&m--------------------");
        lines.add("&7Kliknij, aby otworzyć &f/kit");
        return lines;
    }

    private List<String> buildDailyLines() {
        List<String> lines = new ArrayList<>();
        lines.add("&a&l✦ Daily ✦");
        lines.add("&7Komenda: &f/daily");
        lines.add("&7Odbieraj codzienną nagrodę - 7 dni z rzędu!");
        lines.add("&8&m--------------------");
        lines.add("&7Kliknij, aby odebrać &f/daily");
        return lines;
    }

    private List<String> buildShopLines() {
        List<String> lines = new ArrayList<>();
        lines.add("&d&l✦ Sklep ✦");
        lines.add("&7Komenda: &f/sklep");
        lines.add("&8&m--------------------");
        lines.add("&7Kliknij, aby otworzyć &f/sklep");
        return lines;
    }
}
