// File: src/main/java/net/bumpier/bedpmultipliers/managers/MultiplierManager.java
package net.bumpier.bedpmultipliers.managers;

import net.bumpier.bedpmultipliers.utils.DebugLogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class MultiplierManager {

    private final JavaPlugin plugin;
    private final net.bumpier.bedpmultipliers.managers.ConfigManager configManager;
    private final DebugLogger debugLogger;

    private File dataFile;
    private FileConfiguration dataConfig;

    public MultiplierManager(JavaPlugin plugin, net.bumpier.bedpmultipliers.managers.ConfigManager configManager, DebugLogger debugLogger) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.debugLogger = debugLogger;
        loadData();
    }

    public void loadData() {
        if (dataFile == null) {
            dataFile = new File(plugin.getDataFolder(), "data.yml");
        }
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml!");
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        debugLogger.log("Data file loaded.");
    }

    public void saveData() {
        try {
            dataConfig.save(dataFile);
            debugLogger.log("Data file saved.");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml!");
            e.printStackTrace();
        }
    }

    public double getTotalMultiplier(OfflinePlayer player) {
        double total = configManager.getGlobalMultiplier();
        total += getPlayerMultiplier(player.getUniqueId()) - 1.0;
        total += getPlayerTempMultiplier(player.getUniqueId()) - 1.0;
        total += getGlobalTempMultiplier() - 1.0;
        total += getPermissionMultiplier(player) - 1.0;
        return Math.max(0, total);
    }

    public double getPlayerMultiplier(UUID uuid) {
        return dataConfig.getDouble("players." + uuid + ".multiplier", 1.0);
    }

    public void setPlayerMultiplier(UUID uuid, double amount) {
        dataConfig.set("players." + uuid + ".multiplier", amount);
        saveData();
    }

    public void removePlayerMultiplier(UUID uuid) {
        dataConfig.set("players." + uuid, null);
        saveData();
    }

    public double getPlayerTempMultiplier(UUID uuid) {
        if (getPlayerTempExpiry(uuid) > System.currentTimeMillis()) {
            return dataConfig.getDouble("players." + uuid + ".temp-multiplier", 1.0);
        }
        return 1.0;
    }

    public long getPlayerTempExpiry(UUID uuid) {
        return dataConfig.getLong("players." + uuid + ".temp-expiry", 0);
    }

    public long getPlayerTempDuration(UUID uuid) {
        return dataConfig.getLong("players." + uuid + ".temp-duration", 0);
    }

    public void setPlayerTempMultiplier(UUID uuid, double amount, long duration) {
        dataConfig.set("players." + uuid + ".temp-multiplier", amount);
        dataConfig.set("players." + uuid + ".temp-duration", duration);
        dataConfig.set("players." + uuid + ".temp-expiry", System.currentTimeMillis() + duration);
        saveData();
    }

    public double getGlobalTempMultiplier() {
        if (getGlobalTempExpiry() > System.currentTimeMillis()) {
            return dataConfig.getDouble("global.temp-multiplier", 1.0);
        }
        return 1.0;
    }

    public long getGlobalTempExpiry() {
        return dataConfig.getLong("global.temp-expiry", 0);
    }

    public long getGlobalTempDuration() {
        return dataConfig.getLong("global.temp-duration", 0);
    }

    public void setGlobalTempMultiplier(double amount, long duration) {
        dataConfig.set("global.temp-multiplier", amount);
        dataConfig.set("global.temp-duration", duration);
        dataConfig.set("global.temp-expiry", System.currentTimeMillis() + duration);
        saveData();
    }

    public void removeGlobalTempMultiplier() {
        dataConfig.set("global.temp-multiplier", null);
        dataConfig.set("global.temp-duration", null);
        dataConfig.set("global.temp-expiry", null);
        saveData();
    }

    public double getActiveTempMultiplier(OfflinePlayer player) {
        double personalTemp = getPlayerTempMultiplier(player.getUniqueId());
        double globalTemp = getGlobalTempMultiplier();
        return Math.max(personalTemp, globalTemp);
    }

    public double getPermissionMultiplier(OfflinePlayer player) {
        if (!player.isOnline() || player.getPlayer() == null) {
            return 1.0;
        }
        double highestPermMulti = 0;
        for (PermissionAttachmentInfo p : player.getPlayer().getEffectivePermissions()) {
            String perm = p.getPermission();
            if (perm.startsWith("bmultipliers.multi.") && p.getValue()) {
                try {
                    double multi = Double.parseDouble(perm.substring("bmultipliers.multi.".length()));
                    if (multi > highestPermMulti) {
                        highestPermMulti = multi;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return highestPermMulti > 0 ? highestPermMulti : 1.0;
    }
}