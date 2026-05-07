package io.smallrye.common.serial;

/**
 * The serialized representation of a built-in (platform or application) class loader.
 * These class loaders are singletons within a JVM, so they are represented as
 * singleton instances rather than by name.
 */
public final class SerializedBuiltInClassLoader extends Serialized {
    private static final SerializedBuiltInClassLoader PLATFORM = new SerializedBuiltInClassLoader(Kind.PLATFORM);
    private static final SerializedBuiltInClassLoader APP = new SerializedBuiltInClassLoader(Kind.APP);

    private final Kind kind;

    private SerializedBuiltInClassLoader(final Kind kind) {
        this.kind = kind;
    }

    /**
     * {@return the class loader represented by this serialized form (not {@code null})}
     */
    public ClassLoader classLoader() {
        return kind.classLoader;
    }

    /**
     * {@return the singleton instance representing the platform class loader}
     */
    public static SerializedBuiltInClassLoader forPlatformClassLoader() {
        return PLATFORM;
    }

    /**
     * {@return the singleton instance representing the application (system) class loader}
     */
    public static SerializedBuiltInClassLoader forAppClassLoader() {
        return APP;
    }

    /**
     * The kind of built-in class loader.
     */
    public enum Kind {
        /**
         * The platform class loader.
         */
        PLATFORM(ClassLoader.getPlatformClassLoader()),
        /**
         * The application (system) class loader.
         */
        APP(ClassLoader.getSystemClassLoader()),
        ;

        final ClassLoader classLoader;

        Kind(final ClassLoader classLoader) {
            this.classLoader = classLoader;
        }
    }
}
