// File: src/main/java/net/bumpier/bedpmultipliers/listeners/MultiplierListener.java
package net.bumpier.bedpmultipliers.listeners;

// Import the official event from the EdPrisonCore API
import com.edwardbelt.edprison.events.EdPrisonAddMultiplierCurrency;
import net.bumpier.bedpmultipliers.managers.MultiplierManager;
import net.bumpier.bedpmultipliers.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MultiplierListener implements Listener {

    private final MultiplierManager multiplierManager;
    private final DebugLogger debugLogger;

    public MultiplierListener(MultiplierManager multiplierManager, DebugLogger debugLogger) {
        this.multiplierManager = multiplierManager;
        this.debugLogger = debugLogger;
    }

    // Note: This is where the plugin interacts with EdPrisonCore.
    // It now listens for the correct, official event class.
    @EventHandler(priority = EventPriority.LOW)
    public void onCurrencyReceive(EdPrisonAddMultiplierCurrency event) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(event.getUUID());
        if (player == null) return;

        double pluginMultiplier = multiplierManager.getTotalMultiplier(player);

        // We use addMultiplier instead of setMultiplier to stack with other bonuses.
        // We subtract 1.0 because the base is already 1.0 (100%). We are adding the *bonus*.
        double bonus = pluginMultiplier - 1.0;

        if (bonus > 0) {
            event.addMultiplier(bonus);
            debugLogger.log("Applied a " + bonus + "x bonus multiplier to " + player.getName());
        }
    }
}