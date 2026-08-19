package com.dziubek.combatlog;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wszystkie teleporty (spawn, tpa, home) przechodzą przez to - gracz musi postać
 * N sekund bez ruchu (domyślnie 5), inaczej teleport się anuluje. Anuluje się też
 * automatycznie gdy gracz wejdzie w walkę w trakcie odliczania.
 */
public class TeleportDelayManager {

    private final CombatLogPlugin plugin;
    private final Map<UUID, BukkitTask> pending = new HashMap<>();

    public TeleportDelayManager(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public void cancel(UUID uuid) {
        BukkitTask task = pending.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    public void start(Player player, Location destination, String label) {
        cancel(player.getUniqueId());

        int delaySeconds = plugin.getConfig().getInt("teleport-delay-seconds", 5);
        Location startLoc = player.getLocation();

        player.sendMessage("§eTeleportacja (" + label + ") za " + delaySeconds + "s. Nie ruszaj się!");

        BukkitRunnable runnable = new BukkitRunnable() {
            int ticksLeft = delaySeconds * 20;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    pending.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                if (plugin.getCombatManager().isTagged(player.getUniqueId())) {
                    player.sendMessage("§cTeleportacja anulowana - jesteś w walce!");
                    pending.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                if (hasMoved(player.getLocation(), startLoc)) {
                    player.sendMessage("§cTeleportacja anulowana - poruszyłeś się!");
                    pending.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                ticksLeft -= 20;

                if (ticksLeft <= 0) {
                    player.teleport(destination);
                    player.sendMessage("§aTeleportowano!");
                    pending.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent("§e" + label + " za " + (ticksLeft / 20) + "s... §7(nie ruszaj się)"));
            }
        };

        BukkitTask task = runnable.runTaskTimer(plugin, 20L, 20L);
        pending.put(player.getUniqueId(), task);
    }

    private boolean hasMoved(Location a, Location b) {
        return a.getBlockX() != b.getBlockX()
                || a.getBlockY() != b.getBlockY()
                || a.getBlockZ() != b.getBlockZ();
    }
}
