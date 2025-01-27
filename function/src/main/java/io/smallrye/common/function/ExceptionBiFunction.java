package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A two-argument function which can throw an exception.
 *
 * @param <T> the first argument type
 * @param <U> the second argument type
 * @param <R> the result type
 * @param <E> the exception type
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

    /**
     * {@return a function which passes the result of this function through the given function}
     *
     * @param after the next function (must not be {@code null})
     * @param <R2> the intermediate result type
     */
    default <R2> ExceptionBiFunction<T, U, R2, E> andThen(ExceptionFunction<? super R, ? extends R2, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return (t, u) -> after.apply(apply(t, u));
    }

    /**
     * {@return a consumer which passes the value given to the given consumer through this function}
     *
     * @param after the consumer (must not be {@code null})
     */
    default ExceptionBiConsumer<T, U, E> andThen(ExceptionConsumer<R, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return (t, u) -> after.accept(apply(t, u));
    }

    /**
     * {@return a supplier which passes the results of the given suppliers through this function}
     *
     * @param before1 the supplier for the first argument (must not be {@code null})
     * @param before2 the supplier for the second argument (must not be {@code null})
     */
    default ExceptionSupplier<R, E> compose(ExceptionSupplier<? extends T, ? extends E> before1,
            ExceptionSupplier<? extends U, ? extends E> before2) {
        Assert.checkNotNullParam("before1", before1);
        Assert.checkNotNullParam("before2", before2);
        return () -> apply(before1.get(), before2.get());
    }
}
