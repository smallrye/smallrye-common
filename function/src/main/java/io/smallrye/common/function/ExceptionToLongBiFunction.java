package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A two-argument function which can throw an exception.
 *
 * @param <T> the first argument type
 * @param <U> the second argument type
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ExceptionToLongBiFunction<T, U, E extends Exception> {
    /**
     * Apply this function to the given arguments.
     *
     * @param t the first argument
     * @param u the second argument
     * @return the function result
     * @throws E if an exception occurs
     */
    long apply(T t, U u) throws E;

    /**
     * {@return a function which applies the result of this function to the given function}
     *
     * @param after the next function (must not be {@code null})
     * @param <R> the final result type
     */
    default <R> ExceptionBiFunction<T, U, R, E> andThen(ExceptionLongFunction<R, E> after) {
        Assert.checkNotNullParam("after", after);
        return (t, u) -> after.apply(apply(t, u));
    }
}
