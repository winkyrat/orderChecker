package dev.donutprofit.orderscanner.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScanCursorTest {

    @TempDir
    Path tempDir;

    @Test
    void startsAtZeroWhenNoCursorExists() {
        assertEquals(0, ScanCursor.load(tempDir, 60));
    }

    @Test
    void roundTripsTheNextIndex() throws Exception {
        ScanCursor.save(tempDir, 42);

        assertEquals(42, ScanCursor.load(tempDir, 60));
    }

    @Test
    void wrapsWhenTheItemListShrank() throws Exception {
        ScanCursor.save(tempDir, 42);

        assertEquals(2, ScanCursor.load(tempDir, 40), "42 % 40 keeps the scan inside the list");
    }

    @Test
    void ignoresABrokenCursorFile() throws Exception {
        Files.writeString(tempDir.resolve(ScanCursor.FILE_NAME), "not a number");

        assertEquals(0, ScanCursor.load(tempDir, 60));
    }

    @Test
    void anEmptyListCannotProduceAnIndex() {
        assertEquals(0, ScanCursor.load(tempDir, 0));
    }
}
