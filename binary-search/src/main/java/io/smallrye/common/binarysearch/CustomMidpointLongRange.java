package io.smallrye.common.binarysearch;

import java.util.Comparator;
import java.util.function.BiPredicate;
import java.util.function.LongBinaryOperator;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

import io.smallrye.common.function.ObjLongFunction;
import io.smallrye.common.function.ObjLongPredicate;

/**
 *
 */
public final class CustomMidpointLongRange {
    private CustomMidpointLongRange() {
    }

    static final CustomMidpointLongRange instance = new CustomMidpointLongRange();

    public long find(long from, long to, LongBinaryOperator midpoint, LongPredicate test) {
        return find(test, from, to, midpoint, LongPredicate::test);
    }

    public <C> long find(C collection, long from, long to, LongBinaryOperator midpoint, ObjLongPredicate<C> test) {
        long low = from;
        long high = to;

        long mid = midpoint.applyAsLong(low, high);
        long newMid;
        for (;;) {
            if (test.test(collection, mid)) {
                high = mid;
                newMid = midpoint.applyAsLong(low, high);
                if (mid == newMid) {
                    return low;
                }
            } else {
                low = mid;
                newMid = midpoint.applyAsLong(low, high);
                if (mid == newMid) {
                    return high;
                }
            }
            mid = newMid;
        }
    }

    public <C, K> long find(C collection, long from, long to, LongBinaryOperator midpoint, ObjLongFunction<C, K> keyExtractor,
            Predicate<K> test) {
        return find(collection, test, from, to, midpoint, keyExtractor, Predicate::test);
    }

    public <C, K, V> long find(C collection, V searchVal, long from, long to, LongBinaryOperator midpoint,
            ObjLongFunction<C, K> keyExtractor, BiPredicate<V, K> keyTester) {
        return find(collection, from, to, midpoint, (c, i) -> keyTester.test(searchVal, keyExtractor.apply(c, i)));
    }

    public <C, K> long findFirst(C collection, K searchKey, long from, long to, LongBinaryOperator midpoint,
            ObjLongFunction<C, K> keyExtractor, Comparator<? super K> cmp) {
        return find(collection, searchKey, from, to, midpoint, keyExtractor, (v, k) -> cmp.compare(v, k) >= 0);
    }

    public <C, K extends Comparable<? super K>> long findFirst(C collection, K searchKey, long from, long to,
            LongBinaryOperator midpoint, ObjLongFunction<C, K> keyExtractor) {
        return findFirst(collection, searchKey, from, to, midpoint, keyExtractor, Comparator.naturalOrder());
    }
}
