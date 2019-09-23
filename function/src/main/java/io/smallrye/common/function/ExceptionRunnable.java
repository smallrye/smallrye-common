package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * An operation that can throw an exception.
 */
public interface ExceptionRunnable<E extends Exception> {
    /**
     * Run the operation.
     *
     * @throws E if an exception occurs
     */
    void run() throws E;

    default ExceptionRunnable<E> andThen(ExceptionRunnable<? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return () -> {
            run();
            after.run();
        };
    }

    default ExceptionRunnable<E> compose(ExceptionRunnable<? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return () -> {
            before.run();
            run();
        };
    }
}
