package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A two-argument function which can throw an exception.
 */
@FunctionalInterface
public interface ExceptionBiFunction<T, U, R, E extends Exception> {
    /**
     * Applies this function to the given arguments.
     *
     * @param t the first argument
     * @param u the second argument
     * @return the function result
     * @throws E if an exception occurs
     */
    R apply(T t, U u) throws E;

    default <R2> ExceptionBiFunction<T, U, R2, E> andThen(ExceptionFunction<? super R, ? extends R2, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return (t, u) -> after.apply(apply(t, u));
    }

    default ExceptionBiConsumer<T, U, E> andThen(ExceptionConsumer<R, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return (t, u) -> after.accept(apply(t, u));
    }

    default ExceptionSupplier<R, E> compose(ExceptionSupplier<? extends T, ? extends E> before1,
            ExceptionSupplier<? extends U, ? extends E> before2) {
        Assert.checkNotNullParam("before1", before1);
        Assert.checkNotNullParam("before2", before2);
        return () -> apply(before1.get(), before2.get());
    }
}
