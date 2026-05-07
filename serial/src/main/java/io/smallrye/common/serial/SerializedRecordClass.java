package io.smallrye.common.serial;

import java.io.ObjectStreamField;
import java.util.Map;

/**
 * The serialized representation of a {@code record} class.
 * Record class descriptors carry a stream field layout (one level only, no superclass chain),
 * and instances are reconstructed via the canonical constructor during deserialization.
 */
public final class SerializedRecordClass extends SerializedFieldedClass {

    /**
     * Construct a new instance.
     *
     * @param name the class name (must not be {@code null})
     * @param classLoader the serialized class loader (must not be {@code null} but may be {@link SerializedNull#INSTANCE})
     * @param uid the serial version UID
     * @param fieldIndex the field index map (must not be {@code null})
     * @param primitiveBufferSize the size of the primitive buffer for this class
     * @param objectBufferSize the size of the object buffer for this class
     */
    public SerializedRecordClass(final String name, final Serialized classLoader, final long uid,
            final Map<String, ObjectStreamField> fieldIndex, final int primitiveBufferSize, final int objectBufferSize) {
        super(name, classLoader, uid, fieldIndex, primitiveBufferSize, objectBufferSize);
    }
}
