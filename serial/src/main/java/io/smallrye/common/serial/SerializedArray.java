package io.smallrye.common.serial;

/**
 * The serialized representation of an array.
 * This is the common supertype for all serialized array types, both primitive and object.
 */
public abstract class SerializedArray extends Serialized {
    SerializedArray() {
    }

    /**
     * {@return a copy of the array corresponding to this serialized representation (not {@code null})}
     * The returned value is always a fresh copy; modifications to it do not affect this instance.
     * The runtime type of the returned object is the appropriate array type
     * (e.g., {@code int[]} for {@link SerializedIntArray}).
     */
    public abstract Object asArray();
}
