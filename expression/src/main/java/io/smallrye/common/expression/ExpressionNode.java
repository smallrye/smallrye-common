package io.smallrye.common.expression;

import java.util.HashSet;

import io.smallrye.common.function.ExceptionBiConsumer;

class ExpressionNode extends Node {
    private final boolean generalExpression;
    private final Node key;
    private final Node defaultValue;

    ExpressionNode(final boolean generalExpression, final Node key, final Node defaultValue) {
        this.generalExpression = generalExpression;
        this.key = key;
        this.defaultValue = defaultValue;
    }

    <E extends Exception> void emit(final ResolveContext<E> context,
            final ExceptionBiConsumer<ResolveContext<E>, StringBuilder, E> resolveFunction) throws E {
        ExpressionNode oldCurrent = context.setCurrent(this);
        try {
            resolveFunction.accept(context, context.getStringBuilder());
        } finally {
            context.setCurrent(oldCurrent);
        }
    }

    void catalog(final HashSet<String> strings) {
        if (key instanceof LiteralNode) {
            strings.add(key.toString());
        } else {
            key.catalog(strings);
        }
        defaultValue.catalog(strings);
    }

    boolean isGeneralExpression() {
        return generalExpression;
    }

    Node getKey() {
        return key;
    }

    Node getDefaultValue() {
        return defaultValue;
    }

    public String toString() {
        return String.format("Expr<%s:%s>", key, defaultValue);
    }
}
