package com.dziubek.combatlog;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class HomeManager {

    public static final int MAX_HOMES = 10;

    private final CombatLogPlugin plugin;
    private final File file;
    private final FileConfiguration data;

    public HomeManager(CombatLogPlugin plugin) {
        this.plugin = plugin;

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Nie udało się utworzyć homes.yml: " + e.getMessage());
            }
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    private String path(UUID uuid, int slot) {
        return uuid.toString() + "." + slot;
    }

    public boolean isSet(UUID uuid, int slot) {
        return data.contains(path(uuid, slot) + ".world");
    }

    public Location getHome(UUID uuid, int slot) {
        String base = path(uuid, slot);
        if (!data.contains(base + ".world")) {
            return null;
        }

        World world = Bukkit.getWorld(data.getString(base + ".world"));
        if (world == null) {
            return null;
        }

        double x = data.getDouble(base + ".x");
        double y = data.getDouble(base + ".y");
        double z = data.getDouble(base + ".z");
        float yaw = (float) data.getDouble(base + ".yaw");
        float pitch = (float) data.getDouble(base + ".pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    public void setHome(UUID uuid, int slot, Location location) {
        String base = path(uuid, slot);
        data.set(base + ".world", location.getWorld().getName());
        data.set(base + ".x", location.getX());
        data.set(base + ".y", location.getY());
        data.set(base + ".z", location.getZ());
        data.set(base + ".yaw", location.getYaw());
        data.set(base + ".pitch", location.getPitch());
        save();
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Nie udało się zapisać homes.yml: " + e.getMessage());
        }
    }
}
