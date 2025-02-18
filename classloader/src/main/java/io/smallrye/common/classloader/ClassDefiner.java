package io.smallrye.common.classloader;

import java.lang.invoke.MethodHandles;

/**
 * A utility to define classes within a target lookup.
 */
@SuppressWarnings("removal")
public class ClassDefiner {
    private ClassDefiner() {
    }

    /**
     * Define a class.
     *
     * @param lookup the lookup of the class (must not be {@code null})
     * @param parent the host class to define the new class to (must not be {@code null})
     * @param className the name of the new class (must not be {@code null})
     * @param classBytes the bytes of the new class (must not be {@code null})
     * @return the defined class (not {@code null})
     */
    public static Class<?> defineClass(MethodHandles.Lookup lookup, Class<?> parent, String className, byte[] classBytes) {
        try {
            MethodHandles.Lookup privateLookupIn = MethodHandles.privateLookupIn(parent, lookup);
            return privateLookupIn.defineClass(classBytes);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }
}
