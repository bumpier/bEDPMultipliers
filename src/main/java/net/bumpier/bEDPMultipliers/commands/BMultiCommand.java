// File: src/main/java/net/bumpier/bedpmultipliers/commands/BMultiCommand.java
package net.bumpier.bedpmultipliers.commands;

import net.bumpier.bedpmultipliers.managers.ConfigManager;
import net.bumpier.bedpmultipliers.managers.MultiplierManager;
import net.bumpier.bedpmultipliers.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class BMultiCommand implements CommandExecutor {

    private final MultiplierManager multiplierManager;
    private final ConfigManager configManager;

    public BMultiCommand(MultiplierManager multiplierManager, ConfigManager configManager) {
        this.multiplierManager = multiplierManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set":
                handleSet(sender, args);
                break;
            case "settemp":
                handleSetTemp(sender, args);
                break;
            case "remove":
                handleRemove(sender, args);
                break;
            case "check": // Added an explicit "check" subcommand for clarity
            default:
                handleCheck(sender, args);
                break;
        }
        return true;
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bmultipliers.player")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        OfflinePlayer target;
        if (args.length >= 2) {
            target = Bukkit.getOfflinePlayer(args[1]);
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /bmulti check <player>");
            return;
        }

        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(configManager.getMessage("player-not-found"));
            return;
        }

        double totalMulti = multiplierManager.getTotalMultiplier(target);
        sender.sendMessage(configManager.getMessage("check-multiplier")
                .replace("%player%", target.getName())
                .replace("%multiplier%", String.valueOf(totalMulti)));
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bmultipliers.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /bmulti set <player/global> <amount>");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(configManager.getMessage("invalid-number"));
            return;
        }

        String targetName = args[1];
        if (targetName.equalsIgnoreCase("global")) {
            configManager.setGlobalMultiplier(amount);
            sender.sendMessage(configManager.getMessage("set-global-multiplier").replace("%amount%", String.valueOf(amount)));
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (target == null || !target.hasPlayedBefore()) {
                sender.sendMessage(configManager.getMessage("player-not-found"));
                return;
            }
            multiplierManager.setPlayerMultiplier(target.getUniqueId(), amount);
            sender.sendMessage(configManager.getMessage("set-player-multiplier")
                    .replace("%player%", target.getName())
                    .replace("%amount%", String.valueOf(amount)));
        }
    }

    private void handleSetTemp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bmultipliers.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /bmulti settemp <player/global> <amount> <time>");
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

        String targetName = args[1];
        String formattedTime = TimeUtil.formatDuration(duration);

        if (targetName.equalsIgnoreCase("global")) {
            multiplierManager.setGlobalTempMultiplier(amount, duration);
            sender.sendMessage(configManager.getMessage("set-global-temp-multiplier")
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%time%", formattedTime));
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (target == null || !target.hasPlayedBefore()) {
                sender.sendMessage(configManager.getMessage("player-not-found"));
                return;
            }
            multiplierManager.setPlayerTempMultiplier(target.getUniqueId(), amount, duration);
            sender.sendMessage(configManager.getMessage("set-player-temp-multiplier")
                    .replace("%player%", target.getName())
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
            sender.sendMessage(ChatColor.RED + "Usage: /bmulti remove <player/global>");
            return;
        }

        String targetName = args[1];
        if (targetName.equalsIgnoreCase("global")) {
            configManager.setGlobalMultiplier(1.0); // Reset to default
            multiplierManager.removeGlobalTempMultiplier();
            sender.sendMessage(configManager.getMessage("remove-global-multiplier"));
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (target == null || !target.hasPlayedBefore()) {
                sender.sendMessage(configManager.getMessage("player-not-found"));
                return;
            }
            multiplierManager.removePlayerMultiplier(target.getUniqueId());
            sender.sendMessage(configManager.getMessage("remove-player-multiplier").replace("%player%", target.getName()));
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- bEDPMultipliers Help ---");
        if (sender.hasPermission("bmultipliers.player")) {
            sender.sendMessage(ChatColor.YELLOW + "/bmulti <player> - Check a player's multiplier.");
        }
        if (sender.hasPermission("bmultipliers.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/bmulti set <player/global> <amount> - Set a multiplier.");
            sender.sendMessage(ChatColor.YELLOW + "/bmulti settemp <player/global> <amount> <time> - Set a temporary multiplier (e.g., 1d2h3m).");
            sender.sendMessage(ChatColor.YELLOW + "/bmulti remove <player/global> - Remove a multiplier.");
        }
    }
    // Note: This class handles all command logic, splitting functionality into private methods for clarity.
}