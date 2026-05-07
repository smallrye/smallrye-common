package io.smallrye.common.serial.impl.providers;

import java.io.IOException;

import io.smallrye.common.serial.Serialized;
import io.smallrye.common.serial.SerializedClass;
import io.smallrye.common.serial.spi.ObjectDeserializer;

/**
 * Deserializer that handles {@link SerializedClass} instances.
 */
public final class ClassDeserializer implements ObjectDeserializer {
    /**
     * Construct a new instance.
     */
    public ClassDeserializer() {
    }

    public Object deserialize(final Context ctxt, final Serialized serialized) throws IOException, ClassNotFoundException {
        if (serialized instanceof SerializedClass sc) {
            return Class.forName(sc.name(), false, ctxt.deserialize(sc.classLoader(), ClassLoader.class));
        } else {
            return ctxt.next();
        }
    }

    public int priority() {
        return PRIORITY_CLASS;
    }
}
