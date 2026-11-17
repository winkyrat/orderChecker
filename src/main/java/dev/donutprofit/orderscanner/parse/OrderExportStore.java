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
 * <p>A menu page is the unit of replacement for manual captures: capturing page
 * 1 of an item replaces the rows that page 1 had before, and leaves page 2
 * untouched. That keeps the snapshot equal to the last observed order book
 * instead of mixing old and new rows for the same slot.
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
     * <p>Two kinds of row arrive here: the rows of the item that was searched,
     * tagged with that item, and the rows of every other item that shared the
     * page, tagged with the search that surfaced them.
     *
     * <p>The second kind is kept. It is a real observation, and it gives an item
     * a price level without spending a search on it. But it is a slice of a
     * price band at a page that means nothing for that item, so it is treated as
     * a fallback: as soon as the item is searched on its own, every by-product
     * row of that item is dropped and replaced by the direct scan. That is what
     * stops a ladder from being inflated by rows nobody asked for.
     *
     * <p>Rows tagged with a {@code search} replace every previous row of that
     * same search, whatever page they were on: a search is one observation of
     * one item, so an item that moved from page 43 to page 41 must not leave its
     * old page-43 rows behind. Untagged rows (manual captures) keep the older
     * page-based rule.
     *
     * @return the snapshot path.
     */
    public static Path mergeIntoSnapshot(Path directory, OrderExport export) throws IOException {
        return mergeIntoSnapshot(directory, export, null);
    }

    /**
     * Same as {@link #mergeIntoSnapshot(Path, OrderExport)}, plus the id of the
     * item that was searched.
     *
     * <p>A page 1 can legitimately produce no rows at all, for example when
     * every order on it was filtered out as enchanted. The previous observation
     * of that search must not survive an empty observation, so the search is
     * passed explicitly and retired like any other page-1 capture.
     *
     * @param searchedItemId the item id the scanner asked for, or {@code null}.
     */
    public static Path mergeIntoSnapshot(Path directory, OrderExport export, String searchedItemId)
            throws IOException {
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
        Set<String> directItems = new HashSet<>();
        for (OrderExport.Row row : rows.values()) {
            if (isDirect(row)) {
                directItems.add(row.itemId());
            }
        }
        for (OrderExport.Row row : export.orders()) {
            capturedItems.add(row.itemId());
            if (row.search() != null) {
                searches.add(row.search());
            }
            if (isDirect(row)) {
                directItems.add(row.itemId());
            }
        }

        // Every search starts on page 1, and a deep search captures one page at
        // a time. Clearing on every page would make page 2 delete what page 1
        // just stored, so the previous observation is retired only on page 1.
        boolean startsSearch = export.page() == 1;
        if (searchedItemId != null && startsSearch) {
            // An empty page 1 has no rows to tag, so the search it belongs to
            // has to be named explicitly for the retirement below to see it.
            searches.add(searchedItemId);
        }

        rows.entrySet().removeIf(entry -> {
            OrderExport.Row row = entry.getValue();
            if (row.search() != null) {
                // A new observation of the same search replaces the old one, and
                // a direct scan retires the by-products of that item.
                return (startsSearch && searches.contains(row.search()))
                        || (!isDirect(row) && directItems.contains(row.itemId()));
            }
            return row.page() == export.page() && capturedItems.contains(row.itemId());
        });

        for (OrderExport.Row row : export.orders()) {
            // A by-product never overrides a real scan of that item.
            if (!isDirect(row) && row.search() != null && directItems.contains(row.itemId())) {
                continue;
            }
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

    /** A row the user asked for, as opposed to one that shared the page. */
    private static boolean isDirect(OrderExport.Row row) {
        return row.search() != null && row.search().equals(row.itemId());
    }

    private static String key(OrderExport.Row row) {
        if (row.search() != null && !row.search().equals(row.itemId())) {
            // A by-product row has no meaningful page of its own: its identity
            // is the order itself, so two searches that surface it collapse
            // instead of piling up.
            return "i|" + row.itemId() + "|" + row.unitPrice() + "|" + row.quantityRemaining();
        }
        return "a|" + row.itemId() + "|" + row.page() + "|" + row.slot();
    }
}
