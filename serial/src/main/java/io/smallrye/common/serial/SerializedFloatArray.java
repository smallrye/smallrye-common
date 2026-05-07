package io.smallrye.common.serial;

/**
 * The serialized representation of a {@code float[]} instance.
 */
public final class SerializedFloatArray extends SerializedArray {
    private final float[] array;

    /**
     * Construct a new instance.
     *
     * @param array the array (must not be {@code null})
     */
    public SerializedFloatArray(final float[] array) {
        this.array = array.clone();
    }

    /**
     * {@return the array corresponding to this serialized representation (not {@code null})}
     */
    public float[] asArray() {
        return array.clone();
    }
}
