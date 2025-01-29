package io.smallrye.common.function;

import java.util.Objects;

/**
 * A predicate that operates on an object and an integer.
 */
public interface ObjIntPredicate<T> {
    boolean test(T object, int value);

    default ObjIntPredicate<T> and(ObjIntPredicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t, i) -> test(t, i) && other.test(t, i);
    }

    default ObjIntPredicate<T> negate() {
        return (t, i) -> !test(t, i);
    }

    default ObjIntPredicate<T> or(ObjIntPredicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t, i) -> test(t, i) || other.test(t, i);
    }
}
