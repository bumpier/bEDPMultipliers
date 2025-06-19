// File: src/main/java/net/bumpier/bedpmultipliers/managers/GUIManager.java
package net.bumpier.bedpmultipliers.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public class GUIManager {

    private final JavaPlugin plugin;
    private FileConfiguration guisConfig;
    private final File guisFile;

    public GUIManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.guisFile = new File(plugin.getDataFolder(), "guis.yml");
        loadGuiConfigs();
    }

    public void loadGuiConfigs() {
        if (!guisFile.exists()) {
            plugin.saveResource("guis.yml", false);
        }
        guisConfig = YamlConfiguration.loadConfiguration(guisFile);
    }

    // --- General ---
    public String getStorageGuiTitle() { return guisConfig.getString("storage-gui.title", "&8Storage - Page %page%/%max_pages%"); }
    public int getStorageGuiSize() { return guisConfig.getInt("storage-gui.size", 54); }
    public List<Integer> getStorageGuiItemSlots() { return guisConfig.getIntegerList("storage-gui.item-slots"); }
    public List<String> getStorageItemLore() { return guisConfig.getStringList("storage-gui.item-lore"); }

    // --- Navigation ---
    public String getPrevPageItemMaterial() { return guisConfig.getString("storage-gui.previous-page-item.material", "ARROW"); }
    public String getPrevPageItemName() { return guisConfig.getString("storage-gui.previous-page-item.name", "&cPrevious Page"); }
    public List<String> getPrevPageItemLore() { return guisConfig.getStringList("storage-gui.previous-page-item.lore"); }
    public int getStorageGuiPrevPageSlot() { return guisConfig.getInt("storage-gui.previous-page-item.slot", 0); }

    public String getNextPageItemMaterial() { return guisConfig.getString("storage-gui.next-page-item.material", "ARROW"); }
    public String getNextPageItemName() { return guisConfig.getString("storage-gui.next-page-item.name", "&aNext Page"); }
    public List<String> getNextPageItemLore() { return guisConfig.getStringList("storage-gui.next-page-item.lore"); }
    public int getStorageGuiNextPageSlot() { return guisConfig.getInt("storage-gui.next-page-item.slot", 8); }

    // --- Filler ---
    public boolean isFillerItemEnabled() { return guisConfig.getBoolean("storage-gui.filler-item.enabled", false); }
    public String getFillerItemMaterial() { return guisConfig.getString("storage-gui.filler-item.material", "GRAY_STAINED_GLASS_PANE"); }
    public String getFillerItemName() { return guisConfig.getString("storage-gui.filler-item.name", " "); }

    // --- Sort/Filter Buttons ---
    public int getSortByAmountSlot() { return guisConfig.getInt("storage-gui.sort-by-amount-item.slot"); }
    public String getSortByAmountMaterial() { return guisConfig.getString("storage-gui.sort-by-amount-item.material"); }
    public String getSortByAmountName() { return guisConfig.getString("storage-gui.sort-by-amount-item.name"); }
    public List<String> getSortByAmountLore() { return guisConfig.getStringList("storage-gui.sort-by-amount-item.lore"); }

    public int getSortByTimeSlot() { return guisConfig.getInt("storage-gui.sort-by-time-item.slot"); }
    public String getSortByTimeMaterial() { return guisConfig.getString("storage-gui.sort-by-time-item.material"); }
    public String getSortByTimeName() { return guisConfig.getString("storage-gui.sort-by-time-item.name"); }
    public List<String> getSortByTimeLore() { return guisConfig.getStringList("storage-gui.sort-by-time-item.lore"); }

    public int getFilterByCurrencySlot() { return guisConfig.getInt("storage-gui.filter-by-currency-item.slot"); }
    public String getFilterByCurrencyMaterial() { return guisConfig.getString("storage-gui.filter-by-currency-item.material"); }
    public String getFilterByCurrencyName() { return guisConfig.getString("storage-gui.filter-by-currency-item.name"); }
    public List<String> getFilterByCurrencyLore() { return guisConfig.getStringList("storage-gui.filter-by-currency-item.lore"); }
}