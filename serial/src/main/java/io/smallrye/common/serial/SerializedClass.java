package io.smallrye.common.serial;

import io.smallrye.common.constraint.Assert;

/**
 * The serialized representation of a {@link Class}.
 * <p>
 * This is the sealed base of the class descriptor hierarchy. Every class descriptor
 * carries a {@linkplain #name() class name} and a {@linkplain #classLoader() class loader}.
 * Subtypes add data specific to the serialization mechanism of the class they describe.
 *
 * @see SerializedNonSerializableClass
 * @see SerializedVersionedClass
 * @see SerializedArrayClass
 */
public abstract sealed class SerializedClass extends Serialized
        permits SerializedNonSerializableClass, SerializedVersionedClass, SerializedArrayClass {

    private final String name;
    private final Serialized classLoader;

    /**
     * Construct a new instance.
     *
     * @param name the class name (must not be {@code null})
     * @param classLoader the serialized class loader (must not be {@code null} but may be {@link SerializedNull#INSTANCE})
     */
    SerializedClass(final String name, final Serialized classLoader) {
        this.name = Assert.checkNotNullParam("name", name);
        this.classLoader = Assert.checkNotNullParam("classLoader", classLoader);
    }

    /**
     * {@return the name of the serialized class (not {@code null})}
     */
    public String name() {
        return name;
    }

    /**
     * {@return the serialized representation of a class loader which can load this class (not {@code null})}
     */
    public Serialized classLoader() {
        return classLoader;
    }
}
