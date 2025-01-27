package io.smallrye.common.function;

/**
 * A two-argument predicate which can throw an exception.
 *
 * @param <T> the first argument type
 * @param <U> the second argument type
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ExceptionBiPredicate<T, U, E extends Exception> {
    /**
     * Evaluate this predicate on the given arguments.
     *
     * @param t the first argument
     * @param u the second argument
     * @return {@code true} if the predicate passes, {@code false} otherwise
     * @throws E if an exception occurs
     */
    boolean test(T t, U u) throws E;

    /**
     * {@return a predicate which is {@code true} only when this and the given predicate return {@code true}}
     *
     * @param other the other predicate (must not be {@code null})
     */
    default ExceptionBiPredicate<T, U, E> and(ExceptionBiPredicate<T, U, E> other) {
        return (t, u) -> test(t, u) && other.test(t, u);
    }

    /**
     * {@return a predicate which is {@code true} when either this or the given predicate return {@code true}}
     *
     * @param other the other predicate (must not be {@code null})
     */
    default ExceptionBiPredicate<T, U, E> or(ExceptionBiPredicate<T, U, E> other) {
        return (t, u) -> test(t, u) || other.test(t, u);
    }

    /**
     * {@return a predicate which is {@code true} when either this or the given predicate (but not both) return {@code true}}
     *
     * @param other the other predicate (must not be {@code null})
     */
    default ExceptionBiPredicate<T, U, E> xor(ExceptionBiPredicate<T, U, E> other) {
        return (t, u) -> test(t, u) != other.test(t, u);
    }

    /**
     * {@return a predicate which is {@code true} when this predicate is {@code false}, and vice-versa}
     */
    default ExceptionBiPredicate<T, U, E> not() {
        return (t, u) -> !test(t, u);
    }
}
