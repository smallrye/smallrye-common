package io.smallrye.common.binarysearch;

import java.util.Comparator;
import java.util.function.BiPredicate;
import java.util.function.IntBinaryOperator;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import io.smallrye.common.function.ObjIntFunction;
import io.smallrye.common.function.ObjIntPredicate;

/**
 * Binary searches over an integer range.
 *
 * @see BinarySearch#intRange()
 */
public final class IntRange {
    private final IntBinaryOperator midpoint;

    private IntRange(IntBinaryOperator midpoint) {
        this.midpoint = midpoint;
    }

    static final IntRange signed = new IntRange(IntRange::signedMidpoint);
    static final IntRange unsigned = new IntRange(IntRange::unsignedMidpoint);

    private static int signedMidpoint(int from, int to) {
        return from + (to - from >> 1);
    }

    private static int unsignedMidpoint(int from, int to) {
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
    public int find(int from, int to, IntPredicate test) {
        return find(test, from, to, IntPredicate::test);
    }

    public <C> int find(C collection, int from, int to, ObjIntPredicate<C> test) {
        return customMidpoint().find(collection, from, to, midpoint, test);
    }

    public <C, K> int find(C collection, int from, int to, ObjIntFunction<C, K> keyExtractor, Predicate<K> test) {
        return customMidpoint().find(collection, from, to, midpoint, keyExtractor, test);
    }

    public <C, K, V> int find(C collection, V searchVal, int from, int to, ObjIntFunction<C, K> keyExtractor,
            BiPredicate<V, K> keyTester) {
        return customMidpoint().find(collection, searchVal, from, to, midpoint, keyExtractor, keyTester);
    }

    public <C, K> int findFirst(C collection, K searchKey, int from, int to, ObjIntFunction<C, K> keyExtractor,
            Comparator<? super K> cmp) {
        return customMidpoint().findFirst(collection, searchKey, from, to, midpoint, keyExtractor, cmp);
    }

    public <C, K extends Comparable<? super K>> int findFirst(C collection, K searchKey, int from, int to,
            ObjIntFunction<C, K> keyExtractor) {
        return customMidpoint().findFirst(collection, searchKey, from, to, midpoint, keyExtractor);
    }

    /**
     * {@return an object which can perform binary searches over a signed range (not <code>null</code>)}
     */
    public IntRange signed() {
        return signed;
    }

    /**
     * {@return an object which can perform binary searches over an unsigned range (not <code>null</code>)}
     */
    public IntRange unsigned() {
        return unsigned;
    }

    /**
     * {@return an object which performs binary searches over a range defined by a custom midpoint function (not
     * <code>null</code>)}
     */
    public CustomMidpointIntRange customMidpoint() {
        return CustomMidpointIntRange.instance;
    }
}
