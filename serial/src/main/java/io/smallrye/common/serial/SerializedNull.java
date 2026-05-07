package io.smallrye.common.serial;

/**
 * The serialized representation of a {@code null} reference.
 * There is only one instance of this class, accessible via {@link #INSTANCE}.
 */
public final class SerializedNull extends Serialized {

    /**
     * The singleton instance.
     */
    public static final SerializedNull INSTANCE = new SerializedNull();

    private SerializedNull() {
    }
}
