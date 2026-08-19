package com.dziubek.combatlog;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CrateCommand implements CommandExecutor {

    private final CombatLogPlugin plugin;

    public CrateCommand(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    private static final String ADMIN_PERMISSION = "combatlog.crate.admin";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUżycie: /crate create <nazwa> §7| §c/crate givekey <nazwa> <gracz> [ilość] §7| §c/crate list");
            return true;
        }

        String sub = args[0].toLowerCase();

        // "list" moze kazdy, reszta to admin
        if (!sub.equals("list") && !sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage("§cNie masz uprawnień do zarządzania skrzyniami.");
            return true;
        }

        switch (sub) {
            case "create":
                return handleCreate(sender, args);
            case "givekey":
                return handleGiveKey(sender, args);
            case "list":
                List<String> names = plugin.getCrates().names();
                sender.sendMessage("§eSkrzynie: §f" + (names.isEmpty() ? "brak" : String.join(", ", names)));
                return true;
            default:
                sender.sendMessage("§cNieznana podkomenda. Użyj: create, givekey, list.");
                return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tej komendy może użyć tylko gracz.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUżycie: /crate create <nazwa>");
            return true;
        }

        Player player = (Player) sender;
        String name = args[1];

        Inventory inv = Bukkit.createInventory(new CrateConfigGuiHolder(name), 27, "§6§lKonfiguracja: §f" + name);
        List<ItemStack> existing = plugin.getCrates().getRewards(name);
        for (int i = 0; i < existing.size() && i < 27; i++) {
            inv.setItem(i, existing.get(i));
        }

        player.openInventory(inv);
        player.sendMessage("§aUmieść przedmioty, które mają wypadać z tej skrzyni.");
        player.sendMessage("§7Ilość sztuk = szansa (waga) ORAZ ilość jaką dostanie gracz. Zamknij ekwipunek, aby zapisać.");
        return true;
    }

    private boolean handleGiveKey(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUżycie: /crate givekey <nazwa> <gracz> [ilość]");
            return true;
        }

        String name = args[1];
        if (!plugin.getCrates().exists(name)) {
            sender.sendMessage("§cSkrzynia '" + name + "' nie istnieje. Najpierw /crate create " + name);
            return true;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§cGracz '" + args[2] + "' nie jest online.");
            return true;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cIlość musi być liczbą.");
                return true;
            }
        }

        ItemStack key = plugin.getCrates().createKey(name, amount);
        target.getInventory().addItem(key);

        sender.sendMessage("§aDano " + amount + "x klucz do '" + name + "' graczowi " + target.getName() + ".");
        target.sendMessage("§aOtrzymujesz " + amount + "x klucz do skrzyni '" + name + "'!");
        return true;
    }
}
