package io.smallrye.common.serial.impl.providers;

import java.io.IOException;

import io.smallrye.common.serial.Serialized;
import io.smallrye.common.serial.impl.Util;
import io.smallrye.common.serial.spi.ObjectSerializer;

/**
 * Serializer that handles {@code record} instances.
 */
public final class RecordSerializer implements ObjectSerializer {

    /**
     * Construct a new instance.
     */
    public RecordSerializer() {
    }

    public Serialized serialize(final Context ctxt, final Object object) throws IOException {
        if (object.getClass().isRecord()) {
            return Util.newSerializedRecord(object, ctxt);
        } else {
            return ctxt.next();
        }
    }

    public int priority() {
        return PRIORITY_BASIC;
    }
}
