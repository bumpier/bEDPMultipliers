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
        }.runTaskTimerAsynchronously(plugin, 0L, 20L); // Update every second
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        for (UUID uuid : activeBossBars.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removeBossBar(player);
            }
        }
        activeBossBars.clear();
    }

    public void updateBossBarForPlayer(Player player) {
        if (!configManager.isBossBarEnabled()) {
            removeBossBar(player);
            return;
        }

        UUID uuid = player.getUniqueId();
        long personalExpiry = multiplierManager.getPlayerTempExpiry(uuid);
        long globalExpiry = multiplierManager.getGlobalTempExpiry();

        // Player's personal multiplier takes priority
        if (personalExpiry > System.currentTimeMillis()) {
            double amount = multiplierManager.getPlayerTempMultiplier(uuid);
            long totalDuration = multiplierManager.getPlayerTempDuration(uuid);
            createOrUpdateBar(player, personalExpiry, totalDuration, amount, false);
        } else if (globalExpiry > System.currentTimeMillis()) {
            double amount = multiplierManager.getGlobalTempMultiplier();
            long totalDuration = multiplierManager.getGlobalTempDuration();
            createOrUpdateBar(player, globalExpiry, totalDuration, amount, true);
        } else {
            removeBossBar(player);
        }
    }

    private void createOrUpdateBar(Player player, long expiryTime, long totalDuration, double amount, boolean isGlobal) {
        BossBar bar = activeBossBars.computeIfAbsent(player.getUniqueId(), k -> {
            BossBar newBar = Bukkit.createBossBar("", configManager.getBossBarColor(), configManager.getBossBarStyle());
            newBar.addPlayer(player);
            return newBar;
        });

        long remainingMillis = expiryTime - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            removeBossBar(player);
            return;
        }

        double progress = Math.max(0, (double) remainingMillis / totalDuration);
        String time = TimeUtil.formatDuration(remainingMillis);

        String titleKey = isGlobal ? "bossbar-global-text" : "bossbar-personal-text";
        String title = configManager.getMessage(titleKey)
                .replace("%amount%", String.valueOf(amount))
                .replace("%time%", time);

        bar.setProgress(progress);
        bar.setTitle(title);
    }

    public void removeBossBar(Player player) {
        BossBar bar = activeBossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }
}