// File: src/main/java/net/bumpier/bedpmultipliers/managers/VoucherManager.java
package net.bumpier.bedpmultipliers.managers;

import net.bumpier.bedpmultipliers.data.StoredVoucher;
import net.bumpier.bedpmultipliers.utils.TimeUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

public class VoucherManager {

    private final JavaPlugin plugin;
    private final net.bumpier.bedpmultipliers.managers.ConfigManager configManager;
    private final NamespacedKey amountKey;
    private final NamespacedKey durationKey;
    private final NamespacedKey currencyKey;

    public VoucherManager(JavaPlugin plugin, net.bumpier.bedpmultipliers.managers.ConfigManager configManager, net.bumpier.bedpmultipliers.utils.DebugLogger debugLogger) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.amountKey = new NamespacedKey(plugin, "voucher_amount");
        this.durationKey = new NamespacedKey(plugin, "voucher_duration");
        this.currencyKey = new NamespacedKey(plugin, "voucher_currency");
    }

    public ItemStack createVoucherItem(StoredVoucher voucher) {
        String formattedDuration = TimeUtil.formatDuration(voucher.getDuration());
        ItemStack item = new ItemStack(Material.valueOf(configManager.getVoucherMaterial()));
        ItemMeta meta = item.getItemMeta();

        String displayName = configManager.getVoucherName()
                .replace("%multiplier%", String.valueOf(voucher.getAmount()))
                .replace("%currency%", configManager.getFormattedCurrency(voucher.getCurrency()));
        meta.setDisplayName(configManager.formatColors(displayName));

        List<String> lore = configManager.getVoucherLore().stream()
                .map(line -> configManager.formatColors(line
                        .replace("%multiplier%", String.valueOf(voucher.getAmount()))
                        .replace("%currency%", configManager.getFormattedCurrency(voucher.getCurrency()))
                        .replace("%time%", formattedDuration)))
                .collect(Collectors.toList());
        meta.setLore(lore);

        meta.addEnchant(Enchantment.FORTUNE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // --- PDC Data ---
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(amountKey, PersistentDataType.DOUBLE, voucher.getAmount());
        pdc.set(durationKey, PersistentDataType.LONG, voucher.getDuration());
        pdc.set(currencyKey, PersistentDataType.STRING, voucher.getCurrency());
        // --- End PDC Data ---

        item.setItemMeta(meta);
        return item;
    }
}