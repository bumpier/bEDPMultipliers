// File: src/main/java/net/bumpier/bedpmultipliers/listeners/MultiplierListener.java
package net.bumpier.bedpmultipliers.listeners;

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

    @EventHandler(priority = EventPriority.LOW)
    public void onCurrencyReceive(EdPrisonAddMultiplierCurrency event) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(event.getUUID());
        if (player == null) return;

        String currency = event.getCurrency();
        if (currency == null || currency.isEmpty()) {
            debugLogger.log("Event currency is null or empty, skipping multiplier check.");
            return;
        }

        double pluginMultiplier = multiplierManager.getTotalMultiplier(player, currency.toLowerCase());

        double bonus = pluginMultiplier - 1.0;

        if (bonus > 0) {
            event.addMultiplier(bonus);
            debugLogger.log("Applied a " + bonus + "x bonus multiplier to " + player.getName() + " for currency '" + currency + "'");
        }
    }
}