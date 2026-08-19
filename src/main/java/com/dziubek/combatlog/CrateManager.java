package com.dziubek.combatlog;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CrateManager {

    private final CombatLogPlugin plugin;
    private final File file;
    private final FileConfiguration data;
    private final NamespacedKey keyTag;

    // blockKey ("world,x,y,z") -> nazwa skrzyni, przebudowywana po każdej zmianie
    private final Map<String, String> locationCache = new HashMap<>();

    public CrateManager(CombatLogPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.file = new File(plugin.getDataFolder(), "crates.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Nie udało się utworzyć crates.yml: " + e.getMessage());
            }
        }
        this.data = YamlConfiguration.loadConfiguration(file);
        this.keyTag = new NamespacedKey(plugin, "crate_key");
        rebuildLocationCache();
    }

    public boolean exists(String name) {
        return data.contains(name);
    }

    public List<String> names() {
        return new ArrayList<>(data.getKeys(false));
    }

    public void saveCrate(String name, ItemStack[] contents) {
        data.set(name + ".rewards", null);
        int idx = 0;
        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            data.set(name + ".rewards." + idx + ".item", item);
            idx++;
        }
        save();
    }

    /**
     * Ilość przedmiotu w slocie = zarówno WAGA (szansa) jak i ILOŚĆ jaką gracz dostanie po wylosowaniu.
     */
    public List<ItemStack> getRewards(String name) {
        List<ItemStack> list = new ArrayList<>();
        ConfigurationSection section = data.getConfigurationSection(name + ".rewards");
        if (section == null) {
            return list;
        }
        for (String key : section.getKeys(false)) {
            ItemStack item = data.getItemStack(name + ".rewards." + key + ".item");
            if (item != null) {
                list.add(item);
            }
        }
        return list;
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Nie udało się zapisać crates.yml: " + e.getMessage());
        }
    }

    public ItemStack createKey(String crateName, int amount) {
        ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK, amount);
        ItemMeta meta = key.getItemMeta();
        meta.setDisplayName("§e§lKlucz do skrzyni: §f" + crateName);

        List<String> lore = new ArrayList<>();
        lore.add("§7Kliknij PRAWYM przyciskiem,");
        lore.add("§7aby otworzyć skrzynię!");
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(keyTag, PersistentDataType.STRING, crateName);
        key.setItemMeta(meta);
        return key;
    }

    public String getKeyCrateName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(keyTag, PersistentDataType.STRING);
    }

    // ===================== Fizyczne skrzynie w świecie =====================

    public boolean bindLocation(String name, Location location) {
        if (!exists(name) || location.getWorld() == null) {
            return false;
        }

        String id = UUID.randomUUID().toString();
        String path = name + ".locations." + id;
        data.set(path + ".world", location.getWorld().getName());
        data.set(path + ".x", location.getBlockX());
        data.set(path + ".y", location.getBlockY());
        data.set(path + ".z", location.getBlockZ());
        save();
        rebuildLocationCache();
        createHologram(name, id, location);
        return true;
    }

    /**
     * Odpina fizyczną skrzynię wskazaną blokiem. Zwraca nazwę configu skrzyni, który został odpięty, lub null.
     */
    public String unbindLocation(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        String world = location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        for (String name : names()) {
            ConfigurationSection locations = data.getConfigurationSection(name + ".locations");
            if (locations == null) {
                continue;
            }
            for (String id : new ArrayList<>(locations.getKeys(false))) {
                String base = name + ".locations." + id;
                if (world.equals(data.getString(base + ".world"))
                        && x == data.getInt(base + ".x")
                        && y == data.getInt(base + ".y")
                        && z == data.getInt(base + ".z")) {
                    data.set(base, null);
                    save();
                    rebuildLocationCache();
                    removeHologram(name, id);
                    return name;
                }
            }
        }
        return null;
    }

    public String getCrateNameAt(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        return locationCache.get(blockKey(location));
    }

    public int countLocations(String name) {
        ConfigurationSection locations = data.getConfigurationSection(name + ".locations");
        return locations == null ? 0 : locations.getKeys(false).size();
    }

    public CrateEffect getEffect(String name) {
        CrateEffect effect = CrateEffect.fromString(data.getString(name + ".effect"));
        return effect != null ? effect : CrateEffect.FLAME;
    }

    public void setEffect(String name, CrateEffect effect) {
        data.set(name + ".effect", effect.name());
        save();
        refreshHolograms(name);
    }

    public String getHologramTitle(String name) {
        return data.getString(name + ".hologram-title", "&6&l✦ " + name + " ✦");
    }

    public void setHologramTitle(String name, String title) {
        data.set(name + ".hologram-title", title);
        save();
        refreshHolograms(name);
    }

    public void refreshAllHolograms() {
        for (String name : names()) {
            refreshHolograms(name);
        }
    }

    private void refreshHolograms(String name) {
        if (!plugin.getDecentHolograms().isAvailable()) {
            return;
        }
        ConfigurationSection locations = data.getConfigurationSection(name + ".locations");
        if (locations == null) {
            return;
        }
        for (String id : locations.getKeys(false)) {
            Location location = readLocation(name, id);
            if (location != null) {
                createHologram(name, id, location);
            }
        }
    }

    private Location readLocation(String name, String id) {
        String base = name + ".locations." + id;
        String worldName = data.getString(base + ".world");
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, data.getInt(base + ".x"), data.getInt(base + ".y"), data.getInt(base + ".z"));
    }

    private void createHologram(String name, String id, Location blockLocation) {
        Location above = blockLocation.clone().add(0.5, 1.5, 0.5);
        plugin.getDecentHolograms().createInfoHologram(hologramId(name, id), above, buildHologramLines(name));
    }

    private void removeHologram(String name, String id) {
        plugin.getDecentHolograms().removeHologram(hologramId(name, id));
    }

    private String hologramId(String name, String id) {
        return "crate_" + name + "_" + id.substring(0, 8);
    }

    private List<String> buildHologramLines(String name) {
        List<String> lines = new ArrayList<>();
        lines.add(getHologramTitle(name));
        lines.add("&7Kliknij PPM z kluczem, by otworzyć!");
        lines.add("&8Efekt: &f" + getEffect(name).getDisplayName());
        return lines;
    }

    private void rebuildLocationCache() {
        locationCache.clear();
        for (String name : names()) {
            ConfigurationSection locations = data.getConfigurationSection(name + ".locations");
            if (locations == null) {
                continue;
            }
            for (String id : locations.getKeys(false)) {
                String base = name + ".locations." + id;
                String world = data.getString(base + ".world");
                if (world == null) {
                    continue;
                }
                int x = data.getInt(base + ".x");
                int y = data.getInt(base + ".y");
                int z = data.getInt(base + ".z");
                locationCache.put(world + "," + x + "," + y + "," + z, name);
            }
        }
    }

    private String blockKey(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }
}
