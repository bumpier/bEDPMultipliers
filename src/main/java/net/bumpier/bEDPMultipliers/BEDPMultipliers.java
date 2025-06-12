// File: src/main/java/net/bumpier/bedpmultipliers/BEDPMultipliers.java
package net.bumpier.bedpmultipliers;

import net.bumpier.bedpmultipliers.commands.BMultiCommand;
import net.bumpier.bedpmultipliers.listeners.MultiplierListener;
import net.bumpier.bedpmultipliers.listeners.PlayerConnectionListener;
import net.bumpier.bedpmultipliers.managers.BossBarManager;
import net.bumpier.bedpmultipliers.managers.ConfigManager;
import net.bumpier.bedpmultipliers.managers.MultiplierManager;
import net.bumpier.bedpmultipliers.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class BEDPMultipliers extends JavaPlugin {

    private ConfigManager configManager;
    private MultiplierManager multiplierManager;
    private BossBarManager bossBarManager;
    private DebugLogger debugLogger;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("EdPrison") == null) {
            getLogger().severe("EdPrison not found! Disabling bEDPMultipliers.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.configManager = new ConfigManager(this);
        this.debugLogger = new DebugLogger(configManager);
        this.multiplierManager = new MultiplierManager(this, configManager, debugLogger);
        this.bossBarManager = new BossBarManager(this, configManager, multiplierManager);

        // Initialize the boss bar manager after all other managers are ready.
        bossBarManager.initialize();

        registerCommands();
        registerListeners();

        debugLogger.log("bEDPMultipliers has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (bossBarManager != null) {
            bossBarManager.shutdown();
        }
        if (multiplierManager != null) {
            multiplierManager.saveData();
        }
        if (debugLogger != null) {
            debugLogger.log("bEDPMultipliers has been disabled.");
        }
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("bmulti")).setExecutor(new BMultiCommand(multiplierManager, configManager));
        debugLogger.log("Commands registered.");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new MultiplierListener(multiplierManager, debugLogger), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(bossBarManager), this);
        debugLogger.log("Listeners registered.");
    }
}