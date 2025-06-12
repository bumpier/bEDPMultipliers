// File: src/main/java/net/bumpier/bedpmultipliers/managers/MultiplierManager.java
package net.bumpier.bedpmultipliers.managers;

import net.bumpier.bedpmultipliers.utils.DebugLogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class MultiplierManager {

    // Note: Made this a static final constant for clarity and performance.
    private static final String GLOBAL_CURRENCY_KEY = "__global__";

    private final JavaPlugin plugin;
    private final net.bumpier.bedpmultipliers.managers.ConfigManager configManager;
    private final DebugLogger debugLogger;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private boolean dirty = false; // Note: New dirty flag for optimized saving.

    public MultiplierManager(JavaPlugin plugin, net.bumpier.bedpmultipliers.managers.ConfigManager configManager, DebugLogger debugLogger) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.debugLogger = debugLogger;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        loadData();
    }

    private void loadData() {
        if (!dataFile.exists()) {
            try {
                if (dataFile.createNewFile()) {
                    debugLogger.log("Created a new data.yml file.");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml! " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        debugLogger.log("Data file loaded.");
    }

    // Note: saveData now only writes to disk if changes have been made (or if forced).
    public synchronized void saveData(boolean force) {
        if (!dirty && !force) {
            return;
        }
        try {
            dataConfig.save(dataFile);
            dirty = false;
            debugLogger.log("Data file saved to disk.");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml!");
            e.printStackTrace();
        }
    }

    public double getTotalMultiplier(OfflinePlayer player, String currency) {
        double permanentPlayerMulti = getPlayerPermanentMultiplier(player.getUniqueId(), currency);
        double temporaryPlayerMulti = getPlayerTemporaryMultiplier(player.getUniqueId(), currency);
        double temporaryGlobalMulti = getGlobalTemporaryMultiplier(currency);
        double permissionMulti = getPermissionMultiplier(player);
        double configGlobalMulti = configManager.getGlobalMultiplier();

        double total = configGlobalMulti + (permanentPlayerMulti - 1.0) + (temporaryPlayerMulti - 1.0) + (temporaryGlobalMulti - 1.0) + (permissionMulti - 1.0);
        return Math.max(1.0, total);
    }

    public String getGlobalCurrencyKey() {
        return GLOBAL_CURRENCY_KEY;
    }

    public void setPlayerPermanentMultiplier(UUID uuid, String currency, double amount) {
        dataConfig.set("players." + uuid + ".permanent." + currency, amount);
        dirty = true;
    }

    public void removePlayerPermanentMultiplier(UUID uuid, String currency) {
        dataConfig.set("players." + uuid + ".permanent." + currency, null);
        dirty = true;
    }

    public void setPlayerTemporaryMultiplier(UUID uuid, String currency, double amount, long duration) {
        String path = "players." + uuid + ".temporary." + currency;
        dataConfig.set(path + ".amount", amount);
        dataConfig.set(path + ".duration", duration);
        dataConfig.set(path + ".expiry", System.currentTimeMillis() + duration);
        dataConfig.set(path + ".appliedAt", System.currentTimeMillis());
        dirty = true;
    }

    public void removePlayerTemporaryMultiplier(UUID uuid, String currency) {
        dataConfig.set("players." + uuid + ".temporary." + currency, null);
        dirty = true;
    }

    public void setGlobalTemporaryMultiplier(String currency, double amount, long duration) {
        String path = "global.temporary." + currency;
        dataConfig.set(path + ".amount", amount);
        dataConfig.set(path + ".duration", duration);
        dataConfig.set(path + ".expiry", System.currentTimeMillis() + duration);
        dataConfig.set(path + ".appliedAt", System.currentTimeMillis());
        dirty = true;
    }

    public void removeGlobalTemporaryMultiplier(String currency) {
        dataConfig.set("global.temporary." + currency, null);
        dirty = true;
    }

    // --- Getter methods remain largely unchanged ---

    public double getPlayerPermanentMultiplier(UUID uuid, String currency) {
        String path = "players." + uuid + ".permanent.";
        return dataConfig.getDouble(path + currency, dataConfig.getDouble(path + GLOBAL_CURRENCY_KEY, 1.0));
    }

    public Map<String, Double> getAllPlayerPermanentMultipliers(UUID uuid) {
        Map<String, Double> multipliers = new HashMap<>();
        ConfigurationSection section = dataConfig.getConfigurationSection("players." + uuid + ".permanent");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                multipliers.put(key.equals(GLOBAL_CURRENCY_KEY) ? "All" : key, section.getDouble(key));
            }
        }
        return multipliers;
    }

    public double getPlayerTemporaryMultiplier(UUID uuid, String currency) {
        Map<String, Object> specificData = getPlayerTemporaryData(uuid, currency);
        if (isTempActive(specificData)) {
            return (double) specificData.getOrDefault("amount", 1.0);
        }
        Map<String, Object> globalData = getPlayerTemporaryData(uuid, GLOBAL_CURRENCY_KEY);
        if (isTempActive(globalData)) {
            return (double) globalData.getOrDefault("amount", 1.0);
        }
        return 1.0;
    }

    public Map<String, Object> getPlayerTemporaryData(UUID uuid, String currency) {
        ConfigurationSection section = dataConfig.getConfigurationSection("players." + uuid + ".temporary." + currency);
        return section != null ? section.getValues(false) : Collections.emptyMap();
    }

    public Map<String, Map<String, Object>> getAllPlayerTemporaryMultipliers(UUID uuid) {
        Map<String, Map<String, Object>> multipliers = new HashMap<>();
        ConfigurationSection section = dataConfig.getConfigurationSection("players." + uuid + ".temporary");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Map<String, Object> data = section.getConfigurationSection(key).getValues(false);
                if (isTempActive(data)) {
                    multipliers.put(key.equals(GLOBAL_CURRENCY_KEY) ? "All" : key, data);
                }
            }
        }
        return multipliers;
    }

    public double getGlobalTemporaryMultiplier(String currency) {
        Map<String, Object> specificData = getGlobalTemporaryData(currency);
        if (isTempActive(specificData)) {
            return (double) specificData.getOrDefault("amount", 1.0);
        }
        Map<String, Object> globalData = getGlobalTemporaryData(GLOBAL_CURRENCY_KEY);
        if (isTempActive(globalData)) {
            return (double) globalData.getOrDefault("amount", 1.0);
        }
        return 1.0;
    }

    public Map<String, Object> getGlobalTemporaryData(String currency) {
        ConfigurationSection section = dataConfig.getConfigurationSection("global.temporary." + currency);
        return section != null ? section.getValues(false) : Collections.emptyMap();
    }

    public double getPermissionMultiplier(OfflinePlayer player) {
        if (!player.isOnline() || player.getPlayer() == null) return 1.0;
        double highestPermMulti = 0;
        for (PermissionAttachmentInfo p : player.getPlayer().getEffectivePermissions()) {
            String perm = p.getPermission();
            if (perm.startsWith("bmultipliers.multi.") && p.getValue()) {
                try {
                    double multi = Double.parseDouble(perm.substring("bmultipliers.multi.".length()));
                    if (multi > highestPermMulti) highestPermMulti = multi;
                } catch (NumberFormatException ignored) {}
            }
        }
        return highestPermMulti > 0 ? highestPermMulti : 1.0;
    }

    public Map<String, Object> getBossBarDisplayData(UUID uuid) {
        List<Map<String, Object>> activeMultipliers = new ArrayList<>();
        ConfigurationSection personalSection = dataConfig.getConfigurationSection("players." + uuid + ".temporary");
        if (personalSection != null) {
            personalSection.getKeys(false).stream()
                    .map(key -> {
                        Map<String, Object> data = new HashMap<>(personalSection.getConfigurationSection(key).getValues(false));
                        data.put("currency", key);
                        return data;
                    })
                    .filter(this::isTempActive)
                    .forEach(activeMultipliers::add);
        }
        ConfigurationSection globalSection = dataConfig.getConfigurationSection("global.temporary");
        if (globalSection != null) {
            globalSection.getKeys(false).stream()
                    .map(key -> {
                        Map<String, Object> data = new HashMap<>(globalSection.getConfigurationSection(key).getValues(false));
                        data.put("currency", key);
                        return data;
                    })
                    .filter(this::isTempActive)
                    .forEach(activeMultipliers::add);
        }
        return activeMultipliers.stream()
                .max(Comparator.comparingLong(m -> (long) m.getOrDefault("appliedAt", 0L)))
                .orElse(null);
    }

    private boolean isTempActive(Map<String, Object> data) {
        if (data == null || data.isEmpty()) return false;
        long expiry = (long) data.getOrDefault("expiry", 0L);
        return expiry > System.currentTimeMillis();
    }

    public double getGlobalTotalMultiplier(OfflinePlayer player) {
        double permanentPlayerMulti = getPlayerPermanentMultiplier(player.getUniqueId(), GLOBAL_CURRENCY_KEY);
        double temporaryPlayerMulti = getPlayerTemporaryMultiplier(player.getUniqueId(), GLOBAL_CURRENCY_KEY);
        double temporaryGlobalMulti = getGlobalTemporaryMultiplier(GLOBAL_CURRENCY_KEY);
        double permissionMulti = getPermissionMultiplier(player);
        double configGlobalMulti = configManager.getGlobalMultiplier();
        double total = configGlobalMulti + (permanentPlayerMulti - 1.0) + (temporaryPlayerMulti - 1.0) + (temporaryGlobalMulti - 1.0) + (permissionMulti - 1.0);
        return Math.max(1.0, total);
    }

    public double getGlobalActiveTempMultiplier(OfflinePlayer player) {
        double playerTemp = getPlayerTemporaryMultiplier(player.getUniqueId(), GLOBAL_CURRENCY_KEY);
        double globalTemp = getGlobalTemporaryMultiplier(GLOBAL_CURRENCY_KEY);
        return Math.max(playerTemp, globalTemp);
    }
}