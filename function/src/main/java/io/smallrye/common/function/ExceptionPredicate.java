package io.smallrye.common.function;

/**
 * A one-argument predicate which can throw an exception.
 *
 * @param <T> the argument type
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ExceptionPredicate<T, E extends Exception> {
    /**
     * Evaluate this predicate on the given arguments.
     *
     * @param t the first argument
     * @return {@code true} if the predicate passes, {@code false} otherwise
     * @throws E if an exception occurs
     */
    boolean test(T t) throws E;

    /**
     * {@return a predicate which is {@code true} only when this and the given predicate return {@code true}}
     *
     * @param other the other predicate (must not be {@code null})
     */
    default ExceptionPredicate<T, E> and(ExceptionPredicate<T, E> other) {
        return t -> test(t) && other.test(t);
    }

    /**
     * {@return a predicate which is {@code true} when either this or the given predicate return {@code true}}
     *
     * @param other the other predicate (must not be {@code null})
     */
    default ExceptionPredicate<T, E> or(ExceptionPredicate<T, E> other) {
        return t -> test(t) || other.test(t);
    }

    /**
     * {@return a predicate which is {@code true} when either this or the given predicate (but not both) return {@code true}}
     *
     * @param other the other predicate (must not be {@code null})
     */
    default ExceptionPredicate<T, E> xor(ExceptionPredicate<T, E> other) {
        return t -> test(t) != other.test(t);
    }

    /**
     * {@return a predicate which is {@code true} when this predicate is {@code false}, and vice-versa}
     */
    default ExceptionPredicate<T, E> not() {
        return t -> !test(t);
    }

    /**
     * {@return a bi-predicate which is {@code true} when this predicate is {@code true} for its first argument, and the given
     * predicate is {@code true} for its second argument}
     *
     * @param other the other predicate (must not be {@code null})
     * @param <U> the second argument type
     */
    default <U> ExceptionBiPredicate<T, U, E> with(ExceptionPredicate<? super U, ? extends E> other) {
        return (t, u) -> test(t) && other.test(u);
    }
}
