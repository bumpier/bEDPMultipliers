// File: src/main/java/net/bumpier/bedpmultipliers/managers/ConfigManager.java
package net.bumpier.bedpmultipliers.managers;

import net.bumpier.bedpmultipliers.utils.DebugLogger;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ConfigManager {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messagesConfig;
    private final File configFile;
    private final File messagesFile;

    private boolean debug;
    private double globalMultiplier;
    private boolean bossBarEnabled;
    private BarColor bossBarColor;
    private BarStyle bossBarStyle;
    private Map<String, String> currencyFormats;
    private String pluginPrefix;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        loadConfigs();
    }

    public void loadConfigs() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        loadValues();
    }

    private void loadValues() {
        this.pluginPrefix = messagesConfig.getString("plugin-prefix", "&8[&6Multipliers&8] &r");
        this.debug = config.getBoolean("debug", false);
        this.globalMultiplier = config.getDouble("global-multiplier", 1.0);
        this.bossBarEnabled = config.getBoolean("bossbar.enabled", true);
        try {
            this.bossBarColor = BarColor.valueOf(config.getString("bossbar.color", "GREEN").toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid BossBar color in config.yml. Defaulting to GREEN.");
            this.bossBarColor = BarColor.GREEN;
        }
        try {
            this.bossBarStyle = BarStyle.valueOf(config.getString("bossbar.style", "SOLID").toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid BossBar style in config.yml. Defaulting to SOLID.");
            this.bossBarStyle = BarStyle.SOLID;
        }

        this.currencyFormats = new HashMap<>();
        ConfigurationSection currencySection = config.getConfigurationSection("currencies");
        if (currencySection != null) {
            for (String key : currencySection.getKeys(false)) {
                String id = currencySection.getString(key + ".id");
                String formatted = currencySection.getString(key + ".formatted");
                if (id != null && !id.isEmpty() && formatted != null) {
                    currencyFormats.put(id.toLowerCase(), formatColors(formatted));
                }
            }
        }
    }

    // Note: This is the new helper method to capitalize the first letter of a word.
    private String capitalize(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
    }

    // Note: This method now uses the capitalize method as a fallback.
    public String getFormattedCurrency(String currencyId) {
        if (currencyId.equalsIgnoreCase("All")) return "All";
        // If a formatted name exists in the config, use it. Otherwise, capitalize the ID.
        return currencyFormats.getOrDefault(currencyId.toLowerCase(), capitalize(currencyId));
    }

    public String getMessage(String path) {
        String message = messagesConfig.getString(path, "&cMessage not found: " + path);
        if (message.isEmpty()) {
            return "";
        }
        return formatColors(pluginPrefix + message);
    }

    public List<String> getMessageList(String path) {
        return messagesConfig.getStringList(path).stream()
                .map(this::formatColors)
                .collect(Collectors.toList());
    }

    public String formatColors(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        while (matcher.find()) {
            text = text.replace(matcher.group(), ChatColor.of("#" + matcher.group(1)).toString());
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    // Getters
    public boolean isDebug() { return debug; }
    public double getGlobalMultiplier() { return globalMultiplier; }
    public boolean isBossBarEnabled() { return bossBarEnabled; }
    public BarColor getBossBarColor() { return bossBarColor; }
    public BarStyle getBossBarStyle() { return bossBarStyle; }
}