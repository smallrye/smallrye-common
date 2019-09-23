package io.smallrye.common.expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.function.ExceptionBiConsumer;

/**
 * A compiled property-expansion expression string. An expression string is a mix of plain strings and expression
 * segments, which are wrapped by the sequence "{@code ${ ... }}".
 */
public final class Expression {
    private final Node content;
    private final Set<String> referencedStrings;

    Expression(Node content) {
        this.content = content;
        HashSet<String> strings = new HashSet<>();
        content.catalog(strings);
        referencedStrings = strings.isEmpty() ? Collections.emptySet()
                : strings.size() == 1 ? Collections.singleton(strings.iterator().next()) : Collections.unmodifiableSet(strings);
    }

    /**
     * Get the immutable set of string keys that are referenced by expressions in this compiled expression. If there
     * are no expansions in this expression, the set is empty. Note that this will <em>not</em> include any string keys
     * that themselves contain expressions, in the case that {@link Flag#NO_RECURSE_KEY} was not specified.
     *
     * @return the immutable set of strings (not {@code null})
     */
    public Set<String> getReferencedStrings() {
        return referencedStrings;
    }

    /**
     * Evaluate the expression with the given expansion function, which may throw a checked exception. The given "function"
     * is a predicate which returns {@code true} if the expansion succeeded or {@code false} if it failed (in which case
     * a default value may be used). If expansion succeeds, the expansion function should append the result to the
     * given {@link StringBuilder}.
     *
     * @param expandFunction the expansion function to apply (must not be {@code null})
     * @param <E> the exception type thrown by the expansion function
     * @return the expanded string
     * @throws E if the expansion function throws an exception
     */
    public <E extends Exception> String evaluateException(
            final ExceptionBiConsumer<ResolveContext<E>, StringBuilder, E> expandFunction) throws E {
        Assert.checkNotNullParam("expandFunction", expandFunction);
        final StringBuilder b = new StringBuilder();
        content.emit(new ResolveContext<E>(expandFunction, b), expandFunction);
        return b.toString();
    }

    /**
     * Evaluate the expression with the given expansion function. The given "function"
     * is a predicate which returns {@code true} if the expansion succeeded or {@code false} if it failed (in which case
     * a default value may be used). If expansion succeeds, the expansion function should append the result to the
     * given {@link StringBuilder}.
     *
     * @param expandFunction the expansion function to apply (must not be {@code null})
     * @return the expanded string
     */
    public String evaluate(BiConsumer<ResolveContext<RuntimeException>, StringBuilder> expandFunction) {
        return evaluateException(expandFunction::accept);
    }

    /**
     * Evaluate the expression using a default expansion function that evaluates system and environment properties
     * in the JBoss style (i.e. using the prefix {@code "env."} to designate an environment property).
     * The caller must have all required security manager permissions.
     *
     * @param failOnNoDefault {@code true} to throw an {@link IllegalArgumentException} if an unresolvable key has no
     *        default value; {@code false} to expand such keys to an empty string
     * @return the expanded string
     */
    public String evaluateWithPropertiesAndEnvironment(boolean failOnNoDefault) {
        return evaluate((c, b) -> {
            final String key = c.getKey();
            if (key.startsWith("env.")) {
                final String env = key.substring(4);
                final String val = System.getenv(env);
                if (val == null) {
                    if (failOnNoDefault && !c.hasDefault()) {
                        throw Messages.unresolvedEnvironmentProperty(env);
                    }
                    c.expandDefault();
                } else {
                    b.append(val);
                }
            } else {
                final String val = System.getProperty(key);
                if (val == null) {
                    if (failOnNoDefault && !c.hasDefault()) {
                        throw Messages.unresolvedSystemProperty(key);
                    }
                    c.expandDefault();
                } else {
                    b.append(val);
                }
            }
        });
    }

    /**
     * Evaluate the expression using a default expansion function that evaluates system properties.
     * The caller must have all required security manager permissions.
     *
     * @param failOnNoDefault {@code true} to throw an {@link IllegalArgumentException} if an unresolvable key has no
     *        default value; {@code false} to expand such keys to an empty string
     * @return the expanded string
     */
    public String evaluateWithProperties(boolean failOnNoDefault) {
        return evaluate((c, b) -> {
            final String key = c.getKey();
            final String val = System.getProperty(key);
            if (val == null) {
                if (failOnNoDefault && !c.hasDefault()) {
                    throw Messages.unresolvedSystemProperty(key);
                }
                c.expandDefault();
            } else {
                b.append(val);
            }
        });
    }

