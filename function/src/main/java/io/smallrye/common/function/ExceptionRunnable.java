package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * An operation that can throw an exception.
 *
 * @param <E> the exception type
 */
public interface ExceptionRunnable<E extends Exception> {
    /**
     * Run the operation.
     *
     * @throws E if an exception occurs
     */
    void run() throws E;

    /**
     * {@return a runnable which runs this task and then the given task}
     *
     * @param after the other task
     */
    default ExceptionRunnable<E> andThen(ExceptionRunnable<? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return () -> {
            run();
            after.run();
        };
    }

    /**
     * {@return a runnable which runs the given task and then this task}
     *
     * @param before the other task
     */
    default ExceptionRunnable<E> compose(ExceptionRunnable<? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return () -> {
            before.run();
            run();
        };
    }
}
