package com.dziubek.combatlog;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class HomeGuiManager {

    private final CombatLogPlugin plugin;

    public HomeGuiManager(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new HomeGuiHolder(), 18, "§6§lTwoje Home (" + HomeManager.MAX_HOMES + ")");

        for (int slot = 1; slot <= HomeManager.MAX_HOMES; slot++) {
            boolean set = plugin.getHomes().isSet(player.getUniqueId(), slot);

            ItemStack item = new ItemStack(set ? Material.RED_BED : Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(set ? "§a§lHome " + slot : "§7§lHome " + slot + " §8(puste)");

            List<String> lore = new ArrayList<>();
            if (set) {
                lore.add("§7Kliknij, aby się teleportować");
            } else {
                lore.add("§7Nieustawione");
                lore.add("§7Użyj: §f/home set " + slot);
            }
            meta.setLore(lore);
            item.setItemMeta(meta);

            inv.setItem(slot - 1, item);
        }

        GuiDecor.fillEmpty(inv);
        player.openInventory(inv);
        GuiDecor.playOpenSound(player);
    }
}