    /**
     * Evaluate the expression using a default expansion function that evaluates environment properties.
     * The caller must have all required security manager permissions.
     *
     * @param failOnNoDefault {@code true} to throw an {@link IllegalArgumentException} if an unresolvable key has no
     *        default value; {@code false} to expand such keys to an empty string
     * @return the expanded string
     */
    public String evaluateWithEnvironment(boolean failOnNoDefault) {
        return evaluate((c, b) -> {
            final String key = c.getKey();
            final String val = System.getenv(key);
            if (val == null) {
                if (failOnNoDefault && !c.hasDefault()) {
                    throw Messages.unresolvedEnvironmentProperty(key);
                }
                c.expandDefault();
            } else {
                b.append(val);
            }
        });
    }

    /**
     * Compile an expression string.
     *
     * @param string the expression string (must not be {@code null})
     * @param flags optional flags to apply which affect the compilation
     * @return the compiled expression (not {@code null})
     */
    public static Expression compile(String string, Flag... flags) {
        return compile(string, flags == null || flags.length == 0 ? NO_FLAGS : EnumSet.of(flags[0], flags));
    }

    /**
     * Compile an expression string.
     *
     * @param string the expression string (must not be {@code null})
     * @param flags optional flags to apply which affect the compilation (must not be {@code null})
     * @return the compiled expression (not {@code null})
     */
    public static Expression compile(String string, EnumSet<Flag> flags) {
        Assert.checkNotNullParam("string", string);
        Assert.checkNotNullParam("flags", flags);
        final Node content;
        final Itr itr;
        if (flags.contains(Flag.NO_TRIM)) {
            itr = new Itr(string);
        } else {
            itr = new Itr(string.trim());
        }
        content = parseString(itr, true, false, false, flags);
        return content == Node.NULL ? EMPTY : new Expression(content);
    }

    private static final Expression EMPTY = new Expression(Node.NULL);

    static final class Itr {
        private final String str;
        private int idx;

        Itr(final String str) {
            this.str = str;
        }

        boolean hasNext() {
            return idx < str.length();
        }

        int next() {
            final int idx = this.idx;
            try {
                return str.codePointAt(idx);
            } finally {
                this.idx = str.offsetByCodePoints(idx, 1);
            }
        }

        int prev() {
            final int idx = this.idx;
            try {
                return str.codePointBefore(idx);
            } finally {
                this.idx = str.offsetByCodePoints(idx, -1);
            }
        }

        int getNextIdx() {
            return idx;
        }

        int getPrevIdx() {
            return str.offsetByCodePoints(idx, -1);
        }

        String getStr() {
            return str;
        }

        int peekNext() {
            return str.codePointAt(idx);
        }

        int peekPrev() {
            return str.codePointBefore(idx);
        }

        void rewind(final int newNext) {
            idx = newNext;
        }
    }

