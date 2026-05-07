package io.smallrye.common.serial;

import java.io.IOException;

/**
 * A deserializer converts a serialized representation into some kind of object reference.
 */
public interface Deserializer {
    /**
     * Deserialize the given representation into an object.
     *
     * @param serialized the serialized representation (must not be {@code null})
     * @return the deserialized object reference
     * @throws IOException if deserialization fails due to an I/O error
     * @throws ClassNotFoundException if deserialization fails due to a missing required class
     */
    Object deserialize(Serialized serialized) throws IOException, ClassNotFoundException;

    /**
     * Deserialize the given representation into an object of the expected type.
     *
     * @param serialized the serialized representation (must not be {@code null})
     * @param type the expected type of the deserialized object (must not be {@code null})
     * @param <T> the expected type
     * @return the deserialized object reference
     * @throws IOException if deserialization fails due to an I/O error
     * @throws ClassNotFoundException if deserialization fails due to a missing required class
     * @throws ClassCastException if the deserialized object is not an instance of the expected type
     */
    default <T> T deserialize(Serialized serialized, Class<T> type) throws IOException, ClassNotFoundException {
        return type.cast(deserialize(serialized));
    }

    /**
     * Deserialize the given representation into a {@link Class} object.
     *
     * @param serialized the serialized representation (must not be {@code null})
     * @return the deserialized class
     * @throws IOException if deserialization fails due to an I/O error
     * @throws ClassNotFoundException if deserialization fails due to a missing required class
     * @throws ClassCastException if the deserialized object is not a {@link Class}
     */
    default Class<?> deserializeClass(Serialized serialized) throws IOException, ClassNotFoundException {
        return deserialize(serialized, Class.class);
    }

    /**
     * Deserialize the given representation into a {@link Class} object of the expected type.
     *
     * @param serialized the serialized representation (must not be {@code null})
     * @param type the expected bound of the deserialized class (must not be {@code null})
     * @param <T> the expected bound type
     * @return the deserialized class
     * @throws IOException if deserialization fails due to an I/O error
     * @throws ClassNotFoundException if deserialization fails due to a missing required class
     * @throws ClassCastException if the deserialized class is not assignable to the expected type
     */
    default <T> Class<? extends T> deserializeClass(Serialized serialized, Class<T> type)
            throws IOException, ClassNotFoundException {
        return deserializeClass(serialized).asSubclass(type);
    }

    /**
     * {@return {@code true} if this deserializer has previously deserialized the given object, or {@code false} if it has not}
     *
     * @param serialized the serialized representation (must not be {@code null})
     */
    boolean hasDeserialized(Serialized serialized);
}
