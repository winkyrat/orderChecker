package dev.donutprofit.orderscanner.parse;

import com.google.gson.Gson;
import dev.donutprofit.orderscanner.dump.ScreenDump;
import dev.donutprofit.orderscanner.dump.SlotDump;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderMenuParserTest {

    static ScreenDump fixture(String name) throws Exception {
        try (InputStreamReader reader = new InputStreamReader(
                OrderMenuParserTest.class.getResourceAsStream("/fixtures/" + name),
                StandardCharsets.UTF_8)) {
            ScreenDump dump = new Gson().fromJson(reader, ScreenDump.class);
            assertNotNull(dump, "fixture must deserialize: " + name);
            return dump;
        }
    }

    static ScreenDump glass() throws Exception {
        return fixture("orders-glass-page1.json");
    }

    @Test
    void readsEveryOrderRowFromTheRealDump() throws Exception {
        OrderMenu menu = OrderMenuParser.parse(glass());

        assertEquals(1, menu.page());
        assertEquals(45, menu.orders().size(), "45 order slots were captured");
    }

    @Test
    void convertsPriceAndDeliveredLore() throws Exception {
        OrderMenu menu = OrderMenuParser.parse(glass());
        ParsedOrder first = menu.orders().get(0);

        assertEquals(0, first.slot());
        assertEquals("minecraft:green_stained_glass", first.itemId());
        assertEquals("Green Stained Glass", first.displayName());
        assertEquals(4000L, first.unitPrice(), "$ 4K each");
        assertEquals(872L, first.delivered(), "872/1k Delivered");
        assertEquals(1000L, first.required(), "872/1k Delivered");
        assertEquals(128L, first.quantityRemaining());
        assertEquals("buy", first.side());
    }

    @Test
    void ignoresNavigationAndPlayerInventorySlots() throws Exception {
        OrderMenu menu = OrderMenuParser.parse(glass());

        assertTrue(menu.orders().stream().noneMatch(order -> order.itemId().equals("minecraft:hopper")),
                "the filter button and the hopper stack in the inventory must not become orders");
        assertTrue(menu.orders().stream().noneMatch(order -> order.itemId().equals("minecraft:dropper")),
                "player inventory stacks carry '~$' totals, not per-unit prices");
        assertTrue(menu.orders().stream().noneMatch(order -> order.itemId().equals("minecraft:arrow")),
                "the next page button is not an order");
    }

    @Test
    void everyRowIsUsableByTheContract() throws Exception {
        for (String name : List.of(
                "orders-glass-page1.json",
                "orders-diamond-block-page1.json",
                "orders-obsidian-page1.json")) {
            OrderMenu menu = OrderMenuParser.parse(fixture(name));

            for (ParsedOrder order : menu.orders()) {
                assertTrue(order.unitPrice() > 0, "unitPrice must be positive in " + name + ": " + order);
                assertTrue(order.quantityRemaining() > 0,
                        "quantityRemaining must be positive in " + name + ": " + order);
                assertFalse(order.itemId().isBlank(), "itemId must be present");
                assertEquals("buy", order.side());
            }
        }
    }

    @Test
    void keepsTheLastRowOfThePage() throws Exception {
        OrderMenu menu = OrderMenuParser.parse(glass());
        ParsedOrder last = menu.orders().get(menu.orders().size() - 1);

        assertEquals(44, last.slot());
        assertEquals("minecraft:glass", last.itemId());
        assertEquals(1000L, last.unitPrice());
        assertEquals(3L, last.delivered());
        assertEquals(1000L, last.required());
        assertEquals(997L, last.quantityRemaining());
    }

    @Test
    void readsAPageFilledWithOneItem() throws Exception {
        OrderMenu menu = OrderMenuParser.parse(fixture("orders-diamond-block-page1.json"));

        assertEquals(45, menu.orders().size());
        assertTrue(menu.orders().stream()
                        .allMatch(order -> order.itemId().equals("minecraft:diamond_block")),
                "a search result can fill the whole page with the same item");
        assertEquals(98100L, menu.orders().get(0).unitPrice(), "$ 98.1K each");
        assertEquals(80000L, menu.orders().get(44).unitPrice(), "$ 80K each");
    }

    @Test
    void floorsDecimalPrices() throws Exception {
        OrderMenu menu = OrderMenuParser.parse(fixture("orders-obsidian-page1.json"));

        ParsedOrder decimal = menu.orders().stream()
                .filter(order -> order.slot() == 43)
                .findFirst()
                .orElseThrow();
        assertEquals(511L, decimal.unitPrice(), "$ 511.2 each is stored as 511 whole units");
    }

    @Test
    void readsTwoItemVariantsFromOneSearch() throws Exception {
        OrderMenu menu = OrderMenuParser.parse(fixture("orders-obsidian-page1.json"));

        Set<String> items = menu.orders().stream()
                .map(ParsedOrder::itemId)
                .collect(Collectors.toSet());
        assertEquals(Set.of("minecraft:obsidian", "minecraft:crying_obsidian"), items);
    }

    @Test
    void skipsFullyDeliveredOrders() {
        SlotDump slot = new SlotDump(
                0, 0, 0, 0, "container", "minecraft:stone", 1, "Stone", false,
                List.of("", "$ 1K each", "5/5 Delivered", "", "Click to deliver items"),
                List.of());
        ScreenDump dump = new ScreenDump(
                1, "0.2.0", "2026-11-13T00:00:00Z", "1.21.11", "screen", "Orders (Page 1)", "",
                "menu", 0, 640, 480,
                new SlotDump(-1, -1, -1, -1, "", "", 0, "", false, List.of(), List.of()),
                1, List.of(slot));

        OrderMenu menu = OrderMenuParser.parse(dump);

        assertTrue(menu.orders().isEmpty());
        assertTrue(menu.warnings().stream().anyMatch(w -> w.contains("fully delivered")));
    }

    @Test
    void exportsContractCompatibleJson() throws Exception {
        OrderMenu menu = OrderMenuParser.parse(glass());
        OrderExport export = OrderExport.of(
                menu, "0.2.0", "2026-11-13T04:24:17Z", "Orders (Page 1)");

        String json = export.toJson();

        assertTrue(json.contains("\"schemaVersion\": 1"));
        assertTrue(json.contains("\"source\": \"manual\""));
        assertTrue(json.contains("\"itemId\": \"minecraft:green_stained_glass\""));
        assertTrue(json.contains("\"unitPrice\": 4000"));
        assertTrue(json.contains("\"quantityRemaining\": 128"));
        assertTrue(json.contains("\"side\": \"buy\""));
        assertTrue(json.contains("\"verified\": false"));
        assertTrue(json.contains("\"observedAt\": \"2026-11-13T04:24:17Z\""));
        assertTrue(json.contains("\"page\": 1"), "rows carry the page they came from");
        assertFalse(json.contains("~$"), "inventory totals must not leak into the export");
    }
}