    private static Node parseString(Itr itr, final boolean allowExpr, final boolean endOnBrace, final boolean endOnColon,
            final EnumSet<Flag> flags) {
        int ignoreBraceLevel = 0;
        final List<Node> list = new ArrayList<>();
        int start = itr.getNextIdx();
        while (itr.hasNext()) {
            // index of this character
            int idx = itr.getNextIdx();
            int ch = itr.next();
            switch (ch) {
                case '$': {
                    if (!allowExpr) {
                        // TP 1
                        // treat as plain content
                        continue;
                    }
                    // check to see if it's a dangling $
                    if (!itr.hasNext()) {
                        if (!flags.contains(Flag.LENIENT_SYNTAX)) {
                            // TP 2
                            throw invalidExpressionSyntax(itr.getStr(), idx);
                        }
                        // TP 3
                        list.add(new LiteralNode(itr.getStr(), start, itr.getNextIdx()));
                        start = itr.getNextIdx();
                        continue;
                    }
                    // enqueue what we have acquired so far
                    if (idx > start) {
                        // TP 4
                        list.add(new LiteralNode(itr.getStr(), start, idx));
                    }
                    // next char should be an expression starter of some sort
                    idx = itr.getNextIdx();
                    ch = itr.next();
                    switch (ch) {
                        case '{': {
                            // ${
                            boolean general = flags.contains(Flag.GENERAL_EXPANSION) && itr.hasNext() && itr.peekNext() == '{';
                            // consume double-{
                            if (general)
                                itr.next();
                            // set start to the beginning of the key for later
                            start = itr.getNextIdx();
                            // the expression name starts in the next position
                            Node keyNode = parseString(itr, !flags.contains(Flag.NO_RECURSE_KEY), true, true, flags);
                            if (!itr.hasNext()) {
                                if (!flags.contains(Flag.LENIENT_SYNTAX)) {
                                    // TP 5
                                    throw invalidExpressionSyntax(itr.getStr(), itr.getNextIdx());
                                }
                                // TP 6
                                // otherwise treat it as a properly terminated expression
                                list.add(new ExpressionNode(general, keyNode, Node.NULL));
                                start = itr.getNextIdx();
                                continue;
                            } else if (itr.peekNext() == ':') {
                                if (flags.contains(Flag.DOUBLE_COLON) && itr.hasNext() && itr.peekNext() == ':') {
                                    // TP 7
                                    // OK actually the whole thing is really going to be part of the key
                                    // Best approach is, rewind and do it over again, but without end-on-colon
                                    itr.rewind(start);
                                    keyNode = parseString(itr, !flags.contains(Flag.NO_RECURSE_KEY), true, false, flags);
                                    list.add(new ExpressionNode(general, keyNode, Node.NULL));
                                } else {
                                    // TP 8
                                    itr.next(); // consume it
                                    final Node defaultValueNode = parseString(itr, !flags.contains(Flag.NO_RECURSE_DEFAULT),
                                            true, false, flags);
                                    list.add(new ExpressionNode(general, keyNode, defaultValueNode));
                                }
                                // now expect }
                                if (!itr.hasNext()) {
                                    if (!flags.contains(Flag.LENIENT_SYNTAX)) {
                                        // TP 9
                                        throw invalidExpressionSyntax(itr.getStr(), itr.getNextIdx());
                                    }
                                    // TP 10
                                    // otherwise treat it as a properly terminated expression
                                    start = itr.getNextIdx();
                                    continue;
                                } else {
                                    // TP 11
                                    assert itr.peekNext() == '}';
                                    itr.next(); // consume
                                    if (general) {
                                        if (!itr.hasNext()) {
                                            if (!flags.contains(Flag.LENIENT_SYNTAX)) {
                                                // TP 11_1
                                                throw invalidExpressionSyntax(itr.getStr(), itr.getNextIdx());
                                            }
                                            // TP 11_2
                                            // otherwise treat it as a properly terminated expression
                                            start = itr.getNextIdx();
                                            continue;
                                        } else {
                                            if (itr.peekNext() == '}') {
                                                itr.next(); // consume it
                                                // TP 11_3
                                                start = itr.getNextIdx();
                                                continue;
                                            } else {
                                                if (!flags.contains(Flag.LENIENT_SYNTAX)) {
                                                    // TP 11_4
                                                    throw invalidExpressionSyntax(itr.getStr(), itr.getNextIdx());
                                                }
                                                // otherwise treat it as a properly terminated expression
                                                start = itr.getNextIdx();
                                                continue;
                                            }
                                        }
                                    } else {
                                        start = itr.getNextIdx();
                                        continue;
                                    }
                                    //throw Assert.unreachableCode();
                                }
                            } else {
                                // TP 12
                                assert itr.peekNext() == '}';
                                itr.next(); // consume
                                list.add(new ExpressionNode(general, keyNode, Node.NULL));
                                if (general) {
                                    if (!itr.hasNext()) {
                                        if (!flags.contains(Flag.LENIENT_SYNTAX)) {
                                            // TP 12_1
                                            throw invalidExpressionSyntax(itr.getStr(), itr.getNextIdx());
                                        }
                                        // TP 12_2
                                        // otherwise treat it as a properly terminated expression
                                        start = itr.getNextIdx();
                                        continue;
                                    } else {
                                        if (itr.peekNext() == '}') {
                                            itr.next(); // consume it
                                            // TP 12_3
                                            start = itr.getNextIdx();
                                            continue;
                                        } else {
                                            if (!flags.contains(Flag.LENIENT_SYNTAX)) {
                                                // TP 12_4
                                                throw invalidExpressionSyntax(itr.getStr(), itr.getNextIdx());
                                            }
                                            // otherwise treat it as a properly terminated expression
                                            start = itr.getNextIdx();
                                            continue;
                                        }
                                    }
                                }
                                start = itr.getNextIdx();
                                continue;
                            }
                            //throw Assert.unreachableCode();
                        }
                        case '$': {
                            // $$
                            if (flags.contains(Flag.MINI_EXPRS)) {
                                // TP 13
                                list.add(new ExpressionNode(false, LiteralNode.DOLLAR, Node.NULL));
                            } else {
                                // just resolve $$ to $
                                // TP 14
                                list.add(LiteralNode.DOLLAR);
                            }
                            start = itr.getNextIdx();
                            continue;
                        }
                        case '}': {
                            // $}
                            if (flags.contains(Flag.MINI_EXPRS)) {
                                // TP 15
                                list.add(new ExpressionNode(false, LiteralNode.CLOSE_BRACE, Node.NULL));
                                start = itr.getNextIdx();
                                continue;
                            } else if (endOnBrace) {
                                if (flags.contains(Flag.LENIENT_SYNTAX)) {
                                    // TP 16
                                    // just treat the $ that we got like plain text, and return
                                    list.add(LiteralNode.DOLLAR);
                                    itr.prev(); // back up to point at } again
                                    return Node.fromList(list);
                                } else {
                                    // TP 17
                                    throw invalidExpressionSyntax(itr.getStr(), idx);
                                }
                            } else {
                                if (flags.contains(Flag.LENIENT_SYNTAX)) {
                                    // TP 18
                                    // just treat $} like plain text
                                    list.add(LiteralNode.DOLLAR);
                                    list.add(LiteralNode.CLOSE_BRACE);
                                    start = itr.getNextIdx();
                                    continue;
                                } else {
                                    // TP 19
                                    throw invalidExpressionSyntax(itr.getStr(), idx);
                                }
                            }
                            //throw Assert.unreachableCode();
                        }
                        case ':': {
                            // $:
                            if (flags.contains(Flag.MINI_EXPRS)) {
                                // $: is an expression
                                // TP 20
                                list.add(new ExpressionNode(false, LiteralNode.COLON, Node.NULL));
                                start = itr.getNextIdx();
                                continue;
                            } else if (endOnColon) {
                                if (flags.contains(Flag.LENIENT_SYNTAX)) {
                                    // TP 21
                                    // just treat the $ that we got like plain text, and return
                                    itr.prev(); // back up to point at : again
                                    list.add(LiteralNode.DOLLAR);
                                    return Node.fromList(list);
                                } else {
                                    // TP 22
                                    throw invalidExpressionSyntax(itr.getStr(), idx);
                                }
                            } else {
                                if (flags.contains(Flag.LENIENT_SYNTAX)) {
                                    // TP 23
                                    // just treat $: like plain text
                                    list.add(LiteralNode.DOLLAR);
                                    list.add(LiteralNode.COLON);
                                    start = itr.getNextIdx();
                                    continue;
                                } else {
                                    // TP 24
                                    throw invalidExpressionSyntax(itr.getStr(), idx);
                                }
                            }
                            //throw Assert.unreachableCode();
                        }
                        default: {
                            // $ followed by anything else
                            if (flags.contains(Flag.MINI_EXPRS)) {
                                // TP 25
                                list.add(new ExpressionNode(false, new LiteralNode(itr.getStr(), idx, itr.getNextIdx()),
                                        Node.NULL));
                                start = itr.getNextIdx();
                                continue;
                            } else if (flags.contains(Flag.LENIENT_SYNTAX)) {
                                // TP 26
                                // just treat it as literal
                                start = itr.getPrevIdx() - 1; // we can use 1 here because unicode '$' is one char in size
                                continue;
                            } else {
                                // TP 27
                                throw invalidExpressionSyntax(itr.getStr(), idx);
                            }
                            //throw Assert.unreachableCode();
                        }
                    }
                    //throw Assert.unreachableCode();
                }
                case ':': {
                    if (endOnColon) {
                        // TP 28
                        itr.prev(); // back up to point at : again
                        if (idx > start) {
                            list.add(new LiteralNode(itr.getStr(), start, idx));
                        }
                        return Node.fromList(list);
                    } else {
                        // TP 29
                        // plain content always
                        continue;
                    }
                    //throw Assert.unreachableCode();
                }
                case '{': {
                    if (!flags.contains(Flag.NO_SMART_BRACES)) {
                        // TP 1.2
                        ignoreBraceLevel++;
                    }
                    // TP 1.3
                    continue;
                }
                case '}': {
                    if (!flags.contains(Flag.NO_SMART_BRACES) && ignoreBraceLevel > 0) {
                        // TP 1.1
                        ignoreBraceLevel--;
                        continue;
                    } else if (endOnBrace) {
                        // TP 30
                        itr.prev(); // back up to point at } again
                        // TP 46 // allow an empty default value
                        if (idx >= start) {
                            list.add(new LiteralNode(itr.getStr(), start, idx));
                        }
                        return Node.fromList(list);
                    } else {
                        // TP 31
                        // treat as plain content
                        continue;
                    }
                    //throw Assert.unreachableCode();
                }
                case '\\': {
                    if (flags.contains(Flag.ESCAPES)) {
                        if (idx > start) {
                            list.add(new LiteralNode(itr.getStr(), start, idx));
                            start = idx;
                        }
                        if (!itr.hasNext()) {
                            if (flags.contains(Flag.LENIENT_SYNTAX)) {
                                // just treat it like plain content
                                // TP 33
                                continue;
                            } else {
                                // TP 34
                                throw invalidExpressionSyntax(itr.getStr(), idx);
                            }
                        } else {
                            ch = itr.next();
                            final LiteralNode node;
                            switch (ch) {
                                case 'n': {
                                    // TP 35
                                    node = LiteralNode.NEWLINE;
                                    break;
                                }
                                case 'r': {
                                    // TP 36
                                    node = LiteralNode.CARRIAGE_RETURN;
                                    break;
                                }
                                case 't': {
                                    // TP 37
                                    node = LiteralNode.TAB;
                                    break;
                                }
                                case 'b': {
                                    // TP 38
                                    node = LiteralNode.BACKSPACE;
                                    break;
                                }
                                case 'f': {
                                    // TP 39
                                    node = LiteralNode.FORM_FEED;
                                    break;
                                }
                                case '\\': {
                                    // TP 45
                                    node = LiteralNode.BACKSLASH;
                                    break;
                                }
                                default: {
                                    if (flags.contains(Flag.LENIENT_SYNTAX)) {
                                        // TP 40
                                        // just append the literal character after the \, whatever it was
                                        start = itr.getPrevIdx();
                                        continue;
                                    }
                                    // TP 41
                                    throw invalidExpressionSyntax(itr.getStr(), idx);
                                }
                            }
                            list.add(node);
                            start = itr.getNextIdx();
                            continue;
                        }
                    }
                    // TP 42
                    // otherwise, just...
                    continue;
                }
                default: {
                    // TP 43
                    // treat as plain content
                    //noinspection UnnecessaryContinue
                    continue;
                }
            }
            //throw Assert.unreachableCode();
        }
        final int length = itr.getStr().length();
        if (length > start) {
            // TP 44
            list.add(new LiteralNode(itr.getStr(), start, length));
        }
        return Node.fromList(list);
    }

