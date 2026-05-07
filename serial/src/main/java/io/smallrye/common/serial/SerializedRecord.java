package io.smallrye.common.serial;

import java.io.IOException;
import java.io.ObjectStreamField;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Map;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.serial.impl.ClassLocal;
import io.smallrye.common.serial.impl.Util;
import io.smallrye.common.serial.spi.ObjectSerializer;

/**
 * The serialized representation of a {@code record} instance.
 * <p>
 * Unlike {@link SerializedSerializable}, records have exactly one level of field data
 * (no superclass chain), no custom {@code writeObject}/{@code readObject} methods,
 * and no ancillary stream data. On deserialization, records are reconstructed via their
 * canonical constructor.
 */
public final class SerializedRecord extends Serialized {

    /**
     * Cached record component accessor handles, keyed by record class.
     * The handles are parallel to the array returned by {@link Class#getRecordComponents()}.
     */
    static final ClassLocal<MethodHandle[]> RECORD_ACCESSORS = new ClassLocal<>(type -> {
        RecordComponent[] components = type.getRecordComponents();
        MethodHandle[] handles = new MethodHandle[components.length];
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
            for (int i = 0; i < components.length; i++) {
                handles[i] = lookup.unreflect(components[i].getAccessor());
            }
        } catch (IllegalAccessException e) {
            throw Util.asError(e);
        }
        return handles;
    });

    private final SerializedRecordClass recordClass;
    private final SerialData fieldData;

    /**
     * Construct a new instance from pre-existing serialization data.
     * This constructor is intended for wire protocol readers and manual graph construction.
     *
     * @param recordClass the class descriptor for the record (must not be {@code null})
     * @param fieldData the single level of field data (must not be {@code null})
     */
    public SerializedRecord(final SerializedRecordClass recordClass, final SerialData fieldData) {
        this.recordClass = Assert.checkNotNullParam("recordClass", recordClass);
        this.fieldData = Assert.checkNotNullParam("fieldData", fieldData);
    }

    /**
     * Construct a new instance during serialization by capturing data from a live record.
     * Reads component values directly via cached method handle accessors, bypassing the
     * {@code defaultWriteObject} mechanism which is not available for record classes.
     * Pre-registers this instance for circular reference handling before capturing field data.
     *
     * @param record the record being serialized (must not be {@code null})
     * @param ctxt the serializer context (must not be {@code null})
     * @throws IOException if serialization fails
     */
    SerializedRecord(final Object record, final ObjectSerializer.Context ctxt) throws IOException {
        this.recordClass = (SerializedRecordClass) ctxt.serialize(record.getClass());
        ctxt.preSetSerialized(this);

        Class<?> recordType = record.getClass();
        Map<String, ObjectStreamField> fields = recordClass.streamFields();
        int primSize = recordClass.primitiveBufferSize();
        int objSize = recordClass.objectBufferSize();
        byte[] primData = primSize == 0 ? null : new byte[primSize];
        Serialized[] objData = objSize == 0 ? null : new Serialized[objSize];

        RecordComponent[] components = recordType.getRecordComponents();
        MethodHandle[] accessors = ((SerialContext.SerializerContextImpl) ctxt).classLocal(RECORD_ACCESSORS, recordType);

        for (int i = 0; i < components.length; i++) {
            ObjectStreamField field = fields.get(components[i].getName());
            if (field == null) {
                // component is not in the serializable field set (transient, etc.)
                continue;
            }
            Object value;
            try {
                value = accessors[i].invoke(record);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw Util.sneak(e);
            }
            if (field.isPrimitive()) {
                int offset = field.getOffset();
                switch (field.getTypeCode()) {
                    case 'Z' -> primData[offset] = (byte) ((boolean) value ? 1 : 0);
                    case 'B' -> primData[offset] = (byte) value;
                    case 'C' -> Util.BE16.set(primData, offset, (short) (char) value);
                    case 'S' -> Util.BE16.set(primData, offset, (short) value);
                    case 'I' -> Util.BE32.set(primData, offset, (int) value);
                    case 'J' -> Util.BE64.set(primData, offset, (long) value);
                    case 'F' -> Util.BE32.set(primData, offset, Float.floatToRawIntBits((float) value));
                    case 'D' -> Util.BE64.set(primData, offset, Double.doubleToRawLongBits((double) value));
                    default -> throw Assert.impossibleSwitchCase(field.getTypeCode());
                }
            } else {
                objData[field.getOffset()] = ctxt.serialize(value);
            }
        }

        this.fieldData = new SerialData(
                recordClass,
                primData == null ? StreamData.OfBytes.EMPTY : StreamData.of(primData),
                objData == null ? StreamData.OfObjects.EMPTY : StreamData.of(objData),
                List.of());
    }

    /**
     * {@return the class descriptor for the record (not {@code null})}
     */
    public SerializedRecordClass recordClass() {
        return recordClass;
    }

    /**
     * {@return the field data for the record (not {@code null})}
     */
    public SerialData fieldData() {
        return fieldData;
    }
}
