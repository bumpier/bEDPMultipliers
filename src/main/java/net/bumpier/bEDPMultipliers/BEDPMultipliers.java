// File: src/main/java/net/bumpier/bedpmultipliers/BEDPMultipliers.java
package net.bumpier.bedpmultipliers;

import net.bumpier.bedpmultipliers.api.MultiplierPlaceholders;
import net.bumpier.bedpmultipliers.commands.BMultiCommand;
import net.bumpier.bedpmultipliers.listeners.MultiplierListener;
import net.bumpier.bedpmultipliers.listeners.PlayerConnectionListener;
import net.bumpier.bedpmultipliers.managers.BossBarManager;
import net.bumpier.bedpmultipliers.managers.ConfigManager;
import net.bumpier.bedpmultipliers.managers.MultiplierManager;
import net.bumpier.bedpmultipliers.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public final class BEDPMultipliers extends JavaPlugin {

    private final ConfigManager configManager = new ConfigManager(this);
    private final DebugLogger debugLogger = new DebugLogger(configManager);
    private final MultiplierManager multiplierManager = new MultiplierManager(this, configManager, debugLogger);
    private final BossBarManager bossBarManager = new BossBarManager(this, configManager, multiplierManager);
    private BukkitTask dataSaveTask;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("EdPrison") == null) {
            getLogger().severe("EdPrison not found! Disabling bEDPMultipliers.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        bossBarManager.initialize();

        BMultiCommand commandExecutor = new BMultiCommand(this, multiplierManager, configManager);
        Objects.requireNonNull(getCommand("bmulti")).setExecutor(commandExecutor);
        Objects.requireNonNull(getCommand("bmulti")).setTabCompleter(commandExecutor);

        getServer().getPluginManager().registerEvents(new MultiplierListener(multiplierManager, debugLogger), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(bossBarManager), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MultiplierPlaceholders(this, multiplierManager).register();
            debugLogger.log("Successfully hooked into PlaceholderAPI.");
        }

        // Schedule periodic data saving
        startDataSaveTask();

        debugLogger.log("bEDPMultipliers has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (dataSaveTask != null) {
            dataSaveTask.cancel();
        }
        bossBarManager.shutdown();
        multiplierManager.saveData(true); // Force save on shutdown
        debugLogger.log("bEDPMultipliers has been disabled.");
    }

    public void reloadPlugin() {
        debugLogger.log("Reloading plugin configurations...");
        configManager.loadConfigs();

        bossBarManager.shutdown();
        bossBarManager.initialize();

        if (dataSaveTask != null) {
            dataSaveTask.cancel();
        }
        startDataSaveTask();

        debugLogger.log("Plugin configurations reloaded.");
    }

    private void startDataSaveTask() {
        // Save data every 5 minutes (20 ticks/sec * 60 sec/min * 5 min)
        long saveInterval = 20 * 60 * 5;
        this.dataSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            multiplierManager.saveData(false); // Save only if dirty
        }, saveInterval, saveInterval);
        debugLogger.log("Data saving task scheduled to run every 5 minutes.");
    }
}