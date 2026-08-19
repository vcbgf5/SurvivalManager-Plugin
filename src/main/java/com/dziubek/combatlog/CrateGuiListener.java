package com.dziubek.combatlog;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class CrateGuiListener implements Listener {

    private final CombatLogPlugin plugin;

    public CrateGuiListener(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof CrateRollGuiHolder) {
            // GUI animacji losowania - gracz nic stąd nie może wyjąć, nagrodę i tak dostaje na koniec
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof CrateConfigGuiHolder)) {
            return;
        }
        CrateConfigGuiHolder holder = (CrateConfigGuiHolder) event.getInventory().getHolder();
        plugin.getCrates().saveCrate(holder.getCrateName(), event.getInventory().getContents());

        HumanEntity human = event.getPlayer();
        human.sendMessage("§aZapisano konfigurację skrzyni '" + holder.getCrateName() + "'.");
    }
}
