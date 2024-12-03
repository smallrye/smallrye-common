package io.smallrye.common.search;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

public final class TestObjRange {
    private static final BigInteger MINUS_100 = BigInteger.valueOf(-100);
    private static final BigInteger MINUS_TEN = BigInteger.valueOf(-10);

    private static final BigInteger FOUR = BigInteger.valueOf(4);
    private static final BigInteger FIVE = BigInteger.valueOf(5);
    private static final BigInteger SIX = BigInteger.valueOf(6);

    public TestObjRange() {
    }

    private static BigInteger biMidPoint(BigInteger a, BigInteger b) {
        return a.add(b.subtract(a).shiftRight(1));
    }

    @Test
    public void testEmptyRange() {
        assertEquals(ONE, BinarySearch.objRange().find(ONE, ONE, TestObjRange::biMidPoint, i -> true));
    }

    @Test
    public void testRange() {
        assertEquals(ZERO, BinarySearch.objRange().find(ZERO, TEN, TestObjRange::biMidPoint, i -> i.compareTo(MINUS_100) >= 0));
        assertEquals(ZERO, BinarySearch.objRange().find(ZERO, TEN, TestObjRange::biMidPoint, i -> i.compareTo(ZERO) >= 0));
        assertEquals(ONE, BinarySearch.objRange().find(ZERO, TEN, TestObjRange::biMidPoint, i -> i.compareTo(ZERO) > 0));
        assertEquals(FOUR, BinarySearch.objRange().find(ZERO, TEN, TestObjRange::biMidPoint, i -> i.compareTo(FOUR) >= 0));
        assertEquals(FIVE, BinarySearch.objRange().find(ZERO, TEN, TestObjRange::biMidPoint, i -> i.compareTo(FOUR) > 0));
        assertEquals(FIVE, BinarySearch.objRange().find(ZERO, TEN, TestObjRange::biMidPoint, i -> i.compareTo(FIVE) >= 0));
        assertEquals(SIX, BinarySearch.objRange().find(ZERO, TEN, TestObjRange::biMidPoint, i -> i.compareTo(FIVE) > 0));
        assertEquals(TEN, BinarySearch.objRange().find(ZERO, TEN, TestObjRange::biMidPoint, i -> i.compareTo(TEN) >= 0));
        assertEquals(TEN, BinarySearch.objRange().find(ZERO, TEN, TestObjRange::biMidPoint, i -> false));
        assertEquals(ZERO, BinarySearch.objRange().find(ZERO, TEN, TestObjRange::biMidPoint, i -> true));
        assertEquals(MINUS_TEN, BinarySearch.objRange().find(MINUS_TEN, TEN, TestObjRange::biMidPoint, i -> true));
    }

    @Test
    public void testAlgorithm() {
        BigInteger bigNumber = BigInteger.valueOf(307031012708191L);
        BigInteger square = bigNumber.multiply(bigNumber);

        // search between 0 and square for bigNumber
        assertEquals(bigNumber, BinarySearch.objRange().find(
                ZERO,
                square,
                TestObjRange::biMidPoint,
                val -> val.multiply(val).compareTo(square) >= 0));
    }
}
