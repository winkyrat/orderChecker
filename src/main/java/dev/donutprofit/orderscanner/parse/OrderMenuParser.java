package dev.donutprofit.orderscanner.parse;

import dev.donutprofit.orderscanner.dump.ComponentDump;
import dev.donutprofit.orderscanner.dump.ScreenDump;
import dev.donutprofit.orderscanner.dump.SlotDump;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads order rows from a captured orders screen.
 *
 * <p>Pure Java on purpose: no Minecraft types, so it runs in unit tests against
 * real dumps. The only inputs are the lore lines the client already received.
 *
 * <p>Layout observed on DonutSMP (2026-11-13): a six-row chest where the first
 * 45 slots are orders, the last row holds navigation and the bottom inventory
 * belongs to the player. Order slots are recognized by their lore, not by their
 * position, so a layout change does not silently produce garbage.
 *
 * <p>Only plain items are exported: a row whose item carries enchantments is
 * skipped, because enchantments move the price away from the plain item that
 * crafting profit compares against.
 */
public final class OrderMenuParser {
    /** {@code "$ 4K each"} — the price the order pays per unit. */
    private static final Pattern PRICE_EACH =
            Pattern.compile("^\\$\\s*(\\S+)\\s+each$", Pattern.CASE_INSENSITIVE);
    /** {@code "872/1k Delivered"} — delivered first, required second. */
    private static final Pattern DELIVERED =
            Pattern.compile("^(.+?)\\s*/\\s*(.+?)\\s+Delivered$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAGE =
            Pattern.compile("Page\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LORE_ONLY_VALUE = Pattern.compile("^~\\$.*");

    /** Data component that carries enchantments on gear and tools. */
    private static final String ENCHANTMENTS = "minecraft:enchantments";
    /** Data component that carries enchantments on enchanted books. */
    private static final String STORED_ENCHANTMENTS = "minecraft:stored_enchantments";
    /** How an empty enchantment component renders; anything else has entries. */
    private static final String NO_ENCHANTMENTS = "enchantments={}";

    private OrderMenuParser() {
    }

    /**
     * True when the title looks like the orders menu the scanner can read, e.g.
     * {@code Orders (Page 1)}. Used to decide whether an open screen is ours
     * before touching it.
     */
    public static boolean looksLikeOrdersMenu(String title) {
        if (title == null) {
            return false;
        }
        String trimmed = title.trim();
        return trimmed.equals("Orders") || trimmed.startsWith("Orders (");
    }

    public static OrderMenu parse(ScreenDump dump) {
        List<ParsedOrder> orders = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> skippedItemIds = new ArrayList<>();
        int page = 1;

        String title = dump.screenTitle() == null ? "" : dump.screenTitle();
        Matcher pageMatcher = PAGE.matcher(title);
        if (pageMatcher.find()) {
            page = Integer.parseInt(pageMatcher.group(1));
        } else {
            warnings.add("No page number in title '" + title + "'; assuming page 1.");
        }

        for (SlotDump slot : dump.slots()) {
            if (slot.itemId() == null || slot.itemId().isEmpty()) {
                continue;
            }
            String priceText = null;
            String deliveredText = null;
            for (String line : slot.lore()) {
                if (line == null || line.isBlank() || LORE_ONLY_VALUE.matcher(line).matches()) {
                    continue;
                }
                if (priceText == null) {
                    Matcher price = PRICE_EACH.matcher(line.trim());
                    if (price.matches()) {
                        priceText = price.group(1);
                        continue;
                    }
                }
                if (deliveredText == null) {
                    Matcher delivered = DELIVERED.matcher(line.trim());
                    if (delivered.matches()) {
                        deliveredText = line.trim();
                    }
                }
            }

            if (priceText == null || deliveredText == null) {
                continue;
            }

            Matcher delivered = DELIVERED.matcher(deliveredText);
            if (!delivered.matches()) {
                continue;
            }

            long unitPrice;
            long deliveredCount;
            long requiredCount;
            try {
                unitPrice = GameNumber.parseFloor(priceText);
                deliveredCount = GameNumber.parse(delivered.group(1));
                requiredCount = GameNumber.parse(delivered.group(2));
            } catch (IllegalArgumentException error) {
                warnings.add("Slot " + slot.index() + " (" + slot.itemId() + "): " + error.getMessage());
                continue;
            }

            if (requiredCount <= 0) {
                warnings.add("Slot " + slot.index() + " (" + slot.itemId()
                        + "): required quantity is " + requiredCount + "; skipped.");
                continue;
            }
            if (deliveredCount > requiredCount) {
                warnings.add("Slot " + slot.index() + " (" + slot.itemId()
                        + "): delivered " + deliveredCount + " exceeds required " + requiredCount + ".");
            }

            long remaining = requiredCount - deliveredCount;
            if (remaining <= 0) {
                warnings.add("Slot " + slot.index() + " (" + slot.itemId()
                        + "): already fully delivered; skipped.");
                continue;
            }
            if (unitPrice <= 0) {
                warnings.add("Slot " + slot.index() + " (" + slot.itemId()
                        + "): price is " + unitPrice + "; skipped.");
                continue;
            }

            // Enchanted gear is priced by its enchantments, not by the plain
            // item, so mixing it in would inflate crafting costs and profit.
            // Only plain items are priced for now; the row is remembered so the
            // autonomous scanner still knows the page held the item it asked for.
            if (hasEnchantments(slot)) {
                skippedItemIds.add(slot.itemId().toLowerCase(Locale.ROOT));
                continue;
            }

            orders.add(new ParsedOrder(
                    slot.index(),
                    slot.itemId().toLowerCase(Locale.ROOT),
                    slot.name(),
                    unitPrice,
                    deliveredCount,
                    requiredCount,
                    remaining));
        }

        if (!skippedItemIds.isEmpty()) {
            warnings.add("Skipped " + skippedItemIds.size()
                    + " enchanted order rows; only plain items are priced.");
        }

        if (orders.isEmpty()) {
            if (skippedItemIds.isEmpty()) {
                warnings.add("No order rows found. Is this the orders menu?");
            } else {
                warnings.add("No plain order rows found; every row was enchanted.");
            }
        }

        return new OrderMenu(
                page,
                List.copyOf(orders),
                List.copyOf(warnings),
                List.copyOf(skippedItemIds));
    }

    /**
     * True when the item carries at least one enchantment.
     *
     * <p>An unenchanted item still exposes {@code minecraft:enchantments}, but
     * its value renders as {@code ItemEnchantments{enchantments={}}}. Enchanted
     * books store theirs in {@code minecraft:stored_enchantments}. A missing
     * component means no enchantments.
     */
    private static boolean hasEnchantments(SlotDump slot) {
        for (ComponentDump component : slot.components()) {
            if (component == null || component.id() == null) {
                continue;
            }
            if (!ENCHANTMENTS.equals(component.id()) && !STORED_ENCHANTMENTS.equals(component.id())) {
                continue;
            }
            String value = component.value() == null ? "" : component.value();
            if (!value.contains(NO_ENCHANTMENTS)) {
                return true;
            }
        }
        return false;
    }
}
