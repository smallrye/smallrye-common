package io.smallrye.common.serial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.serial.impl.CapturingObjectOutputStream;
import io.smallrye.common.serial.impl.WriteUtil;
import io.smallrye.common.serial.spi.ObjectSerializer;

public final class SerializedSerializable extends Serialized {

    private final SerializedSerializableClass streamClass;
    private final List<SerialData> data;

    /**
     * Construct a new instance from pre-existing serialization data.
     * This constructor is intended for wire protocol readers and manual graph construction.
     *
     * @param streamClass the class descriptor for the serialized object (must not be {@code null})
     * @param data the per-class-level serialization data, ordered from root to leaf (must not be {@code null})
     */
    public SerializedSerializable(final SerializedSerializableClass streamClass, final List<SerialData> data) {
        this.streamClass = Assert.checkNotNullParam("streamClass", streamClass);
        this.data = List.copyOf(data);
    }

    /**
     * Construct a new instance during serialization by capturing data from a live object.
     *
     * @param object the object being serialized (must not be {@code null})
     * @param ctxt the context (must not be {@code null})
     */
    SerializedSerializable(final Object object, final ObjectSerializer.Context ctxt) throws IOException {
        this.streamClass = (SerializedSerializableClass) ctxt.serialize(object.getClass());
        ctxt.preSetSerialized(this);
        this.data = List.copyOf(buildData(ctxt, object, object.getClass(), streamClass, new ArrayList<>()));
    }

    /**
     * {@return the class descriptor for this serialized object (not {@code null})}
     */
    public SerializedSerializableClass serializedClass() {
        return streamClass;
    }

    /**
     * {@return the per-class-level serialization data, ordered from root to leaf (not {@code null})}
     */
    public List<SerialData> data() {
        return data;
    }

    /**
     * Find the serialization data for a specific class level by class name.
     *
     * @param className the fully qualified class name (must not be {@code null})
     * @return the serialization data for that class level, or {@code null} if not found
     */
    public SerialData dataFor(String className) {
        Assert.checkNotNullParam("className", className);
        for (SerialData d : data) {
            if (d.serializedClass().name().equals(className)) {
                return d;
            }
        }
        return null;
    }

    /**
     * Find the serialization data for a specific class level by local class.
     * The lookup is performed by matching the class name.
     *
     * @param clazz the local class (must not be {@code null})
     * @return the serialization data for that class level, or {@code null} if not found
     */
    public SerialData dataFor(Class<?> clazz) {
        return dataFor(Assert.checkNotNullParam("clazz", clazz).getName());
    }

    /**
     * Find the serialization data for a specific class level by serialized class descriptor.
     * The lookup is performed by identity comparison against the {@link SerialData#serializedClass()} reference.
     *
     * @param serializedClass the serialized class descriptor (must not be {@code null})
     * @return the serialization data for that class level, or {@code null} if not found
     */
    public SerialData dataFor(SerializedClass serializedClass) {
        Assert.checkNotNullParam("serializedClass", serializedClass);
        for (SerialData d : data) {
            if (d.serializedClass() == serializedClass) {
                return d;
            }
        }
        return null;
    }

    /**
     * Walk the class hierarchy from root to leaf, producing a {@link SerialData} entry for each serializable
     * class level. The local class hierarchy ({@code clazz}) and remote stream class hierarchy
     * ({@code streamClass}) are walked in parallel to handle mismatches.
     */
    private List<SerialData> buildData(ObjectSerializer.Context context, Object object, Class<?> clazz,
            SerializedSerializableClass streamClass, List<SerialData> data) throws IOException {
        if (streamClass == null) {
            // no remote class available, skip the local class
            if (clazz != null) {
                buildData(context, object, clazz.getSuperclass(), null, data);
            }
            return data;
        } else {
            if (clazz == null) {
                buildData(context, object, null, streamClass.superClass(), data);
                // no local class available, write defaults
                data.add(new SerialData(
                        streamClass,
                        StreamData.of(new byte[streamClass.primitiveBufferSize()]),
                        StreamData.of(Collections.nCopies(streamClass.objectBufferSize(), SerializedNull.INSTANCE)),
                        List.of()));
                return data;
            } else {
                buildData(context, object, clazz.getSuperclass(), streamClass.superClass(), data);
                try (CapturingObjectOutputStream oos = new CapturingObjectOutputStream(context, clazz, object,
                        streamClass.streamFields())) {
                    if (WriteUtil.hasWriteObject(clazz)) {
                        WriteUtil.writeObject(clazz, object, oos);
                    } else {
                        WriteUtil.defaultWriteObject(clazz, object, oos);
                    }
                    oos.close();
                    data.add(new SerialData(streamClass, oos.primitiveFieldData(), oos.objectFieldData(), oos.streamData()));
                    return data;
                }
            }
        }
    }
}
