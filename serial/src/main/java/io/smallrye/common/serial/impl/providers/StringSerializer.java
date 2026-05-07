package io.smallrye.common.serial.impl.providers;

import java.io.IOException;

import io.smallrye.common.serial.Serialized;
import io.smallrye.common.serial.SerializedString;
import io.smallrye.common.serial.spi.ObjectSerializer;

/**
 * Serializer that handles {@link String} objects.
 * <p>
 * Strings are serialized as {@link SerializedString} rather than as regular serializable objects,
 * matching the Java serialization wire protocol where strings have a dedicated representation
 * (TC_STRING / TC_LONGSTRING).
 */
public final class StringSerializer implements ObjectSerializer {
    /**
     * Construct a new instance.
     */
    public StringSerializer() {
    }

    /**
     * {@inheritDoc}
     */
    public Serialized serialize(final Context ctxt, final Object object) throws IOException {
        if (object instanceof String s) {
            return new SerializedString(s);
        } else {
            return ctxt.next();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int priority() {
        return PRIORITY_BASIC;
    }
}
