package com.dziubek.combatlog;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public class CombatDamageListener implements Listener {

    private final CombatLogPlugin plugin;

    public CombatDamageListener(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();

        Player attacker = resolvePlayerAttacker(event);
        if (attacker == null) {
            return;
        }
        if (attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        long duration = plugin.getCombatDurationSeconds();
        plugin.getCombatManager().tag(victim.getUniqueId(), duration);
        plugin.getCombatManager().tag(attacker.getUniqueId(), duration);
    }

    private Player resolvePlayerAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            return (Player) event.getDamager();
        }
        if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player) {
                return (Player) source;
            }
        }
        return null;
    }
}
