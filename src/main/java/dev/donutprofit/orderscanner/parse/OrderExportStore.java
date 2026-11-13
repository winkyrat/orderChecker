package dev.donutprofit.orderscanner.parse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists parsed orders in two shapes:
 *
 * <ul>
 *   <li>one file per capture, exactly as seen (audit trail);</li>
 *   <li>{@code orders.json}, a rolling snapshot with the best buy price per
 *       item, which is the file DonutProfit loads.</li>
 * </ul>
 *
 * <p>No Minecraft types here, so the merge can be tested without the game.
 */
public final class OrderExportStore {
    public static final String SNAPSHOT_FILE = "orders.json";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

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
     * Merges the capture into {@code orders.json}, keeping the highest unit price
     * per item and that row's own timestamp. Returns the snapshot path.
     */
    public static Path mergeIntoSnapshot(Path directory, OrderExport export) throws IOException {
        Files.createDirectories(directory);
        Path snapshot = directory.resolve(SNAPSHOT_FILE);

        Map<String, OrderExport.Row> best = new LinkedHashMap<>();
        if (Files.exists(snapshot)) {
            OrderExport existing = read(snapshot);
            if (existing != null) {
                for (OrderExport.Row row : existing.orders()) {
                    best.put(row.itemId(), row);
                }
            }
        }

        for (OrderExport.Row row : export.orders()) {
            OrderExport.Row previous = best.get(row.itemId());
            if (previous == null || isBetter(row, previous)) {
                best.put(row.itemId(), row);
            }
        }

        List<String> warnings = new ArrayList<>(export.warnings());
        if (Files.exists(snapshot)) {
            warnings.add(0, "Merged with previous observations; each row keeps its own observedAt.");
        }

        OrderExport merged = new OrderExport(
                OrderExport.SCHEMA_VERSION,
                export.modVersion(),
                OrderExport.SOURCE,
                export.observedAt(),
                export.capturedAt(),
                export.screenTitle(),
                export.page(),
                List.copyOf(best.values()),
                List.copyOf(warnings));

        Files.writeString(snapshot, merged.toJson(), StandardCharsets.UTF_8);
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

    private static boolean isBetter(OrderExport.Row candidate, OrderExport.Row current) {
        if (candidate.unitPrice() != current.unitPrice()) {
            return candidate.unitPrice() > current.unitPrice();
        }
        return candidate.quantityRemaining() > current.quantityRemaining();
    }
}
