// File: src/main/java/net/bumpier/bedpmultipliers/listeners/VoucherListener.java
package net.bumpier.bedpmultipliers.listeners;

import net.bumpier.bedpmultipliers.managers.ConfigManager;
import net.bumpier.bedpmultipliers.managers.MultiplierManager;
import net.bumpier.bedpmultipliers.utils.DebugLogger;
import net.bumpier.bedpmultipliers.utils.TimeUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class VoucherListener implements Listener {
    private final MultiplierManager multiplierManager;
    private final ConfigManager configManager;
    private final DebugLogger debugLogger;
    private final NamespacedKey amountKey;
    private final NamespacedKey durationKey;
    private final NamespacedKey currencyKey;


    public VoucherListener(JavaPlugin plugin, MultiplierManager multiplierManager, ConfigManager configManager, DebugLogger debugLogger) {
        this.multiplierManager = multiplierManager;
        this.configManager = configManager;
        this.debugLogger = debugLogger;
        this.amountKey = new NamespacedKey(plugin, "voucher_amount");
        this.durationKey = new NamespacedKey(plugin, "voucher_duration");
        this.currencyKey = new NamespacedKey(plugin, "voucher_currency");
    }

    @EventHandler
    public void onVoucherRedeem(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir() || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(amountKey, PersistentDataType.DOUBLE)) {
            return; // Not a voucher
        }

        event.setCancelled(true);

        double amount = pdc.get(amountKey, PersistentDataType.DOUBLE);
        long duration = pdc.get(durationKey, PersistentDataType.LONG);
        String currency = pdc.get(currencyKey, PersistentDataType.STRING);

        if (amount <= 0 || duration <= 0 || currency == null || currency.isEmpty()) {
            if (configManager.isDebug()) {
                debugLogger.log("Player " + player.getName() + " tried to redeem a voucher with invalid data.");
            }
            return;
        }

        multiplierManager.setPlayerTemporaryMultiplier(player.getUniqueId(), currency, amount, duration);
        item.setAmount(item.getAmount() - 1);

        player.sendMessage(configManager.getMessage("multiplier-applied-storage")
                .replace("%multiplier%", String.valueOf(amount))
                .replace("%currency%", configManager.getFormattedCurrency(currency))
                .replace("%time%", TimeUtil.formatDuration(duration)));
        if (configManager.isDebug()) {
            debugLogger.log("Player " + player.getName() + " redeemed a voucher for " + amount + "x " + currency + " for " + TimeUtil.formatDuration(duration) + ".");
        }
    }
}