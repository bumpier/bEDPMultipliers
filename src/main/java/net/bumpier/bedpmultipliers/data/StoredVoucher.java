// File: src/main/java/net/bumpier/bedpmultipliers/data/StoredVoucher.java
package net.bumpier.bedpmultipliers.data;

import java.util.Objects;

public class StoredVoucher {
    private final double amount;
    private final long duration;
    private final String currency;
    private final long receivedAt;

    public StoredVoucher(double amount, long duration, String currency) {
        this.amount = amount;
        this.duration = duration;
        this.currency = currency;
        this.receivedAt = System.currentTimeMillis();
    }

    public double getAmount() {
        return amount;
    }

    public long getDuration() {
        return duration;
    }

    public String getCurrency() {
        return currency;
    }

    public long getReceivedAt() {
        return receivedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoredVoucher that = (StoredVoucher) o;
        return Double.compare(that.amount, amount) == 0 &&
                duration == that.duration &&
                receivedAt == that.receivedAt &&
                Objects.equals(currency, that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, duration, currency, receivedAt);
    }
}