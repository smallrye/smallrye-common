package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A two-argument object and long integer consumer which can throw an exception.
 */
@FunctionalInterface
public interface ExceptionObjLongConsumer<T, E extends Exception> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first argument
     * @param value the second argument
     * @throws E if an exception occurs
     */
    void accept(T t, long value) throws E;

    default ExceptionObjLongConsumer<T, E> andThen(ExceptionObjLongConsumer<? super T, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return (t, v) -> {
            accept(t, v);
            after.accept(t, v);
        };
    }

    default ExceptionObjLongConsumer<T, E> compose(ExceptionObjLongConsumer<? super T, ? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return (t, v) -> {
            before.accept(t, v);
            accept(t, v);
        };
    }
}
