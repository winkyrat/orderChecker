package dev.donutprofit.orderscanner.parse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Export document consumed by DonutProfit's {@code parseOrdersJson}.
 *
 * <p>Top-level fields beyond {@code orders} are ignored by the loader, so they
 * are free to carry provenance. Every row keeps {@code displayName},
 * {@code page} and {@code slot} for auditing; the loader strips unknown keys.
 */
public record OrderExport(
        int schemaVersion,
        String modVersion,
        String source,
        String observedAt,
        String capturedAt,
        String screenTitle,
        int page,
        List<Row> orders,
        List<String> warnings) {

    public static final int SCHEMA_VERSION = 1;
    public static final String SOURCE = "manual";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /**
     * One row of {@code orderRecordSchema}. {@code unitPrice} is in whole
     * currency units and {@code quantityRemaining} is positive by construction.
     *
     * <p>Every menu row is exported, not only the best price: DonutProfit builds
     * a demand ladder per item, so dropping the cheaper rows would understate
     * how much the market is actually buying.
     *
     * <p>{@code search} is the item id whose {@code /order} search produced the
     * row, or {@code null} for a manual capture. It lets the snapshot replace a
     * whole search at once, so a row cannot survive from an older observation
     * of the same item at a page it no longer occupies. DonutProfit strips the
     * key.
     */
    public record Row(
            String itemId,
            String side,
            long unitPrice,
            long quantityRemaining,
            String observedAt,
            String displayName,
            int page,
            int slot,
            boolean verified,
            String search) {
    }

    public static OrderExport of(OrderMenu menu, String modVersion, String capturedAt, String screenTitle) {
        List<Row> rows = new ArrayList<>(menu.orders().size());
        for (ParsedOrder order : menu.orders()) {
            rows.add(new Row(
                    order.itemId(),
                    order.side(),
                    order.unitPrice(),
                    order.quantityRemaining(),
                    capturedAt,
                    order.displayName(),
                    menu.page(),
                    order.slot(),
                    false,
                    null));
        }
        return new OrderExport(
                SCHEMA_VERSION,
                modVersion,
                SOURCE,
                capturedAt,
                capturedAt,
                screenTitle,
                menu.page(),
                List.copyOf(rows),
                menu.warnings());
    }

    /**
     * Keeps only the rows of the item that was searched and stamps them with the
     * search that produced them.
     *
     * <p>Paging towards a cheap item walks dozens of pages of other items. Those
     * rows are real, but they are a slice of somebody else's price band at a page
     * that means nothing for that item's own search: keeping them inflates its
     * demand ladder and leaves stale rows behind once the item is scanned
     * directly. The autonomous scan therefore stores only the item it asked for.
     */
    public OrderExport onlyFor(String itemId) {
        List<Row> kept = new ArrayList<>();
        for (Row row : orders) {
            if (row.itemId().equals(itemId)) {
                kept.add(new Row(
                        row.itemId(),
                        row.side(),
                        row.unitPrice(),
                        row.quantityRemaining(),
                        row.observedAt(),
                        row.displayName(),
                        row.page(),
                        row.slot(),
                        row.verified(),
                        itemId));
            }
        }
        return new OrderExport(
                schemaVersion,
                modVersion,
                source,
                observedAt,
                capturedAt,
                screenTitle,
                page,
                List.copyOf(kept),
                warnings);
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public boolean isEmpty() {
        return orders.isEmpty();
    }
}
