package com.dziubek.combatlog;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Obsługuje kliknięcie w fizycznie postawioną i przypiętą (/crate bind) skrzynię w świecie.
 * Wymaga klucza pasującego do danej skrzyni - w przeciwieństwie do starego CrateKeyListener,
 * który pozwala otwierać kluczem "w powietrzu" (dla skrzyń bez przypiętej lokalizacji).
 */
public class CrateBlockListener implements Listener {

    private final CombatLogPlugin plugin;

    public CrateBlockListener(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        String crateName = plugin.getCrates().getCrateNameAt(event.getClickedBlock().getLocation());
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

        if (plugin.getCrates().isPrivate(crateName)) {
            String permission = plugin.getCrates().getPrivatePermission(crateName);
            if (!player.hasPermission(permission)) {
                player.sendMessage("§cTa skrzynia jest prywatna - brakuje Ci uprawnienia §f" + permission);
                return;
            }
        }

        ItemStack item = event.getItem();
        String keyCrateName = plugin.getCrates().getKeyCrateName(item);
        if (keyCrateName == null || !keyCrateName.equals(crateName)) {
            player.sendMessage("§cPotrzebujesz klucza do skrzyni '" + crateName + "', aby ją otworzyć!");
            return;
        }

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }

        plugin.getCrates().getEffect(crateName).play(plugin, event.getClickedBlock().getLocation());
        CrateRollAnimation.play(plugin, player, crateName, rewards);
    }
}
