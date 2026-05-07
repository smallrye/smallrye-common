package io.smallrye.common.serial.impl.providers;

import java.io.IOException;

import io.smallrye.common.serial.Serialized;
import io.smallrye.common.serial.SerializedSerializableClass;
import io.smallrye.common.serial.impl.Util;
import io.smallrye.common.serial.spi.ObjectSerializer;

/**
 * Serializer that handles {@link java.io.Serializable} objects (excluding records,
 * enums, and externalizables, which are handled by their own serializers).
 */
public final class SerializableSerializer implements ObjectSerializer {

    /**
     * Construct a new instance.
     */
    public SerializableSerializer() {
    }

    public Serialized serialize(final Context ctxt, final Object object) throws IOException {
        Serialized clazz = ctxt.serialize(object.getClass());
        return clazz instanceof SerializedSerializableClass ? Util.newSerializedSerializable(object, ctxt) : ctxt.next();
    }

    public int priority() {
        return PRIORITY_SERIALIZABLE;
    }
}
