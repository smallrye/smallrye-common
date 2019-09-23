package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A two-argument consumer which can throw an exception.
 */
@FunctionalInterface
public interface ExceptionBiConsumer<T, U, E extends Exception> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first argument
     * @param u the second argument
     * @throws E if an exception occurs
     */
    void accept(T t, U u) throws E;

    default ExceptionBiConsumer<T, U, E> andThen(ExceptionBiConsumer<? super T, ? super U, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }

    default ExceptionRunnable<E> compose(ExceptionSupplier<? extends T, ? extends E> before1,
            ExceptionSupplier<? extends U, ? extends E> before2) {
        Assert.checkNotNullParam("before1", before1);
        Assert.checkNotNullParam("before2", before2);
        return () -> accept(before1.get(), before2.get());
    }
}
