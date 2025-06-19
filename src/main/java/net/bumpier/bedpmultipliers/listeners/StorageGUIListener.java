// File: src/main/java/net/bumpier/bedpmultipliers/listeners/StorageGUIListener.java
package net.bumpier.bedpmultipliers.listeners;

import net.bumpier.bedpmultipliers.data.StoredVoucher;
import net.bumpier.bedpmultipliers.managers.*;
import net.bumpier.bedpmultipliers.utils.SortType;
import net.bumpier.bedpmultipliers.utils.TimeUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;

public class StorageGUIListener implements Listener {

    private final JavaPlugin plugin;
    private final StorageManager storageManager;
    private final MultiplierManager multiplierManager;
    private final VoucherManager voucherManager;
    private final ConfigManager configManager;
    private final GUIManager guiManager;

    public StorageGUIListener(JavaPlugin plugin, StorageManager storageManager, MultiplierManager multiplierManager, VoucherManager voucherManager, ConfigManager configManager, GUIManager guiManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
        this.multiplierManager = multiplierManager;
        this.voucherManager = voucherManager;
        this.configManager = configManager;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        String configTitle = configManager.formatColors(guiManager.getStorageGuiTitle());
        if (!title.startsWith(configTitle.split("%page%")[0])) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        int clickedSlot = event.getRawSlot();

        if (clickedItem == null || clickedItem.getType().isAir()) return;

        int currentPage = storageManager.getPlayerPage(player.getUniqueId());
        if (currentPage == -1) return; // Player session not found, should not happen

        // --- Navigation ---
        if (clickedSlot == guiManager.getStorageGuiPrevPageSlot()) {
            storageManager.openStorageGUI(player, currentPage - 1);
            return;
        }
        if (clickedSlot == guiManager.getStorageGuiNextPageSlot()) {
            storageManager.openStorageGUI(player, currentPage + 1);
            return;
        }

        // --- Sorting and Filtering ---
        if (handleSortAndFilter(player, clickedSlot)) return;

        // --- Voucher Interaction ---
        handleVoucherClick(player, clickedItem, event);
    }

    private boolean handleSortAndFilter(Player player, int slot) {
        SortType currentSort = storageManager.getPlayerSort(player.getUniqueId());
        boolean stateChanged = true;

        if (slot == guiManager.getSortByAmountSlot()) {
            if (currentSort == SortType.AMOUNT_ASC) storageManager.setPlayerSort(player.getUniqueId(), SortType.AMOUNT_DESC);
            else if (currentSort == SortType.AMOUNT_DESC) storageManager.setPlayerSort(player.getUniqueId(), SortType.DEFAULT);
            else storageManager.setPlayerSort(player.getUniqueId(), SortType.AMOUNT_ASC);
        } else if (slot == guiManager.getSortByTimeSlot()) {
            if (currentSort == SortType.TIME_ASC) storageManager.setPlayerSort(player.getUniqueId(), SortType.TIME_DESC);
            else if (currentSort == SortType.TIME_DESC) storageManager.setPlayerSort(player.getUniqueId(), SortType.DEFAULT);
            else storageManager.setPlayerSort(player.getUniqueId(), SortType.TIME_ASC);
        } else if (slot == guiManager.getFilterByCurrencySlot()) {
            List<String> currencies = storageManager.getAvailableCurrencies(player.getUniqueId());
            if (currencies.size() <= 1) return true; // Don't cycle if there's nothing to cycle to
            String currentFilter = storageManager.getPlayerCurrencyFilter(player.getUniqueId());
            int currentIndex = currencies.indexOf(currentFilter);
            String nextFilter = currencies.get((currentIndex + 1) % currencies.size());
            storageManager.setPlayerCurrencyFilter(player.getUniqueId(), nextFilter);
        } else {
            stateChanged = false; // Not a sort/filter button
        }

        if (stateChanged) {
            storageManager.openStorageGUI(player, 1);
        }

        return stateChanged;
    }

    private void handleVoucherClick(Player player, ItemStack item, InventoryClickEvent event) {
        if (item.getItemMeta() == null) return;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(storageManager.getStorageIdKey(), PersistentDataType.LONG)) return;

        long voucherId = pdc.get(storageManager.getStorageIdKey(), PersistentDataType.LONG);
        Optional<StoredVoucher> voucherOpt = storageManager.getStoredVouchers(player.getUniqueId()).stream()
                .filter(v -> v.getReceivedAt() == voucherId)
                .findFirst();

        voucherOpt.ifPresent(voucher -> {
            player.closeInventory();
            if (event.isLeftClick()) {
                multiplierManager.setPlayerTemporaryMultiplier(player.getUniqueId(), voucher.getCurrency(), voucher.getAmount(), voucher.getDuration());
                storageManager.removeVoucherFromStorage(player.getUniqueId(), voucher);
                player.sendMessage(configManager.getMessage("multiplier-applied-storage")
                        .replace("%multiplier%", String.valueOf(voucher.getAmount()))
                        .replace("%currency%", configManager.getFormattedCurrency(voucher.getCurrency()))
                        .replace("%time%", TimeUtil.formatDuration(voucher.getDuration())));
            } else if (event.isRightClick()) {
                ItemStack voucherItem = voucherManager.createVoucherItem(voucher);
                player.getInventory().addItem(voucherItem);
                storageManager.removeVoucherFromStorage(player.getUniqueId(), voucher);
                player.sendMessage(configManager.getMessage("voucher-withdrawn"));
            }
        });
    }
}