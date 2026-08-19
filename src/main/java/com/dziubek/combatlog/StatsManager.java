package com.dziubek.combatlog;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class StatsManager {

    private final CombatLogPlugin plugin;
    private final File file;
    private final FileConfiguration data;

    public StatsManager(CombatLogPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.file = new File(plugin.getDataFolder(), "playerstats.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Nie udało się utworzyć playerstats.yml: " + e.getMessage());
            }
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public void recordCrateOpened(UUID uuid, String name) {
        increment(uuid, name, "crates-opened");
    }

    public void recordDailyClaim(UUID uuid, String name) {
        increment(uuid, name, "daily-claims");
    }

    public void recordMoneySpent(UUID uuid, String name, double amount) {
        String path = "players." + uuid + ".money-spent";
        data.set(path, data.getDouble(path, 0) + amount);
        touchName(uuid, name);
        save();
    }

    public int getCratesOpened(UUID uuid) {
        return data.getInt("players." + uuid + ".crates-opened", 0);
    }

    public int getDailyClaims(UUID uuid) {
        return data.getInt("players." + uuid + ".daily-claims", 0);
    }

    public double getMoneySpent(UUID uuid) {
        return data.getDouble("players." + uuid + ".money-spent", 0);
    }

    private void increment(UUID uuid, String name, String key) {
        String path = "players." + uuid + "." + key;
        data.set(path, data.getInt(path, 0) + 1);
        touchName(uuid, name);
        save();
    }

    private void touchName(UUID uuid, String name) {
        if (name != null) {
            data.set("players." + uuid + ".name", name);
        }
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Nie udało się zapisać playerstats.yml: " + e.getMessage());
        }
    }
}
