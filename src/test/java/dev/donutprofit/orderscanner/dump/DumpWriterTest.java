package dev.donutprofit.orderscanner.dump;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DumpWriterTest {
    @TempDir
    Path tempDir;

    @Test
    void writesACompatibleJsonDocument() throws Exception {
        SlotDump slot = new SlotDump(
                10, 1, 80, 40, "net.minecraft.world.SimpleContainer",
                "minecraft:diamond", 3, "Diamond", true,
                List.of("Buy order: 64x", "$1,250 each"),
                List.of(new ComponentDump("minecraft:custom_name", "Diamond")));

        ScreenDump dump = new ScreenDump(
                DumpWriter.DUMP_VERSION, "0.1.0", "2026-11-13T12:00:00Z", "1.21.11",
                "net.minecraft.client.gui.screens.inventory.ContainerScreen",
                "Orders", "Orders", "net.minecraft.world.inventory.ChestMenu", 0,
                640, 480, new SlotDump(-1, -1, -1, -1, "", "", 0, "", false, List.of(), List.of()),
                1, List.of(slot));

        Path file = DumpWriter.write(tempDir, "Orders / Diamond", dump);
        assertTrue(Files.exists(file));
        assertTrue(file.getFileName().toString().startsWith("orders_diamond-"));

        String json = Files.readString(file);
        assertTrue(json.contains("\"itemId\": \"minecraft:diamond\""));
        assertTrue(json.contains("\"lore\""));
        assertTrue(json.contains("$1,250 each"));
        assertEquals(DumpWriter.DUMP_VERSION, 1);
    }

    @Test
    void sanitizesUnsafeFileNames() {
        assertEquals("screen", DumpWriter.sanitize("  "));
        assertEquals("a_b_c", DumpWriter.sanitize("a/b:c"));
        assertEquals("x".repeat(48), DumpWriter.sanitize("x".repeat(80)));
    }
}
