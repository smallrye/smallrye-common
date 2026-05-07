package io.smallrye.common.serial;

/**
 * Abstract base for class descriptors that carry a serial version UID.
 * This includes {@linkplain SerializedExternalizableClass externalizable},
 * {@linkplain SerializedEnumClass enum}, and {@linkplain SerializedFieldedClass fielded} classes.
 */
public abstract sealed class SerializedVersionedClass extends SerializedClass
        permits SerializedExternalizableClass, SerializedEnumClass, SerializedFieldedClass {

    private final long uid;

    /**
     * Construct a new instance.
     *
     * @param name the class name (must not be {@code null})
     * @param classLoader the serialized class loader (must not be {@code null} but may be {@link SerializedNull#INSTANCE})
     * @param uid the serial version UID
     */
    SerializedVersionedClass(final String name, final Serialized classLoader, final long uid) {
        super(name, classLoader);
        this.uid = uid;
    }

    /**
     * {@return the serial version UID of the stream class}
     */
    public long serialVersionUID() {
        return uid;
    }
}
