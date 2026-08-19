package com.dziubek.combatlog;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class HomeGuiListener implements Listener {

    private final CombatLogPlugin plugin;

    public HomeGuiListener(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof HomeGuiHolder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= HomeManager.MAX_HOMES) {
            return;
        }
        int homeNumber = rawSlot + 1;

        Location home = plugin.getHomes().getHome(player.getUniqueId(), homeNumber);
        if (home == null) {
            player.sendMessage("§cTen home nie jest ustawiony. Użyj: /home set " + homeNumber);
            return;
        }

        if (plugin.getCombatManager().isTagged(player.getUniqueId())) {
            long left = plugin.getCombatManager().secondsLeft(player.getUniqueId());
            player.sendMessage("§cNie możesz teleportować się do home podczas walki! (zostało " + left + "s)");
            player.closeInventory();
            return;
        }

        player.closeInventory();
        plugin.getTeleportDelay().start(player, home, "Home #" + homeNumber);
    }
}
