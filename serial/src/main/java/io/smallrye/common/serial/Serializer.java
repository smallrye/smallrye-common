package io.smallrye.common.serial;

import java.io.IOException;

/**
 * A serializer converts some kind of object reference into a serialized representation of it.
 */
public interface Serializer {
    /**
     * Serialize the given object reference into a serialized representation.
     *
     * @param object the object reference to serialize
     * @return the serialized representation (must not be {@code null})
     * @throws IOException if serialization failed due to an I/O error
     */
    Serialized serialize(Object object) throws IOException;

    /**
     * {@return {@code true} if this serializer has previously serialized the given object, or {@code false} if it has not}
     *
     * @param object the object
     */
    boolean hasSerialized(Object object);
}
