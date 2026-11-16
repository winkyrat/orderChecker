package dev.donutprofit.orderscanner.scan;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanScheduleTest {

    private static ScanConfig config(int minSeconds, int maxSeconds, int pauseChance,
                                     int pauseMin, int pauseMax) {
        return new ScanConfig(
                ScanConfig.CATALOGUE_ITEMS, minSeconds, maxSeconds, pauseChance,
                pauseMin, pauseMax, 40, 30, 3, 250, 350, 900, 4000).sanitized();
    }

    @Test
    void delaysStayInsideTheConfiguredWindow() {
        ScanSchedule schedule = new ScanSchedule(new Random(42));
        ScanConfig config = config(12, 35, 0, 120, 300);

        for (int i = 0; i < 1000; i++) {
            long delay = schedule.nextDelayMillis(config);
            assertTrue(delay >= 12_000L && delay <= 35_000L, "out of range: " + delay);
        }
    }

    @Test
    void longPausesHappenWhenTheChanceIsCertain() {
        ScanSchedule schedule = new ScanSchedule(new Random(7));
        ScanConfig config = config(12, 35, 100, 120, 300);

        for (int i = 0; i < 100; i++) {
            long delay = schedule.nextDelayMillis(config);
            assertTrue(delay >= 120_000L && delay <= 300_000L, "expected a long pause: " + delay);
        }
    }

    @Test
    void aZeroChanceNeverPauses() {
        ScanSchedule schedule = new ScanSchedule(new Random(11));
        ScanConfig config = config(12, 35, 0, 120, 300);

        for (int i = 0; i < 500; i++) {
            assertTrue(schedule.nextDelayMillis(config) <= 35_000L);
        }
    }

    @Test
    void pageDelayIsShortAndBounded() {
        ScanSchedule schedule = new ScanSchedule(new Random(3));
        ScanConfig config = config(12, 35, 0, 120, 300);

        for (int i = 0; i < 200; i++) {
            long delay = schedule.pageDelayMillis(config);
            assertTrue(delay >= 350L && delay <= 900L, "out of range: " + delay);
        }
    }

    @Test
    void mostGapsClusterNearTheShortEndOfTheWindow() {
        ScanSchedule schedule = new ScanSchedule(new Random(19));
        ScanConfig config = config(5, 12, 0, 60, 120);

        int draws = 10_000;
        int lowerHalf = 0;
        for (int i = 0; i < draws; i++) {
            long delay = schedule.nextDelayMillis(config);
            assertTrue(delay >= 5_000L && delay <= 12_000L, "out of range: " + delay);
            if (delay <= 8_000L) {
                lowerHalf++;
            }
        }

        assertTrue(lowerHalf > draws * 60 / 100,
                "a uniform draw would put ~43% in the lower half; expected a skew, got " + lowerHalf);
    }

    @Test
    void aSingleSecondWindowDoesNotThrow() {
        ScanSchedule schedule = new ScanSchedule(new Random(5));
        ScanConfig config = config(20, 20, 0, 120, 300);

        assertEquals(20_000L, schedule.nextDelayMillis(config));
        assertFalse(config.items().isEmpty());
    }
}
