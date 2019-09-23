package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A two-argument object and integer consumer which can throw an exception.
 */
@FunctionalInterface
public interface ExceptionObjIntConsumer<T, E extends Exception> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first argument
     * @param value the second argument
     * @throws E if an exception occurs
     */
    void accept(T t, int value) throws E;

    default ExceptionObjIntConsumer<T, E> andThen(ExceptionObjIntConsumer<? super T, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return (t, v) -> {
            accept(t, v);
            after.accept(t, v);
        };
    }

    default ExceptionObjIntConsumer<T, E> compose(ExceptionObjIntConsumer<? super T, ? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return (t, v) -> {
            before.accept(t, v);
            accept(t, v);
        };
    }

    default ExceptionObjIntConsumer<T, E> andThen(ExceptionObjLongConsumer<? super T, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return (t, v) -> {
            accept(t, v);
            after.accept(t, v);
        };
    }

    default ExceptionObjIntConsumer<T, E> compose(ExceptionObjLongConsumer<? super T, ? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return (t, v) -> {
            before.accept(t, v);
            accept(t, v);
        };
    }
}
