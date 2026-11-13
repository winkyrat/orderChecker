package dev.donutprofit.orderscanner.client;

import dev.donutprofit.orderscanner.dump.ComponentDump;
import dev.donutprofit.orderscanner.dump.DumpWriter;
import dev.donutprofit.orderscanner.dump.ScreenDump;
import dev.donutprofit.orderscanner.dump.SlotDump;
import dev.donutprofit.orderscanner.parse.OrderExport;
import dev.donutprofit.orderscanner.parse.OrderExportStore;
import dev.donutprofit.orderscanner.parse.OrderMenu;
import dev.donutprofit.orderscanner.parse.OrderMenuParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only reconnaissance. It never clicks, never sends commands and never
 * modifies the menu: it copies what the client already received.
 */
public final class ScreenDumper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenDumper.MOD_ID);
    private static final String MOD_ID = OrderScannerClient.MOD_ID;

    private ScreenDumper() {
    }

    public static void dump(Minecraft client, Screen screen) {
        try {
            if (screen == null) {
                notifyPlayer(client, "No screen open. Nothing to dump.");
                return;
            }

            ScreenDump dump = capture(client, screen);
            Path directory = FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve(MOD_ID)
                    .resolve("dumps");
            Path file = DumpWriter.write(directory, dump.screenTitle(), dump);
            LOGGER.info("Wrote screen dump to {}", file);

            OrderMenu menu = OrderMenuParser.parse(dump);
            String extra = "";
            if (!menu.orders().isEmpty()) {
                OrderExport export = OrderExport.of(
                        menu, dump.modVersion(), dump.capturedAt(), dump.screenTitle());
                Path capture = OrderExportStore.writeCapture(directory, export);
                Path snapshot = OrderExportStore.mergeIntoSnapshot(directory, export);
                LOGGER.info("Exported {} orders to {} (snapshot {})",
                        menu.orders().size(), capture, snapshot);
                extra = " | " + menu.orders().size() + " orders -> " + snapshot.getFileName();
            }

            playFeedback(client, 1.6F);
            notifyPlayer(client, "Dump saved: " + file + extra);
        } catch (Exception error) {
            LOGGER.error("Could not write screen dump", error);
            playFeedback(client, 0.6F);
            notifyPlayer(client, "Dump failed: " + error.getClass().getSimpleName());
        }
    }

    /**
     * Chat is hidden while a container screen is open, so a sound is the only
     * immediate feedback the player gets. Pitch tells success from failure.
     */
    private static void playFeedback(Minecraft client, float pitch) {
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    private static ScreenDump capture(Minecraft client, Screen screen) {
        String modVersion = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");

        List<SlotDump> slots = new ArrayList<>();
        SlotDump carried = emptySlot();
        String menuClass = "";
        int menuId = -1;

        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            AbstractContainerMenu menu = containerScreen.getMenu();
            menuClass = menu.getClass().getName();
            menuId = menu.containerId;
            carried = dumpStack(-1, -1, -1, -1, "carried", menu.getCarried());
            for (Slot slot : menu.slots) {
                slots.add(dumpStack(
                        slot.index,
                        slot.getContainerSlot(),
                        slot.x,
                        slot.y,
                        slot.container.getClass().getName(),
                        slot.getItem()));
            }
        }

        return new ScreenDump(
                DumpWriter.DUMP_VERSION,
                modVersion,
                Instant.now().toString(),
                client.getLaunchedVersion(),
                screen.getClass().getName(),
                screen.getTitle().getString(),
                screen.getTitle().toString(),
                menuClass,
                menuId,
                screen.width,
                screen.height,
                carried,
                screen instanceof AbstractContainerScreen<?> ? slots.size() : 0,
                slots);
    }

    private static SlotDump dumpStack(
            int index,
            int containerSlot,
            int x,
            int y,
            String containerClass,
            ItemStack stack) {
        if (stack.isEmpty()) {
            return new SlotDump(index, containerSlot, x, y, containerClass, "", 0, "", false, List.of(), List.of());
        }

        List<String> loreLines = new ArrayList<>();
        var lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                loreLines.add(line.getString());
            }
        }

        List<ComponentDump> components = new ArrayList<>();
        for (TypedDataComponent<?> component : stack.getComponents()) {
            var id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type());
            components.add(new ComponentDump(
                    id == null ? "unknown" : id.toString(),
                    String.valueOf(component.value())));
        }

        return new SlotDump(
                index,
                containerSlot,
                x,
                y,
                containerClass,
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.getCount(),
                stack.getHoverName().getString(),
                stack.has(DataComponents.CUSTOM_NAME),
                List.copyOf(loreLines),
                List.copyOf(components));
    }

    private static SlotDump emptySlot() {
        return new SlotDump(-1, -1, -1, -1, "", "", 0, "", false, List.of(), List.of());
    }

    private static void notifyPlayer(Minecraft client, String message) {
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal("[DOS] " + message), false);
        }
    }
}
