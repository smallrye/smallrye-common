package io.smallrye.common.function;

import java.util.Objects;
import java.util.function.Function;

/**
 * A function which operates on an object and an integer, yielding an object.
 */
public interface ObjIntFunction<T, R> {
    R apply(T arg1, int arg2);

    default <V> ObjIntFunction<T, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t, int u) -> after.apply(apply(t, u));
    }
}
