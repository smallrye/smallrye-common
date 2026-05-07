package io.smallrye.common.serial.impl.providers;

import java.io.Externalizable;
import java.io.IOException;

import io.smallrye.common.serial.Serialized;
import io.smallrye.common.serial.SerializedExternalizable;
import io.smallrye.common.serial.spi.ObjectSerializer;

/**
 * Serializer that handles {@link Externalizable} objects.
 */
public final class ExternalizableSerializer implements ObjectSerializer {
    /**
     * Construct a new instance.
     */
    public ExternalizableSerializer() {
    }

    public Serialized serialize(final Context ctxt, final Object object) throws IOException {
        if (object instanceof Externalizable ext) {
            return new SerializedExternalizable(ctxt, ext);
        } else {
            return ctxt.next();
        }
    }

    public int priority() {
        return PRIORITY_EXTERNALIZABLE;
    }
}
