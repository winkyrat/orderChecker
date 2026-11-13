package dev.donutprofit.orderscanner.dump;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;

/** Serializes a {@link ScreenDump} to disk. Pure I/O, no Minecraft types. */
public final class DumpWriter {
    public static final int DUMP_VERSION = 1;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private DumpWriter() {
    }

    public static String toJson(ScreenDump dump) {
        return GSON.toJson(dump);
    }

    public static Path write(Path directory, String fileBase, ScreenDump dump) throws IOException {
        Files.createDirectories(directory);
        String base = sanitize(fileBase);
        String fileName = base + "-" + Instant.now().toEpochMilli() + ".json";
        Path target = directory.resolve(fileName);
        Files.writeString(target, toJson(dump), StandardCharsets.UTF_8);
        return target;
    }

    /** Keeps file names portable on Windows and readable in a directory list. */
    public static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "screen";
        }
        String cleaned = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (cleaned.isEmpty()) {
            return "screen";
        }
        return cleaned.length() > 48 ? cleaned.substring(0, 48) : cleaned;
    }
}
