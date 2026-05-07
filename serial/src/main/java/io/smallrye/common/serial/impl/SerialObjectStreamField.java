package io.smallrye.common.serial.impl;

import java.io.ObjectStreamField;

/**
 * An {@link ObjectStreamField} subclass that exposes the {@link #setOffset(int)} mutator.
 * This is used to construct field descriptors with explicitly assigned buffer offsets,
 * such as when reconstructing a field layout from serialized data or when the JDK
 * does not assign offsets (e.g. for record classes).
 */
public final class SerialObjectStreamField extends ObjectStreamField {
    /**
     * Construct a new instance.
     *
     * @param name the field name (must not be {@code null})
     * @param type the field type (must not be {@code null})
     */
    public SerialObjectStreamField(final String name, final Class<?> type) {
        super(name, type);
    }

    /**
     * Set the buffer offset for this field.
     *
     * @param offset the offset value
     */
    public void setOffset(final int offset) {
        super.setOffset(offset);
    }
}
