// File: src/main/java/net/bumpier/bedpmultipliers/listeners/PlayerConnectionListener.java
package net.bumpier.bedpmultipliers.listeners;

import net.bumpier.bedpmultipliers.managers.BossBarManager;
import net.bumpier.bedpmultipliers.managers.StorageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final BossBarManager bossBarManager;
    private final StorageManager storageManager;

    public PlayerConnectionListener(BossBarManager bossBarManager, StorageManager storageManager) {
        this.bossBarManager = bossBarManager;
        this.storageManager = storageManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        bossBarManager.updateBossBarForPlayer(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        bossBarManager.removeBossBar(event.getPlayer());
        storageManager.clearPlayerData(event.getPlayer().getUniqueId());
    }
}