// File: src/main/java/net/bumpier/bedpmultipliers/api/MultiplierPlaceholders.java
package net.bumpier.bedpmultipliers.api;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.bumpier.bedpmultipliers.BEDPMultipliers;
import net.bumpier.bedpmultipliers.managers.MultiplierManager;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public class MultiplierPlaceholders extends PlaceholderExpansion {

    private final BEDPMultipliers plugin;
    private final MultiplierManager multiplierManager;
    private final DecimalFormat df = new DecimalFormat("#.##");

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
        return "bumpier.dev";
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

        // Handles %bmulti_multi_<currency>%
        if (params.startsWith("multi_")) {
            String currency = params.substring("multi_".length()).toLowerCase();
            if (currency.isEmpty()) {
                return "0.0";
            }
            double totalMulti = multiplierManager.getTotalMultiplier(player, currency);
            return df.format(totalMulti);
        }

        switch (params) {
            case "rankmulti":
                return df.format(multiplierManager.getPermissionMultiplier(player));
            case "personalmulti":
                return df.format(multiplierManager.getPlayerPermanentMultiplier(player.getUniqueId(), multiplierManager.getGlobalCurrencyKey()));
            case "tempmulti":
                return df.format(multiplierManager.getGlobalActiveTempMultiplier(player));
            case "totalmulti":
                return df.format(multiplierManager.getGlobalTotalMultiplier(player));
        }

        return null;
    }
}