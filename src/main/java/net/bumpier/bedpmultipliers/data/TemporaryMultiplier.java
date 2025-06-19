// File: src/main/java/net/bumpier/bedpmultipliers/data/TemporaryMultiplier.java
package net.bumpier.bedpmultipliers.data;

public class TemporaryMultiplier {

    private final double amount;
    private final long duration;
    private final long expiry;
    private final long appliedAt;

    public TemporaryMultiplier(double amount, long duration, long expiry, long appliedAt) {
        this.amount = amount;
        this.duration = duration;
        this.expiry = expiry;
        this.appliedAt = appliedAt;
    }

    public double getAmount() {
        return amount;
    }

    public long getDuration() {
        return duration;
    }

    public long getExpiry() {
        return expiry;
    }

    public long getAppliedAt() { return appliedAt; }

    public boolean isActive() {
        return System.currentTimeMillis() < expiry;
    }
}