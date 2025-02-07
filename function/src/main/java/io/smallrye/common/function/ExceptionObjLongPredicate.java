package io.smallrye.common.function;

import java.util.Objects;

/**
 * A predicate that operates on an object and a long integer.
 *
 * @param <T> the first argument type
 * @param <E> the exception type
 */
public interface ExceptionObjLongPredicate<T, E extends Exception> {
    /**
     * Evaluate this predicate on the given arguments.
     *
     * @param object the first argument
     * @param value the second argument
     * @return {@code true} if the predicate passes, {@code false} otherwise
     * @throws E if an exception occurs
     */
    boolean test(T object, long value) throws E;

    /**
     * {@return a predicate which is {@code true} only when this and the given predicate return {@code true}}
     *
     * @param other the other predicate (must not be {@code null})
     */
    default ExceptionObjLongPredicate<T, E> and(ExceptionObjLongPredicate<? super T, ? extends E> other) {
        Objects.requireNonNull(other);
        return (t, i) -> test(t, i) && other.test(t, i);
    }

    /**
     * {@return a predicate which is {@code true} when this predicate is {@code false}, and vice-versa}
     */
    default ExceptionObjLongPredicate<T, E> negate() {
        return (t, i) -> !test(t, i);
    }

    /**
     * {@return a predicate which is {@code true} when either this or the given predicate return {@code true}}
     *
     * @param other the other predicate (must not be {@code null})
     */
    default ExceptionObjLongPredicate<T, E> or(ExceptionObjLongPredicate<? super T, ? extends E> other) {
        Objects.requireNonNull(other);
        return (t, i) -> test(t, i) || other.test(t, i);
    }
}
