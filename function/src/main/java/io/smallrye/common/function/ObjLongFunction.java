package io.smallrye.common.function;

import java.util.Objects;
import java.util.function.Function;

/**
 * A function which operates on an object and a long integer, yielding an object.
 *
 * @param <T> the first argument type
 * @param <R> the result type
 */
public interface ObjLongFunction<T, R> {
    /**
     * Applies this function to the given arguments.
     *
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @return the function result
     */
    R apply(T arg1, long arg2);

    /**
     * {@return a function that applies the given function to the result of this function}
     *
     * @param after the next function (must not be {@code null})
     * @param <V> the final return type
     */
    default <V> ObjLongFunction<T, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t, long u) -> after.apply(apply(t, u));
    }
}
