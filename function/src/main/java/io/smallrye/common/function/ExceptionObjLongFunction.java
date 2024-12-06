package io.smallrye.common.function;

import java.util.Objects;

/**
 * A function which operates on an object and a long integer, yielding an object.
 */
public interface ExceptionObjLongFunction<T, R, E extends Exception> {
    R apply(T arg1, long arg2) throws E;

    default <V> ExceptionObjLongFunction<T, V, E> andThen(ExceptionFunction<? super R, ? extends V, ? extends E> after)
            throws E {
        Objects.requireNonNull(after);
        return (T t, long u) -> after.apply(apply(t, u));
    }
}
