package com.dziubek.combatlog;

import org.bukkit.Location;
import org.bukkit.Material;
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
    private static final double DEFAULT_HEIGHT_ABOVE_BLOCK = 3.5;
    private static final long IDLE_PERIOD_MS = 2200;
    private static final long HIGHLIGHT_PERIOD_MS = 450;
    private static final long HIGHLIGHT_DURATION_MS = 3000;
    private static final long CYCLE_INTERVAL_TICKS = 20L;
    private static final long SPIN_INTERVAL_TICKS = 2L;
    private static final long RECONCILE_INTERVAL_TICKS = 100L;

    // "spadanie" wygranej z gory na miejsce spoczynku, zwalniajac pod koniec (ease-out)
    private static final double DROP_START_OFFSET = 4.0;
    private static final long DROP_DURATION_MS = 1100;

    private final CombatLogPlugin plugin;
    private final NamespacedKey ownerTag;
    private final Map<String, Entry> entries = new HashMap<>();
    private double heightAboveBlock;

    public CrateItemDisplayManager(CombatLogPlugin plugin) {
        this.plugin = plugin;
        this.ownerTag = new NamespacedKey(plugin, "crate_item_display_owner");
        this.heightAboveBlock = plugin.getConfig().getDouble("crate-item-display.height", DEFAULT_HEIGHT_ABOVE_BLOCK);
    }

    public void start() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::spin, 0L, SPIN_INTERVAL_TICKS);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cycle, 0L, CYCLE_INTERVAL_TICKS);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::reconcile, RECONCILE_INTERVAL_TICKS, RECONCILE_INTERVAL_TICKS);
    }

    public double getHeight() {
        return heightAboveBlock;
    }

    /**
     * Usuwa WSZYSTKIE oznaczone naszym tagiem encje we wszystkich załadowanych światach,
     * niezależnie od pozycji - wywoływane raz przy starcie pluginu, zanim postawimy świeży
     * zestaw displayów. Łapie "osierocone" encje sprzed zmiany wysokości/kolejnych /reload,
     * których nie znalazłoby przeszukiwanie samej okolicy jednego bloku - to właśnie one
     * zostają wtedy w świecie na stałe: bez skalowania i animacji (nikt już ich nie ożywia),
     * więc wyglądają jak duży, nieruchomy przedmiot obok normalnie kręcącego się displaya.
     */
    public void purgeOrphans() {
        int removed = 0;
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(ItemDisplay.class)) {
                if (entity.getScoreboardTags().contains(TAG)) {
                    entity.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("Usunięto " + removed + " osieroconych pływających przedmiotów nad skrzyniami sprzed restartu.");
        }
    }

    /**
     * Zmienia wysokość na żywo, bez restartu - zapisuje w config.yml i od razu przestawia
     * wszystkie już postawione displaye na nową wysokość (teleportacja, bez ich niszczenia).
     */
    public void setHeight(double newHeight) {
        this.heightAboveBlock = newHeight;
        plugin.getConfig().set("crate-item-display.height", newHeight);
        plugin.saveConfig();

        for (Entry entry : entries.values()) {
            if (!entry.display.isValid()) {
                continue;
            }
            Location newAnchor = entry.blockLocation.clone().add(0.5, heightAboveBlock, 0.5);
            entry.display.teleport(newAnchor);
        }
    }

    /**
     * Co RECONCILE_INTERVAL_TICKS sprawdza, czy każda przypięta fizyczna skrzynia ma żywy
     * pływający display - jeśli brakuje (np. coś go usunęło), stawia nowy.
     */
    private void reconcile() {
        for (String name : plugin.getCrates().names()) {
            for (Location location : plugin.getCrates().getAllLocations(name)) {
                Entry entry = entries.get(blockKey(location));
                if (entry == null || !entry.display.isValid()) {
                    spawnDisplay(name, location);
                }
            }
        }
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

        Location spawnAt = blockLocation.clone().add(0.5, heightAboveBlock, 0.5);
        List<CrateReward> rewards = plugin.getCrates().getRewards(crateName);
        if (rewards.isEmpty()) {
            plugin.getLogger().warning("Skrzynia '" + crateName + "' nie ma jeszcze skonfigurowanych nagród - "
                    + "pływający przedmiot nad nią pokaże tylko zastępczą ikonę skrzyni.");
        }

        ItemDisplay display = world.spawn(spawnAt, ItemDisplay.class, e -> {
            e.setBillboard(Display.Billboard.FIXED);
            e.setGravity(false);
            e.setPersistent(false);
            e.setInvulnerable(true);
            e.getPersistentDataContainer().set(ownerTag, PersistentDataType.STRING, crateName);
            e.addScoreboardTag(TAG);
            e.setItemStack(rewards.isEmpty() ? placeholderItem() : rewards.get(0).item().clone());
        });

        entries.put(blockKey(blockLocation), new Entry(display, crateName, blockLocation.clone()));
        plugin.getLogger().info("Postawiono pływający przedmiot nad skrzynią '" + crateName + "' w "
                + world.getName() + " (" + blockLocation.getBlockX() + "," + blockLocation.getBlockY()
                + "," + blockLocation.getBlockZ() + ").");
    }

    private static ItemStack placeholderItem() {
        return new ItemStack(Material.CHEST);
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
        long now = System.currentTimeMillis();
        entry.highlightUntil = now + HIGHLIGHT_DURATION_MS;
        entry.dropStartAt = now;
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

            float translateY = bob;
            if (entry.dropStartAt > 0) {
                long elapsed = now - entry.dropStartAt;
                if (elapsed < DROP_DURATION_MS) {
                    double t = Math.min(1.0, elapsed / (double) DROP_DURATION_MS);
                    translateY += (float) ((1.0 - easeOutCubic(t)) * DROP_START_OFFSET);
                } else {
                    entry.dropStartAt = 0L;
                }
            }

            Transformation transform = new Transformation(
                    new Vector3f(0f, translateY, 0f),
                    new Quaternionf(new AxisAngle4f(angle, 0f, 1f, 0f)),
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()
            );
            display.setInterpolationDelay(0);
            display.setInterpolationDuration((int) SPIN_INTERVAL_TICKS);
            display.setTransformation(transform);
        }
    }

    /**
     * Szybki start, płynne zwolnienie pod koniec - "spadający" przedmiot dobija do miejsca
     * spoczynku bez szarpnięcia.
     */
    private static double easeOutCubic(double t) {
        double f = t - 1.0;
        return f * f * f + 1.0;
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

    /**
     * Szuka w kolumnie nad blokiem (nie tylko na aktualnej wysokości) - żeby złapać też
     * displaye postawione zanim wysokość została zmieniona komendą /crate setdisplayheight.
     */
    private void removeStrayEntities(Location blockLocation) {
        World world = blockLocation.getWorld();
        if (world == null) {
            return;
        }
        Location center = blockLocation.clone().add(0.5, 3.5, 0.5);
        for (Entity entity : world.getNearbyEntities(center, 0.6, 4.0, 0.6)) {
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
        final Location blockLocation;
        int rewardIndex = 0;
        long highlightUntil = 0L;
        long dropStartAt = 0L;

        Entry(ItemDisplay display, String crateName, Location blockLocation) {
            this.display = display;
            this.crateName = crateName;
            this.blockLocation = blockLocation;
        }
    }
}
