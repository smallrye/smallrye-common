package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A one-argument long integer function which can throw an exception.
 *
 * @param <R> the result type
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ExceptionLongFunction<R, E extends Exception> {
    /**
     * Applies this function to the given arguments.
     *
     * @param value the argument
     * @return the function result
     * @throws E if an exception occurs
     */
    R apply(long value) throws E;

    /**
     * {@return a function which applies the result of this function to another function}
     *
     * @param after the other function (must not be {@code null})
     * @param <R2> the result type
     */
    default <R2> ExceptionLongFunction<R2, E> andThen(ExceptionFunction<? super R, ? extends R2, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.apply(apply(t));
    }

    /**
     * {@return a function which applies the result of another function to this function}
     *
     * @param before the other function (must not be {@code null})
     * @param <T> the initial argument type
     */
    default <T> ExceptionFunction<T, R, E> compose(ExceptionToLongFunction<? super T, ? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return t -> apply(before.apply(t));
    }
}
