package dev.donutprofit.orderscanner.scan;

/**
 * Tracks the hard limits of one scanning session.
 *
 * <p>Three caps: a maximum number of item searches, a maximum wall-clock
 * duration and a maximum number of pages read. Reaching any one stops the scan;
 * it does not silently continue with a different item.
 *
 * <p>The page cap exists because paginating towards a cheap item can take many
 * clicks. It is the direct control on how much clicking one session is allowed
 * to do, independently of how many searches it ran.
 */
public final class ScanSession {
    private final int maxItems;
    private final int maxPages;
    private final long maxMillis;
    private final long startedAtMillis;
    private int items;
    private int pages;

    public ScanSession(ScanConfig config, long startedAtMillis) {
        this.maxItems = config.maxItemsPerSession();
        this.maxPages = config.maxPagesPerSession();
        this.maxMillis = config.maxSessionMinutes() * 60_000L;
        this.startedAtMillis = startedAtMillis;
    }

    public void recordItem() {
        items++;
    }

    public void recordPage() {
        pages++;
    }

    public int items() {
        return items;
    }

    public int pages() {
        return pages;
    }

    public long elapsedMillis(long nowMillis) {
        return Math.max(0L, nowMillis - startedAtMillis);
    }

    public boolean itemsExhausted() {
        return items >= maxItems;
    }

    public boolean pagesExhausted() {
        return pages >= maxPages;
    }

    public boolean timeExhausted(long nowMillis) {
        return elapsedMillis(nowMillis) >= maxMillis;
    }

    public boolean shouldStop(long nowMillis) {
        return itemsExhausted() || pagesExhausted() || timeExhausted(nowMillis);
    }

    /** Human-readable reason for the stop, or {@code null} while it can continue. */
    public String stopReason(long nowMillis) {
        if (itemsExhausted()) {
            return "reached the " + maxItems + " search cap";
        }
        if (pagesExhausted()) {
            return "reached the " + maxPages + " page cap";
        }
        if (timeExhausted(nowMillis)) {
            return "reached the " + (maxMillis / 60_000L) + " minute cap";
        }
        return null;
    }
}
