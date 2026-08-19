package com.dziubek.combatlog;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Map;

public class ShopGuiListener implements Listener {

    private final CombatLogPlugin plugin;

    public ShopGuiListener(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ShopMainGuiHolder) {
            handleMainClick(event);
            return;
        }
        if (event.getInventory().getHolder() instanceof ShopCategoryGuiHolder) {
            handleCategoryClick(event);
        }
    }

    private void handleMainClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player) || event.getCurrentItem() == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        List<String> categories = plugin.getShop().getCategories();
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= categories.size()) {
            return;
        }
        plugin.getShopGui().openCategory(player, categories.get(slot));
    }

    private void handleCategoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player) || event.getCurrentItem() == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ShopCategoryGuiHolder holder = (ShopCategoryGuiHolder) event.getInventory().getHolder();

        Map<Integer, ShopManager.ShopItemData> items = plugin.getShop().getItems(holder.getCategoryId());
        int slot = event.getRawSlot();

        int i = 0;
        ShopManager.ShopItemData clicked = null;
        for (Map.Entry<Integer, ShopManager.ShopItemData> entry : items.entrySet()) {
            if (i == slot) {
                clicked = entry.getValue();
                break;
            }
            i++;
        }
        if (clicked == null) {
            return;
        }

        purchase(player, clicked);
    }

    private void purchase(Player player, ShopManager.ShopItemData item) {
        if (plugin.getEconomy() == null) {
            player.sendMessage("§cSklep jest niedostępny - brak podłączonego systemu ekonomii (Vault + EssentialsX).");
            return;
        }

        double balance = plugin.getEconomy().getBalance(player);
        if (balance < item.price) {
            player.sendMessage("§cNie masz wystarczająco środków! Potrzebujesz §f" + item.price + "§c, masz §f" + String.format("%.2f", balance) + "§c.");
            return;
        }

        plugin.getEconomy().withdrawPlayer(player, item.price);

        if (item.type == ShopItemType.KIT) {
            plugin.getKits().grantPurchase(item.kitName, player.getUniqueId());
            player.sendMessage("§aOdblokowano kit '" + item.kitName + "'! Użyj §f/kit " + item.kitName + " §aaby go odebrać.");
        } else {
            for (String cmd : item.commands) {
                String parsed = cmd.replace("%player%", player.getName());
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), parsed);
            }
            player.sendMessage("§aZakupiono! Pobrano §f" + item.price + "§a z konta.");
        }

        player.closeInventory();
    }
}
