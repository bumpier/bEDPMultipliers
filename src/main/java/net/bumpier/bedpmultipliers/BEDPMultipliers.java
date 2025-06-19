// File: src/main/java/net/bumpier/bedpmultipliers/BEDPMultipliers.java
package net.bumpier.bedpmultipliers;

import net.bumpier.bedpmultipliers.api.MultiplierPlaceholders;
import net.bumpier.bedpmultipliers.commands.BMultiCommand;
import net.bumpier.bedpmultipliers.listeners.MultiplierListener;
import net.bumpier.bedpmultipliers.listeners.PlayerConnectionListener;
import net.bumpier.bedpmultipliers.listeners.StorageGUIListener;
import net.bumpier.bedpmultipliers.listeners.VoucherListener;
import net.bumpier.bedpmultipliers.managers.*;
import net.bumpier.bedpmultipliers.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public final class BEDPMultipliers extends JavaPlugin {

    private MultiplierManager multiplierManager;
    private BukkitTask dataSaveTask;
    private ConfigManager configManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("EdPrison") == null) {
            getLogger().severe("EdPrison not found! Disabling bEDPMultipliers.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.configManager = new ConfigManager(this);
        this.guiManager = new GUIManager(this);
        DebugLogger debugLogger = new DebugLogger(configManager);
        this.multiplierManager = new MultiplierManager(this, configManager, debugLogger);
        VoucherManager voucherManager = new VoucherManager(this, configManager, debugLogger);
        BossBarManager bossBarManager = new BossBarManager(this, configManager, multiplierManager);
        StorageManager storageManager = new StorageManager(this, multiplierManager, voucherManager, configManager, guiManager);

        bossBarManager.initialize();

        BMultiCommand commandExecutor = new BMultiCommand(this, multiplierManager, configManager, voucherManager, storageManager);
        Objects.requireNonNull(getCommand("bmulti")).setExecutor(commandExecutor);
        Objects.requireNonNull(getCommand("bmulti")).setTabCompleter(commandExecutor);

        getServer().getPluginManager().registerEvents(new MultiplierListener(multiplierManager, debugLogger), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(bossBarManager), this);
        getServer().getPluginManager().registerEvents(new VoucherListener(this, multiplierManager, configManager, debugLogger), this);
        getServer().getPluginManager().registerEvents(new StorageGUIListener(storageManager, multiplierManager, voucherManager, configManager, guiManager), this);


        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MultiplierPlaceholders(this, multiplierManager).register();
            if (configManager.isDebug()) {
                debugLogger.log("Successfully hooked into PlaceholderAPI.");
            }
        }

        startDataSaveTask();
        debugLogger.log("bEDPMultipliers has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (dataSaveTask != null) {
            dataSaveTask.cancel();
        }
        if (multiplierManager != null) {
            multiplierManager.saveData(true);
        }
    }

    public void reloadPlugin() {
        if (dataSaveTask != null) {
            dataSaveTask.cancel();
        }
        this.configManager.loadConfigs();
        this.guiManager.loadGuiConfigs();
        if (multiplierManager != null) {
            multiplierManager.saveData(true);
        }
        startDataSaveTask();
        getLogger().info("bEDPMultipliers has been reloaded.");
    }

    private void startDataSaveTask() {
        long saveInterval = 20 * 60 * 5; // 5 minutes
        this.dataSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (this.multiplierManager != null) {
                this.multiplierManager.saveData(false);
            }
        }, saveInterval, saveInterval);
    }
}