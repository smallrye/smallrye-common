package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A one-argument consumer which can throw an exception.
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

    default ExceptionConsumer<T, E> andThen(ExceptionConsumer<? super T, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> {
            accept(t);
            after.accept(t);
        };
    }

    default ExceptionConsumer<T, E> compose(ExceptionConsumer<? super T, ? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return t -> {
            accept(t);
            before.accept(t);
        };
    }

    default ExceptionRunnable<E> compose(ExceptionSupplier<? extends T, ? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return () -> accept(before.get());
    }
}
