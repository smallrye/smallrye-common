package io.smallrye.common.serial;

import java.io.ObjectStreamField;
import java.util.Map;

/**
 * Abstract base for class descriptors that carry a stream field layout.
 * This includes {@linkplain SerializedSerializableClass serializable} and
 * {@linkplain SerializedRecordClass record} classes.
 */
public abstract sealed class SerializedFieldedClass extends SerializedVersionedClass
        permits SerializedSerializableClass, SerializedRecordClass {

    private final Map<String, ObjectStreamField> fieldIndex;
    private final int primitiveBufferSize;
    private final int objectBufferSize;

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
    SerializedFieldedClass(final String name, final Serialized classLoader, final long uid,
            final Map<String, ObjectStreamField> fieldIndex, final int primitiveBufferSize, final int objectBufferSize) {
        super(name, classLoader, uid);
        this.fieldIndex = Map.copyOf(fieldIndex);
        this.primitiveBufferSize = primitiveBufferSize;
        this.objectBufferSize = objectBufferSize;
    }

    /**
     * {@return the serializable stream field layout for this class}
     * The returned map excludes any {@code transient} or {@code static} fields.
     */
    public Map<String, ObjectStreamField> streamFields() {
        return fieldIndex;
    }

    /**
     * {@return the size of the primitive field buffer needed to serialize instances of this class}
     * Note that this does not include the size of any superclass or subclass in the hierarchy.
     */
    public int primitiveBufferSize() {
        return primitiveBufferSize;
    }

    /**
     * {@return the size of the object field buffer needed to serialize instances of this class}
     * Note that this does not include the size of any superclass or subclass in the hierarchy.
     */
    public int objectBufferSize() {
        return objectBufferSize;
    }
}
