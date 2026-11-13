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

    private static OrderExport export(String observedAt, OrderExport.Row... rows) {
        return new OrderExport(
                OrderExport.SCHEMA_VERSION, "0.2.0", OrderExport.SOURCE, observedAt, observedAt,
                "Orders (Page 1)", 1, List.of(rows), List.of());
    }

    private static OrderExport.Row row(String itemId, long price, long remaining, String observedAt) {
        return new OrderExport.Row(itemId, "buy", price, remaining, observedAt, itemId, 0, false);
    }

    @Test
    void keepsTheBestPricePerItemAcrossCaptures() throws Exception {
        Path snapshot = OrderExportStore.mergeIntoSnapshot(tempDir, export(
                "2026-11-13T00:00:00Z",
                row("minecraft:glass", 1000, 500, "2026-11-13T00:00:00Z"),
                row("minecraft:diamond", 2000, 10, "2026-11-13T00:00:00Z")));

        assertTrue(Files.exists(snapshot));
        assertEquals(OrderExportStore.SNAPSHOT_FILE, snapshot.getFileName().toString());

        OrderExportStore.mergeIntoSnapshot(tempDir, export(
                "2026-11-13T01:00:00Z",
                row("minecraft:glass", 1500, 5, "2026-11-13T01:00:00Z"),
                row("minecraft:iron_ingot", 500, 900, "2026-11-13T01:00:00Z")));

        OrderExport merged = OrderExportStore.read(snapshot);
        assertNotNull(merged);
        assertEquals(3, merged.orders().size());

        OrderExport.Row glass = find(merged, "minecraft:glass");
        assertEquals(1500L, glass.unitPrice(), "higher price wins");
        assertEquals(5L, glass.quantityRemaining());
        assertEquals("2026-11-13T01:00:00Z", glass.observedAt(), "row keeps its own capture time");

        assertEquals(2000L, find(merged, "minecraft:diamond").unitPrice());
        assertEquals(500L, find(merged, "minecraft:iron_ingot").unitPrice());
    }

    @Test
    void lowerPriceDoesNotOverwriteTheSnapshot() throws Exception {
        OrderExportStore.mergeIntoSnapshot(tempDir, export(
                "2026-11-13T00:00:00Z",
                row("minecraft:glass", 1500, 5, "2026-11-13T00:00:00Z")));

        Path snapshot = OrderExportStore.mergeIntoSnapshot(tempDir, export(
                "2026-11-13T01:00:00Z",
                row("minecraft:glass", 1000, 500, "2026-11-13T01:00:00Z")));

        OrderExport merged = OrderExportStore.read(snapshot);
        assertNotNull(merged);
        assertEquals(1500L, find(merged, "minecraft:glass").unitPrice());
    }

    @Test
    void writesOneFilePerCapture() throws Exception {
        Path capture = OrderExportStore.writeCapture(tempDir, export(
                "2026-11-13T00:00:00Z",
                row("minecraft:glass", 1000, 500, "2026-11-13T00:00:00Z")));

        assertTrue(Files.exists(capture));
        assertTrue(capture.getFileName().toString().startsWith("orders-p1-"));
        assertTrue(capture.getFileName().toString().endsWith(".json"));
    }

    private static OrderExport.Row find(OrderExport export, String itemId) {
        return export.orders().stream()
                .filter(row -> row.itemId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing row for " + itemId));
    }
}
