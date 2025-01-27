package io.smallrye.common.function;

import io.smallrye.common.constraint.Assert;

/**
 * A unary operator which can throw an exception.
 *
 * @param <T> the argument type
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ExceptionUnaryOperator<T, E extends Exception> extends ExceptionFunction<T, T, E> {
    /**
     * {@return a unary operator that passes the result of this operator through the given operator}
     *
     * @param after the next operator (must not be {@code null})
     */
    default ExceptionUnaryOperator<T, E> andThen(ExceptionUnaryOperator<T, ? extends E> after) {
        Assert.checkNotNullParam("after", after);
        return t -> after.apply(apply(t));
    }

    /**
     * {@return a unary operator which passes the result of the given operator through this operator}
     *
     * @param before the first operator (must not be {@code null})
     */
    default ExceptionUnaryOperator<T, E> compose(ExceptionUnaryOperator<T, ? extends E> before) {
        Assert.checkNotNullParam("before", before);
        return t -> apply(before.apply(t));
    }

    /**
     * {@return a unary operator which is implemented by the given function}
     *
     * @param func the function (must not be {@code null})
     * @param <T> the argument type
     * @param <E> the exception type
     */
    static <T, E extends Exception> ExceptionUnaryOperator<T, E> of(ExceptionFunction<T, T, E> func) {
        return func instanceof ExceptionUnaryOperator ? (ExceptionUnaryOperator<T, E>) func : func::apply;
    }

    /**
     * {@return the identity operator, which returns its argument}
     *
     * @param <T> the argument type
     * @param <E> the exception type
     */
    static <T, E extends Exception> ExceptionUnaryOperator<T, E> identity() {
        return t -> t;
    }
}
