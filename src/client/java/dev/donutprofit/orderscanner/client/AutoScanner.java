package dev.donutprofit.orderscanner.client;

import dev.donutprofit.orderscanner.dump.ScreenDump;
import dev.donutprofit.orderscanner.parse.OrderMenuParser;
import dev.donutprofit.orderscanner.scan.ScanConfig;
import dev.donutprofit.orderscanner.scan.ScanConfigStore;
import dev.donutprofit.orderscanner.scan.ScanCursor;
import dev.donutprofit.orderscanner.scan.ScanSchedule;
import dev.donutprofit.orderscanner.scan.ScanSession;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;

/**
 * Autonomous scan, phase 3.
 *
 * <p>It sends {@code /order <item>} as the player would, waits for the menu,
 * reads it, clicks "Next page" until the requested item appears, closes it and
 * waits a jittered delay before the next item. It stops at the first page that
 * contains the item, because the list is sorted by price and that page already
 * holds its most expensive orders. It does not fake packets, does not move the
 * player, does not hide itself and does not claim to be undetectable: the only
 * thing it avoids is a perfectly regular cadence.
 *
 * <p>Safety rules: never acts while another menu is open, stops when the player
 * starts playing (movement, attack, use), stops after three consecutive
 * failures, and stops at the item and time caps from {@code scan.json}.
 */
