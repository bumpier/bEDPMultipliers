// File: src/main/java/net/bumpier/bedpmultipliers/commands/BMultiCommand.java
package net.bumpier.bedpmultipliers.commands;

import net.bumpier.bedpmultipliers.BEDPMultipliers;
import net.bumpier.bedpmultipliers.managers.ConfigManager;
import net.bumpier.bedpmultipliers.managers.MultiplierManager;
import net.bumpier.bedpmultipliers.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class BMultiCommand implements TabExecutor {

    private final BEDPMultipliers plugin;
    private final MultiplierManager multiplierManager;
    private final ConfigManager configManager;

    public BMultiCommand(BEDPMultipliers plugin, MultiplierManager multiplierManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.multiplierManager = multiplierManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if ("reload".equals(subCommand)) {
            handleReload(sender);
            return true;
        }

        switch (subCommand) {
            case "set" -> handleSet(sender, args);
            case "settemp" -> handleSetTemp(sender, args);
            case "remove" -> handleRemove(sender, args);
            default -> handleCheck(sender, args);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(List.of("set", "settemp", "remove", "reload"));
            return Stream.concat(
                    subCommands.stream(),
                    Bukkit.getOnlinePlayers().stream().map(Player::getName)
            ).filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (List.of("set", "settemp", "remove").contains(args[0].toLowerCase())) {
                List<String> completions = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                completions.add("global");
                return completions.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            } else if (sender.hasPermission("bmultipliers.player") && !args[0].equalsIgnoreCase("reload")) {
                return List.of("list").stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    private void sendUsage(CommandSender sender) {
        configManager.getMessageList("help-message").forEach(sender::sendMessage);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("bmultipliers.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return;
        }
        plugin.reloadPlugin();
        sender.sendMessage(configManager.getMessage("reload-success"));
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bmultipliers.player")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(configManager.getMessage("player-not-found"));
            return;
        }

        if (args.length > 1 && args[1].equalsIgnoreCase("list")) {
            listAllMultipliers(sender, target);
        } else if (args.length > 1) {
            String currency = args[1].toLowerCase();
            double totalMulti = multiplierManager.getTotalMultiplier(target, currency);
            sender.sendMessage(configManager.getMessage("check-multiplier")
                    .replace("%player%", target.getName())
                    .replace("%currency%", configManager.getFormattedCurrency(currency))
                    .replace("%multiplier%", String.valueOf(totalMulti)));
        } else {
            sender.sendMessage(configManager.getMessage("check-specify-currency").replace("%player%", target.getName()));
        }
    }

    private void listAllMultipliers(CommandSender sender, OfflinePlayer target) {
        sender.sendMessage(configManager.getMessage("check-header").replace("%player%", target.getName()));
        boolean hasMultipliers = false;

        Map<String, Double> permanent = multiplierManager.getAllPlayerPermanentMultipliers(target.getUniqueId());
        if (!permanent.isEmpty()) {
            hasMultipliers = true;
            permanent.forEach((currency, amount) ->
                    sender.sendMessage(configManager.getMessage("check-line-permanent")
                            .replace("%currency%", configManager.getFormattedCurrency(currency))
                            .replace("%amount%", String.valueOf(amount)))
            );
        }

        Map<String, Map<String, Object>> temporary = multiplierManager.getAllPlayerTemporaryMultipliers(target.getUniqueId());
        if (!temporary.isEmpty()) {
            hasMultipliers = true;
            temporary.forEach((currency, data) -> {
                long expiry = (long) data.getOrDefault("expiry", 0L);
                String timeLeft = TimeUtil.formatDuration(expiry - System.currentTimeMillis());
                sender.sendMessage(configManager.getMessage("check-line-temporary")
                        .replace("%currency%", configManager.getFormattedCurrency(currency))
                        .replace("%amount%", String.valueOf(data.get("amount")))
                        .replace("%time%", timeLeft));
            });
        }

        double rankMulti = multiplierManager.getPermissionMultiplier(target);
        if (rankMulti > 1.0) {
            hasMultipliers = true;
            sender.sendMessage(configManager.getMessage("check-rank").replace("%amount%", String.valueOf(rankMulti)));
        }

        if (!hasMultipliers) {
            sender.sendMessage(configManager.getMessage("check-none"));
        }
        sender.sendMessage(configManager.getMessage("check-footer"));
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bmultipliers.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(configManager.getMessage("usage-set"));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(configManager.getMessage("invalid-number"));
            return;
        }
        String currency = (args.length > 3) ? args[3].toLowerCase() : multiplierManager.getGlobalCurrencyKey();
        String currencyDisplay = currency.equals(multiplierManager.getGlobalCurrencyKey()) ? "All" : configManager.getFormattedCurrency(currency);
        String targetName = args[1];

        if (targetName.equalsIgnoreCase("global")) {
            sender.sendMessage(configManager.getMessage("error-global-permanent"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(configManager.getMessage("player-not-found"));
            return;
        }
        multiplierManager.setPlayerPermanentMultiplier(target.getUniqueId(), currency, amount);
        sender.sendMessage(configManager.getMessage("set-player-multiplier")
                .replace("%player%", target.getName())
                .replace("%currency%", currencyDisplay)
                .replace("%amount%", String.valueOf(amount)));
    }

    private void handleSetTemp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bmultipliers.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(configManager.getMessage("usage-settemp"));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(configManager.getMessage("invalid-number"));
            return;
        }
        long duration = TimeUtil.parseTime(args[3]);
        if (duration <= 0) {
            sender.sendMessage(configManager.getMessage("invalid-time-format"));
            return;
        }

        String currency = (args.length > 4) ? args[4].toLowerCase() : multiplierManager.getGlobalCurrencyKey();
        String currencyDisplay = currency.equals(multiplierManager.getGlobalCurrencyKey()) ? "All" : configManager.getFormattedCurrency(currency);
        String formattedTime = TimeUtil.formatDuration(duration);
        String targetName = args[1];

        if (targetName.equalsIgnoreCase("global")) {
            multiplierManager.setGlobalTemporaryMultiplier(currency, amount, duration);
            sender.sendMessage(configManager.getMessage("set-global-temp-multiplier")
                    .replace("%currency%", currencyDisplay)
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%time%", formattedTime));
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(configManager.getMessage("player-not-found"));
                return;
            }
            multiplierManager.setPlayerTemporaryMultiplier(target.getUniqueId(), currency, amount, duration);
            sender.sendMessage(configManager.getMessage("set-player-temp-multiplier")
                    .replace("%player%", target.getName())
                    .replace("%currency%", currencyDisplay)
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%time%", formattedTime));
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bmultipliers.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(configManager.getMessage("usage-remove"));
            return;
        }
        String targetName = args[1];
        String currency = (args.length > 2) ? args[2].toLowerCase() : multiplierManager.getGlobalCurrencyKey();
        String currencyDisplay = currency.equals(multiplierManager.getGlobalCurrencyKey()) ? "All" : configManager.getFormattedCurrency(currency);

        if (targetName.equalsIgnoreCase("global")) {
            multiplierManager.removeGlobalTemporaryMultiplier(currency);
            sender.sendMessage(configManager.getMessage("remove-global-multiplier").replace("%currency%", currencyDisplay));
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(configManager.getMessage("player-not-found"));
                return;
            }
            multiplierManager.removePlayerPermanentMultiplier(target.getUniqueId(), currency);
            multiplierManager.removePlayerTemporaryMultiplier(target.getUniqueId(), currency);
            sender.sendMessage(configManager.getMessage("remove-player-multiplier")
                    .replace("%player%", target.getName())
                    .replace("%currency%", currencyDisplay));
        }
    }
}