    private static IllegalArgumentException invalidExpressionSyntax(final String string, final int index) {
        String msg = Messages.invalidExpressionSyntax(index);
        StringBuilder b = new StringBuilder(msg.length() + string.length() + string.length() + 5);
        b.append(msg);
        b.append('\n').append('\t').append(string);
        b.append('\n').append('\t');
        for (int i = 0; i < index; i = string.offsetByCodePoints(i, 1)) {
            final int cp = string.codePointAt(i);
            if (Character.isWhitespace(cp)) {
                b.append(cp);
            } else if (Character.isValidCodePoint(cp) && !Character.isISOControl(cp)) {
                b.append(' ');
            }
        }
        b.append('^');
        return new IllegalArgumentException(b.toString());
    }

    private static final EnumSet<Flag> NO_FLAGS = EnumSet.noneOf(Flag.class);

    /**
     * Flags that can apply to a property expression compilation
     */
    public enum Flag {
        /**
         * Do not trim leading and trailing whitespace off of the expression string before parsing it.
         */
        NO_TRIM,
        /**
         * Ignore syntax problems instead of throwing an exception.
         */
        LENIENT_SYNTAX,
        /**
         * Support single-character expressions that can be interpreted without wrapping in curly braces.
         */
        MINI_EXPRS,
        /**
         * Do not support recursive expression expansion in the key part of the expression.
         */
        NO_RECURSE_KEY,
        /**
         * Do not support recursion in default values.
         */
        NO_RECURSE_DEFAULT,
        /**
         * Do not support smart braces.
         */
        NO_SMART_BRACES,
        /**
         * Support {@code Policy} file style "general" expansion alternate expression syntax. "Smart" braces
         * will only work if the opening brace is not the first character in the expression key.
         */
        GENERAL_EXPANSION,
        /**
         * Support standard escape sequences in plain text and default value fields, which begin with a backslash ("{@code \}")
         * character.
         */
        ESCAPES,
        /**
         * Treat expressions containing a double-colon delimiter as special, encoding the entire content into the key.
         */
        DOUBLE_COLON,
    }
}
