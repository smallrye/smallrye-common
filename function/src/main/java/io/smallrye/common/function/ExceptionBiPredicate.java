package io.smallrye.common.function;

/**
 * A two-argument predicate which can throw an exception.
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

    default ExceptionBiPredicate<T, U, E> and(ExceptionBiPredicate<T, U, E> other) {
        return (t, u) -> test(t, u) && other.test(t, u);
    }

    default ExceptionBiPredicate<T, U, E> or(ExceptionBiPredicate<T, U, E> other) {
        return (t, u) -> test(t, u) || other.test(t, u);
    }

    default ExceptionBiPredicate<T, U, E> xor(ExceptionBiPredicate<T, U, E> other) {
        return (t, u) -> test(t, u) != other.test(t, u);
    }

    default ExceptionBiPredicate<T, U, E> not() {
        return (t, u) -> !test(t, u);
    }
}
