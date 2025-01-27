package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A two-argument function which can throw an exception.
 *
 * @param <T> the type of the object argument
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ExceptionToLongFunction<T, E extends Exception> {
    /**
     * Apply this function to the given arguments.
     *
     * @param t the first argument
     * @return the function result
     * @throws E if an exception occurs
     */
    long apply(T t) throws E;

    /**
     * {@return a function which passes the result of this function through the given function}
     *
     * @param after the function (must not be {@code null})
     * @param <R> the result type
     */
    default <R> ExceptionFunction<T, R, E> andThen(ExceptionLongFunction<R, E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.apply(apply(t));
    }

    /**
     * {@return a function which passes the result of the given function through this function}
     *
     * @param before the function (must not be {@code null})
     * @param <T2> the initial argument type
     */
    default <T2> ExceptionToLongFunction<T2, E> compose(ExceptionFunction<? super T2, ? extends T, ? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return t -> apply(before.apply(t));
    }
}
