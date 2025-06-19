// File: src/main/java/net/bumpier/bedpmultipliers/api/MultiplierPlaceholders.java
package net.bumpier.bedpmultipliers.api;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.bumpier.bedpmultipliers.BEDPMultipliers;
import net.bumpier.bedpmultipliers.managers.MultiplierManager;
import net.bumpier.bedpmultipliers.utils.TimeUtil;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class MultiplierPlaceholders extends PlaceholderExpansion {
    private final BEDPMultipliers plugin;
    private final MultiplierManager multiplierManager;


    public MultiplierPlaceholders(BEDPMultipliers plugin, MultiplierManager multiplierManager) {
        this.plugin = plugin;
        this.multiplierManager = multiplierManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "bmulti";
    }

    @Override
    public @NotNull String getAuthor() {
        return "bumpier";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // %bmulti_total_<currency>%
        if (params.startsWith("total_")) {
            String currency = params.substring(6);
            return String.format("%.2f", multiplierManager.getTotalMultiplier(player, currency));
        }

        // %bmulti_active_temp_currency%
        // %bmulti_active_temp_amount%
        // %bmulti_active_temp_time%
        if (params.startsWith("active_temp_")) {
            Map<String, Object> bossBarData = multiplierManager.getBossBarDisplayData(player.getUniqueId());
            if (bossBarData == null) return "None";

            return switch (params.substring(12)) {
                case "currency" -> (String) bossBarData.getOrDefault("currency", "N/A");
                case "amount" -> String.format("%.2f", bossBarData.getOrDefault("amount", 0.0));
                case "time" -> {
                    long expiry = (long) bossBarData.getOrDefault("expiry", 0L);
                    yield TimeUtil.formatDuration(expiry - System.currentTimeMillis());
                }
                default -> null;
            };
        }

        return switch (params.toLowerCase()) {
            case "global_active_temp" -> String.format("%.2f", multiplierManager.getGlobalActiveTempMultiplier(player));
            case "global_total" -> String.format("%.2f", multiplierManager.getGlobalTotalMultiplier(player));
            default -> null;
        };
    }
}