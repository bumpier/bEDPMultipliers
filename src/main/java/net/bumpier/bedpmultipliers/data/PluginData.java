// File: src/main/java/net/bumpier/bedpmultipliers/data/PluginData.java
package net.bumpier.bedpmultipliers.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PluginData {
    private Map<UUID, net.bumpier.bedpmultipliers.data.PlayerData> players = new HashMap<>();
    private net.bumpier.bedpmultipliers.data.PlayerData global = new net.bumpier.bedpmultipliers.data.PlayerData();

    public Map<UUID, net.bumpier.bedpmultipliers.data.PlayerData> getPlayers() {
        return players;
    }

    public net.bumpier.bedpmultipliers.data.PlayerData getGlobal() {
        return global;
    }
}