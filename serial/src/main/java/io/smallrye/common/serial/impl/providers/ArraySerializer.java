package io.smallrye.common.serial.impl.providers;

import java.io.IOException;

import io.smallrye.common.serial.Serialized;
import io.smallrye.common.serial.SerializedBooleanArray;
import io.smallrye.common.serial.SerializedByteArray;
import io.smallrye.common.serial.SerializedCharArray;
import io.smallrye.common.serial.SerializedDoubleArray;
import io.smallrye.common.serial.SerializedFloatArray;
import io.smallrye.common.serial.SerializedIntArray;
import io.smallrye.common.serial.SerializedLongArray;
import io.smallrye.common.serial.SerializedObjectArray;
import io.smallrye.common.serial.SerializedShortArray;
import io.smallrye.common.serial.impl.Util;
import io.smallrye.common.serial.spi.ObjectSerializer;

/**
 * Serializer that handles array objects (both primitive and object arrays).
 * <p>
 * Primitive arrays are serialized as their corresponding {@code Serialized*Array} types.
 * Object arrays are serialized as {@link SerializedObjectArray} with each element serialized recursively.
 */
public final class ArraySerializer implements ObjectSerializer {
    /**
     * Construct a new instance.
     */
    public ArraySerializer() {
    }

    /**
     * {@inheritDoc}
     */
    public Serialized serialize(final Context ctxt, final Object object) throws IOException {
        if (object instanceof boolean[] a) {
            return new SerializedBooleanArray(a);
        } else if (object instanceof byte[] a) {
            return new SerializedByteArray(a);
        } else if (object instanceof char[] a) {
            return new SerializedCharArray(a);
        } else if (object instanceof short[] a) {
            return new SerializedShortArray(a);
        } else if (object instanceof int[] a) {
            return new SerializedIntArray(a);
        } else if (object instanceof long[] a) {
            return new SerializedLongArray(a);
        } else if (object instanceof float[] a) {
            return new SerializedFloatArray(a);
        } else if (object instanceof double[] a) {
            return new SerializedDoubleArray(a);
        } else if (object instanceof Object[] a) {
            return Util.newSerializedObjectArray(a, ctxt);
        } else {
            return ctxt.next();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int priority() {
        return PRIORITY_ARRAY;
    }
}
