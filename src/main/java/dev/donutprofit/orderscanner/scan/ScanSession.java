package dev.donutprofit.orderscanner.scan;

/**
 * Tracks the hard limits of one scanning session.
 *
 * <p>Two caps, both required by the owner: a maximum number of item searches and
 * a maximum wall-clock duration. Reaching either one stops the scan; it does not
 * silently continue with a different item.
 */
public final class ScanSession {
    private final int maxItems;
    private final long maxMillis;
    private final long startedAtMillis;
    private int items;

    public ScanSession(ScanConfig config, long startedAtMillis) {
        this.maxItems = config.maxItemsPerSession();
        this.maxMillis = config.maxSessionMinutes() * 60_000L;
        this.startedAtMillis = startedAtMillis;
    }

    public void recordItem() {
        items++;
    }

    public int items() {
        return items;
    }

    public long elapsedMillis(long nowMillis) {
        return Math.max(0L, nowMillis - startedAtMillis);
    }

    public boolean itemsExhausted() {
        return items >= maxItems;
    }

    public boolean timeExhausted(long nowMillis) {
        return elapsedMillis(nowMillis) >= maxMillis;
    }

    public boolean shouldStop(long nowMillis) {
        return itemsExhausted() || timeExhausted(nowMillis);
    }

    /** Human-readable reason for the stop, or {@code null} while it can continue. */
    public String stopReason(long nowMillis) {
        if (itemsExhausted()) {
            return "reached the " + maxItems + " search cap";
        }
        if (timeExhausted(nowMillis)) {
            return "reached the " + (maxMillis / 60_000L) + " minute cap";
        }
        return null;
    }
}
