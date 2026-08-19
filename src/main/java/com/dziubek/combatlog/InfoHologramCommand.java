package com.dziubek.combatlog;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class InfoHologramCommand implements CommandExecutor {

    private static final String ADMIN_PERMISSION = "combatlog.infoholo.admin";

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
            player.sendMessage("§cUżycie: /infoholo create <id> §7| §c/infoholo remove <id>");
            return true;
        }

        String sub = args[0].toLowerCase();
        String id = args[1];

        if (sub.equals("remove")) {
            plugin.getDecentHolograms().removeHologram(id);
            player.sendMessage("§aUsunięto hologram '" + id + "'.");
            return true;
        }

        if (!sub.equals("create")) {
            player.sendMessage("§cUżycie: /infoholo create <id> §7| §c/infoholo remove <id>");
            return true;
        }

        List<String> lines = buildLines();
        plugin.getDecentHolograms().createInfoHologram(id, player.getLocation(), lines);

        // klik na hologram (dowolna strona 0) -> wyświetla graczowi rozbudowane info na czacie.
        // Robimy to komendą konsoli /dh p addaction - jeśli składnia różni się
        // w Twojej wersji DecentHolograms, sprawdź w grze: /dh p help addaction
        plugin.getServer().dispatchCommand(
                plugin.getServer().getConsoleSender(),
                "dh p addaction " + id + " 0 left_click COMMAND:/info-serwer"
        );

        player.sendMessage("§aStworzono informacyjny hologram '" + id + "' w tym miejscu.");
        return true;
    }

    private List<String> buildLines() {
        List<String> lines = new ArrayList<>();

        lines.add("&6&l✦✦✦ &e&lSURVIVAL &6&l✦✦✦");
        lines.add("&e&l▶ Kliknij mnie po więcej info! &e◀");
        lines.add("&7Wszystko czego potrzebujesz w jednym miejscu");
        lines.add("&8&m                                        ");

        lines.add("#ICON: CHEST");
        lines.add("&e&lSkrzynie &7» &f/crate");
        List<String> crateNames = plugin.getCrates().names();
        lines.add(crateNames.isEmpty()
                ? "&7Jeszcze brak skonfigurowanych skrzyń"
                : "&7Dostępne: &f" + String.join("&7, &f", crateNames));
        lines.add(" ");

        lines.add("#ICON: DIAMOND_SWORD");
        lines.add("&b&lKity &7» &f/kit <nazwa>");
        List<String> kitNames = plugin.getKits().names();
        lines.add(kitNames.isEmpty()
                ? "&7Jeszcze brak skonfigurowanych kitów"
                : "&7Dostępne: &f" + String.join("&7, &f", kitNames));
        lines.add(" ");

        lines.add("#ICON: SUNFLOWER");
        lines.add("&a&lDaily &7» &f/daily");
        lines.add("&7Odbieraj codzienną nagrodę - 7 dni z rzędu!");
        lines.add(" ");

        lines.add("#ICON: EMERALD");
        lines.add("&d&lSklep &7» &f/sklep");
        List<String> categories = plugin.getShop().getCategories();
        lines.add(categories.isEmpty()
                ? "&7Sklep jeszcze pusty"
                : "&7Kategorie: &f" + String.join("&7, &f", categories));
        lines.add(" ");

        lines.add("&8&m                                        ");
        lines.add("&7Kliknij hologram &e» &f/info-serwer");

        return lines;
    }
}
