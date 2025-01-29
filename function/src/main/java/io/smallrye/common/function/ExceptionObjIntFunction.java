package io.smallrye.common.function;

import java.util.Objects;

/**
 * A function which operates on an object and an integer, yielding an object.
 */
public interface ExceptionObjIntFunction<T, R, E extends Exception> {
    R apply(T arg1, int arg2) throws E;

    default <V> ExceptionObjIntFunction<T, V, E> andThen(ExceptionFunction<? super R, ? extends V, ? extends E> after)
            throws E {
        Objects.requireNonNull(after);
        return (T t, int u) -> after.apply(apply(t, u));
    }
}
