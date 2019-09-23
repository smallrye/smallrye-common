package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A binary operator which can throw an exception.
 */
@FunctionalInterface
public interface ExceptionBinaryOperator<T, E extends Exception> extends ExceptionBiFunction<T, T, T, E> {

    default ExceptionBinaryOperator<T, E> andThen(ExceptionUnaryOperator<T, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return (t, u) -> after.apply(apply(t, u));
    }
}
