package io.smallrye.common.expression;

import static java.lang.invoke.MethodHandles.lookup;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = "SRCOM", length = 5)
interface Messages {
    Messages msg = org.jboss.logging.Messages.getBundle(lookup(), Messages.class);

    @Message(id = 1000, value = "Invalid expression syntax at position %d")
    String invalidExpressionSyntax(int index);

    @Message(id = 1001, value = "No environment property found named \"%s\"")
    IllegalArgumentException unresolvedEnvironmentProperty(String name);

    @Message(id = 1002, value = "No system property found named \"%s\"")
    IllegalArgumentException unresolvedSystemProperty(String name);
}
