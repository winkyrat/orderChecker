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
        return new OrderExport.Row(itemId, "buy", price, remaining, observedAt, itemId, page, slot, false, null);
    }

    private static OrderExport.Row searchedRow(String itemId, int page, int slot, long price, long remaining,
                                               String observedAt) {
        return new OrderExport.Row(
                itemId, "buy", price, remaining, observedAt, itemId, page, slot, false, itemId);
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

    @Test
    void aSearchReplacesItsOwnRowsOnEveryPage() throws Exception {
        OrderExportStore.mergeIntoSnapshot(tempDir, export(42, "2026-11-16T08:25:31Z",
                searchedRow("minecraft:diamond", 42, 41, 11300, 1959, "2026-11-16T08:25:31Z"),
                searchedRow("minecraft:diamond", 42, 44, 11200, 1700, "2026-11-16T08:25:31Z")));

        // The item moved up one page: the old page 42 must not survive.
        Path snapshot = OrderExportStore.mergeIntoSnapshot(tempDir, export(41, "2026-11-16T09:10:00Z",
                searchedRow("minecraft:diamond", 41, 3, 12000, 900, "2026-11-16T09:10:00Z")));

        OrderExport merged = OrderExportStore.read(snapshot);
        assertNotNull(merged);
        assertEquals(1, merged.orders().size(), "one search, one observation, not a growing pile of pages");
        assertEquals(41, merged.orders().get(0).page());
        assertEquals(12000L, merged.orders().get(0).unitPrice());
    }

    @Test
    void aSearchLeavesOtherSearchesAlone() throws Exception {
        OrderExportStore.mergeIntoSnapshot(tempDir, export(1, "2026-11-16T09:00:00Z",
                searchedRow("minecraft:emerald", 1, 0, 5000, 10, "2026-11-16T09:00:00Z")));

        Path snapshot = OrderExportStore.mergeIntoSnapshot(tempDir, export(41, "2026-11-16T09:10:00Z",
                searchedRow("minecraft:diamond", 41, 3, 12000, 900, "2026-11-16T09:10:00Z")));

        OrderExport merged = OrderExportStore.read(snapshot);
        assertNotNull(merged);
        assertEquals(2, merged.orders().size());
        assertEquals(5000L, find(merged, "minecraft:emerald", 1).unitPrice());
        assertEquals(12000L, find(merged, "minecraft:diamond", 41).unitPrice());
    }

    @Test
    void onlyForKeepsTheSearchedItemAndTagsIt() throws Exception {
        OrderExport page = export(42, "2026-11-16T08:25:31Z",
                searchedRow("minecraft:diamond", 42, 41, 11300, 1959, "2026-11-16T08:25:31Z"),
                row("minecraft:diamond_sword", 42, 0, 12100, 5, "2026-11-16T08:25:31Z"),
                row("minecraft:diamond_helmet", 42, 2, 14000, 8, "2026-11-16T08:25:31Z"));

        OrderExport stored = page.onlyFor("minecraft:diamond");

        assertEquals(1, stored.orders().size(), "paging towards an item must not store the other items");
        OrderExport.Row kept = stored.orders().get(0);
        assertEquals("minecraft:diamond", kept.itemId());
        assertEquals("minecraft:diamond", kept.search());
        assertEquals(42, kept.page());
    }

    @Test
    void onlyForIsEmptyWhenTheItemIsNotOnThePage() {
        OrderExport page = export(1, "2026-11-16T08:25:31Z",
                row("minecraft:diamond_sword", 1, 0, 1600000, 5, "2026-11-16T08:25:31Z"));

        assertTrue(page.onlyFor("minecraft:diamond").isEmpty());
    }

    private static OrderExport.Row find(OrderExport export, String itemId, int page) {
        return export.orders().stream()
                .filter(row -> row.itemId().equals(itemId) && row.page() == page)
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing row for " + itemId + " page " + page));
    }
}
