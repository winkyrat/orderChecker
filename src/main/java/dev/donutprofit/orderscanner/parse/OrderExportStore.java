package dev.donutprofit.orderscanner.parse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persists parsed orders in two shapes:
 *
 * <ul>
 *   <li>one file per capture, exactly as seen (audit trail);</li>
 *   <li>{@code orders.json}, a rolling snapshot with every known order row,
 *       which is the file DonutProfit loads.</li>
 * </ul>
 *
 * <p>A menu page is the unit of replacement: capturing page 1 of an item
 * replaces the rows that page 1 had before, and leaves page 2 untouched. That
 * keeps the snapshot equal to the last observed order book instead of mixing
 * old and new rows for the same slot.
 *
 * <p>No Minecraft types here, so the merge can be tested without the game.
 */
public final class OrderExportStore {
    public static final String SNAPSHOT_FILE = "orders.json";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final Comparator<OrderExport.Row> ROW_ORDER =
            Comparator.comparing(OrderExport.Row::itemId)
                    .thenComparingInt(OrderExport.Row::page)
                    .thenComparingInt(OrderExport.Row::slot);

    private OrderExportStore() {
    }

    /** Writes one capture next to the raw dumps and returns the new file. */
    public static Path writeCapture(Path directory, OrderExport export) throws IOException {
        Files.createDirectories(directory);
        String base = "orders-p" + export.page() + "-" + Instant.now().toEpochMilli();
        Path target = directory.resolve(base + ".json");
        Files.writeString(target, export.toJson(), StandardCharsets.UTF_8);
        return target;
    }

    /**
     * Merges the capture into {@code orders.json}.
     *
     * <p>Rows tagged with a {@code search} replace every previous row of that
     * same search, whatever page they were on: a search is one observation of
     * one item, so an item that moved from page 43 to page 41 must not leave its
     * old page-43 rows behind. Untagged rows (manual captures) keep the older
     * page-based rule: the captured page replaces itself and nothing else.
     *
     * <p>Rows from other searches and other pages keep their own
     * {@code observedAt}.
     *
     * @return the snapshot path.
     */
    public static Path mergeIntoSnapshot(Path directory, OrderExport export) throws IOException {
        Files.createDirectories(directory);
        Path snapshot = directory.resolve(SNAPSHOT_FILE);

        Map<String, OrderExport.Row> rows = new LinkedHashMap<>();
        if (Files.exists(snapshot)) {
            OrderExport existing = read(snapshot);
            if (existing != null) {
                for (OrderExport.Row row : existing.orders()) {
                    rows.put(key(row), row);
                }
            }
        }

        Set<String> capturedItems = new HashSet<>();
        Set<String> searches = new HashSet<>();
        for (OrderExport.Row row : export.orders()) {
            capturedItems.add(row.itemId());
            if (row.search() != null) {
                searches.add(row.search());
            }
        }
        rows.entrySet().removeIf(entry -> {
            OrderExport.Row row = entry.getValue();
            if (row.search() != null) {
                return searches.contains(row.search());
            }
            return row.page() == export.page() && capturedItems.contains(row.itemId());
        });

        for (OrderExport.Row row : export.orders()) {
            rows.put(key(row), row);
        }

        List<OrderExport.Row> merged = new ArrayList<>(rows.values());
        merged.sort(ROW_ORDER);

        List<String> warnings = new ArrayList<>(export.warnings());
        if (Files.exists(snapshot)) {
            warnings.add(0, "Merged with previous observations; each row keeps its own observedAt.");
        }

        OrderExport document = new OrderExport(
                OrderExport.SCHEMA_VERSION,
                export.modVersion(),
                OrderExport.SOURCE,
                export.observedAt(),
                export.capturedAt(),
                export.screenTitle(),
                export.page(),
                List.copyOf(merged),
                List.copyOf(warnings));

        Files.writeString(snapshot, document.toJson(), StandardCharsets.UTF_8);
        return snapshot;
    }

    /** @return the parsed snapshot, or {@code null} when the file is unusable. */
    public static OrderExport read(Path file) {
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            return GSON.fromJson(text, OrderExport.class);
        } catch (IOException | RuntimeException error) {
            return null;
        }
    }

    private static String key(OrderExport.Row row) {
        return row.itemId() + "|" + row.page() + "|" + row.slot();
    }
}
