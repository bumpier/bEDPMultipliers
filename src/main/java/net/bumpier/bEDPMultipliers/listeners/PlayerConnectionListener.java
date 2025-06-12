// File: src/main/java/net/bumpier/bedpmultipliers/listeners/PlayerConnectionListener.java
package net.bumpier.bedpmultipliers.listeners;

import net.bumpier.bedpmultipliers.managers.BossBarManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final BossBarManager bossBarManager;

    public PlayerConnectionListener(BossBarManager bossBarManager) {
        this.bossBarManager = bossBarManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Delay by one tick to ensure player is fully loaded
        bossBarManager.updateBossBarForPlayer(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        bossBarManager.removeBossBar(event.getPlayer());
    }
}