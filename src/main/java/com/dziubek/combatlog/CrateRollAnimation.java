package com.dziubek.combatlog;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class CrateRollAnimation {

    private static final int TOTAL_STEPS = 24;

    public static void play(CombatLogPlugin plugin, Player player, String crateName, List<ItemStack> rewards) {
        Inventory inv = Bukkit.createInventory(new CrateRollGuiHolder(), 9, "§6§lOtwieranie: §f" + crateName);

        ItemStack glass = glassPane();
        for (int i = 0; i < 9; i++) {
            if (i != 4) {
                inv.setItem(i, glass);
            }
        }

        player.openInventory(inv);
        step(plugin, player, inv, crateName, rewards, new Random(), 0);
    }

    private static void step(CombatLogPlugin plugin, Player player, Inventory inv, String crateName,
                              List<ItemStack> rewards, Random random, int tick) {

        if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(inv)) {
            return; // gracz zamknął GUI wcześniej - przerywamy
        }

        if (tick < TOTAL_STEPS) {
            ItemStack randomItem = rewards.get(random.nextInt(rewards.size())).clone();
            inv.setItem(4, randomItem);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.0f + (tick * 0.02f));

            // opóźnienie rośnie pod koniec animacji, żeby "rolka" wizualnie zwalniała
            long delay = 2 + (tick / 4);
            int next = tick + 1;
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> step(plugin, player, inv, crateName, rewards, random, next), delay);
        } else {
            ItemStack won = pickWeighted(rewards, random).clone();
            inv.setItem(4, won);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            Map<Integer, ItemStack> leftover = player.getInventory().addItem(won.clone());
            for (ItemStack extra : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), extra);
            }

            String name = itemDisplayName(won);
            player.sendMessage("§aWygrałeś: §f" + name + " §7(x" + won.getAmount() + ") §7ze skrzyni '" + crateName + "'!");
        }
    }

    private static ItemStack pickWeighted(List<ItemStack> rewards, Random random) {
        int totalWeight = 0;
        for (ItemStack item : rewards) {
            totalWeight += Math.max(1, item.getAmount());
        }
        int roll = random.nextInt(totalWeight) + 1;
        int cumulative = 0;
        for (ItemStack item : rewards) {
            cumulative += Math.max(1, item.getAmount());
            if (roll <= cumulative) {
                return item;
            }
        }
        return rewards.get(rewards.size() - 1);
    }

    private static String itemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().toString();
    }

    private static ItemStack glassPane() {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);
        return glass;
    }
}
