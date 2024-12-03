package io.smallrye.common.function;

import java.util.Objects;

/**
 * A function which operates on an object and an integer, yielding an object.
 *
 * @param <T> the first argument type
 * @param <R> the result type
 * @param <E> the exception type
 */
public interface ExceptionObjIntFunction<T, R, E extends Exception> {
    /**
     * Applies this function to the given arguments.
     *
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @return the function result
     * @throws E if an exception occurs
     */
    R apply(T arg1, int arg2) throws E;

    /**
     * {@return a function that applies the given function to the result of this function}
     *
     * @param after the next function (must not be {@code null})
     * @param <V> the final return type
     */
    default <V> ExceptionObjIntFunction<T, V, E> andThen(ExceptionFunction<? super R, ? extends V, ? extends E> after) {
        Objects.requireNonNull(after);
        return (T t, int u) -> after.apply(apply(t, u));
    }
}
