package dev.donutprofit.orderscanner.parse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderExportStoreTest {

    @TempDir
    Path tempDir;

    private static OrderExport export(int page, String observedAt, OrderExport.Row... rows) {
        return new OrderExport(
                OrderExport.SCHEMA_VERSION, "0.3.0", OrderExport.SOURCE, observedAt, observedAt,
                "Orders (Page " + page + ")", page, List.of(rows), List.of());
    }

    private static OrderExport.Row row(String itemId, int page, int slot, long price, long remaining,
                                       String observedAt) {
        return new OrderExport.Row(itemId, "buy", price, remaining, observedAt, itemId, page, slot, false);
    }

    @Test
    void keepsEveryRowOfTheSameItemInsteadOfOnlyTheBestPrice() throws Exception {
        Path snapshot = OrderExportStore.mergeIntoSnapshot(tempDir, export(1, "2026-11-13T00:00:00Z",
                row("minecraft:diamond_block", 1, 0, 98100, 10, "2026-11-13T00:00:00Z"),
                row("minecraft:diamond_block", 1, 1, 98000, 64, "2026-11-13T00:00:00Z"),
                row("minecraft:diamond_block", 1, 2, 80000, 128, "2026-11-13T00:00:00Z")));

        OrderExport merged = OrderExportStore.read(snapshot);
        assertNotNull(merged);
        assertEquals(3, merged.orders().size(), "a demand ladder is many rows for one item");
    }

    @Test
    void recapturingAPageReplacesThatPageOnly() throws Exception {
        OrderExportStore.mergeIntoSnapshot(tempDir, export(1, "2026-11-13T00:00:00Z",
                row("minecraft:diamond_block", 1, 0, 98100, 10, "2026-11-13T00:00:00Z"),
                row("minecraft:diamond_block", 1, 1, 98000, 64, "2026-11-13T00:00:00Z")));
        OrderExportStore.mergeIntoSnapshot(tempDir, export(2, "2026-11-13T00:05:00Z",
                row("minecraft:diamond_block", 2, 0, 40000, 512, "2026-11-13T00:05:00Z")));

        Path snapshot = OrderExportStore.mergeIntoSnapshot(tempDir, export(1, "2026-11-13T00:10:00Z",
                row("minecraft:diamond_block", 1, 0, 99500, 3, "2026-11-13T00:10:00Z")));

        OrderExport merged = OrderExportStore.read(snapshot);
        assertNotNull(merged);
        assertEquals(2, merged.orders().size(), "page 1 replaced, page 2 kept");

        OrderExport.Row pageOne = find(merged, "minecraft:diamond_block", 1);
        assertEquals(99500L, pageOne.unitPrice());
        assertEquals("2026-11-13T00:10:00Z", pageOne.observedAt());

        OrderExport.Row pageTwo = find(merged, "minecraft:diamond_block", 2);
        assertEquals(40000L, pageTwo.unitPrice());
        assertEquals("2026-11-13T00:05:00Z", pageTwo.observedAt(), "other pages keep their timestamp");
    }

    @Test
    void keepsItemsThatTheCaptureDidNotMention() throws Exception {
        OrderExportStore.mergeIntoSnapshot(tempDir, export(1, "2026-11-13T00:00:00Z",
                row("minecraft:glass", 1, 0, 4000, 128, "2026-11-13T00:00:00Z"),
                row("minecraft:tinted_glass", 1, 1, 1700, 100, "2026-11-13T00:00:00Z")));

        Path snapshot = OrderExportStore.mergeIntoSnapshot(tempDir, export(1, "2026-11-13T01:00:00Z",
                row("minecraft:glass", 1, 0, 4200, 100, "2026-11-13T01:00:00Z")));

        OrderExport merged = OrderExportStore.read(snapshot);
        assertNotNull(merged);
        assertEquals(2, merged.orders().size());
        assertEquals(4200L, find(merged, "minecraft:glass", 1).unitPrice());
        assertEquals(1700L, find(merged, "minecraft:tinted_glass", 1).unitPrice());
    }

    @Test
    void writesOneFilePerCapture() throws Exception {
        Path capture = OrderExportStore.writeCapture(tempDir, export(2, "2026-11-13T00:00:00Z",
                row("minecraft:glass", 2, 0, 1000, 500, "2026-11-13T00:00:00Z")));

        assertTrue(Files.exists(capture));
        assertTrue(capture.getFileName().toString().startsWith("orders-p2-"));
        assertTrue(capture.getFileName().toString().endsWith(".json"));
    }

    private static OrderExport.Row find(OrderExport export, String itemId, int page) {
        return export.orders().stream()
                .filter(row -> row.itemId().equals(itemId) && row.page() == page)
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing row for " + itemId + " page " + page));
    }
}
