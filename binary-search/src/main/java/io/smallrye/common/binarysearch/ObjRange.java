package io.smallrye.common.binarysearch;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

/**
 * Binary searches over a range of objects.
 *
 * @see BinarySearch#objRange()
 */
public final class ObjRange {
    private ObjRange() {
    }

    static final ObjRange instance = new ObjRange();

    public <T> T find(T from, T to, BinaryOperator<T> midpoint, Predicate<T> test) {
        return find(test, from, to, midpoint, Predicate::test);
    }

    public <T, C> T find(C collection, T from, T to, BinaryOperator<T> midpoint, BiPredicate<C, T> test) {
        T low = from;
        T high = to;

        T mid = midpoint.apply(low, high);
        T newMid;
        for (;;) {
            if (test.test(collection, mid)) {
                high = mid;
                newMid = midpoint.apply(low, high);
                if (Objects.equals(mid, newMid)) {
                    return low;
                }
            } else {
                low = mid;
                newMid = midpoint.apply(low, high);
                if (Objects.equals(mid, newMid)) {
                    return high;
                }
            }
            mid = newMid;
        }
    }

    public <T, C, K> T find(C collection, T from, T to, BinaryOperator<T> midpoint, BiFunction<C, T, K> keyExtractor,
            Predicate<K> test) {
        return find(collection, from, to, midpoint, (c, idx) -> test.test(keyExtractor.apply(c, idx)));
    }
}
