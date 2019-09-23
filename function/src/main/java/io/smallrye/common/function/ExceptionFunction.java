package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A one-argument function which can throw an exception.
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

    default <R2> ExceptionFunction<T, R2, E> andThen(ExceptionFunction<? super R, ? extends R2, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.apply(apply(t));
    }

    default <R2> ExceptionFunction<T, R2, E> andThen(
            ExceptionBiFunction<? super T, ? super R, ? extends R2, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.apply(t, apply(t));
    }

    default <T2> ExceptionFunction<T2, R, E> compose(ExceptionFunction<? super T2, ? extends T, ? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return t -> apply(before.apply(t));
    }

    default ExceptionConsumer<T, E> andThen(ExceptionConsumer<? super R, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.accept(apply(t));
    }

    default ExceptionConsumer<T, E> andThen(ExceptionBiConsumer<? super T, ? super R, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.accept(t, apply(t));
    }

    default ExceptionPredicate<T, E> andThen(ExceptionPredicate<? super R, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.test(apply(t));
    }

    default ExceptionPredicate<T, E> andThen(ExceptionBiPredicate<? super T, ? super R, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.test(t, apply(t));
    }

    default ExceptionSupplier<R, E> compose(ExceptionSupplier<? extends T, ? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return () -> apply(before.get());
    }
}
