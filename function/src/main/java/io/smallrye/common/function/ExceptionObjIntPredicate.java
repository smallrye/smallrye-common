package io.smallrye.common.function;

import java.util.Objects;

/**
 * A predicate that operates on an object and an integer.
 */
public interface ExceptionObjIntPredicate<T, E extends Exception> {
    boolean test(T object, int value) throws E;

    default ExceptionObjIntPredicate<T, E> and(ExceptionObjIntPredicate<? super T, ? extends E> other) {
        Objects.requireNonNull(other);
        return (t, i) -> test(t, i) && other.test(t, i);
    }

    default ExceptionObjIntPredicate<T, E> negate() {
        return (t, i) -> !test(t, i);
    }

    default ExceptionObjIntPredicate<T, E> or(ExceptionObjIntPredicate<? super T, ? extends E> other) {
        Objects.requireNonNull(other);
        return (t, i) -> test(t, i) || other.test(t, i);
    }
}
