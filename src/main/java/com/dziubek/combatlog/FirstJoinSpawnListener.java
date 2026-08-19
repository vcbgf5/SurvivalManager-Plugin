package com.dziubek.combatlog;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class FirstJoinSpawnListener implements Listener {

    private final CombatLogPlugin plugin;

    public FirstJoinSpawnListener(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.hasPlayedBefore()) {
            return;
        }
        if (!plugin.hasSurvivalSpawn()) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.teleport(plugin.getSurvivalSpawn());
            }
        });
    }
}
