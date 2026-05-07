package io.smallrye.common.serial;

/**
 * The serialized representation of an {@code int[]} instance.
 */
public final class SerializedIntArray extends SerializedArray {
    private final int[] array;

    /**
     * Construct a new instance.
     *
     * @param array the array (must not be {@code null})
     */
    public SerializedIntArray(final int[] array) {
        this.array = array.clone();
    }

    /**
     * {@return the array corresponding to this serialized representation (not {@code null})}
     */
    public int[] asArray() {
        return array.clone();
    }
}
