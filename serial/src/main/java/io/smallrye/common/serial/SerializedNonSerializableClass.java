package io.smallrye.common.serial;

/**
 * The serialized representation of a class that does not implement {@link java.io.Serializable}.
 * Non-serializable class descriptors carry only a name and class loader.
 */
public final class SerializedNonSerializableClass extends SerializedClass {

    /**
     * Construct a new instance.
     *
     * @param name the class name (must not be {@code null})
     * @param classLoader the serialized class loader (must not be {@code null} but may be {@link SerializedNull#INSTANCE})
     */
    public SerializedNonSerializableClass(final String name, final Serialized classLoader) {
        super(name, classLoader);
    }
}
