package io.smallrye.common.serial;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.serial.spi.ObjectSerializer;

/**
 * The serialized representation of an array of objects.
 */
public final class SerializedObjectArray extends SerializedArray {
    private final SerializedArrayClass arrayType;
    private final Serialized[] array;

    /**
     * Construct a new instance from pre-existing serialized elements.
     * This constructor is intended for wire protocol readers and manual graph construction.
     *
     * @param arrayType the type of the array (must not be {@code null})
     * @param array the array of serialized objects (must not be {@code null})
     */
    public SerializedObjectArray(final SerializedArrayClass arrayType, final Serialized[] array) {
        this.arrayType = Assert.checkNotNullParam("arrayType", arrayType);
        this.array = array.clone();
    }

    /**
     * Construct a new instance during serialization by capturing elements from a live array.
     * Pre-registers this instance in the identity map before serializing elements, so that
     * circular references (e.g., an array containing itself) are handled correctly.
     *
     * @param source the source object array (must not be {@code null})
     * @param ctxt the serializer context (must not be {@code null})
     */
    SerializedObjectArray(final Object[] source, final ObjectSerializer.Context ctxt) throws IOException {
        this.arrayType = (SerializedArrayClass) ctxt.serialize(source.getClass());
        this.array = new Serialized[source.length];
        ctxt.preSetSerialized(this);
        for (int i = 0; i < source.length; i++) {
            this.array[i] = ctxt.serialize(source[i]);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Serialized[] asArray() {
        return array.clone();
    }

    /**
     * {@return the length of the serialized array}
     */
    public int length() {
        return array.length;
    }

    /**
     * {@return the array element with the given index (not {@code null})}
     *
     * @param index the array index, which must be in range
     * @throws ArrayIndexOutOfBoundsException if the array index is not valid
     */
    public Serialized get(int index) {
        return array[index];
    }

    /**
     * {@return the class descriptor for the array type (not {@code null})}
     */
    public SerializedArrayClass arrayType() {
        return arrayType;
    }
}
