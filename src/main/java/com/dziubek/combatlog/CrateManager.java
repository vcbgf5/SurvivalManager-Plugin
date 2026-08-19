package com.dziubek.combatlog;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CrateManager {

    private final CombatLogPlugin plugin;
    private final File file;
    private final FileConfiguration data;
    private final NamespacedKey keyTag;

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
}
