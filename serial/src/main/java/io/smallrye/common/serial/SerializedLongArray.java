package io.smallrye.common.serial;

/**
 * The serialized representation of a {@code long[]} instance.
 */
public final class SerializedLongArray extends SerializedArray {
    private final long[] array;

    /**
     * Construct a new instance.
     *
     * @param array the array (must not be {@code null})
     */
    public SerializedLongArray(final long[] array) {
        this.array = array.clone();
    }

    /**
     * {@return the array corresponding to this serialized representation (not {@code null})}
     */
    public long[] asArray() {
        return array.clone();
    }
}
