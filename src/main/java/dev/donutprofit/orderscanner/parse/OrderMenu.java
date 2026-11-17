package dev.donutprofit.orderscanner.parse;

import java.util.List;

/**
 * Result of reading one orders screen: the rows plus anything worth reporting.
 *
 * <p>{@code skippedItemIds} holds the item ids of order rows that were dropped
 * because the item carries enchantments. They are kept apart from
 * {@code orders} so the autonomous scanner can still tell that a page contains
 * the item it asked for even when every row of that item was filtered out.
 */
public record OrderMenu(
        int page,
        List<ParsedOrder> orders,
        List<String> warnings,
        List<String> skippedItemIds) {
}
