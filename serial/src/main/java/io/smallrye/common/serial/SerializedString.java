package io.smallrye.common.serial;

import io.smallrye.common.constraint.Assert;

/**
 * The serialized representation of a {@link String} value.
 * <p>
 * In the Java serialization wire protocol, strings are represented directly as TC_STRING or TC_LONGSTRING,
 * not as regular serializable objects.
 */
public final class SerializedString extends Serialized {
    private final String string;

    /**
     * Construct a new instance.
     *
     * @param string the string value (must not be {@code null})
     */
    public SerializedString(final String string) {
        this.string = Assert.checkNotNullParam("string", string);
    }

    /**
     * {@return the string value (not {@code null})}
     */
    public String string() {
        return string;
    }
}
