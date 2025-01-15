package io.smallrye.common.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public final class TestIntRange {
    public TestIntRange() {
    }

    private static final int MAX_INT_UNSIGNED = 0xffffffff; // == -1

    @Test
    public void testEmptyRange() {
        assertEquals(1, BinarySearch.intRange().find(1, 1, i -> true));
        assertEquals(10, BinarySearch.intRange().find(10, 10, i -> true));
    }

    @Test
    public void testRange() {
        assertEquals(0, BinarySearch.intRange().find(0, 10, i -> i >= -100));
        assertEquals(0, BinarySearch.intRange().find(0, 10, i -> i >= 0));
        assertEquals(1, BinarySearch.intRange().find(0, 10, i -> i > 0));
        assertEquals(4, BinarySearch.intRange().find(0, 10, i -> i >= 4));
        assertEquals(5, BinarySearch.intRange().find(0, 10, i -> i > 4));
        assertEquals(5, BinarySearch.intRange().find(0, 10, i -> i >= 5));
        assertEquals(6, BinarySearch.intRange().find(0, 10, i -> i > 5));
        assertEquals(10, BinarySearch.intRange().find(0, 10, i -> i >= 10));
        assertEquals(10, BinarySearch.intRange().find(0, 10, i -> false));
        assertEquals(0, BinarySearch.intRange().find(0, 10, i -> true));
        // signed-specific ranges
        assertEquals(-10, BinarySearch.intRange().find(-10, 10, i -> true));
    }

    @Test
    public void testRangeUnsigned() {
        assertEquals(0, BinarySearch.intRange().unsigned().find(0, 10, i -> i >= 0));
        assertEquals(1, BinarySearch.intRange().unsigned().find(0, 10, i -> i > 0));
        assertEquals(4, BinarySearch.intRange().unsigned().find(0, 10, i -> i >= 4));
        assertEquals(5, BinarySearch.intRange().unsigned().find(0, 10, i -> i > 4));
        assertEquals(5, BinarySearch.intRange().unsigned().find(0, 10, i -> i >= 5));
        assertEquals(6, BinarySearch.intRange().unsigned().find(0, 10, i -> i > 5));
        assertEquals(10, BinarySearch.intRange().unsigned().find(0, 10, i -> i >= 10));
        assertEquals(10, BinarySearch.intRange().unsigned().find(0, 10, i -> false));
        assertEquals(0, BinarySearch.intRange().unsigned().find(0, 10, i -> true));
        // unsigned-specific ranges
        assertEquals(MAX_INT_UNSIGNED, BinarySearch.intRange().find(0, MAX_INT_UNSIGNED, i -> false));
        assertEquals(0, BinarySearch.intRange().unsigned().find(0, MAX_INT_UNSIGNED, i -> true));
        assertEquals(0x8000_0000, BinarySearch.intRange().unsigned().find(0, MAX_INT_UNSIGNED,
                i -> Integer.compareUnsigned(i, 0x8000_0000) >= 0));
    }

    @Test
    public void testBigRange() {
        assertEquals(1025, BinarySearch.intRange().find(0, Integer.MAX_VALUE, i -> i >= 1025));
        assertEquals(1026, BinarySearch.intRange().find(0, Integer.MAX_VALUE, i -> i >= 1026));
        assertEquals(99210, BinarySearch.intRange().find(49203, 848392, i -> i >= 99210));
        assertEquals(Integer.MAX_VALUE, BinarySearch.intRange().find(0, Integer.MAX_VALUE, i -> false));
        assertEquals(0, BinarySearch.intRange().find(0, Integer.MAX_VALUE, i -> true));
    }

    @Test
    public void testRevRange() {
        assertEquals(5, BinarySearch.intRange().find(9, -1, i -> i < 5));
        assertEquals(6, BinarySearch.intRange().find(9, -1, i -> i <= 5));
        assertEquals(0, BinarySearch.intRange().find(9, -1, i -> i < 0));
        assertEquals(1, BinarySearch.intRange().find(9, -1, i -> i <= 0));
    }
}
