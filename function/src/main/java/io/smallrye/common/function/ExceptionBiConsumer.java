package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A two-argument consumer which can throw an exception.
 *
 * @param <T> the first argument type
 * @param <U> the second argument type
 * @param <E> the exception type
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

    /**
     * {@return a consumer which passes the arguments to this consumer followed by the given consumer}
     *
     * @param after the next consumer (must not be {@code null})
     */
    default ExceptionBiConsumer<T, U, E> andThen(ExceptionBiConsumer<? super T, ? super U, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }

    /**
     * {@return a runnable which passes the results of the given suppliers through this consumer}
     *
     * @param before1 the supplier for the first argument (must not be {@code null})
     * @param before2 the supplier for the second argument (must not be {@code null})
     */
    default ExceptionRunnable<E> compose(ExceptionSupplier<? extends T, ? extends E> before1,
            ExceptionSupplier<? extends U, ? extends E> before2) {
        Assert.checkNotNullParam("before1", before1);
        Assert.checkNotNullParam("before2", before2);
        return () -> accept(before1.get(), before2.get());
    }
}
