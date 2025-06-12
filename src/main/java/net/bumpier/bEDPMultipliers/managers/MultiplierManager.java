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

public class MultiplierManager {

    private final JavaPlugin plugin;
    private final net.bumpier.bedpmultipliers.managers.ConfigManager configManager;
    private final DebugLogger debugLogger;
    private File dataFile;
    private FileConfiguration dataConfig;
    private final String GLOBAL_CURRENCY_KEY = "__global__";

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

    public void saveData() {
        try {
            dataConfig.save(dataFile);
            debugLogger.log("Data file saved.");
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

        double total = configGlobalMulti;
        total += (permanentPlayerMulti - 1.0);
        total += (temporaryPlayerMulti - 1.0);
        total += (temporaryGlobalMulti - 1.0);
        total += (permissionMulti - 1.0);

        return Math.max(1.0, total);
    }

    public String getGlobalCurrencyKey() {
        return GLOBAL_CURRENCY_KEY;
    }

    public double getPlayerPermanentMultiplier(UUID uuid, String currency) {
        String path = "players." + uuid + ".permanent.";
        return dataConfig.getDouble(path + currency, dataConfig.getDouble(path + GLOBAL_CURRENCY_KEY, 1.0));
    }

    public void setPlayerPermanentMultiplier(UUID uuid, String currency, double amount) {
        dataConfig.set("players." + uuid + ".permanent." + currency, amount);
        saveData();
    }

    public void removePlayerPermanentMultiplier(UUID uuid, String currency) {
        dataConfig.set("players." + uuid + ".permanent." + currency, null);
        saveData();
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

    public void setPlayerTemporaryMultiplier(UUID uuid, String currency, double amount, long duration) {
        String path = "players." + uuid + ".temporary." + currency;
        dataConfig.set(path + ".amount", amount);
        dataConfig.set(path + ".duration", duration);
        dataConfig.set(path + ".expiry", System.currentTimeMillis() + duration);
        dataConfig.set(path + ".appliedAt", System.currentTimeMillis());
        saveData();
    }

    public void removePlayerTemporaryMultiplier(UUID uuid, String currency) {
        dataConfig.set("players." + uuid + ".temporary." + currency, null);
        saveData();
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

    public void setGlobalTemporaryMultiplier(String currency, double amount, long duration) {
        String path = "global.temporary." + currency;
        dataConfig.set(path + ".amount", amount);
        dataConfig.set(path + ".duration", duration);
        dataConfig.set(path + ".expiry", System.currentTimeMillis() + duration);
        dataConfig.set(path + ".appliedAt", System.currentTimeMillis());
        saveData();
    }

    public void removeGlobalTemporaryMultiplier(String currency) {
        dataConfig.set("global.temporary." + currency, null);
        saveData();
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
                        data.put("isGlobal", false);
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
                        data.put("isGlobal", true);
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

    // --- New Methods for Old Placeholders ---

    /**
     * Calculates the total multiplier using only non-currency-specific values.
     * For use with the old %bmulti_totalmulti% placeholder.
     */
    public double getGlobalTotalMultiplier(OfflinePlayer player) {
        double permanentPlayerMulti = getPlayerPermanentMultiplier(player.getUniqueId(), GLOBAL_CURRENCY_KEY);
        double temporaryPlayerMulti = getPlayerTemporaryMultiplier(player.getUniqueId(), GLOBAL_CURRENCY_KEY);
        double temporaryGlobalMulti = getGlobalTemporaryMultiplier(GLOBAL_CURRENCY_KEY);
        double permissionMulti = getPermissionMultiplier(player);
        double configGlobalMulti = configManager.getGlobalMultiplier();

        double total = configGlobalMulti;
        total += (permanentPlayerMulti - 1.0);
        total += (temporaryPlayerMulti - 1.0);
        total += (temporaryGlobalMulti - 1.0);
        total += (permissionMulti - 1.0);

        return Math.max(1.0, total);
    }

    /**
     * Gets the highest active "all-currency" temporary multiplier for a player.
     * For use with the old %bmulti_tempmulti% placeholder.
     */
    public double getGlobalActiveTempMultiplier(OfflinePlayer player) {
        double playerTemp = getPlayerTemporaryMultiplier(player.getUniqueId(), GLOBAL_CURRENCY_KEY);
        double globalTemp = getGlobalTemporaryMultiplier(GLOBAL_CURRENCY_KEY);
        return Math.max(playerTemp, globalTemp);
    }
}