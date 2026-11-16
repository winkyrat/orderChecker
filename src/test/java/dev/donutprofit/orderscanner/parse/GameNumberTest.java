package dev.donutprofit.orderscanner.parse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameNumberTest {

    @Test
    void parsesSuffixedValues() {
        assertEquals(4000L, GameNumber.parse("4K"));
        assertEquals(1900L, GameNumber.parse("1.9K"));
        assertEquals(6200L, GameNumber.parse("6.2k"));
        assertEquals(38100L, GameNumber.parse("38.1k"));
        assertEquals(128000L, GameNumber.parse("128k"));
        assertEquals(1000000L, GameNumber.parse("1m"));
        assertEquals(1000000L, GameNumber.parse("1M"));
        assertEquals(2000000000L, GameNumber.parse("2b"));
    }

    @Test
    void parsesPlainValues() {
        assertEquals(1000L, GameNumber.parse("1000"));
        assertEquals(1250L, GameNumber.parse("1,250"));
        assertEquals(0L, GameNumber.parse("0"));
        assertEquals(703L, GameNumber.parse("702.8"), "rounds half-up");
    }

    @Test
    void floorsPricesThatTheMenuPrintsWithDecimals() {
        assertEquals(511L, GameNumber.parseFloor("511.2"));
        assertEquals(702L, GameNumber.parseFloor("702.8"));
        assertEquals(98100L, GameNumber.parseFloor("98.1K"), "suffixes are exact");
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertEquals(4000L, GameNumber.parse(" 4K "));
    }

    @Test
    void rejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> GameNumber.parse(null));
        assertThrows(IllegalArgumentException.class, () -> GameNumber.parse(""));
        assertThrows(IllegalArgumentException.class, () -> GameNumber.parse("abc"));
        assertThrows(IllegalArgumentException.class, () -> GameNumber.parse("1x"));
        assertThrows(IllegalArgumentException.class, () -> GameNumber.parse("K"));
    }
}
