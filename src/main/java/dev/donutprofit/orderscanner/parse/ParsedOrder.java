package dev.donutprofit.orderscanner.parse;

/**
 * One order row read from the {@code /order <item>} menu.
 *
 * <p>Values are already normalized to game-currency units, so {@code "$ 4K each"}
 * becomes {@code 4000}. {@code delivered} and {@code required} keep the raw
 * numbers from {@code "872/1k Delivered"} so a reviewer can audit the subtraction.
 */
public record ParsedOrder(
        int slot,
        String itemId,
        String displayName,
        long unitPrice,
        long delivered,
        long required,
        long quantityRemaining) {

    /** The order is a request for players to deliver items, so the buyer side. */
    public String side() {
        return "buy";
    }
}
