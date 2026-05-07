package io.smallrye.common.serial.impl;

import java.lang.invoke.MethodHandle;

import sun.reflect.ReflectionFactory;

@SuppressWarnings("unused") // MR JAR layer
final class JDK24Specific {
    // Much simpler JDK24+ version! Remove once the baseline is 24 or later.
    private static final ReflectionFactory rf = ReflectionFactory.getReflectionFactory();

    private JDK24Specific() {
    }

    static MethodHandle defaultWriteObjectForSerialization(final Class<?> type) {
        return rf.defaultWriteObjectForSerialization(type);
    }

    static MethodHandle defaultReadObjectForSerialization(final Class<?> type) {
        return rf.defaultReadObjectForSerialization(type);
    }
}
