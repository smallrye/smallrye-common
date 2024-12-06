package io.smallrye.common.function;

import java.util.Objects;
import java.util.function.Function;

/**
 * A function which operates on an object and a long integer, yielding an object.
 */
public interface ObjLongFunction<T, R> {
    R apply(T arg1, long arg2);

    default <V> ObjLongFunction<T, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t, long u) -> after.apply(apply(t, u));
    }
}
