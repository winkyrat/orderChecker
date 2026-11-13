package dev.donutprofit.orderscanner.dump;

/**
 * One data component of an item stack, kept as text evidence.
 *
 * <p>The raw text is intentionally lossy: it is enough to reconstruct the menu
 * layout and lore during reconnaissance. The parser never consumes this field
 * as a price source.
 */
public record ComponentDump(String id, String value) {
}
