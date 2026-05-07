package io.smallrye.common.serial;

/**
 * The serialized representation of a {@code double[]} instance.
 */
public final class SerializedDoubleArray extends SerializedArray {
    private final double[] array;

    /**
     * Construct a new instance.
     *
     * @param array the array (must not be {@code null})
     */
    public SerializedDoubleArray(final double[] array) {
        this.array = array.clone();
    }

    /**
     * {@return the array corresponding to this serialized representation (not {@code null})}
     */
    public double[] asArray() {
        return array.clone();
    }
}
