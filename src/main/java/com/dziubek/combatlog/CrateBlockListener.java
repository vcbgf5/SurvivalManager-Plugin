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
 * To jedyny sposób na otwarcie skrzyni - klucz "w powietrzu" nie działa, trzeba kliknąć blok.
 * Jeśli skrzynia ma ustawiony darmowy cooldown (/crate setfreecooldown), gracz bez klucza
 * może ją i tak otworzyć raz na X godzin.
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
        boolean hasValidKey = keyCrateName != null && keyCrateName.equals(crateName);

        if (!hasValidKey) {
            if (plugin.getCrates().canUseFreeOpen(crateName, player.getUniqueId())) {
                plugin.getCrates().markFreeUsed(crateName, player.getUniqueId());
                player.sendMessage("§aOtwierasz skrzynię '" + crateName + "' za darmo!");
                plugin.getCrates().getEffect(crateName).play(plugin, event.getClickedBlock().getLocation());
                CrateRollAnimation.play(plugin, player, crateName, rewards);
                return;
            }

            long freeLeft = plugin.getCrates().freeSecondsLeft(crateName, player.getUniqueId());
            if (freeLeft > 0) {
                player.sendMessage("§cPotrzebujesz klucza do skrzyni '" + crateName
                        + "' §7(darmowe otwarcie za " + formatDuration(freeLeft) + ")");
            } else {
                player.sendMessage("§cPotrzebujesz klucza do skrzyni '" + crateName + "', aby ją otworzyć!");
            }
            plugin.getCratePreviewGui().open(player, crateName);
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

    private static String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        if (hours > 0) {
            return hours + "h " + minutes + "min";
        }
        long seconds = totalSeconds % 60;
        if (minutes > 0) {
            return minutes + "min " + seconds + "s";
        }
        return seconds + "s";
    }
}
