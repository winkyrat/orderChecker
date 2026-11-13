package dev.donutprofit.orderscanner.parse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Export document consumed by DonutProfit's {@code parseOrdersJson}.
 *
 * <p>Top-level fields beyond {@code orders} are ignored by the loader, so they
 * are free to carry provenance. Every row keeps {@code displayName} and
 * {@code slot} for auditing; the loader strips unknown keys.
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
     */
    public record Row(
            String itemId,
            String side,
            long unitPrice,
            long quantityRemaining,
            String observedAt,
            String displayName,
            int slot,
            boolean verified) {
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
                    order.slot(),
                    false));
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

    public String toJson() {
        return GSON.toJson(this);
    }

    public boolean isEmpty() {
        return orders.isEmpty();
    }
}