public final class AutoScanner {
    private enum Phase { OFF, WAIT, AWAIT_SCREEN, SETTLE_PAGE, CLOSE_WAIT }

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderScannerClient.MOD_ID);
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    private final ScanSchedule schedule = new ScanSchedule(new Random());

    private ScanConfig config = ScanConfig.defaults();
    private ScanSession session;
    private Phase phase = Phase.OFF;
    private long deadline;
    private int itemIndex;
    private int page;
    private int failures;
    private int totalRows;
    private String currentItem = "";
    private String lastFingerprint = "";
    private boolean targetSeen;
    private int pagesThisItem;
    private boolean itemInProgress;
    private boolean itemTruncated;
    private String truncationReason = "";

    public boolean isRunning() {
        return phase != Phase.OFF;
    }

    public void toggle(Minecraft client) {
        if (isRunning()) {
            stop(client, "stopped by key");
        } else {
            start(client);
        }
    }

    private Path configDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve(OrderScannerClient.MOD_ID);
    }

    private void start(Minecraft client) {
        if (client.player == null || client.level == null) {
            ScreenDumper.notifyPlayer(client, "Join a world before starting the scan.");
            return;
        }
        if (client.screen != null) {
            ScreenDumper.notifyPlayer(client, "Close the open menu before starting the scan.");
            return;
        }

        try {
            config = ScanConfigStore.loadOrCreate(configDirectory());
        } catch (Exception error) {
            LOGGER.warn("Could not read scan.json; using defaults", error);
            config = ScanConfig.defaults();
        }

        session = new ScanSession(config, System.currentTimeMillis());
        itemIndex = ScanCursor.load(configDirectory(), config.items().size());
        page = 1;
        failures = 0;
        totalRows = 0;
        currentItem = "";
        lastFingerprint = "";
        targetSeen = false;
        pagesThisItem = 0;
        itemInProgress = false;
        itemTruncated = false;
        truncationReason = "";
        phase = Phase.WAIT;
        deadline = 0L;

        ScreenDumper.playFeedback(client, 1.3F);
        ScreenDumper.notifyPlayer(client, "Scan on: " + config.items().size()
                + " items, cap " + config.maxItemsPerSession() + " searches / "
                + config.maxSessionMinutes() + " min. F9 stops.");
        LOGGER.info("Scan started: {} items, {}-{} s, cap {} searches / {} min, starting at {}",
                config.items().size(), config.minDelaySeconds(), config.maxDelaySeconds(),
                config.maxItemsPerSession(), config.maxSessionMinutes(),
                config.items().get(itemIndex));
    }

    public void stop(Minecraft client, String reason) {
        if (!isRunning()) {
            return;
        }
        phase = Phase.OFF;
        closeScreen(client);
        String summary = session == null ? "" : " (" + session.items() + " searches, " + totalRows + " rows)";
        String cut = "";
        if (itemInProgress) {
            cut = "; " + shortName(currentItem) + " was cut after " + pagesThisItem
                    + " pages, its price band may be incomplete";
        }
        ScreenDumper.notifyPlayer(client, "Scan off: " + reason + summary + cut);
        ScreenDumper.playFeedback(client, 0.7F);
        LOGGER.info("Scan stopped: {}{}{}", reason, summary, cut);
        itemInProgress = false;
    }

    public void tick(Minecraft client) {
        if (!isRunning()) {
            return;
        }
        if (client.player == null || client.level == null) {
            stop(client, "left the world");
            return;
        }

        long now = System.currentTimeMillis();
        // While the item is closing, let finishItem() run first: it records the
        // search and reports the real stop reason. Stopping here would drop that
        // bookkeeping and make a completed item look like it never happened.
        if (phase != Phase.CLOSE_WAIT && session.shouldStop(now)) {
            stop(client, session.stopReason(now));
            return;
        }
        if (!client.isWindowActive()) {
            stop(client, "the window lost focus");
            return;
        }
        if (userIsPlaying(client)) {
            stop(client, "you started playing");
            return;
        }

        switch (phase) {
            case WAIT -> {
                if (now < deadline || client.screen != null) {
                    return;
                }
                sendNextCommand(client, now);
            }
            case AWAIT_SCREEN -> awaitScreen(client, now);
            case SETTLE_PAGE -> {
                if (now < deadline) {
                    return;
                }
                if (client.screen instanceof AbstractContainerScreen<?> container
                        && isOrdersScreen(container)) {
                    capturePage(client, container, now);
                } else {
                    fail(client, "the next page did not open");
                }
            }
            case CLOSE_WAIT -> {
                if (now >= deadline) {
                    finishItem(client);
                }
            }
            default -> {
                // OFF: nothing to do.
            }
        }
    }

    private void sendNextCommand(Minecraft client, long now) {
        currentItem = config.items().get(itemIndex % config.items().size());
        page = 1;
        lastFingerprint = "";
        targetSeen = false;
        pagesThisItem = 0;
        itemInProgress = true;
        itemTruncated = false;
        truncationReason = "";

        String name = shortName(currentItem);
        client.player.connection.sendCommand("order " + name);
        LOGGER.info("Scan: /order {} (search {}/{})",
                name, session.items() + 1, config.maxItemsPerSession());

        phase = Phase.AWAIT_SCREEN;
        deadline = now + config.screenTimeoutMillis();
    }

    private void awaitScreen(Minecraft client, long now) {
        Screen screen = client.screen;
        if (screen instanceof AbstractContainerScreen<?> container && isOrdersScreen(container)) {
            capturePage(client, container, now);
            return;
        }
        if (now < deadline) {
            return;
        }
        if (screen != null) {
            stop(client, "another menu is open");
            return;
        }
        fail(client, "the menu did not open");
    }

    private void capturePage(Minecraft client, AbstractContainerScreen<?> container, long now) {
        ScreenDump dump = ScreenDumper.capture(client, container);
        ScreenDumper.ExportResult result;
        try {
            result = ScreenDumper.exportAndStore(dump, currentItem);
        } catch (Exception error) {
            LOGGER.error("Scan: could not export page {} of {}", page, currentItem, error);
            fail(client, "could not write the export");
            return;
        }

        failures = 0;
        totalRows += result.rows();
        session.recordPage();
        pagesThisItem++;

        // Log what the menu actually contained, not the item that was searched:
        // a generic search returns every variant whose id or name matches.
        boolean pageHasTarget = result.itemIds().contains(currentItem);
        LOGGER.info("Scan: /order {} page {} -> {} rows {}",
                shortName(currentItem), page, result.rows(), result.itemIds());
        if (pageHasTarget) {
            targetSeen = true;
        }

        AbstractContainerMenu menu = container.getMenu();
        String fingerprint = fingerprint(menu);
        boolean samePage = fingerprint.equals(lastFingerprint);
        lastFingerprint = fingerprint;

        int nextSlot = nextPageSlot(menu);
        boolean hasNext = nextSlot >= 0 && !samePage;
        boolean pageCapHit = hasNext && page >= config.maxPages();
        boolean budgetHit = hasNext && session.pagesExhausted();
        boolean canPage = hasNext && !pageCapHit && !budgetHit;

        // The list is sorted by price, highest first, so the first page that
        // contains the requested item already holds its most expensive orders.
        // One page is enough: stop here instead of walking the whole band.
        if (canPage && !pageHasTarget) {
            client.gameMode.handleInventoryMouseClick(
                    menu.containerId, nextSlot, 0, ClickType.PICKUP, client.player);
            page++;
            phase = Phase.SETTLE_PAGE;
            deadline = now + schedule.pageDelayMillis(config);
            return;
        }

        // The item never showed up and a cap stopped the search: say so instead
        // of pretending the list simply ended.
        if (!pageHasTarget) {
            if (pageCapHit) {
                itemTruncated = true;
                truncationReason = "the " + config.maxPages() + " page per-item cap";
            } else if (budgetHit) {
                itemTruncated = true;
                truncationReason = "the " + session.pages() + " page session budget";
            }
            if (itemTruncated) {
                LOGGER.warn("Scan: /order {} cut after {} pages by {} before finding {}",
                        shortName(currentItem), pagesThisItem, truncationReason, currentItem);
            }
        }

        closeScreen(client);
        phase = Phase.CLOSE_WAIT;
        deadline = now + 300L;
    }

    private void finishItem(Minecraft client) {
        session.recordItem();
        itemIndex++;
        int pagesRead = pagesThisItem;
        page = 1;
        itemInProgress = false;
        persistCursor();

        // Report before the stop check: a budget that ran out during this item
        // still has to say whether the item was found and how far it got.
        String note = reportOutcome(pagesRead);

        long now = System.currentTimeMillis();
        if (session.shouldStop(now)) {
            stop(client, session.stopReason(now));
            return;
        }

        phase = Phase.WAIT;
        deadline = now + schedule.nextDelayMillis(config);

        if (client.player != null) {
            long seconds = Math.max(1L, (deadline - now) / 1000L);
            client.player.displayClientMessage(Component.literal(
                    "[DOS] " + session.items() + "/" + config.maxItemsPerSession()
                            + " " + shortName(currentItem) + note + " · next in " + seconds + "s"), true);
        }
    }

    /** Logs the real outcome of one search and returns the short action-bar note. */
    private String reportOutcome(int pagesRead) {
        if (targetSeen) {
            LOGGER.info("Scan: /order {} done -> {} pages, {} found",
                    shortName(currentItem), pagesRead, currentItem);
            return "";
        }
        if (itemTruncated) {
            LOGGER.warn("Scan: /order {} did not find {} in {} pages; {} stopped it",
                    shortName(currentItem), currentItem, pagesRead, truncationReason);
            return " (" + shortName(currentItem) + " cut at " + pagesRead + " pages)";
        }
        LOGGER.warn("Scan: /order {} never returned {} within {} pages",
                shortName(currentItem), currentItem, pagesRead);
        return " (no " + shortName(currentItem) + " in " + pagesRead + " pages)";
    }

    /** Saves where the next session should continue, so the whole list gets covered. */
    private void persistCursor() {
        int size = config.items().size();
        if (size == 0) {
            return;
        }
        try {
            ScanCursor.save(configDirectory(), itemIndex % size);
        } catch (IOException error) {
            LOGGER.warn("Scan: could not save the item cursor", error);
        }
    }

    private void fail(Minecraft client, String reason) {
        failures++;
        LOGGER.warn("Scan: {} ({})", reason, currentItem);
        closeScreen(client);
        itemIndex++;
        page = 1;
        itemInProgress = false;

        if (failures >= MAX_CONSECUTIVE_FAILURES) {
            stop(client, reason + " three times in a row");
            return;
        }

        phase = Phase.WAIT;
        deadline = System.currentTimeMillis() + schedule.nextDelayMillis(config);
        ScreenDumper.notifyPlayer(client, "Skipped " + shortName(currentItem) + ": " + reason);
    }

    private static void closeScreen(Minecraft client) {
        if (client.player != null) {
            client.player.closeContainer();
        }
    }

    private static boolean isOrdersScreen(AbstractContainerScreen<?> container) {
        return OrderMenuParser.looksLikeOrdersMenu(container.getTitle().getString());
    }

    private static int nextPageSlot(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains("next page")) {
                return slot.index;
            }
            var lore = stack.get(DataComponents.LORE);
            if (lore != null) {
                for (Component line : lore.lines()) {
                    if (line.getString().toLowerCase(Locale.ROOT).contains("next page")) {
                        return slot.index;
                    }
                }
            }
        }
        return -1;
    }

    /** Cheap content hash used to notice that a page click did nothing. */
    private static String fingerprint(AbstractContainerMenu menu) {
        StringBuilder builder = new StringBuilder(menu.slots.size() * 8);
        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getItem();
            builder.append(slot.index).append(':');
            if (stack.isEmpty()) {
                builder.append('-');
            } else {
                builder.append(stack.getItem()).append('x').append(stack.getCount());
                builder.append('|').append(stack.getHoverName().getString());
            }
            builder.append(';');
        }
        return builder.toString();
    }

    private static boolean userIsPlaying(Minecraft client) {
        return client.options.keyUp.isDown()
                || client.options.keyDown.isDown()
                || client.options.keyLeft.isDown()
                || client.options.keyRight.isDown()
                || client.options.keyJump.isDown()
                || client.options.keyAttack.isDown()
                || client.options.keyUse.isDown();
    }

    private static String shortName(String itemId) {
        return itemId.startsWith("minecraft:") ? itemId.substring("minecraft:".length()) : itemId;
    }
}
