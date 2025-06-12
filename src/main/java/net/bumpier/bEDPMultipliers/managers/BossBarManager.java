// File: src/main/java/net/bumpier/bedpmultipliers/managers/BossBarManager.java
package net.bumpier.bedpmultipliers.managers;

import net.bumpier.bedpmultipliers.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarManager {

    private final JavaPlugin plugin;
    private final net.bumpier.bedpmultipliers.managers.ConfigManager configManager;
    private final net.bumpier.bedpmultipliers.managers.MultiplierManager multiplierManager;
    private final Map<UUID, BossBar> activeBossBars = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    public BossBarManager(JavaPlugin plugin, net.bumpier.bedpmultipliers.managers.ConfigManager configManager, net.bumpier.bedpmultipliers.managers.MultiplierManager multiplierManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.multiplierManager = multiplierManager;
    }

    public void initialize() {
        if (!configManager.isBossBarEnabled()) return;
        this.updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateBossBarForPlayer(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        Bukkit.getOnlinePlayers().forEach(this::removeBossBar);
        activeBossBars.clear();
    }

    public void updateBossBarForPlayer(Player player) {
        if (!configManager.isBossBarEnabled()) {
            removeBossBar(player);
            return;
        }
        Map<String, Object> displayData = multiplierManager.getBossBarDisplayData(player.getUniqueId());
        if (displayData != null) {
            createOrUpdateBar(player, displayData);
        } else {
            removeBossBar(player);
        }
    }

    private void createOrUpdateBar(Player player, Map<String, Object> data) {
        BossBar bar = activeBossBars.computeIfAbsent(player.getUniqueId(), k -> {
            BossBar newBar = Bukkit.createBossBar("", configManager.getBossBarColor(), configManager.getBossBarStyle());
            newBar.addPlayer(player);
            return newBar;
        });
        long expiryTime = (long) data.getOrDefault("expiry", 0L);
        long remainingMillis = expiryTime - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            removeBossBar(player);
            return;
        }
        long totalDuration = (long) data.getOrDefault("duration", 0L);
        double amount = (double) data.getOrDefault("amount", 1.0);
        String currencyKey = (String) data.getOrDefault("currency", "Unknown");
        // Use the new formatted currency name, fallback to "Global" for the global key
        String currencyDisplay = currencyKey.equals(multiplierManager.getGlobalCurrencyKey()) ? "Global" : configManager.getFormattedCurrency(currencyKey);

        double progress = (totalDuration > 0) ? Math.max(0, (double) remainingMillis / totalDuration) : 1.0;
        String time = TimeUtil.formatDuration(remainingMillis);

        String title = configManager.getMessage("bossbar-text")
                .replace("%currency%", currencyDisplay)
                .replace("%amount%", String.valueOf(amount))
                .replace("%time%", time);

        bar.setProgress(progress);
        bar.setTitle(title);
        bar.setVisible(true);
    }

    public void removeBossBar(Player player) {
        BossBar bar = activeBossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.setVisible(false);
            bar.removeAll();
        }
    }
}