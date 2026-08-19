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

    public static void play(CombatLogPlugin plugin, Player player, String crateName, List<CrateReward> rewards) {
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
                              List<CrateReward> rewards, Random random, int tick) {

        if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(inv)) {
            return; // gracz zamknął GUI wcześniej - przerywamy
        }

        if (tick < TOTAL_STEPS) {
            ItemStack randomItem = rewards.get(random.nextInt(rewards.size())).item().clone();
            inv.setItem(4, randomItem);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.0f + (tick * 0.02f));

            // opóźnienie rośnie pod koniec animacji, żeby "rolka" wizualnie zwalniała
            long delay = 2 + (tick / 4);
            int next = tick + 1;
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> step(plugin, player, inv, crateName, rewards, random, next), delay);
        } else {
            ItemStack won = pickWeighted(rewards, random).item().clone();
            inv.setItem(4, won);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            Map<Integer, ItemStack> leftover = player.getInventory().addItem(won.clone());
            for (ItemStack extra : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), extra);
            }

            plugin.getStats().recordCrateOpened(player.getUniqueId(), player.getName());

            String name = itemDisplayName(won);
            player.sendMessage("§aWygrałeś: §f" + name + " §7(x" + won.getAmount() + ") §7ze skrzyni '" + crateName + "'!");
        }
    }

    private static CrateReward pickWeighted(List<CrateReward> rewards, Random random) {
        double totalWeight = 0;
        for (CrateReward reward : rewards) {
            totalWeight += Math.max(0, reward.chance());
        }
        if (totalWeight <= 0) {
            return rewards.get(random.nextInt(rewards.size()));
        }
        double roll = random.nextDouble() * totalWeight;
        double cumulative = 0;
        for (CrateReward reward : rewards) {
            cumulative += Math.max(0, reward.chance());
            if (roll < cumulative) {
                return reward;
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
