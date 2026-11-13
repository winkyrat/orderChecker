package dev.donutprofit.orderscanner.dump;

import java.util.List;

/**
 * Immutable snapshot of the screen that was open when the dump key was pressed.
 *
 * <p>No Minecraft type appears here, so this model and its writer can be tested
 * without launching the game.
 */
public record ScreenDump(
        int dumpVersion,
        String modVersion,
        String capturedAt,
        String minecraftVersion,
        String screenClass,
        String screenTitle,
        String screenTitleDebug,
        String menuClass,
        int menuId,
        int screenWidth,
        int screenHeight,
        SlotDump carried,
        int slotCount,
        List<SlotDump> slots) {
}
