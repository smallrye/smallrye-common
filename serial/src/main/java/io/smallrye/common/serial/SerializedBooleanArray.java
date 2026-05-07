package io.smallrye.common.serial;

/**
 * The serialized representation of a {@code boolean[]} instance.
 */
public final class SerializedBooleanArray extends SerializedArray {
    private final boolean[] array;

    /**
     * Construct a new instance.
     *
     * @param array the array (must not be {@code null})
     */
    public SerializedBooleanArray(final boolean[] array) {
        this.array = array.clone();
    }

    /**
     * {@return the array corresponding to this serialized representation (not {@code null})}
     */
    public boolean[] asArray() {
        return array.clone();
    }
}
