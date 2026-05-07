package io.smallrye.common.serial.impl.providers;

import java.io.IOException;

import io.smallrye.common.serial.Serialized;
import io.smallrye.common.serial.impl.ReadUtil;
import io.smallrye.common.serial.spi.ObjectDeserializer;

/**
 * Deserializer that handles {@code readResolve} method invocations.
 */
public final class ReadResolveDeserializer implements ObjectDeserializer {

    /**
     * Construct a new instance.
     */
    public ReadResolveDeserializer() {
    }

    public Object deserialize(final Context ctxt, final Serialized serialized) throws IOException, ClassNotFoundException {
        Object deserialized = ctxt.next();
        if (deserialized != null) {
            if (ReadUtil.hasReadResolve(deserialized.getClass())) {
                deserialized = ReadUtil.readResolve(deserialized);
            }
        }
        return deserialized;
    }

    public int priority() {
        return PRIORITY_REPLACE;
    }
}
