// File: src/main/java/net/bumpier/bedpmultipliers/managers/MultiplierManager.java
package net.bumpier.bedpmultipliers.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
    private final net.bumpier.bedpmultipliers.managers.ConfigManager configManager;
    private final DebugLogger debugLogger;
    private final File dataFile;
    private final Gson gson;

    private PluginData data;
    private boolean dirty = false;

    public MultiplierManager(JavaPlugin plugin, net.bumpier.bedpmultipliers.managers.ConfigManager configManager, DebugLogger debugLogger) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.debugLogger = debugLogger;
        this.dataFile = new File(plugin.getDataFolder(), "data.json"); // Switched to .json
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
                debugLogger.log("Loaded data from data.json");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not read data.json! " + e.getMessage());
                this.data = new PluginData();
            }
        } else {
            this.data = new PluginData();
            debugLogger.log("No existing data file found. Created new data object.");
        }
    }

    public synchronized void saveData(boolean force) {
        if (!dirty && !force) {
            return;
        }
        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(data, writer);
            dirty = false;
            debugLogger.log("Data saved to data.json");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.json! " + e.getMessage());
        }
    }

    public String getGlobalCurrencyKey() {
        return GLOBAL_CURRENCY_KEY;
    }

    // --- Setters (now interact with POJOs) ---

    public void setPlayerPermanentMultiplier(UUID uuid, String currency, double amount) {
        data.getPlayers().computeIfAbsent(uuid, k -> new PlayerData())
                .getPermanent().put(currency, amount);
        dirty = true;
    }

    public void removePlayerPermanentMultiplier(UUID uuid, String currency) {
        Optional.ofNullable(data.getPlayers().get(uuid))
                .ifPresent(pd -> pd.getPermanent().remove(currency));
        dirty = true;
    }

    public void setPlayerTemporaryMultiplier(UUID uuid, String currency, double amount, long duration) {
        TemporaryMultiplier tempMulti = new TemporaryMultiplier(amount, duration, System.currentTimeMillis() + duration, System.currentTimeMillis());
        data.getPlayers().computeIfAbsent(uuid, k -> new PlayerData())
                .getTemporary().put(currency, tempMulti);
        dirty = true;
    }

    public void removePlayerTemporaryMultiplier(UUID uuid, String currency) {
        Optional.ofNullable(data.getPlayers().get(uuid))
                .ifPresent(pd -> pd.getTemporary().remove(currency));
        dirty = true;
    }

    public void setGlobalTemporaryMultiplier(String currency, double amount, long duration) {
        TemporaryMultiplier tempMulti = new TemporaryMultiplier(amount, duration, System.currentTimeMillis() + duration, System.currentTimeMillis());
        data.getGlobal().getTemporary().put(currency, tempMulti);
        dirty = true;
    }

    public void removeGlobalTemporaryMultiplier(String currency) {
        data.getGlobal().getTemporary().remove(currency);
        dirty = true;
    }

    // --- Getters (now read from POJOs) ---

    public double getPlayerPermanentMultiplier(UUID uuid, String currency) {
        PlayerData playerData = data.getPlayers().get(uuid);
        if (playerData == null) return 1.0;
        return playerData.getPermanent().getOrDefault(currency, playerData.getPermanent().getOrDefault(GLOBAL_CURRENCY_KEY, 1.0));
    }

    public double getPlayerTemporaryMultiplier(UUID uuid, String currency) {
        PlayerData playerData = data.getPlayers().get(uuid);
        if (playerData == null) return 1.0;

        TemporaryMultiplier specific = playerData.getTemporary().get(currency);
        if (specific != null && specific.isActive()) {
            return specific.getAmount();
        }

        TemporaryMultiplier global = playerData.getTemporary().get(GLOBAL_CURRENCY_KEY);
        if (global != null && global.isActive()) {
            return global.getAmount();
        }

        return 1.0;
    }

    public double getGlobalTemporaryMultiplier(String currency) {
        TemporaryMultiplier specific = data.getGlobal().getTemporary().get(currency);
        if (specific != null && specific.isActive()) {
            return specific.getAmount();
        }

        TemporaryMultiplier global = data.getGlobal().getTemporary().get(GLOBAL_CURRENCY_KEY);
        if (global != null && global.isActive()) {
            return global.getAmount();
        }

        return 1.0;
    }

    public Map<String, Object> getBossBarDisplayData(UUID uuid) {
        List<Map<String, Object>> activeMultipliers = new ArrayList<>();

        Optional.ofNullable(data.getPlayers().get(uuid)).ifPresent(playerData ->
                playerData.getTemporary().entrySet().stream()
                        .filter(entry -> entry.getValue().isActive())
                        .map(entry -> toMap(entry.getKey(), entry.getValue()))
                        .forEach(activeMultipliers::add)
        );

        data.getGlobal().getTemporary().entrySet().stream()
                .filter(entry -> entry.getValue().isActive())
                .map(entry -> toMap(entry.getKey(), entry.getValue()))
                .forEach(activeMultipliers::add);

        return activeMultipliers.stream()
                .max(Comparator.comparingLong(m -> (long) m.getOrDefault("appliedAt", 0L)))
                .orElse(null);
    }

    private Map<String, Object> toMap(String currency, TemporaryMultiplier tm) {
        Map<String, Object> map = new HashMap<>();
        map.put("currency", currency);
        map.put("amount", tm.getAmount());
        map.put("duration", tm.getDuration());
        map.put("expiry", tm.getExpiry());
        map.put("appliedAt", tm.getAppliedAt());
        return map;
    }

    public double getPermissionMultiplier(OfflinePlayer player) {
        if (!player.isOnline() || player.getPlayer() == null) return 1.0;
        return Arrays.stream(player.getPlayer().getEffectivePermissions().toArray(new PermissionAttachmentInfo[0]))
                .map(PermissionAttachmentInfo::getPermission)
                .filter(p -> p.startsWith("bmultipliers.multi."))
                .mapToDouble(p -> {
                    try {
                        return Double.parseDouble(p.substring("bmultipliers.multi.".length()));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max().orElse(1.0);
    }

    public Map<String, Double> getAllPlayerPermanentMultipliers(UUID uuid) {
        PlayerData playerData = data.getPlayers().get(uuid);
        if (playerData == null) return Collections.emptyMap();
        return playerData.getPermanent().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().equals(GLOBAL_CURRENCY_KEY) ? "All" : entry.getKey(),
                        Map.Entry::getValue
                ));
    }

    public Map<String, Map<String, Object>> getAllPlayerTemporaryMultipliers(UUID uuid) {
        PlayerData playerData = data.getPlayers().get(uuid);
        if (playerData == null) return Collections.emptyMap();
        return playerData.getTemporary().entrySet().stream()
                .filter(entry -> entry.getValue().isActive())
                .collect(Collectors.toMap(
                        entry -> entry.getKey().equals(GLOBAL_CURRENCY_KEY) ? "All" : entry.getKey(),
                        entry -> toMap(entry.getKey(), entry.getValue())
                ));
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

    public double getGlobalTotalMultiplier(OfflinePlayer player) {
        return getTotalMultiplier(player, GLOBAL_CURRENCY_KEY);
    }

    public double getGlobalActiveTempMultiplier(OfflinePlayer player) {
        return getPlayerTemporaryMultiplier(player.getUniqueId(), GLOBAL_CURRENCY_KEY);
    }
}