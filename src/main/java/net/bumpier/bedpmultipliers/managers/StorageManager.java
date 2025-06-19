// File: src/main/java/net/bumpier/bedpmultipliers/managers/StorageManager.java
package net.bumpier.bedpmultipliers.managers;

import net.bumpier.bedpmultipliers.data.PlayerData;
import net.bumpier.bedpmultipliers.data.StoredVoucher;
import net.bumpier.bedpmultipliers.utils.SortType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class StorageManager {
    private final MultiplierManager multiplierManager;
    private final VoucherManager voucherManager;
    private final ConfigManager configManager;
    private final GUIManager guiManager;
    private final NamespacedKey storageIdKey;

    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, SortType> playerSorts = new HashMap<>();
    private final Map<UUID, String> playerCurrencyFilters = new HashMap<>();

    public StorageManager(JavaPlugin plugin, MultiplierManager multiplierManager, VoucherManager voucherManager, ConfigManager configManager, GUIManager guiManager) {
        this.multiplierManager = multiplierManager;
        this.voucherManager = voucherManager;
        this.configManager = configManager;
        this.guiManager = guiManager;
        this.storageIdKey = new NamespacedKey(plugin, "storage_voucher_id");
    }

    public void openStorageGUI(Player player, int page) {
        List<StoredVoucher> vouchers = getStoredVouchers(player.getUniqueId());
        SortType sortType = getPlayerSort(player.getUniqueId());
        String currencyFilter = getPlayerCurrencyFilter(player.getUniqueId());

        // --- Filtering ---
        List<StoredVoucher> processedVouchers = vouchers.stream()
                .filter(v -> currencyFilter.equalsIgnoreCase("All") || v.getCurrency().equalsIgnoreCase(currencyFilter))
                .collect(Collectors.toList());

        // --- Sorting ---
        Comparator<StoredVoucher> comparator = switch (sortType) {
            case AMOUNT_ASC -> Comparator.comparingDouble(StoredVoucher::getAmount);
            case AMOUNT_DESC -> Comparator.comparingDouble(StoredVoucher::getAmount).reversed();
            case TIME_ASC -> Comparator.comparingLong(StoredVoucher::getDuration);
            case TIME_DESC -> Comparator.comparingLong(StoredVoucher::getDuration).reversed();
            default -> Comparator.comparingLong(StoredVoucher::getReceivedAt).reversed();
        };
        processedVouchers.sort(comparator);

        // --- Pagination & GUI Building ---
        List<Integer> itemSlots = guiManager.getStorageGuiItemSlots();
        if (itemSlots.isEmpty()) {
            player.sendMessage(configManager.formatColors("&cGUI Error: Item slots are not configured in guis.yml"));
            return;
        }

        int itemsPerPage = itemSlots.size();
        int maxPages = (int) Math.ceil((double) processedVouchers.size() / itemsPerPage);
        if (maxPages == 0) maxPages = 1;
        if (page < 1) page = 1;
        if (page > maxPages) page = maxPages;
        playerPages.put(player.getUniqueId(), page);

        String title = guiManager.getStorageGuiTitle().replace("%page%", String.valueOf(page)).replace("%max_pages%", String.valueOf(maxPages));
        Inventory gui = Bukkit.createInventory(null, guiManager.getStorageGuiSize(), configManager.formatColors(title));

        // --- Item Placement ---
        placeVoucherItems(gui, processedVouchers, page, itemsPerPage, itemSlots);
        placeNavigationButtons(gui, page, maxPages);
        placeSortAndFilterButtons(gui, sortType, currencyFilter);
        placeFillerItems(gui);

        player.openInventory(gui);
    }

    private void placeVoucherItems(Inventory gui, List<StoredVoucher> vouchers, int page, int itemsPerPage, List<Integer> itemSlots) {
        int startIndex = (page - 1) * itemsPerPage;
        for (int i = 0; i < itemsPerPage; i++) {
            int voucherIndex = startIndex + i;
            if (voucherIndex >= vouchers.size()) break;

            int slot = itemSlots.get(i);
            if (slot >= gui.getSize()) continue;

            StoredVoucher voucher = vouchers.get(voucherIndex);
            gui.setItem(slot, createGuiVoucherItem(voucher));
        }
    }

    private void placeNavigationButtons(Inventory gui, int page, int maxPages) {
        if (page > 1) {
            gui.setItem(guiManager.getStorageGuiPrevPageSlot(), createItem(guiManager.getPrevPageItemMaterial(), guiManager.getPrevPageItemName(), guiManager.getPrevPageItemLore()));
        }
        if (page < maxPages) {
            gui.setItem(guiManager.getStorageGuiNextPageSlot(), createItem(guiManager.getNextPageItemMaterial(), guiManager.getNextPageItemName(), guiManager.getNextPageItemLore()));
        }
    }

    private void placeSortAndFilterButtons(Inventory gui, SortType sortType, String currencyFilter) {
        // Amount Sort Button
        String amountState = switch (sortType) {
            case AMOUNT_ASC -> "Lowest -> Highest";
            case AMOUNT_DESC -> "Highest -> Lowest";
            default -> "Default";
        };
        gui.setItem(guiManager.getSortByAmountSlot(), createDynamicItem(guiManager.getSortByAmountMaterial(), guiManager.getSortByAmountName(), guiManager.getSortByAmountLore(), amountState));

        // Time Sort Button
        String timeState = switch (sortType) {
            case TIME_ASC -> "Shortest -> Longest";
            case TIME_DESC -> "Longest -> Shortest";
            default -> "Default";
        };
        gui.setItem(guiManager.getSortByTimeSlot(), createDynamicItem(guiManager.getSortByTimeMaterial(), guiManager.getSortByTimeName(), guiManager.getSortByTimeLore(), timeState));

        // Currency Filter Button
        gui.setItem(guiManager.getFilterByCurrencySlot(), createDynamicItem(guiManager.getFilterByCurrencyMaterial(), guiManager.getFilterByCurrencyName(), guiManager.getFilterByCurrencyLore(), configManager.getFormattedCurrency(currencyFilter)));
    }

    private void placeFillerItems(Inventory gui) {
        if (guiManager.isFillerItemEnabled()) {
            ItemStack filler = createItem(guiManager.getFillerItemMaterial(), guiManager.getFillerItemName(), Collections.emptyList());
            for (int i = 0; i < gui.getSize(); i++) {
                if (gui.getItem(i) == null) {
                    gui.setItem(i, filler);
                }
            }
        }
    }

    private ItemStack createDynamicItem(String material, String name, List<String> lore, String state) {
        List<String> processedLore = lore.stream().map(l -> l.replace("%state%", state)).collect(Collectors.toList());
        return createItem(material, name, processedLore);
    }

    private ItemStack createItem(String materialName, String name, List<String> lore) {
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.STONE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(configManager.formatColors(name));
            meta.setLore(lore.stream().map(configManager::formatColors).collect(Collectors.toList()));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createGuiVoucherItem(StoredVoucher voucher) {
        ItemStack item = voucherManager.createVoucherItem(voucher);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                lore.addAll(guiManager.getStorageItemLore());
                meta.setLore(lore.stream().map(configManager::formatColors).collect(Collectors.toList()));
            }
            meta.getPersistentDataContainer().set(storageIdKey, PersistentDataType.LONG, voucher.getReceivedAt());
            item.setItemMeta(meta);
        }
        return item;
    }

    public void addVoucherToStorage(UUID uuid, StoredVoucher voucher) {
        multiplierManager.getPlayerData(uuid).getStoredVouchers().add(voucher);
        multiplierManager.setDirty();
    }

    public List<StoredVoucher> getStoredVouchers(UUID uuid) {
        return multiplierManager.getPlayerData(uuid).getStoredVouchers();
    }

    public void removeVoucherFromStorage(UUID uuid, StoredVoucher voucher) {
        PlayerData playerData = multiplierManager.getPlayerData(uuid);
        playerData.getStoredVouchers().remove(voucher);
        multiplierManager.setDirty();
    }

    public int getPlayerPage(UUID uuid) {
        return playerPages.getOrDefault(uuid, -1);
    }

    public void removePlayerFromPageTracking(UUID uuid) {
        playerPages.remove(uuid);
    }

    public NamespacedKey getStorageIdKey() {
        return storageIdKey;
    }

    public SortType getPlayerSort(UUID uuid) { return playerSorts.getOrDefault(uuid, SortType.DEFAULT); }
    public void setPlayerSort(UUID uuid, SortType sortType) { playerSorts.put(uuid, sortType); }
    public String getPlayerCurrencyFilter(UUID uuid) { return playerCurrencyFilters.getOrDefault(uuid, "All"); }
    public void setPlayerCurrencyFilter(UUID uuid, String currency) { playerCurrencyFilters.put(uuid, currency); }

    public List<String> getAvailableCurrencies(UUID uuid) {
        List<String> currencies = getStoredVouchers(uuid).stream().map(StoredVoucher::getCurrency).distinct().collect(Collectors.toList());
        currencies.add(0, "All");
        return currencies;
    }
}