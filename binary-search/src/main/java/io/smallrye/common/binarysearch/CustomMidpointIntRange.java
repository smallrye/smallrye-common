package io.smallrye.common.binarysearch;

import java.util.Comparator;
import java.util.function.BiPredicate;
import java.util.function.IntBinaryOperator;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import io.smallrye.common.function.ObjIntFunction;
import io.smallrye.common.function.ObjIntPredicate;

/**
 * Binary searches over an integer range which use a customized midpoint function.
 *
 * @see IntRange#customMidpoint()
 */
public final class CustomMidpointIntRange {
    private CustomMidpointIntRange() {
    }

    static final CustomMidpointIntRange instance = new CustomMidpointIntRange();

    public int find(int from, int to, IntBinaryOperator midpoint, IntPredicate test) {
        return find(test, from, to, midpoint, IntPredicate::test);
    }

    public <C> int find(C collection, int from, int to, IntBinaryOperator midpoint, ObjIntPredicate<C> test) {
        int low = from;
        int high = to;

        int mid = midpoint.applyAsInt(low, high);
        int newMid;
        for (;;) {
            if (test.test(collection, mid)) {
                high = mid;
                newMid = midpoint.applyAsInt(low, high);
                if (mid == newMid) {
                    return low;
                }
            } else {
                low = mid;
                newMid = midpoint.applyAsInt(low, high);
                if (mid == newMid) {
                    return high;
                }
            }
            mid = newMid;
        }
    }

    public <C, K> int find(C collection, int from, int to, IntBinaryOperator midpoint, ObjIntFunction<C, K> keyExtractor,
            Predicate<K> test) {
        return find(collection, test, from, to, midpoint, keyExtractor, Predicate::test);
    }

    public <C, K, V> int find(C collection, V searchVal, int from, int to, IntBinaryOperator midpoint,
            ObjIntFunction<C, K> keyExtractor, BiPredicate<V, K> keyTester) {
        return find(collection, from, to, midpoint, (c, i) -> keyTester.test(searchVal, keyExtractor.apply(c, i)));
    }

    public <C, K> int findFirst(C collection, K searchKey, int from, int to, IntBinaryOperator midpoint,
            ObjIntFunction<C, K> keyExtractor, Comparator<? super K> cmp) {
        return find(collection, searchKey, from, to, midpoint, keyExtractor, (v, k) -> cmp.compare(v, k) >= 0);
    }

    public <C, K extends Comparable<? super K>> int findFirst(C collection, K searchKey, int from, int to,
            IntBinaryOperator midpoint, ObjIntFunction<C, K> keyExtractor) {
        return findFirst(collection, searchKey, from, to, midpoint, keyExtractor, Comparator.naturalOrder());
    }
}
