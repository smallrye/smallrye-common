package io.smallrye.common.constraint;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = "SRCOM", length = 5)
interface Messages {
    Messages log = org.jboss.logging.Messages.getBundle(Messages.class);

    @Message(id = 0, value = "Parameter '%s' may not be null")
    IllegalArgumentException nullParam(String paramName);

    @Message(id = 1, value = "Parameter '%s' may not be less than %s")
    IllegalArgumentException paramLessThan(String name, long min);

    IllegalArgumentException paramLessThan(String name, double min);

    IllegalArgumentException paramLessThan(String name, Object min);

    @Message(id = 2, value = "Parameter '%s' may not be greater than than %s")
    IllegalArgumentException paramGreaterThan(String name, long max);

    IllegalArgumentException paramGreaterThan(String name, double max);

    IllegalArgumentException paramGreaterThan(String name, Object max);

    @Message(id = 3, value = "Given offset of %d is greater than array length of %d")
    ArrayIndexOutOfBoundsException arrayOffsetGreaterThanLength(int offs, int arrayLength);

    @Message(id = 4, value = "Given offset of %d plus length of %d is greater than array length of %d")
    ArrayIndexOutOfBoundsException arrayOffsetLengthGreaterThanLength(int offs, int len, int arrayLength);

    @Message(id = 5, value = "Array index %d of parameter '%s' may not be null")
    IllegalArgumentException nullArrayParam(int index, String name);

    @Message(id = 6, value = "Parameter '%s' must not be empty")
    IllegalArgumentException emptyParam(String name);

    @Message(id = 7, value = "Internal error: Assertion failure: Unexpectedly null value")
    String unexpectedNullValue();

    @Message(id = 8, value = "Internal error: Assertion failure: Current thread expected to hold lock for %s")
    String expectedLockHold(Object monitor);

    @Message(id = 9, value = "Internal error: Assertion failure: Current thread expected to not hold lock for %s")
    String expectedLockNotHold(Object monitor);

    @Message(id = 10, value = "Internal error: Assertion failure: Expected boolean value to be %s")
    String expectedBoolean(boolean expr);

    @Message(id = 11, value = "Internal error: Unreachable code has been reached")
    IllegalStateException unreachableCode();

    @Message(id = 12, value = "Internal error: Impossible switch condition encountered: %s")
    IllegalStateException impossibleSwitchCase(Object cond);

    @Message(id = 13, value = "Method \"%s\" of class \"%s\" is not supported")
    UnsupportedOperationException unsupported(String methodName, String className);

    @Message(id = 14, value = "Parameter '%s' must be a power of two")
    IllegalArgumentException paramNotPow2(String name);
}
