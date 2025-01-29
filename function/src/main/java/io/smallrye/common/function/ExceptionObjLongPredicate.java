package io.smallrye.common.function;

import java.util.Objects;

/**
 * A predicate that operates on an object and a long integer.
 */
public interface ExceptionObjLongPredicate<T, E extends Exception> {
    boolean test(T object, long value);

    default ExceptionObjLongPredicate<T, E> and(ExceptionObjLongPredicate<? super T, ? extends E> other) {
        Objects.requireNonNull(other);
        return (t, i) -> test(t, i) && other.test(t, i);
    }

    default ExceptionObjLongPredicate<T, E> negate() {
        return (t, i) -> !test(t, i);
    }

    default ExceptionObjLongPredicate<T, E> or(ExceptionObjLongPredicate<? super T, ? extends E> other) {
        Objects.requireNonNull(other);
        return (t, i) -> test(t, i) || other.test(t, i);
    }
}
