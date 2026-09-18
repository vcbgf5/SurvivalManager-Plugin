package com.dziubek.combatlog;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pływający ItemDisplay nad każdą fizyczną skrzynią. Gdy nikt jej nie otwiera, co sekundę
 * przełącza się na kolejny przedmiot z możliwych nagród (w kółko, w kolejności z configu),
 * cały czas wirując. Zaraz po wylosowaniu nagrody pokazuje przez chwilę dokładnie wylosowany
 * przedmiot z szybszym, mocniejszym obrotem, po czym sam wraca do normalnego przełączania.
 */
public class CrateItemDisplayManager {

    private static final String TAG = "sm_crate_item_display";
    private static final double HEIGHT_ABOVE_BLOCK = 1.35;
    private static final long IDLE_PERIOD_MS = 2200;
    private static final long HIGHLIGHT_PERIOD_MS = 450;
    private static final long HIGHLIGHT_DURATION_MS = 3000;
    private static final long CYCLE_INTERVAL_TICKS = 20L;
    private static final long SPIN_INTERVAL_TICKS = 2L;

    private final CombatLogPlugin plugin;
    private final NamespacedKey ownerTag;
    private final Map<String, Entry> entries = new HashMap<>();

    public CrateItemDisplayManager(CombatLogPlugin plugin) {
        this.plugin = plugin;
        this.ownerTag = new NamespacedKey(plugin, "crate_item_display_owner");
    }

    public void start() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::spin, 0L, SPIN_INTERVAL_TICKS);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cycle, 0L, CYCLE_INTERVAL_TICKS);
    }

    /**
     * Usuwa wszystkie pływające displaye - wywoływane przy wyłączaniu/przeładowaniu pluginu,
     * żeby nie zostawić "osieroconych" encji do czasu ponownego /reload.
     */
    public void shutdown() {
        for (Entry entry : entries.values()) {
            if (entry.display.isValid()) {
                entry.display.remove();
            }
        }
        entries.clear();
    }

    /**
     * Tworzy pływający display nad podanym blokiem skrzyni. Usuwa najpierw ewentualne
     * "osierocone" encje sprzed restartu serwera (te trzymane w tej mapie giną razem z JVM,
     * ale same encje w świecie zostają, więc trzeba je posprzątać, zanim postawimy nową).
     */
    public void spawnDisplay(String crateName, Location blockLocation) {
        World world = blockLocation.getWorld();
        if (world == null) {
            return;
        }
        removeStrayEntities(blockLocation);

        Location spawnAt = blockLocation.clone().add(0.5, HEIGHT_ABOVE_BLOCK, 0.5);
        List<CrateReward> rewards = plugin.getCrates().getRewards(crateName);

        ItemDisplay display = world.spawn(spawnAt, ItemDisplay.class, e -> {
            e.setBillboard(Display.Billboard.FIXED);
            e.setGravity(false);
            e.setPersistent(false);
            e.setInvulnerable(true);
            e.getPersistentDataContainer().set(ownerTag, PersistentDataType.STRING, crateName);
            e.addScoreboardTag(TAG);
            if (!rewards.isEmpty()) {
                e.setItemStack(rewards.get(0).item().clone());
            }
        });

        entries.put(blockKey(blockLocation), new Entry(display, crateName));
    }

    public void removeDisplay(Location blockLocation) {
        Entry entry = entries.remove(blockKey(blockLocation));
        if (entry != null && entry.display.isValid()) {
            entry.display.remove();
        }
        removeStrayEntities(blockLocation);
    }

    /**
     * Wywoływane po zakończeniu losowania (CrateRollAnimation) - pokazuje nad konkretną,
     * fizycznie klikniętą skrzynią dokładnie wylosowany przedmiot z mocniejszym obrotem
     * przez kilka sekund, po czym samo przełączanie nagród co sekundę wraca do normy.
     */
    public void highlightWin(Location blockLocation, ItemStack won) {
        Entry entry = entries.get(blockKey(blockLocation));
        if (entry == null) {
            return;
        }
        entry.highlightUntil = System.currentTimeMillis() + HIGHLIGHT_DURATION_MS;
        if (entry.display.isValid()) {
            entry.display.setItemStack(won.clone());
        }
    }

    private void spin() {
        long now = System.currentTimeMillis();
        for (Entry entry : entries.values()) {
            ItemDisplay display = entry.display;
            if (!display.isValid()) {
                continue;
            }
            boolean highlighted = now < entry.highlightUntil;
            long period = highlighted ? HIGHLIGHT_PERIOD_MS : IDLE_PERIOD_MS;
            float angle = (float) ((now % period) / (double) period * Math.PI * 2);
            float bob = (float) (Math.sin(now / 500.0) * 0.05);
            float scale = highlighted ? 0.85f : 0.6f;

            Transformation transform = new Transformation(
                    new Vector3f(0f, bob, 0f),
                    new Quaternionf(new AxisAngle4f(angle, 0f, 1f, 0f)),
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()
            );
            display.setInterpolationDelay(0);
            display.setInterpolationDuration((int) SPIN_INTERVAL_TICKS);
            display.setTransformation(transform);
        }
    }

    private void cycle() {
        long now = System.currentTimeMillis();
        for (Entry entry : entries.values()) {
            if (now < entry.highlightUntil) {
                continue; // trwa pokaz wygranej - nie nadpisuj jej kolejną losową nagrodą
            }
            List<CrateReward> rewards = plugin.getCrates().getRewards(entry.crateName);
            if (rewards.isEmpty()) {
                continue;
            }
            entry.rewardIndex = (entry.rewardIndex + 1) % rewards.size();
            if (entry.display.isValid()) {
                entry.display.setItemStack(rewards.get(entry.rewardIndex).item().clone());
            }
        }
    }

    private void removeStrayEntities(Location blockLocation) {
        World world = blockLocation.getWorld();
        if (world == null) {
            return;
        }
        Location center = blockLocation.clone().add(0.5, HEIGHT_ABOVE_BLOCK, 0.5);
        for (Entity entity : world.getNearbyEntities(center, 0.6, 0.6, 0.6)) {
            if (entity instanceof ItemDisplay && entity.getScoreboardTags().contains(TAG)) {
                entity.remove();
            }
        }
    }

    private static String blockKey(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private static final class Entry {
        final ItemDisplay display;
        final String crateName;
        int rewardIndex = 0;
        long highlightUntil = 0L;

        Entry(ItemDisplay display, String crateName) {
            this.display = display;
            this.crateName = crateName;
        }
    }
}
