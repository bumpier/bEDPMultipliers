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

import java.util.Objects;

public final class BEDPMultipliers extends JavaPlugin {

    private ConfigManager configManager;
    private BossBarManager bossBarManager;
    private DebugLogger debugLogger;
    private MultiplierManager multiplierManager;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("EdPrison") == null) {
            getLogger().severe("EdPrison not found! Disabling bEDPMultipliers.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Simplified and more robust initialization order
        this.configManager = new ConfigManager(this);
        this.debugLogger = new DebugLogger(configManager);
        this.multiplierManager = new MultiplierManager(this, configManager, debugLogger);
        this.bossBarManager = new BossBarManager(this, configManager, multiplierManager);

        bossBarManager.initialize();

        BMultiCommand commandExecutor = new BMultiCommand(this, multiplierManager, configManager);
        Objects.requireNonNull(getCommand("bmulti")).setExecutor(commandExecutor);
        Objects.requireNonNull(getCommand("bmulti")).setTabCompleter(commandExecutor);

        getServer().getPluginManager().registerEvents(new MultiplierListener(multiplierManager, debugLogger), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(bossBarManager), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MultiplierPlaceholders(this, multiplierManager).register();
            debugLogger.log("Successfully hooked into PlaceholderAPI.");
        } else {
            debugLogger.log("PlaceholderAPI not found, skipping hook.");
        }

        debugLogger.log("bEDPMultipliers has been enabled successfully.");
    }

    public void reloadPlugin() {
        debugLogger.log("Reloading plugin configurations...");
        configManager.loadConfigs();
        bossBarManager.shutdown();
        bossBarManager.initialize();
        debugLogger.log("Plugin configurations reloaded.");
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
}