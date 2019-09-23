package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A unary operator which can throw an exception.
 */
@FunctionalInterface
public interface ExceptionUnaryOperator<T, E extends Exception> extends ExceptionFunction<T, T, E> {
    default ExceptionUnaryOperator<T, E> andThen(ExceptionUnaryOperator<T, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.apply(apply(t));
    }

    default ExceptionUnaryOperator<T, E> compose(ExceptionUnaryOperator<T, ? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return t -> apply(before.apply(t));
    }

    static <T, E extends Exception> ExceptionUnaryOperator<T, E> of(ExceptionFunction<T, T, E> func) {
        return func instanceof ExceptionUnaryOperator ? (ExceptionUnaryOperator<T, E>) func : func::apply;
    }

    static <T, E extends Exception> ExceptionUnaryOperator<T, E> identity() {
        return t -> t;
    }
}
