package dev.donutprofit.orderscanner.scan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads {@code scan.json} and writes the default one when it is missing.
 *
 * <p>A missing or invalid field falls back to the default instead of becoming
 * zero, so a partially edited file can never turn into an accidental tight loop.
 */
public final class ScanConfigStore {
    public static final String FILE_NAME = "scan.json";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private ScanConfigStore() {
    }

    public static ScanConfig loadOrCreate(Path directory) throws IOException {
        Path file = directory.resolve(FILE_NAME);
        if (!Files.exists(file)) {
            ScanConfig defaults = ScanConfig.defaults();
            save(directory, defaults);
            return defaults;
        }
        return read(Files.readString(file, StandardCharsets.UTF_8)).sanitized();
    }

    /** @return the parsed config, or the defaults when the text is unusable. */
    public static ScanConfig read(String text) {
        ScanConfig defaults = ScanConfig.defaults();
        try {
            JsonElement parsed = JsonParser.parseString(text);
            if (!parsed.isJsonObject()) {
                return defaults;
            }
            JsonObject root = parsed.getAsJsonObject();
            return new ScanConfig(
                    items(root, defaults.items()),
                    intOr(root, "minDelaySeconds", defaults.minDelaySeconds()),
                    intOr(root, "maxDelaySeconds", defaults.maxDelaySeconds()),
                    intOr(root, "longPauseChancePercent", defaults.longPauseChancePercent()),
                    intOr(root, "longPauseMinSeconds", defaults.longPauseMinSeconds()),
                    intOr(root, "longPauseMaxSeconds", defaults.longPauseMaxSeconds()),
                    intOr(root, "maxItemsPerSession", defaults.maxItemsPerSession()),
                    intOr(root, "maxSessionMinutes", defaults.maxSessionMinutes()),
                    intOr(root, "maxPages", defaults.maxPages()),
                    intOr(root, "maxPagesPerSession", defaults.maxPagesPerSession()),
                    intOr(root, "pageDelayMinMillis", defaults.pageDelayMinMillis()),
                    intOr(root, "pageDelayMaxMillis", defaults.pageDelayMaxMillis()),
                    intOr(root, "screenTimeoutMillis", defaults.screenTimeoutMillis()));
        } catch (RuntimeException error) {
            return defaults;
        }
    }

    public static void save(Path directory, ScanConfig config) throws IOException {
        Files.createDirectories(directory);
        Files.writeString(
                directory.resolve(FILE_NAME),
                GSON.toJson(config.sanitized()),
                StandardCharsets.UTF_8);
    }

    private static List<String> items(JsonObject root, List<String> fallback) {
        JsonElement element = root.get("items");
        if (element == null || !element.isJsonArray()) {
            return fallback;
        }
        JsonArray array = element.getAsJsonArray();
        List<String> items = new ArrayList<>(array.size());
        for (JsonElement entry : array) {
            if (entry.isJsonPrimitive()) {
                items.add(entry.getAsString());
            }
        }
        return items;
    }

    private static int intOr(JsonObject root, String key, int fallback) {
        JsonElement element = root.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (RuntimeException error) {
            return fallback;
        }
    }
}
