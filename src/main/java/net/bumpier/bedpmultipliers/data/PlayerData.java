// File: src/main/java/net/bumpier/bedpmultipliers/data/PlayerData.java
package net.bumpier.bedpmultipliers.data;

import java.util.*;

public class PlayerData {
    private Map<String, Double> permanent = new HashMap<>();
    private Map<String, net.bumpier.bedpmultipliers.data.TemporaryMultiplier> temporary = new HashMap<>();
    private List<net.bumpier.bedpmultipliers.data.StoredVoucher> storedVouchers = new ArrayList<>();

    public Map<String, Double> getPermanent() {
        return permanent;
    }

    public Map<String, net.bumpier.bedpmultipliers.data.TemporaryMultiplier> getTemporary() {
        return temporary;
    }

    public List<net.bumpier.bedpmultipliers.data.StoredVoucher> getStoredVouchers() {
        return storedVouchers;
    }
}