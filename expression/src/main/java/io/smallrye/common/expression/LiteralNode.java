package io.smallrye.common.expression;

import java.io.File;
import java.util.HashSet;

import io.smallrye.common.function.ExceptionBiConsumer;

class LiteralNode extends Node {
    static final LiteralNode DOLLAR = new LiteralNode("$");
    static final LiteralNode CLOSE_BRACE = new LiteralNode("}");
    static final LiteralNode FILE_SEP = new LiteralNode(File.separator);
    static final LiteralNode COLON = new LiteralNode(":");
    static final LiteralNode NEWLINE = new LiteralNode("\n");
    static final LiteralNode CARRIAGE_RETURN = new LiteralNode("\r");
    static final LiteralNode TAB = new LiteralNode("\t");
    static final LiteralNode BACKSPACE = new LiteralNode("\b");
    static final LiteralNode FORM_FEED = new LiteralNode("\f");
    static final LiteralNode BACKSLASH = new LiteralNode("\\");

    private final String literalValue;
    private final int start;
    private final int end;
    private String toString;

    LiteralNode(final String literalValue, final int start, final int end) {
        this.literalValue = literalValue;
        this.start = start;
        this.end = end;
    }

    LiteralNode(final String literalValue) {
        this(literalValue, 0, literalValue.length());
    }

    <E extends Exception> void emit(final ResolveContext<E> context,
            final ExceptionBiConsumer<ResolveContext<E>, StringBuilder, E> resolveFunction) throws E {
        context.getStringBuilder().append(literalValue, start, end);
    }

    void catalog(final HashSet<String> strings) {
    }

    public String toString() {
        final String toString = this.toString;
        return toString != null ? toString : (this.toString = literalValue.substring(start, end));
    }
}
