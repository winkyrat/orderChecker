package dev.donutprofit.orderscanner.scan;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Settings for the autonomous scan, loaded from {@code scan.json}.
 *
 * <p>Defaults are the "medium" preset the owner picked: one search every
 * 12-35 seconds, an occasional long pause, and a hard stop after 40 items or
 * 30 minutes. Nothing here is a guarantee of undetectability; the caps exist to
 * limit exposure, not to hide it.
 *
 * <p>The order list is always read with the "Most Per Item" filter, sorted by
 * price. A cheap item can sit dozens of pages below the expensive gear that
 * shares its name, so {@code maxPages} is only a runaway guard: the real stop
 * is the first page that contains the item, which already holds its most
 * expensive orders. The session-wide page budget bounds the clicking.
 */
public record ScanConfig(
        List<String> items,
        int minDelaySeconds,
        int maxDelaySeconds,
        int longPauseChancePercent,
        int longPauseMinSeconds,
        int longPauseMaxSeconds,
        int maxItemsPerSession,
        int maxSessionMinutes,
        int maxPages,
        int maxPagesPerSession,
        int pageDelayMinMillis,
        int pageDelayMaxMillis,
        int screenTimeoutMillis) {

    /** The 60 items of DonutProfit's demo catalogue, as a starting list. */
    public static final List<String> CATALOGUE_ITEMS = List.of(
            "minecraft:diamond",
            "minecraft:emerald",
            "minecraft:iron_ingot",
            "minecraft:gold_ingot",
            "minecraft:netherite_ingot",
            "minecraft:coal",
            "minecraft:redstone",
            "minecraft:lapis_lazuli",
            "minecraft:quartz",
            "minecraft:amethyst_shard",
            "minecraft:copper_ingot",
            "minecraft:ancient_debris",
            "minecraft:raw_iron",
            "minecraft:raw_gold",
            "minecraft:diamond_block",
            "minecraft:iron_block",
            "minecraft:gold_block",
            "minecraft:emerald_block",
            "minecraft:netherite_block",
            "minecraft:coal_block",
            "minecraft:oak_log",
            "minecraft:oak_planks",
            "minecraft:spruce_log",
            "minecraft:stick",
            "minecraft:apple",
            "minecraft:wheat",
            "minecraft:sugar_cane",
            "minecraft:cocoa_beans",
            "minecraft:sugar",
            "minecraft:leather",
            "minecraft:feather",
            "minecraft:flint",
            "minecraft:string",
            "minecraft:egg",
            "minecraft:milk_bucket",
            "minecraft:rotten_flesh",
            "minecraft:bone",
            "minecraft:gunpowder",
            "minecraft:ender_pearl",
            "minecraft:blaze_rod",
            "minecraft:slime_ball",
            "minecraft:totem_of_undying",
            "minecraft:crafting_table",
            "minecraft:chest",
            "minecraft:furnace",
            "minecraft:torch",
            "minecraft:bread",
            "minecraft:cookie",
            "minecraft:arrow",
            "minecraft:paper",
            "minecraft:book",
            "minecraft:bookshelf",
            "minecraft:diamond_sword",
            "minecraft:diamond_pickaxe",
            "minecraft:iron_sword",
            "minecraft:iron_pickaxe",
            "minecraft:golden_apple",
            "minecraft:glass",
            "minecraft:sand",
            "minecraft:cobblestone");

    public static ScanConfig defaults() {
        return new ScanConfig(
                CATALOGUE_ITEMS,
                12,
                35,
                5,
                120,
                300,
                40,
                30,
                200,
                400,
                350,
                900,
                4000);
    }

    /**
     * Returns a copy that is safe to run: every number clamped to a sane range,
     * delays ordered, and items de-duplicated. A hand-edited file must never
     * turn into a 100 ms loop or a zero-item session.
     */
    public ScanConfig sanitized() {
        List<String> cleanItems = normalizeItems(items);
        if (cleanItems.isEmpty()) {
            cleanItems = CATALOGUE_ITEMS;
        }

        int min = clamp(minDelaySeconds, 3, 3600);
        int max = clamp(maxDelaySeconds, min, 3600);

        int pauseMin = clamp(longPauseMinSeconds, max, 7200);
        int pauseMax = clamp(longPauseMaxSeconds, pauseMin, 7200);

        return new ScanConfig(
                cleanItems,
                min,
                max,
                clamp(longPauseChancePercent, 0, 100),
                pauseMin,
                pauseMax,
                clamp(maxItemsPerSession, 1, 500),
                clamp(maxSessionMinutes, 1, 240),
                clamp(maxPages, 1, 500),
                clamp(maxPagesPerSession, 1, 2000),
                clamp(pageDelayMinMillis, 150, 10000),
                clamp(pageDelayMaxMillis, pageDelayMinMillis, 10000),
                clamp(screenTimeoutMillis, 1000, 15000));
    }

    private static List<String> normalizeItems(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String candidate : raw) {
            if (candidate == null) {
                continue;
            }
            String trimmed = candidate.trim().toLowerCase(Locale.ROOT);
            if (trimmed.isEmpty()) {
                continue;
            }
            unique.add(trimmed);
        }
        return List.copyOf(new ArrayList<>(unique));
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
