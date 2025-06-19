// File: src/main/java/net/bumpier/bedpmultipliers/data/GlobalData.java
package net.bumpier.bedpmultipliers.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Note: This object holds all global (non-player-specific) data.
public class GlobalData {
    private final Map<String, net.bumpier.bedpmultipliers.data.TemporaryMultiplier> temporary = new ConcurrentHashMap<>();

    public Map<String, net.bumpier.bedpmultipliers.data.TemporaryMultiplier> getTemporary() {
        return temporary;
    }
}