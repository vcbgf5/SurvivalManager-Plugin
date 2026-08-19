package com.dziubek.combatlog;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class HomeGuiHolder implements InventoryHolder {

    @Override
    public Inventory getInventory() {
        // nieużywane - to tylko znacznik identyfikujący nasze GUI w InventoryClickEvent
        return null;
    }
}
