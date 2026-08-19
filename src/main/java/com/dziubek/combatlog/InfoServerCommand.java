package com.dziubek.combatlog;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class InfoServerCommand implements CommandExecutor {

    private final CombatLogPlugin plugin;

    public InfoServerCommand(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("§8§m                                                  ");
        sender.sendMessage("§6§l✦ SURVIVAL - INFORMACJE ✦");
        sender.sendMessage("");

        sendCombatInfo(sender);
        sendSpawnInfo(sender);
        sendTpaHomeInfo(sender);
        sendKitInfo(sender);
        sendDailyInfo(sender);
        sendCrateInfo(sender);
        sendShopInfo(sender);

        sender.sendMessage("§8§m                                                  ");
        return true;
    }

    private void sendCombatInfo(CommandSender sender) {
        sender.sendMessage("§c§l⚔ Walka");
        sender.sendMessage("§7Po trafieniu gracza wchodzisz w walkę na §f" + plugin.getCombatDurationSeconds() + "s§7.");
        sender.sendMessage("§7Wyjście z serwera podczas walki = §cśmierć i utrata itemów§7.");
        sender.sendMessage("§7Podczas walki nie możesz użyć §f/spawn§7, §f/tpa §7ani wejść na spawn.");
        sender.sendMessage("");
    }

    private void sendSpawnInfo(CommandSender sender) {
        sender.sendMessage("§b§l⌂ Spawn");
        sender.sendMessage("§f/spawn §7- teleport na spawn (5s bez ruchu, zablokowane w walce)");
        sender.sendMessage("");
    }

    private void sendTpaHomeInfo(CommandSender sender) {
        sender.sendMessage("§d§l➜ Teleportacja");
        sender.sendMessage("§f/tpa <gracz> §7- wysyła prośbę o teleport (ważna " + plugin.getTpa().timeoutSeconds() + "s)");
        sender.sendMessage("§f/tpaccept §7, §f/tpdeny §7- akceptuj/odrzuć");
        sender.sendMessage("§f/home §7- menu z Twoimi domkami (do " + HomeManager.MAX_HOMES + ")");
        sender.sendMessage("§f/home set <numer> §7- zapisz obecne miejsce jako home");
        sender.sendMessage("");
    }

    private void sendKitInfo(CommandSender sender) {
        sender.sendMessage("§b§l⚒ Kity");
        List<String> kitNames = plugin.getKits().names();
        if (kitNames.isEmpty()) {
            sender.sendMessage("§7Serwer nie ma jeszcze skonfigurowanych kitów.");
        } else {
            for (String kit : kitNames) {
                long cooldownHours = plugin.getKits().getCooldownSeconds(kit) / 3600;
                sender.sendMessage("§f/kit " + kit + " §7- odnawia się co §f" + cooldownHours + "h");
            }
        }
        sender.sendMessage("");
    }

    private void sendDailyInfo(CommandSender sender) {
        sender.sendMessage("§a§l☀ Daily");
        sender.sendMessage("§f/daily §7- odbiera dzisiejszą nagrodę (7-dniowa seria, resetuje się po przerwie)");
        sender.sendMessage("");
    }

    private void sendCrateInfo(CommandSender sender) {
        sender.sendMessage("§e§l☐ Skrzynie");
        List<String> crateNames = plugin.getCrates().names();
        if (crateNames.isEmpty()) {
            sender.sendMessage("§7Serwer nie ma jeszcze skonfigurowanych skrzyń.");
        } else {
            for (String crate : crateNames) {
                int rewardCount = plugin.getCrates().getRewards(crate).size();
                sender.sendMessage("§f" + crate + " §7- " + rewardCount + " możliwych nagród");
            }
            sender.sendMessage("§7Zdobądź klucz i kliknij nim prawym, żeby otworzyć.");
        }
        sender.sendMessage("");
    }

    private void sendShopInfo(CommandSender sender) {
        sender.sendMessage("§d§l$ Sklep");
        List<String> categories = plugin.getShop().getCategories();
        if (categories.isEmpty()) {
            sender.sendMessage("§7Sklep jest jeszcze pusty.");
        } else {
            for (String category : categories) {
                String displayName = plugin.getShop().getCategoryDisplayName(category);
                Map<Integer, ShopManager.ShopItemData> items = plugin.getShop().getItems(category);
                sender.sendMessage("§f" + displayName + " §7- " + items.size() + " przedmiotów");
            }
        }
        sender.sendMessage("§7Wpisz §f/sklep §7żeby otworzyć GUI.");
    }
}
