package io.smallrye.common.serial.impl.providers;

import java.io.IOException;

import io.smallrye.common.serial.Serialized;
import io.smallrye.common.serial.SerializedEnum;
import io.smallrye.common.serial.spi.ObjectDeserializer;

/**
 * Deserializer that handles {@link SerializedEnum} instances.
 */
public final class EnumDeserializer implements ObjectDeserializer {
    /**
     * Construct a new instance.
     */
    public EnumDeserializer() {
    }

    public Object deserialize(final Context ctxt, final Serialized serialized) throws IOException, ClassNotFoundException {
        if (serialized instanceof SerializedEnum se) {
            return Enum.valueOf(
                    ctxt.deserializeClass(se.enumClass(), Enum.class),
                    ctxt.deserialize(se.constantName(), String.class));
        } else {
            return ctxt.next();
        }
    }

    public int priority() {
        return PRIORITY_BASIC;
    }
}
