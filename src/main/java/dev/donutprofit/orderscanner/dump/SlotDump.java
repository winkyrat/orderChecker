package dev.donutprofit.orderscanner.dump;

import java.util.List;

/**
 * One menu slot as seen by the client. Empty slots are preserved so the menu
 * layout can be reconstructed from a dump.
 */
public record SlotDump(
        int index,
        int containerSlot,
        int x,
        int y,
        String containerClass,
        String itemId,
        int count,
        String name,
        boolean hasCustomName,
        List<String> lore,
        List<ComponentDump> components) {
}
