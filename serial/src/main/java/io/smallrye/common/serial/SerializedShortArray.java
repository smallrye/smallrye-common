package io.smallrye.common.serial;

/**
 * The serialized representation of a {@code short[]} instance.
 */
public final class SerializedShortArray extends SerializedArray {
    private final short[] array;

    /**
     * Construct a new instance.
     *
     * @param array the array (must not be {@code null})
     */
    public SerializedShortArray(final short[] array) {
        this.array = array;
    }

    /**
     * {@return the array corresponding to this serialized representation (not {@code null})}
     */
    public short[] asArray() {
        return array.clone();
    }
}
