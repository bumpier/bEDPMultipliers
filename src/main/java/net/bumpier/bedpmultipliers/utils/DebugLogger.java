// File: src/main/java/net/bumpier/bedpmultipliers/utils/DebugLogger.java
package net.bumpier.bedpmultipliers.utils;

import net.bumpier.bedpmultipliers.managers.ConfigManager;
import org.bukkit.Bukkit;

public class DebugLogger {
    private final ConfigManager configManager;

    public DebugLogger(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void log(String message) {
        if (configManager.isDebug()) {
            Bukkit.getLogger().info("[bedpmultipliers][DEBUG] " + message);
        }
    }
}