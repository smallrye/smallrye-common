package io.smallrye.common.constraint;

final class Messages {
    private Messages() {
    }

    static IllegalArgumentException nullParam(final String name) {
        return new IllegalArgumentException(String.format("Parameter '%s' may not be null", name));
    }

    static IllegalArgumentException nullArrayParam(final int index, final String name) {
        return new IllegalArgumentException(
                String.format("Array index %d of parameter '%s' may not be null", Integer.valueOf(index), name));
    }

    static IllegalArgumentException emptyParam(final String name) {
        return new IllegalArgumentException(String.format("Parameter '%s' must not be empty", name));
    }

    static IllegalArgumentException paramLessThan(final String name, final Object min) {
        return new IllegalArgumentException(String.format("Parameter '%s' must not be less than '%s'", name, min));
    }

    static IllegalArgumentException paramLessThan(final String name, final int min) {
        return paramLessThan(name, Integer.valueOf(min));
    }

    static IllegalArgumentException paramLessThan(final String name, final long min) {
        return paramLessThan(name, Long.valueOf(min));
    }

    static IllegalArgumentException paramLessThan(final String name, final float min) {
        return paramLessThan(name, Float.valueOf(min));
    }

    static IllegalArgumentException paramLessThan(final String name, final double min) {
        return paramLessThan(name, Double.valueOf(min));
    }

    static IllegalArgumentException paramGreaterThan(final String name, final Object max) {
        return new IllegalArgumentException(String.format("Parameter '%s' must not be greater than '%s'", name, max));
    }

    static IllegalArgumentException paramGreaterThan(final String name, final int max) {
        return paramGreaterThan(name, Integer.valueOf(max));
    }

    static IllegalArgumentException paramGreaterThan(final String name, final long max) {
        return paramGreaterThan(name, Long.valueOf(max));
    }

    static IllegalArgumentException paramGreaterThan(final String name, final float max) {
        return paramGreaterThan(name, Float.valueOf(max));
    }

    static IllegalArgumentException paramGreaterThan(final String name, final double max) {
        return paramGreaterThan(name, Double.valueOf(max));
    }

    static ArrayIndexOutOfBoundsException arrayOffsetGreaterThanLength(final int offs, final int arrayLength) {
        return new ArrayIndexOutOfBoundsException(
                String.format("Given offset of %d is greater than array length of %d",
                        Integer.valueOf(offs),
                        Integer.valueOf(arrayLength)));
    }

    static ArrayIndexOutOfBoundsException arrayOffsetLengthGreaterThanLength(final int offs, final int len,
            final int arrayLength) {
        return new ArrayIndexOutOfBoundsException(
                String.format("Given offset of %d plus length of %d is greater than array length of %d",
                        Integer.valueOf(offs),
                        Integer.valueOf(len),
                        Integer.valueOf(arrayLength)));
    }

    static String unexpectedNullValue() {
        return "Internal error: Assertion failure: Unexpectedly null value";
    }

    static String expectedLockHold(Object monitor) {
        return String.format("Internal error: Assertion failure: Current thread expected to hold lock for %s", monitor);
    }

    static String expectedLockNotHold(Object monitor) {
        return String.format("Internal error: Assertion failure: Current thread expected to not hold lock for %s", monitor);
    }

    static String expectedBoolean(boolean expr) {
        return String.format("Internal error: Assertion failure: Expected boolean value to be %s", Boolean.valueOf(expr));
    }

    static IllegalStateException unreachableCode() {
        return new IllegalStateException("Internal error: Unreachable code has been reached");
    }

    static IllegalStateException impossibleSwitchCase(Object val) {
        return new IllegalStateException(String.format("Internal error: Impossible switch condition encountered: %s", val));
    }

    static UnsupportedOperationException unsupported(final String methodName, final String className) {
        return new UnsupportedOperationException(
                String.format("Method \"%s\" of class \"%s\" is not implemented", methodName, className));
    }
}
