package io.smallrye.common.function;

import java.util.Objects;

/**
 * A predicate that operates on an object and a long integer.
 */
public interface ObjLongPredicate<T> {
    boolean test(T object, long value);

    default ObjLongPredicate<T> and(ObjLongPredicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t, i) -> test(t, i) && other.test(t, i);
    }

    default ObjLongPredicate<T> negate() {
        return (t, i) -> !test(t, i);
    }

    default ObjLongPredicate<T> or(ObjLongPredicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t, i) -> test(t, i) || other.test(t, i);
    }
}
