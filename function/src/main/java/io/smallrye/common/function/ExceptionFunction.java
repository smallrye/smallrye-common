package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A one-argument function which can throw an exception.
 *
 * @param <T> the argument type
 * @param <R> the result type
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ExceptionFunction<T, R, E extends Exception> {
    /**
     * Applies this function to the given arguments.
     *
     * @param t the argument
     * @return the function result
     * @throws E if an exception occurs
     */
    R apply(T t) throws E;

    /**
     * {@return a function which applies the result of this function to the given function}
     *
     * @param after the next function (must not be {@code null})
     * @param <R2> the final result type
     */
    default <R2> ExceptionFunction<T, R2, E> andThen(ExceptionFunction<? super R, ? extends R2, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.apply(apply(t));
    }

    /**
     * {@return a function which applies the argument to, and result of, this function to the given function}
     *
     * @param after the next function (must not be {@code null})
     * @param <R2> the final result type
     */
    default <R2> ExceptionFunction<T, R2, E> andThen(
            ExceptionBiFunction<? super T, ? super R, ? extends R2, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.apply(t, apply(t));
    }

    /**
     * {@return a function which applies the result of the given function to this function}
     *
     * @param before the first function (must not be {@code null})
     * @param <T2> the intermediate argument type
     */
    default <T2> ExceptionFunction<T2, R, E> compose(ExceptionFunction<? super T2, ? extends T, ? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return t -> apply(before.apply(t));
    }

    /**
     * {@return a consumer which passes the result of this function to the given consumer}
     *
     * @param after the consumer (must not be {@code null})
     */
    default ExceptionConsumer<T, E> andThen(ExceptionConsumer<? super R, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.accept(apply(t));
    }

    /**
     * {@return a consumer which passes the argument to, and result of, this function to the given consumer}
     *
     * @param after the consumer (must not be {@code null})
     */
    default ExceptionConsumer<T, E> andThen(ExceptionBiConsumer<? super T, ? super R, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.accept(t, apply(t));
    }

    /**
     * {@return a predicate that is {@code true} when the given predicate is {@code true} for arguments passed through this
     * function}
     *
     * @param after the predicate (must not be {@code null})
     */
    default ExceptionPredicate<T, E> andThen(ExceptionPredicate<? super R, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.test(apply(t));
    }

    /**
     * {@return a predicate that is {@code true} when the given predicate is {@code true} for arguments passed through this
     * function}
     *
     * @param after the predicate (must not be {@code null})
     */
    default ExceptionPredicate<T, E> andThen(ExceptionBiPredicate<? super T, ? super R, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.test(t, apply(t));
    }

    /**
     * {@return a supplier which returns the result of this function when applied to the given supplier}
     *
     * @param before the supplier (must not be {@code null})
     */
    default ExceptionSupplier<R, E> compose(ExceptionSupplier<? extends T, ? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return () -> apply(before.get());
    }
}
