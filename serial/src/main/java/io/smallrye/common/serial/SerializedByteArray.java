package io.smallrye.common.serial;

/**
 * The serialized representation of a {@code byte[]} instance.
 */
public final class SerializedByteArray extends SerializedArray {
    private final byte[] array;

    /**
     * Construct a new instance.
     *
     * @param array the array (must not be {@code null})
     */
    public SerializedByteArray(final byte[] array) {
        this.array = array.clone();
    }

    /**
     * {@return the array corresponding to this serialized representation (not {@code null})}
     */
    public byte[] asArray() {
        return array.clone();
    }
}
