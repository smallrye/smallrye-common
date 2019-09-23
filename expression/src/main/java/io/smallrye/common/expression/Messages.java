package io.smallrye.common.expression;

final class Messages {
    private Messages() {
    }

    static String invalidExpressionSyntax(final int index) {
        return String.format("Invalid expression syntax at position %d", Integer.valueOf(index));
    }

    static IllegalArgumentException unresolvedEnvironmentProperty(final String name) {
        return new IllegalArgumentException(String.format("No environment property found named \"%s\"", name));
    }

    static IllegalArgumentException unresolvedSystemProperty(final String name) {
        return new IllegalArgumentException(String.format("No system property found named \"%s\"", name));
    }
}
