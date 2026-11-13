package dev.donutprofit.orderscanner.parse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the compact numbers DonutSMP prints in item lore, for example
 * {@code 4K}, {@code 1.9K}, {@code 38.1k} or {@code 1m}.
 *
 * <p>The server abbreviates with {@code K} (thousand), {@code M} (million) and
 * probably {@code B} (billion). Decimals are allowed before the suffix. When the
 * result is not a whole number it is rounded half-up, which is the only case
 * where a value is not exact.
 */
public final class GameNumber {
    private static final Pattern NUMBER =
            Pattern.compile("^([0-9][0-9,]*(?:\\.[0-9]+)?)([kKmMbB]?)$");
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000L);

    private GameNumber() {
    }

    /**
     * @throws IllegalArgumentException when the text is not a compact number or
     *                                  does not fit in a {@code long}.
     */
    public static long parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Number is null");
        }
        String text = raw.trim().replace(" ", "");
        Matcher matcher = NUMBER.matcher(text);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a compact number: '" + raw + "'");
        }

        BigDecimal value = new BigDecimal(matcher.group(1).replace(",", ""));
        switch (matcher.group(2).toLowerCase(java.util.Locale.ROOT)) {
            case "k" -> value = value.multiply(THOUSAND);
            case "m" -> value = value.multiply(THOUSAND).multiply(THOUSAND);
            case "b" -> value = value.multiply(THOUSAND).multiply(THOUSAND).multiply(THOUSAND);
            default -> {
                // No suffix: the value is already in currency units.
            }
        }

        try {
            return value.setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("Number out of range: '" + raw + "'", error);
        }
    }
}
