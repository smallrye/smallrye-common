package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A one-argument consumer which can throw an exception.
 *
 * @param <T> the argument type
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ExceptionConsumer<T, E extends Exception> {
    /**
     * Performs this operation on the given argument.
     *
     * @param t the argument
     * @throws E if an exception occurs
     */
    void accept(T t) throws E;

    /**
     * {@return a consumer which passes the argument to this consumer followed by the given consumer}
     *
     * @param after the next consumer (must not be {@code null})
     */
    default ExceptionConsumer<T, E> andThen(ExceptionConsumer<? super T, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> {
            accept(t);
            after.accept(t);
        };
    }

    /**
     * {@return a consumer which passes the argument to the given consumer followed by this consumer}
     *
     * @param before the first consumer (must not be {@code null})
     */
    default ExceptionConsumer<T, E> compose(ExceptionConsumer<? super T, ? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return t -> {
            before.accept(t);
            accept(t);
        };
    }

    /**
     * {@return a runnable which passes the result of the given supplier to this consumer}
     *
     * @param before the suppler (must not be {@code null})
     */
    default ExceptionRunnable<E> compose(ExceptionSupplier<? extends T, ? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return () -> accept(before.get());
    }
}
