package io.smallrye.common.function;

/**
 * A one-argument predicate which can throw an exception.
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

    default ExceptionPredicate<T, E> and(ExceptionPredicate<T, E> other) {
        return t -> test(t) && other.test(t);
    }

    default ExceptionPredicate<T, E> or(ExceptionPredicate<T, E> other) {
        return t -> test(t) || other.test(t);
    }

    default ExceptionPredicate<T, E> xor(ExceptionPredicate<T, E> other) {
        return t -> test(t) != other.test(t);
    }

    default ExceptionPredicate<T, E> not() {
        return t -> !test(t);
    }

    default <U> ExceptionBiPredicate<T, U, E> with(ExceptionPredicate<? super U, ? extends E> other) {
        return (t, u) -> test(t) && other.test(u);
    }
}
