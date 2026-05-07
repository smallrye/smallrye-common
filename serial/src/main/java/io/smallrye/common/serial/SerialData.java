package io.smallrye.common.serial;

import java.io.ObjectStreamField;
import java.util.List;
import java.util.Map;

import io.smallrye.common.constraint.Assert;

/**
 * Serialization data for one class hierarchy level of one object.
 * <p>
 * In addition to the raw data accessors ({@link #primitiveFieldData()}, {@link #objectFieldData()},
 * {@link #streamData()}), this record provides typed field accessors that look up the field by name
 * in the {@linkplain SerializedFieldedClass#streamFields() stream field layout} of the associated
 * {@link #serializedClass()}, resolve its offset, and read the value from the appropriate buffer.
 */
public record SerialData(
        SerializedFieldedClass serializedClass,
        StreamData.OfBytes primitiveFieldData,
        StreamData.OfObjects objectFieldData,
        List<StreamData> streamData) {

    public SerialData {
        Assert.checkNotNullParam("serializedClass", serializedClass);
        Assert.checkNotNullParam("primitiveFieldData", primitiveFieldData);
        Assert.checkNotNullParam("objectFieldData", objectFieldData);
        streamData = List.copyOf(streamData);
    }

    /**
     * Look up a stream field by name.
     *
     * @param fieldName the field name (must not be {@code null})
     * @return the stream field descriptor (not {@code null})
     * @throws IllegalArgumentException if no field with the given name exists at this class level
     */
    private ObjectStreamField field(String fieldName) {
        Map<String, ObjectStreamField> fields = serializedClass.streamFields();
        ObjectStreamField field = fields.get(Assert.checkNotNullParam("fieldName", fieldName));
        if (field == null) {
            throw new IllegalArgumentException("No field named '" + fieldName + "' in class " + serializedClass.name());
        }
        return field;
    }

    /**
     * Get the value of a {@code boolean} field.
     *
     * @param fieldName the field name (must not be {@code null})
     * @return the field value
     * @throws IllegalArgumentException if no field with the given name exists at this class level
     */
    public boolean getBoolean(String fieldName) {
        return primitiveFieldData.getBoolean(field(fieldName).getOffset());
    }

    /**
     * Get the value of a {@code byte} field.
     *
     * @param fieldName the field name (must not be {@code null})
     * @return the field value
     * @throws IllegalArgumentException if no field with the given name exists at this class level
     */
    public byte getByte(String fieldName) {
        return primitiveFieldData.getByte(field(fieldName).getOffset());
    }

    /**
     * Get the value of a {@code char} field.
     *
     * @param fieldName the field name (must not be {@code null})
     * @return the field value
     * @throws IllegalArgumentException if no field with the given name exists at this class level
     */
    public char getChar(String fieldName) {
        return primitiveFieldData.getChar(field(fieldName).getOffset());
    }

    /**
     * Get the value of a {@code short} field.
     *
     * @param fieldName the field name (must not be {@code null})
     * @return the field value
     * @throws IllegalArgumentException if no field with the given name exists at this class level
     */
    public short getShort(String fieldName) {
        return primitiveFieldData.getShort(field(fieldName).getOffset());
    }

    /**
     * Get the value of an {@code int} field.
     *
     * @param fieldName the field name (must not be {@code null})
     * @return the field value
     * @throws IllegalArgumentException if no field with the given name exists at this class level
     */
    public int getInt(String fieldName) {
        return primitiveFieldData.getInt(field(fieldName).getOffset());
    }

    /**
     * Get the value of a {@code long} field.
     *
     * @param fieldName the field name (must not be {@code null})
     * @return the field value
     * @throws IllegalArgumentException if no field with the given name exists at this class level
     */
    public long getLong(String fieldName) {
        return primitiveFieldData.getLong(field(fieldName).getOffset());
    }

    /**
     * Get the value of a {@code float} field.
     *
     * @param fieldName the field name (must not be {@code null})
     * @return the field value
     * @throws IllegalArgumentException if no field with the given name exists at this class level
     */
    public float getFloat(String fieldName) {
        return primitiveFieldData.getFloat(field(fieldName).getOffset());
    }

    /**
     * Get the value of a {@code double} field.
     *
     * @param fieldName the field name (must not be {@code null})
     * @return the field value
     * @throws IllegalArgumentException if no field with the given name exists at this class level
     */
    public double getDouble(String fieldName) {
        return primitiveFieldData.getDouble(field(fieldName).getOffset());
    }

    /**
     * Get the value of an object (reference-typed) field.
     *
     * @param fieldName the field name (must not be {@code null})
     * @return the serialized field value (not {@code null}; may be {@link SerializedNull})
     * @throws IllegalArgumentException if no field with the given name exists at this class level
     */
    public Serialized getObject(String fieldName) {
        return objectFieldData.getObject(field(fieldName).getOffset());
    }
}
