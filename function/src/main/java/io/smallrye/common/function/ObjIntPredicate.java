package io.smallrye.common.function;

import java.util.Objects;

/**
 * A predicate that operates on an object and an integer.
 *
 * @param <T> the first argument type
 */
public interface ObjIntPredicate<T> {
    /**
     * Evaluate this predicate on the given arguments.
     *
     * @param object the first argument
     * @param value the second argument
     * @return {@code true} if the predicate passes, {@code false} otherwise
     */
    boolean test(T object, int value);

    /**
     * {@return a predicate which is {@code true} only when this and the given predicate return {@code true}}
     *
     * @param other the other predicate (must not be {@code null})
     */
    default ObjIntPredicate<T> and(ObjIntPredicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t, i) -> test(t, i) && other.test(t, i);
    }

    /**
     * {@return a predicate which is {@code true} when this predicate is {@code false}, and vice-versa}
     */
    default ObjIntPredicate<T> negate() {
        return (t, i) -> !test(t, i);
    }

    /**
     * {@return a predicate which is {@code true} when either this or the given predicate return {@code true}}
     *
     * @param other the other predicate (must not be {@code null})
     */
    default ObjIntPredicate<T> or(ObjIntPredicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t, i) -> test(t, i) || other.test(t, i);
    }
}
