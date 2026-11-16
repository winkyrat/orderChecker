package dev.donutprofit.orderscanner.scan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Remembers where the next session should start in the item list.
 *
 * <p>A session stops after {@code maxItemsPerSession} searches. With the 60-item
 * catalogue and a 40-search cap, always starting at index 0 would mean the last
 * 20 items are never scanned. Saving the next index makes each session continue
 * where the previous one stopped, so the whole list is covered over a couple of
 * sessions without lengthening any single one.
 *
 * <p>The file is one integer. Anything unreadable falls back to 0, which is the
 * safe default: start at the top of the list.
 */
public final class ScanCursor {
    public static final String FILE_NAME = "scan-cursor.txt";

    private ScanCursor() {
    }

    /** @return a valid index for a list of {@code itemCount}, or 0 when unknown. */
    public static int load(Path directory, int itemCount) {
        if (itemCount <= 0) {
            return 0;
        }
        Path file = directory.resolve(FILE_NAME);
        try {
            if (!Files.exists(file)) {
                return 0;
            }
            String text = Files.readString(file, StandardCharsets.UTF_8).trim();
            return Math.floorMod(Integer.parseInt(text), itemCount);
        } catch (IOException | NumberFormatException error) {
            return 0;
        }
    }

    /** Stores the index of the next item to scan. */
    public static void save(Path directory, int nextIndex) throws IOException {
        Files.createDirectories(directory);
        Files.writeString(
                directory.resolve(FILE_NAME),
                Integer.toString(Math.max(0, nextIndex)),
                StandardCharsets.UTF_8);
    }
}
