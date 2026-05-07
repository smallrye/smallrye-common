package io.smallrye.common.serial;

/**
 * The serialized representation of a {@code char[]} instance.
 */
public final class SerializedCharArray extends SerializedArray {
    private final char[] array;

    /**
     * Construct a new instance.
     *
     * @param array the array (must not be {@code null})
     */
    public SerializedCharArray(final char[] array) {
        this.array = array.clone();
    }

    /**
     * {@return the array corresponding to this serialized representation (not {@code null})}
     */
    public char[] asArray() {
        return array.clone();
    }
}
