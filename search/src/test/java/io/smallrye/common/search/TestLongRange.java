package io.smallrye.common.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public final class TestLongRange {
    public TestLongRange() {
    }

    private static final long MAX_LONG_UNSIGNED = 0xffffffff_ffffffffL; // == -1L

    @Test
    public void testEmptyRange() {
        assertEquals(1, BinarySearch.longRange().find(1, 1, i -> true));
        assertEquals(10, BinarySearch.longRange().find(10, 10, i -> true));
    }

    @Test
    public void testRange() {
        assertEquals(0, BinarySearch.longRange().find(0, 10, i -> i >= -100));
        assertEquals(0, BinarySearch.longRange().find(0, 10, i -> i >= 0));
        assertEquals(1, BinarySearch.longRange().find(0, 10, i -> i > 0));
        assertEquals(4, BinarySearch.longRange().find(0, 10, i -> i >= 4));
        assertEquals(5, BinarySearch.longRange().find(0, 10, i -> i > 4));
        assertEquals(5, BinarySearch.longRange().find(0, 10, i -> i >= 5));
        assertEquals(6, BinarySearch.longRange().find(0, 10, i -> i > 5));
        assertEquals(10, BinarySearch.longRange().find(0, 10, i -> i >= 10));
        assertEquals(10, BinarySearch.longRange().find(0, 10, i -> false));
        assertEquals(0, BinarySearch.longRange().find(0, 10, i -> true));
        // signed-specific ranges
        assertEquals(-10, BinarySearch.longRange().find(-10, 10, i -> true));
    }

    @Test
    public void testRangeUnsigned() {
        assertEquals(0, BinarySearch.longRange().unsigned().find(0, 10, i -> i >= 0));
        assertEquals(1, BinarySearch.longRange().unsigned().find(0, 10, i -> i > 0));
        assertEquals(4, BinarySearch.longRange().unsigned().find(0, 10, i -> i >= 4));
        assertEquals(5, BinarySearch.longRange().unsigned().find(0, 10, i -> i > 4));
        assertEquals(5, BinarySearch.longRange().unsigned().find(0, 10, i -> i >= 5));
        assertEquals(6, BinarySearch.longRange().unsigned().find(0, 10, i -> i > 5));
        assertEquals(10, BinarySearch.longRange().unsigned().find(0, 10, i -> i >= 10));
        assertEquals(10, BinarySearch.longRange().unsigned().find(0, 10, i -> false));
        assertEquals(0, BinarySearch.longRange().unsigned().find(0, 10, i -> true));
        // unsigned-specific ranges
        assertEquals(MAX_LONG_UNSIGNED, BinarySearch.longRange().find(0, MAX_LONG_UNSIGNED, i -> false));
        assertEquals(0, BinarySearch.longRange().unsigned().find(0, MAX_LONG_UNSIGNED, i -> true));
        assertEquals(0x8000_0000_0000_0000L, BinarySearch.longRange().unsigned().find(0, MAX_LONG_UNSIGNED,
                i -> Long.compareUnsigned(i, 0x8000_0000_0000_0000L) >= 0));
    }

    @Test
    public void testBigRange() {
        assertEquals(1025, BinarySearch.longRange().find(0, Long.MAX_VALUE, i -> i >= 1025));
        assertEquals(1026, BinarySearch.longRange().find(0, Long.MAX_VALUE, i -> i >= 1026));
        assertEquals(99210, BinarySearch.longRange().find(49203, 848392, i -> i >= 99210));
        assertEquals(Long.MAX_VALUE, BinarySearch.longRange().find(0, Long.MAX_VALUE, i -> false));
        assertEquals(0, BinarySearch.longRange().find(0, Long.MAX_VALUE, i -> true));
    }

    @Test
    public void testRevRange() {
        assertEquals(5, BinarySearch.longRange().find(9, -1, i -> i < 5));
        assertEquals(6, BinarySearch.longRange().find(9, -1, i -> i <= 5));
        assertEquals(0, BinarySearch.longRange().find(9, -1, i -> i < 0));
        assertEquals(1, BinarySearch.longRange().find(9, -1, i -> i <= 0));
    }
}
