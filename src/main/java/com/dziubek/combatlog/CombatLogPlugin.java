package com.dziubek.combatlog;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

public class CombatLogPlugin extends JavaPlugin {

    private CombatManager combatManager;
    private Location survivalSpawn;
    private TpaManager tpa;
    private HomeManager homes;
    private HomeGuiManager homeGui;
    private KitManager kits;
    private DailyRewardManager daily;
    private CrateManager crates;
    private ShopManager shop;
    private ShopGuiManager shopGui;
    private Economy economy;
    private TeleportDelayManager teleportDelay;
    private ShopConfigManager shopConfig;
    private ShopConfigGuiManager shopConfigGui;
    private DecentHologramsHook decentHolograms;
    private CrateRewardSessionManager crateRewardSessions;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        combatManager = new CombatManager();
        loadSurvivalSpawn();

        tpa = new TpaManager(this);
        homes = new HomeManager(this);
        homeGui = new HomeGuiManager(this);
        kits = new KitManager(this);
        daily = new DailyRewardManager(this);
        crates = new CrateManager(this);
        shop = new ShopManager(this);
        shopGui = new ShopGuiManager(this);
        teleportDelay = new TeleportDelayManager(this);
        shopConfig = new ShopConfigManager();
        shopConfigGui = new ShopConfigGuiManager(this);
        decentHolograms = new DecentHologramsHook(this);
        crateRewardSessions = new CrateRewardSessionManager();
        crates.refreshAllHolograms();

        setupEconomy();

        getServer().getPluginManager().registerEvents(new CombatDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatKickListener(this), this);
        getServer().getPluginManager().registerEvents(new FirstJoinSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new HomeGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new DailyGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new CrateGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new CrateKeyListener(this), this);
        getServer().getPluginManager().registerEvents(new CrateBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new CrateRewardChatListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopConfigGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopChatListener(this), this);

        getCommand("testcombat").setExecutor(new TestCombatCommand(this));
        getCommand("testcombatoff").setExecutor(new TestCombatOffCommand(this));
        getCommand("testcombatstatus").setExecutor(new TestCombatStatusCommand(this));
        getCommand("setsurvivalspawn").setExecutor(new SetSurvivalSpawnCommand(this));
        getCommand("spawn").setExecutor(new LocalSpawnCommand(this));
        getCommand("tpa").setExecutor(new TpaCommand(this));
        getCommand("tpaccept").setExecutor(new TpaAcceptCommand(this));
        getCommand("tpdeny").setExecutor(new TpaDenyCommand(this));
        getCommand("home").setExecutor(new HomeCommand(this));
        getCommand("kit").setExecutor(new KitCommand(this));
        getCommand("daily").setExecutor(new DailyCommand(this));
        getCommand("crate").setExecutor(new CrateCommand(this));
        getCommand("fixmovement").setExecutor(new FixMovementCommand());
        getCommand("sklep").setExecutor(new ShopCommand(this));
        getCommand("infoholo").setExecutor(new InfoHologramCommand(this));
        getCommand("info-serwer").setExecutor(new InfoServerCommand(this));

        getServer().getScheduler().runTaskTimer(this, new CombatActionBarTask(this), 20L, 20L);

        boolean worldGuardFound = getServer().getPluginManager().getPlugin("WorldGuard") != null;
        if (worldGuardFound) {
            getServer().getPluginManager().registerEvents(new SpawnRegionGuardListener(this), this);
            getLogger().info("Wykryto WorldGuard - blokada regionu '" + getProtectedRegionName() + "' podczas walki aktywna.");
        } else {
            getLogger().warning("WorldGuard nie znaleziony - blokada regionu podczas walki WYŁĄCZONA.");
        }

        if (economy == null) {
            getLogger().warning("Vault + system ekonomii (np. EssentialsX) NIE znaleziony - /sklep nie będzie działać dopóki go nie zainstalujesz.");
        } else {
            getLogger().info("Ekonomia podłączona przez Vault: " + economy.getName());
        }

        getLogger().info("CombatLog włączony! Czas walki: " + getCombatDurationSeconds() + "s, spawn survivala ustawiony: " + hasSurvivalSpawn());
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public long getCombatDurationSeconds() {
        return getConfig().getLong("combat-duration-seconds", 10);
    }

    public String getProtectedRegionName() {
        return getConfig().getString("protected-region", "Spawn01");
    }

    public String msg(String key, String def) {
        String raw = getConfig().getString("messages." + key, def);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', raw);
    }

    public boolean hasSurvivalSpawn() {
        return survivalSpawn != null;
    }

    public Location getSurvivalSpawn() {
        return survivalSpawn;
    }

    public void setSurvivalSpawn(Location location) {
        this.survivalSpawn = location;

        FileConfiguration cfg = getConfig();
        cfg.set("survival-spawn.world", location.getWorld().getName());
        cfg.set("survival-spawn.x", location.getX());
        cfg.set("survival-spawn.y", location.getY());
        cfg.set("survival-spawn.z", location.getZ());
        cfg.set("survival-spawn.yaw", location.getYaw());
        cfg.set("survival-spawn.pitch", location.getPitch());
        saveConfig();
    }

    private void loadSurvivalSpawn() {
        FileConfiguration cfg = getConfig();
        String worldName = cfg.getString("survival-spawn.world");
        if (worldName == null) {
            survivalSpawn = null;
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            getLogger().warning("Świat '" + worldName + "' zapisany jako spawn survivala nie istnieje (jeszcze?).");
            survivalSpawn = null;
            return;
        }

        double x = cfg.getDouble("survival-spawn.x");
        double y = cfg.getDouble("survival-spawn.y");
        double z = cfg.getDouble("survival-spawn.z");
        float yaw = (float) cfg.getDouble("survival-spawn.yaw");
        float pitch = (float) cfg.getDouble("survival-spawn.pitch");

        survivalSpawn = new Location(world, x, y, z, yaw, pitch);
    }

    public TpaManager getTpa() {
        return tpa;
    }

    public HomeManager getHomes() {
        return homes;
    }

    public HomeGuiManager getHomeGui() {
        return homeGui;
    }

    public KitManager getKits() {
        return kits;
    }

    public DailyRewardManager getDaily() {
        return daily;
    }

    public CrateManager getCrates() {
        return crates;
    }

    public ShopManager getShop() {
        return shop;
    }

    public ShopGuiManager getShopGui() {
        return shopGui;
    }

    public TeleportDelayManager getTeleportDelay() {
        return teleportDelay;
    }

    public ShopConfigManager getShopConfig() {
        return shopConfig;
    }

    public ShopConfigGuiManager getShopConfigGui() {
        return shopConfigGui;
    }

    public DecentHologramsHook getDecentHolograms() {
        return decentHolograms;
    }

    public CrateRewardSessionManager getCrateRewardSessions() {
        return crateRewardSessions;
    }
}
