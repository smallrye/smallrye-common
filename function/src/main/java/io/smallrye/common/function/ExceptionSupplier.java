package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A supplier which can throw an exception.
 *
 * @param <T> the result type
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ExceptionSupplier<T, E extends Exception> {
    /**
     * Gets a result.
     *
     * @return the result
     * @throws E if an exception occurs
     */
    T get() throws E;

    /**
     * {@return a runnable which passes the result of this supplier to the given consumer}
     *
     * @param after the consumer (must not be {@code null})
     */
    default ExceptionRunnable<E> andThen(ExceptionConsumer<? super T, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return () -> after.accept(get());
    }

    /**
     * {@return a supplier which passes the result of this supplier through the given function}
     *
     * @param after the function (must not be {@code null})
     * @param <R> the result type
     */
    default <R> ExceptionSupplier<R, E> andThen(ExceptionFunction<? super T, ? extends R, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return () -> after.apply(get());
    }
}
