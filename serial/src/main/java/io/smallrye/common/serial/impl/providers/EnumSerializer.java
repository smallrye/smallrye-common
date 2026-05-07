package io.smallrye.common.serial.impl.providers;

import java.io.IOException;

import io.smallrye.common.serial.Serialized;
import io.smallrye.common.serial.SerializedEnum;
import io.smallrye.common.serial.SerializedEnumClass;
import io.smallrye.common.serial.spi.ObjectSerializer;

/**
 * Serializer that handles enum constants.
 */
public final class EnumSerializer implements ObjectSerializer {
    /**
     * Construct a new instance.
     */
    public EnumSerializer() {
    }

    public Serialized serialize(final Context ctxt, final Object object) throws IOException {
        if (object instanceof Enum<?> e) {
            return new SerializedEnum((SerializedEnumClass) ctxt.serialize(e.getDeclaringClass()), ctxt.serialize(e.name()));
        } else {
            return ctxt.next();
        }
    }

    public int priority() {
        return PRIORITY_BASIC;
    }
}
