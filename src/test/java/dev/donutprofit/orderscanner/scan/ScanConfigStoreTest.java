package dev.donutprofit.orderscanner.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanConfigStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void writesTheDefaultFileWhenMissing() throws Exception {
        ScanConfig config = ScanConfigStore.loadOrCreate(tempDir);

        Path file = tempDir.resolve(ScanConfigStore.FILE_NAME);
        assertTrue(Files.exists(file), "the owner must be able to edit the settings");
        assertEquals(ScanConfig.CATALOGUE_ITEMS.size(), config.items().size());
        assertEquals(12, config.minDelaySeconds());
        assertEquals(35, config.maxDelaySeconds());
        assertEquals(200, config.maxPages());
        assertEquals(400, config.maxPagesPerSession());
        assertTrue(Files.readString(file).contains("\"items\""));
    }

    @Test
    void keepsDefaultsForFieldsTheFileDoesNotMention() {
        ScanConfig config = ScanConfigStore.read("{\"minDelaySeconds\": 20}");

        assertEquals(20, config.minDelaySeconds());
        assertEquals(35, config.maxDelaySeconds(), "untouched fields keep the default");
        assertEquals(ScanConfig.CATALOGUE_ITEMS.size(), config.items().size());
    }

    @Test
    void clampsValuesThatWouldBeDangerous() {
        ScanConfig config = ScanConfigStore.read(
                "{\"minDelaySeconds\": 0, \"maxDelaySeconds\": 1, \"maxPages\": 9999,"
                        + " \"maxSessionMinutes\": 100000, \"longPauseChancePercent\": 500}")
                .sanitized();

        assertEquals(3, config.minDelaySeconds(), "never below three seconds");
        assertEquals(3, config.maxDelaySeconds(), "max is raised to match the minimum");
        assertEquals(500, config.maxPages());
        assertEquals(240, config.maxSessionMinutes());
        assertEquals(100, config.longPauseChancePercent());
    }

    @Test
    void fallsBackToDefaultsOnBrokenJson() {
        ScanConfig config = ScanConfigStore.read("{ this is not json");

        assertEquals(12, config.minDelaySeconds());
        assertEquals(ScanConfig.CATALOGUE_ITEMS.size(), config.items().size());
    }

    @Test
    void dropsDuplicateAndBlankItems() {
        ScanConfig config = ScanConfigStore.read(
                "{\"items\": [\"minecraft:glass\", \" MINECRAFT:GLASS \", \"\", \"minecraft:diamond\"]}")
                .sanitized();

        assertEquals(List.of("minecraft:glass", "minecraft:diamond"), config.items());
    }

    @Test
    void anEmptyItemListFallsBackToTheCatalogue() {
        ScanConfig config = ScanConfigStore.read("{\"items\": []}").sanitized();

        assertFalse(config.items().isEmpty());
        assertEquals(ScanConfig.CATALOGUE_ITEMS, config.items());
    }
}
