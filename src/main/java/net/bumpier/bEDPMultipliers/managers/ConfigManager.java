// File: src/main/java/net/bumpier/bedpmultipliers/managers/ConfigManager.java
package net.bumpier.bedpmultipliers.managers;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {

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
    }

    public String getMessage(String path) {
        String message = messagesConfig.getString(path, "&cMessage not found: " + path);
        return formatColors(message);
    }

    private String formatColors(String text) {
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(text);
        while (matcher.find()) {
            text = text.replace(matcher.group(), ChatColor.of("#" + matcher.group(1)).toString());
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public boolean isDebug() {
        return debug;
    }

    public double getGlobalMultiplier() {
        return globalMultiplier;
    }

    public boolean isBossBarEnabled() {
        return bossBarEnabled;
    }

    public BarColor getBossBarColor() {
        return bossBarColor;
    }

    public BarStyle getBossBarStyle() {
        return bossBarStyle;
    }

    public void setGlobalMultiplier(double multiplier) {
        this.globalMultiplier = multiplier;
        config.set("global-multiplier", multiplier);
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml!");
            e.printStackTrace();
        }
    }
}