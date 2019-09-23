package io.smallrye.common.expression;

import io.smallrye.common.function.ExceptionBiConsumer;

/**
 * The expression resolve context, which can be used to query the current expression key, write out expansions or
 * default values, or perform validation.
 * <p>
 * The expression context is not thread-safe and is not valid outside of the property expansion function body.
 *
 * @param <E> the exception type that can be thrown by the expansion function
 */
public final class ResolveContext<E extends Exception> {
    private final ExceptionBiConsumer<ResolveContext<E>, StringBuilder, E> function;
    private StringBuilder builder;
    private ExpressionNode current;

    ResolveContext(final ExceptionBiConsumer<ResolveContext<E>, StringBuilder, E> function, final StringBuilder builder) {
        this.function = function;
        this.builder = builder;
    }

    /**
     * Get the expression resolution key, as a string. If the key contains an expression, it will have been expanded
     * unless {@link Expression.Flag#NO_RECURSE_KEY} was given.
     * The result is not cached and will be re-expanded every time this method is called.
     *
     * @return the expanded key (not {@code null})
     * @throws E if the recursive expansion threw an exception
     */
    public String getKey() throws E {
        if (current == null)
            throw new IllegalStateException();
        final Node key = current.getKey();
        if (key instanceof LiteralNode) {
            return key.toString();
        } else if (key == Node.NULL) {
            return "";
        }
        final StringBuilder b = new StringBuilder();
        emitToBuilder(b, key);
        return b.toString();
    }

    /**
     * Expand the default value to the given string builder. If the default value contains an expression, it will
     * have been expanded unless {@link Expression.Flag#NO_RECURSE_DEFAULT} was given.
     * The result is not cached and will be re-expanded every time this method is called.
     *
     * @param target the string builder target
     * @throws E if the recursive expansion threw an exception
     */
    public void expandDefault(StringBuilder target) throws E {
        if (current == null)
            throw new IllegalStateException();
        emitToBuilder(target, current.getDefaultValue());
    }

    private void emitToBuilder(final StringBuilder target, final Node node) throws E {
        if (node == Node.NULL) {
            return;
        } else if (node instanceof LiteralNode) {
            target.append(node.toString());
            return;
        } else {
            final StringBuilder old = builder;
            try {
                builder = target;
                node.emit(this, function);
            } finally {
                builder = old;
            }
        }
    }

    /**
     * Expand the default value to the current target string builder. If the default value contains an expression, it will
     * have been expanded unless {@link Expression.Flag#NO_RECURSE_DEFAULT} was given.
     * The result is not cached and will be re-expanded every time this method is called.
     *
     * @throws E if the recursive expansion threw an exception
     */
    public void expandDefault() throws E {
        expandDefault(builder);
    }

    /**
     * Expand the default value to a string. If the default value contains an expression, it will
     * have been expanded unless {@link Expression.Flag#NO_RECURSE_DEFAULT} was given.
     * The result is not cached and will be re-expanded every time this method is called.
     *
     * @return the expanded string (not {@code null})
     * @throws E if the recursive expansion threw an exception
     */
    public String getExpandedDefault() throws E {
        if (current == null)
            throw new IllegalStateException();
        final Node defaultValue = current.getDefaultValue();
        if (defaultValue instanceof LiteralNode) {
            return defaultValue.toString();
        } else if (defaultValue == Node.NULL) {
            return "";
        }
        final StringBuilder b = new StringBuilder();
        emitToBuilder(b, defaultValue);
        return b.toString();
    }

    /**
     * Determine if the current expression has a default value.
     *
     * @return {@code true} if there is a default value, {@code false} otherwise
     */
    public boolean hasDefault() {
        return current.getDefaultValue() != Node.NULL;
    }

    StringBuilder getStringBuilder() {
        return builder;
    }

    ExpressionNode setCurrent(final ExpressionNode current) {
        try {
            return this.current;
        } finally {
            this.current = current;
        }
    }
}
