package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A two-argument function which can throw an exception.
 *
 * @param <T> the object argument type
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ExceptionToIntFunction<T, E extends Exception> {
    /**
     * Apply this function to the given arguments.
     *
     * @param t the first argument
     * @return the function result
     * @throws E if an exception occurs
     */
    int apply(T t) throws E;

    /**
     * Apply the given function after this function.
     *
     * @param after the function to apply (must not be {@code null})
     * @return the result of the second function
     * @param <R> the result type
     */
    default <R> ExceptionFunction<T, R, E> andThen(ExceptionIntFunction<? extends R, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.apply(apply(t));
    }

    /**
     * Apply the given function after this function.
     *
     * @param after the function to apply (must not be {@code null})
     * @return the result of the second function
     * @param <R> the result type
     */
    default <R> ExceptionFunction<T, R, E> andThen(ExceptionLongFunction<? extends R, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.apply(apply(t));
    }

    /**
     * Apply this function after the given function.
     *
     * @param before the function to apply first (must not be {@code null})
     * @return the result of the composed function
     * @param <T2> the result type
     */
    default <T2> ExceptionToIntFunction<T2, E> compose(ExceptionFunction<? super T2, ? extends T, ? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return t -> apply(before.apply(t));
    }
}
