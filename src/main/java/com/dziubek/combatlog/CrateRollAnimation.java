package com.dziubek.combatlog;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CrateRollAnimation {

    private static final int TOTAL_STEPS = 24;
    private static final int[] REEL_SLOTS = {2, 3, 4, 5, 6};
    private static final int RESULT_SLOT = 4;

    // progi rzadkosci wg CrateReward.chance() (%) - im nizszy procent, tym rzadszy przedmiot
    private static final double LEGENDARY_THRESHOLD = 5.0;
    private static final double RARE_THRESHOLD = 15.0;

    public static void play(CombatLogPlugin plugin, Player player, String crateName, List<CrateReward> rewards) {
        Inventory inv = Bukkit.createInventory(new CrateRollGuiHolder(), 9, "§6§lOtwieranie: §f" + crateName);

        ItemStack border = borderPane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
        }

        Random random = new Random();
        List<ItemStack> reel = new ArrayList<>();
        for (int i = 0; i < REEL_SLOTS.length; i++) {
            reel.add(rewards.get(random.nextInt(rewards.size())).item());
        }
        renderReel(inv, reel);

        player.openInventory(inv);
        TitleUtil.show(player, "§6§lLosowanie...", "§7" + crateName);
        step(plugin, player, inv, crateName, rewards, reel, random, 0);
    }

    private static void step(CombatLogPlugin plugin, Player player, Inventory inv, String crateName,
                              List<CrateReward> rewards, List<ItemStack> reel, Random random, int tick) {

        if (!player.isOnline()) {
            return; // gracz sie rozlaczyl - nie da sie kontynuowac
        }

        if (!player.getOpenInventory().getTopInventory().equals(inv)) {
            // gracz zamknal GUI w trakcie losowania - otwieramy z powrotem i kontynuujemy,
            // zamkniecie okna NIE przerywa losowania, tylko je chwilowo chowa
            player.openInventory(inv);
        }

        if (tick < TOTAL_STEPS) {
            // szpula "przesuwa sie" - najstarszy przedmiot wypada, nowy losowy wjezdza z prawej
            reel.remove(0);
            reel.add(rewards.get(random.nextInt(rewards.size())).item());
            renderReel(inv, reel);

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.0f + (tick * 0.02f));
            if (tick % 2 == 0) {
                player.spawnParticle(Particle.END_ROD, player.getEyeLocation(), 1, 0.15, 0.1, 0.15, 0.01);
            }

            // im blizej konca, tym mocniej zwalnia - buduje napiecie tuz przed odkryciem
            long delay = 2 + (long) ((tick * (double) tick) / 40.0);
            int next = tick + 1;
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> step(plugin, player, inv, crateName, rewards, reel, random, next), delay);
        } else {
            CrateReward wonReward = pickWeighted(rewards, random);
            ItemStack won = wonReward.item().clone();

            ItemStack frame = borderPane(frameColorFor(wonReward.chance()));
            for (int i = 0; i < 9; i++) {
                inv.setItem(i, frame);
            }
            inv.setItem(RESULT_SLOT, won);

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            Map<Integer, ItemStack> leftover = player.getInventory().addItem(won.clone());
            for (ItemStack extra : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), extra);
            }

            plugin.getStats().recordCrateOpened(player.getUniqueId(), player.getName());

            String name = itemDisplayName(won);
            player.sendMessage("§aWygrałeś: §f" + name + " §7(x" + won.getAmount() + ") §7ze skrzyni '" + crateName + "'!");

            announceRarity(plugin, player, crateName, name, wonReward.chance());
        }
    }

    private static void renderReel(Inventory inv, List<ItemStack> reel) {
        for (int i = 0; i < REEL_SLOTS.length; i++) {
            inv.setItem(REEL_SLOTS[i], reel.get(i).clone());
        }
    }

    private static Material frameColorFor(double chance) {
        if (chance < LEGENDARY_THRESHOLD) {
            return Material.YELLOW_STAINED_GLASS_PANE;
        }
        if (chance < RARE_THRESHOLD) {
            return Material.LIGHT_BLUE_STAINED_GLASS_PANE;
        }
        return Material.BLACK_STAINED_GLASS_PANE;
    }

    /**
     * Rzadsze przedmioty (niższa szansa w CrateReward.chance()) dostają lepszą oprawę:
     * LEGENDARY (&lt;5%) - fajerwerk, złoty tytuł, ogłoszenie na czacie całego serwera.
     * RZADKI (&lt;15%) - mniejszy tytuł tylko dla gracza, bez ogłoszenia.
     * Reszta - bez zmian (już obsłużone wyżej: dźwięk levelup + wiadomość na czacie).
     */
    private static void announceRarity(CombatLogPlugin plugin, Player player, String crateName, String itemName, double chance) {
        if (chance < LEGENDARY_THRESHOLD) {
            TitleUtil.show(player, "§6§l✦ LEGENDARY ✦", "§f" + itemName);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.4f);
            spawnFirework(plugin, player);

            String broadcast = "§6§l✦ §e" + player.getName() + " §6wylosował(a) RZADKI przedmiot §f" + itemName
                    + " §6ze skrzyni '" + crateName + "'! §6§l✦";
            Bukkit.getServer().broadcastMessage(broadcast);
        } else if (chance < RARE_THRESHOLD) {
            TitleUtil.show(player, "§b§lRZADKI!", "§f" + itemName);
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        }
    }

    private static void spawnFirework(CombatLogPlugin plugin, Player player) {
        Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.YELLOW, Color.ORANGE)
                .withFade(Color.RED)
                .with(FireworkEffect.Type.BURST)
                .trail(true)
                .flicker(true)
                .build());
        meta.setPower(0);
        firework.setFireworkMeta(meta);

        plugin.getServer().getScheduler().runTaskLater(plugin, firework::detonate, 2L);
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

    private static ItemStack borderPane(Material material) {
        ItemStack glass = new ItemStack(material);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);
        return glass;
    }
}
