package com.dziubek.combatlog;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CrateKeyListener implements Listener {

    private final CombatLogPlugin plugin;

    public CrateKeyListener(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                && plugin.getCrates().getCrateNameAt(event.getClickedBlock().getLocation()) != null) {
            return; // fizyczną, przypiętą skrzynię obsługuje wyłącznie CrateBlockListener
        }

        ItemStack item = event.getItem();
        String crateName = plugin.getCrates().getKeyCrateName(item);
        if (crateName == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        List<CrateReward> rewards = plugin.getCrates().getRewards(crateName);
        if (rewards.isEmpty()) {
            player.sendMessage("§cTa skrzynia nie ma jeszcze skonfigurowanych nagród.");
            return;
        }

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }

        CrateRollAnimation.play(plugin, player, crateName, rewards);
    }
}
