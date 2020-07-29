package io.smallrye.common.version;

final class JDKSpecific {
    private JDKSpecific() {
    }

    static boolean hasJpms() {
        return false;
    }

    static int compareJpms(String v1, String v2) {
        throw new UnsupportedOperationException("Java 9 only");
    }
}
