// File: src/main/java/net/bumpier/bedpmultipliers/managers/MultiplierManager.java
package net.bumpier.bedpmultipliers.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.bumpier.bedpmultipliers.BEDPMultipliers;
import net.bumpier.bedpmultipliers.data.PlayerData;
import net.bumpier.bedpmultipliers.data.PluginData;
import net.bumpier.bedpmultipliers.data.TemporaryMultiplier;
import net.bumpier.bedpmultipliers.utils.DebugLogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class MultiplierManager {

    private static final String GLOBAL_CURRENCY_KEY = "__global__";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final DebugLogger debugLogger;
    private final File dataFile;
    private final Gson gson;

    private PluginData data;
    private boolean dirty = false;

    public MultiplierManager(JavaPlugin plugin, ConfigManager configManager, DebugLogger debugLogger) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.debugLogger = debugLogger;
        this.dataFile = new File(plugin.getDataFolder(), "data.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadData();
    }

    private void loadData() {
        if (dataFile.exists()) {
            try (FileReader reader = new FileReader(dataFile)) {
                this.data = gson.fromJson(reader, PluginData.class);
                if (this.data == null) {
                    this.data = new PluginData();
                }
                if (configManager.isDebug()) {
                    debugLogger.log("Loaded data from data.json");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not read data.json! " + e.getMessage());
                this.data = new PluginData();
            }
        } else {
            this.data = new PluginData();
            if (configManager.isDebug()) {
                debugLogger.log("No existing data file found. Created new data object.");
            }
        }
    }

    public synchronized void saveData(boolean force) {
        if (!dirty && !force) {
            return;
        }
        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(data, writer);
            dirty = false;
            if (configManager.isDebug()) {
                debugLogger.log("Data saved to data.json");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.json! " + e.getMessage());
        }
    }

    public void setDirty() {
        this.dirty = true;
    }

    public PlayerData getPlayerData(UUID uuid) {
        return data.getPlayers().computeIfAbsent(uuid, k -> new PlayerData());
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }


    // --- Setters ---

    public void setPlayerPermanentMultiplier(UUID uuid, String currency, double amount) {
        getPlayerData(uuid).getPermanent().put(currency, amount);
        dirty = true;
    }

    public void removePlayerPermanentMultiplier(UUID uuid, String currency) {
        getPlayerData(uuid).getPermanent().remove(currency);
        dirty = true;
    }

    public void setPlayerTemporaryMultiplier(UUID uuid, String currency, double amount, long duration) {
        PlayerData playerData = getPlayerData(uuid);
        Map<String, TemporaryMultiplier> tempMap = playerData.getTemporary();
        TemporaryMultiplier existing = tempMap.get(currency);

        long finalDuration = duration;

        if (existing != null && existing.isActive() && Double.compare(existing.getAmount(), amount) == 0) {
            // If an active multiplier of the same amount exists, stack the duration.
            long remainingTime = existing.getExpiry() - System.currentTimeMillis();
            if (remainingTime > 0) {
                finalDuration += remainingTime;
            }
        }

        TemporaryMultiplier newMulti = new TemporaryMultiplier(amount, finalDuration, System.currentTimeMillis() + finalDuration, System.currentTimeMillis());
        tempMap.put(currency, newMulti);
        dirty = true;
    }

    public void removePlayerTemporaryMultiplier(UUID uuid, String currency) {
        getPlayerData(uuid).getTemporary().remove(currency);
        dirty = true;
    }

    public void setGlobalTemporaryMultiplier(String currency, double amount, long duration) {
        Map<String, TemporaryMultiplier> tempMap = data.getGlobal().getTemporary();
        TemporaryMultiplier existing = tempMap.get(currency);

        long finalDuration = duration;

        if (existing != null && existing.isActive() && Double.compare(existing.getAmount(), amount) == 0) {
            // If an active multiplier of the same amount exists, stack the duration.
            long remainingTime = existing.getExpiry() - System.currentTimeMillis();
            if (remainingTime > 0) {
                finalDuration += remainingTime;
            }
        }

        TemporaryMultiplier newMulti = new TemporaryMultiplier(amount, finalDuration, System.currentTimeMillis() + finalDuration, System.currentTimeMillis());
        tempMap.put(currency, newMulti);
        dirty = true;
    }

    public void removeGlobalTemporaryMultiplier(String currency) {
        data.getGlobal().getTemporary().remove(currency);
        dirty = true;
    }

    // --- Getters & Logic ---

    public String getGlobalCurrencyKey() {
        return GLOBAL_CURRENCY_KEY;
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

    public double getPlayerPermanentMultiplier(UUID uuid, String currency) {
        PlayerData pd = getPlayerData(uuid);
        return pd.getPermanent().getOrDefault(currency, pd.getPermanent().getOrDefault(GLOBAL_CURRENCY_KEY, 1.0));
    }

    public double getPlayerTemporaryMultiplier(UUID uuid, String currency) {
        PlayerData playerData = getPlayerData(uuid);
        TemporaryMultiplier specific = playerData.getTemporary().get(currency);
        if (specific != null && specific.isActive()) return specific.getAmount();
        TemporaryMultiplier global = playerData.getTemporary().get(GLOBAL_CURRENCY_KEY);
        if (global != null && global.isActive()) return global.getAmount();
        return 1.0;
    }

    public double getGlobalTemporaryMultiplier(String currency) {
        TemporaryMultiplier specific = data.getGlobal().getTemporary().get(currency);
        if (specific != null && specific.isActive()) return specific.getAmount();
        TemporaryMultiplier global = data.getGlobal().getTemporary().get(GLOBAL_CURRENCY_KEY);
        if (global != null && global.isActive()) return global.getAmount();
        return 1.0;
    }

    public Map<String, Double> getAllPlayerPermanentMultipliers(UUID uuid) {
        return getPlayerData(uuid).getPermanent().entrySet().stream().collect(Collectors.toMap(
                entry -> entry.getKey().equals(GLOBAL_CURRENCY_KEY) ? "All" : configManager.getFormattedCurrency(entry.getKey()),
                Map.Entry::getValue
        ));
    }

    public Map<String, Map<String, Object>> getAllPlayerTemporaryMultipliers(UUID uuid) {
        return getPlayerData(uuid).getTemporary().entrySet().stream()
                .filter(entry -> entry.getValue().isActive())
                .collect(Collectors.toMap(
                        entry -> entry.getKey().equals(GLOBAL_CURRENCY_KEY) ? "All" : configManager.getFormattedCurrency(entry.getKey()),
                        entry -> toMap(entry.getValue())
                ));
    }

    public Map<String, Object> getBossBarDisplayData(UUID uuid) {
        List<Map<String, Object>> activeMultipliers = new ArrayList<>();
        getPlayerData(uuid).getTemporary().forEach((currency, multi) -> {
            if (multi.isActive()) {
                Map<String, Object> map = toMap(multi);
                map.put("currency", currency);
                activeMultipliers.add(map);
            }
        });

        data.getGlobal().getTemporary().forEach((currency, multi) -> {
            if (multi.isActive()) {
                Map<String, Object> map = toMap(multi);
                map.put("currency", currency);
                activeMultipliers.add(map);
            }
        });
        return activeMultipliers.stream()
                .max(Comparator.comparingLong(m -> (long) m.getOrDefault("appliedAt", 0L)))
                .orElse(null);
    }

    public double getPermissionMultiplier(OfflinePlayer player) {
        if (!player.isOnline() || player.getPlayer() == null) return 1.0;

        double highestPermMulti = 0;
        for (PermissionAttachmentInfo p : player.getPlayer().getEffectivePermissions()) {
            String perm = p.getPermission();
            if (perm.startsWith("bmultipliers.multi.")) {
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

    public double getGlobalTotalMultiplier(OfflinePlayer player) {
        return getTotalMultiplier(player, GLOBAL_CURRENCY_KEY);
    }

    public double getGlobalActiveTempMultiplier(OfflinePlayer player) {
        double playerTemp = getPlayerTemporaryMultiplier(player.getUniqueId(), GLOBAL_CURRENCY_KEY);
        double globalTemp = getGlobalTemporaryMultiplier(GLOBAL_CURRENCY_KEY);
        return Math.max(playerTemp, globalTemp);
    }

    private Map<String, Object> toMap(TemporaryMultiplier tm) {
        Map<String, Object> map = new HashMap<>();
        map.put("amount", tm.getAmount());
        map.put("duration", tm.getDuration());
        map.put("expiry", tm.getExpiry());
        map.put("appliedAt", tm.getAppliedAt());
        return map;
    }
}