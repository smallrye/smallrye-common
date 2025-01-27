package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A binary operator which can throw an exception.
 *
 * @param <T> the argument type
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ExceptionBinaryOperator<T, E extends Exception> extends ExceptionBiFunction<T, T, T, E> {

    /**
     * {@return a binary operator which passes the result of this operator through the given unary operator}
     *
     * @param after the post-processing operator (must not be {@code null})
     */
    default ExceptionBinaryOperator<T, E> andThen(ExceptionUnaryOperator<T, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return (t, u) -> after.apply(apply(t, u));
    }
}
