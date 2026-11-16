package dev.donutprofit.orderscanner.scan;

import java.util.Random;

/**
 * Decides how long to wait between actions.
 *
 * <p>Delays are drawn uniformly inside the configured window and, with the
 * configured probability, replaced by a long pause. That produces an uneven
 * cadence instead of a metronome. It is not an anti-detection measure and it is
 * not sold as one: it only avoids a perfectly regular pattern, which is the
 * cheapest thing a server can flag.
 */
public final class ScanSchedule {
    private final Random random;

    public ScanSchedule(Random random) {
        this.random = random;
    }

    /** Wait before the next item search. */
    public long nextDelayMillis(ScanConfig config) {
        if (config.longPauseChancePercent() > 0
                && random.nextInt(100) < config.longPauseChancePercent()) {
            return between(config.longPauseMinSeconds(), config.longPauseMaxSeconds()) * 1000L;
        }
        return between(config.minDelaySeconds(), config.maxDelaySeconds()) * 1000L;
    }

    /** Wait after clicking "Next page", before reading the new contents. */
    public long pageDelayMillis(ScanConfig config) {
        return between(config.pageDelayMinMillis(), config.pageDelayMaxMillis());
    }

    private int between(int minimum, int maximum) {
        if (maximum <= minimum) {
            return minimum;
        }
        return minimum + random.nextInt(maximum - minimum + 1);
    }
}
