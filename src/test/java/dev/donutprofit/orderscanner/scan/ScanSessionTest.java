package dev.donutprofit.orderscanner.scan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanSessionTest {

    private static ScanConfig config(int maxItems, int maxMinutes) {
        return new ScanConfig(
                ScanConfig.CATALOGUE_ITEMS, 12, 35, 5, 120, 300,
                maxItems, maxMinutes, 3, 350, 900, 4000).sanitized();
    }

    @Test
    void stopsAfterTheItemCap() {
        ScanSession session = new ScanSession(config(2, 30), 0L);

        session.recordItem();
        assertFalse(session.shouldStop(1_000L));
        session.recordItem();

        assertTrue(session.shouldStop(1_000L));
        assertEquals("reached the 2 search cap", session.stopReason(1_000L));
    }

    @Test
    void stopsAfterTheTimeCap() {
        ScanSession session = new ScanSession(config(40, 1), 0L);

        assertFalse(session.shouldStop(59_999L));
        assertTrue(session.shouldStop(60_000L));
        assertEquals("reached the 1 minute cap", session.stopReason(60_000L));
    }

    @Test
    void aClockThatGoesBackwardsDoesNotExtendTheSession() {
        ScanSession session = new ScanSession(config(40, 30), 10_000L);

        assertEquals(0L, session.elapsedMillis(5_000L));
        assertNull(session.stopReason(5_000L));
    }
}
