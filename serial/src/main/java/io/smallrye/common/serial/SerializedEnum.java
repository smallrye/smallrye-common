package io.smallrye.common.serial;

import io.smallrye.common.constraint.Assert;

/**
 * The serialized form of a Java {@code enum} constant.
 */
public final class SerializedEnum extends Serialized {
    private final SerializedEnumClass enumClass;
    private final Serialized constantName;

    /**
     * Construct a new instance.
     *
     * @param enumClass the class descriptor of the enum type (must not be {@code null})
     * @param constantName the serialized name of the enum constant (must not be {@code null})
     */
    public SerializedEnum(final SerializedEnumClass enumClass, final Serialized constantName) {
        this.enumClass = Assert.checkNotNullParam("enumClass", enumClass);
        this.constantName = Assert.checkNotNullParam("constantName", constantName);
    }

    /**
     * {@return the class descriptor of the enum type (not {@code null})}
     */
    public SerializedEnumClass enumClass() {
        return enumClass;
    }

    /**
     * {@return the serialized name of the enum constant (not {@code null})}
     */
    public Serialized constantName() {
        return constantName;
    }
}
