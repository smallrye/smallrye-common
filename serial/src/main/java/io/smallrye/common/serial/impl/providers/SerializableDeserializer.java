package io.smallrye.common.serial.impl.providers;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import io.smallrye.common.serial.SerialData;
import io.smallrye.common.serial.Serialized;
import io.smallrye.common.serial.SerializedSerializable;
import io.smallrye.common.serial.SerializedSerializableClass;
import io.smallrye.common.serial.impl.CapturedObjectInputStream;
import io.smallrye.common.serial.impl.ReadUtil;
import io.smallrye.common.serial.spi.ObjectDeserializer;

/**
 * Deserializer that handles {@link SerializedSerializable} instances.
 */
public final class SerializableDeserializer implements ObjectDeserializer {
    /**
     * Construct a new instance.
     */
    public SerializableDeserializer() {
    }

    public Object deserialize(final Context ctxt, final Serialized serialized) throws IOException, ClassNotFoundException {
        if (serialized instanceof SerializedSerializable ser) {
            SerializedSerializableClass type = ser.serializedClass();
            Class<?> clazz = ctxt.deserializeClass(type);
            if (clazz.isInterface() || clazz.isPrimitive() || clazz.isEnum() || clazz.isArray()) {
                throw new InvalidObjectException("Serialized " + clazz + " must be a regular class");
            }
            if (Serializable.class.isAssignableFrom(clazz)) {
                if (Externalizable.class.isAssignableFrom(clazz)) {
                    throw new InvalidObjectException("Serialized " + clazz + " must not implement Externalizable");
                }
                Object object = ReadUtil.newSerializableInstance(clazz);
                ctxt.preSetObject(object);
                List<SerialData> data = ser.data();
                deserialize(ctxt, data, data.size() - 1, object, clazz, type, clazz);
                return object;
            } else {
                return ctxt.next();
            }
        } else {
            return ctxt.next();
        }
    }

    private void deserialize(final Context ctxt, final List<SerialData> data, final int idx, final Object object,
            final Class<?> local, final SerializedSerializableClass remoteSer, final Class<?> remote)
            throws IOException, ClassNotFoundException {
        if (remote == local) {
            // first do parent, then this level
            if (idx > 0) {
                if (Serializable.class.isAssignableFrom(local.getSuperclass())) {
                    // regular recurse to superclass
                    deserialize(ctxt, data, idx - 1, object, local.getSuperclass(), remoteSer.superClass(),
                            ctxt.deserializeClass(remoteSer.superClass()));
                } else {
                    // extra data; ignore it and continue on
                }
            } else if (Serializable.class.isAssignableFrom(local.getSuperclass())) {
                // this will go down the readObjectNoData route
                deserialize(ctxt, data, idx, object, local.getSuperclass(), remoteSer, remote);
            } else {
                // no supertypes remain; just continue on
            }
            final Map<String, ObjectStreamField> fields = remoteSer.streamFields();
            final SerialData item = data.get(idx);
            CapturedObjectInputStream ois = new CapturedObjectInputStream(ctxt, local, object, fields,
                    item.primitiveFieldData(), item.objectFieldData(), item.streamData());
            if (ReadUtil.hasReadObject(local)) {
                ReadUtil.readObject(local, object, ois);
            } else {
                ReadUtil.defaultReadObject(local, object, ois);
            }
        } else if (remote.isAssignableFrom(local)) {
            // remote is a supertype of local; we have no data locally
            // first deserialize parent
            deserialize(ctxt, data, idx, object, local.getSuperclass(), remoteSer, remote);
            if (ReadUtil.hasReadObjectNoData(local)) {
                ReadUtil.readObjectNoData(local, object);
            }
        } else if (local.isAssignableFrom(remote)) {
            // ignore remote data for this one
            if (idx > 0) {
                deserialize(ctxt, data, idx - 1, object, local, remoteSer.superClass(),
                        ctxt.deserializeClass(remoteSer.superClass()));
            }
        } else {
            throw new InvalidObjectException("No relationship between local " + local + " and remote " + remote);
        }
    }

    public int priority() {
        return PRIORITY_SERIALIZABLE;
    }
}
