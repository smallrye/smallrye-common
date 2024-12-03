package io.smallrye.common.binarysearch;

import java.util.Comparator;
import java.util.function.BiPredicate;
import java.util.function.LongBinaryOperator;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

import io.smallrye.common.function.ObjLongFunction;
import io.smallrye.common.function.ObjLongPredicate;

/**
 * Binary searches over a long integer range.
 *
 * @see BinarySearch#longRange()
 */
public final class LongRange {
    private final LongBinaryOperator midpoint;

    private LongRange(LongBinaryOperator midpoint) {
        this.midpoint = midpoint;
    }

    static final LongRange signed = new LongRange(LongRange::signedMidpoint);
    static final LongRange unsigned = new LongRange(LongRange::unsignedMidpoint);

    private static long signedMidpoint(long from, long to) {
        return from + (to - from >> 1);
    }

    private static long unsignedMidpoint(long from, long to) {
        return from + (to - from >>> 1);
    }

    /**
     * Get the lowest index within the given range that satisfies the given test.
     *
     * @param from the low end of the range (inclusive)
     * @param to the high end of the range (exclusive)
     * @param test the test (must not be {@code null})
     * @return the lowest index within the range which satisfies the test, or {@code to} if no values satisfy the range
     */
    public long find(long from, long to, LongPredicate test) {
        return find(test, from, to, LongPredicate::test);
    }

    public <C> long find(C collection, long from, long to, ObjLongPredicate<C> test) {
        return customMidpoint().find(collection, from, to, midpoint, test);
    }

    public <C, K> long find(C collection, long from, long to, ObjLongFunction<C, K> keyExtractor, Predicate<K> test) {
        return customMidpoint().find(collection, from, to, midpoint, keyExtractor, test);
    }

    public <C, K, V> long find(C collection, V searchVal, long from, long to, ObjLongFunction<C, K> keyExtractor,
            BiPredicate<V, K> keyTester) {
        return customMidpoint().find(collection, searchVal, from, to, midpoint, keyExtractor, keyTester);
    }

    public <C, K> long findFirst(C collection, K searchKey, long from, long to, ObjLongFunction<C, K> keyExtractor,
            Comparator<? super K> cmp) {
        return customMidpoint().findFirst(collection, searchKey, from, to, midpoint, keyExtractor, cmp);
    }

    public <C, K extends Comparable<? super K>> long findFirst(C collection, K searchKey, long from, long to,
            ObjLongFunction<C, K> keyExtractor) {
        return customMidpoint().findFirst(collection, searchKey, from, to, midpoint, keyExtractor);
    }

    /**
     * {@return an object which can perform binary searches over a signed range (not <code>null</code>)}
     */
    public LongRange signed() {
        return signed;
    }

    /**
     * {@return an object which can perform binary searches over an unsigned range (not <code>null</code>)}
     */
    public LongRange unsigned() {
        return unsigned;
    }

    /**
     * {@return an object which performs binary searches over a range defined by a custom midpoint function (not
     * <code>null</code>)}
     */
    public CustomMidpointLongRange customMidpoint() {
        return CustomMidpointLongRange.instance;
    }
}
