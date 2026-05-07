package io.smallrye.common.serial;

import java.io.Externalizable;
import java.io.IOException;
import java.util.List;

import io.smallrye.common.serial.impl.CapturingObjectOutput;
import io.smallrye.common.serial.spi.ObjectSerializer;

/**
 * The serialized representation of an {@link Externalizable} object.
 * The object's data is captured as a sequence of {@link StreamData} blocks
 * produced by the object's {@link Externalizable#writeExternal(java.io.ObjectOutput)} method.
 */
public final class SerializedExternalizable extends Serialized {
    private final SerializedExternalizableClass serializedClass;
    private final List<StreamData> data;

    /**
     * Construct a new instance from pre-existing data.
     * This constructor is intended for wire protocol readers and manual graph construction.
     *
     * @param serializedClass the class descriptor for the externalizable object (must not be {@code null})
     * @param data the stream data produced by the object's {@code writeExternal} method (must not be {@code null})
     */
    public SerializedExternalizable(final SerializedExternalizableClass serializedClass, final List<StreamData> data) {
        this.serializedClass = serializedClass;
        this.data = List.copyOf(data);
    }

    /**
     * Construct a new instance during serialization by capturing data from a live externalizable object.
     *
     * @param ctxt the serializer context (must not be {@code null})
     * @param ext the externalizable object (must not be {@code null})
     * @throws IOException if the object's {@code writeExternal} method throws an I/O error
     */
    public SerializedExternalizable(final ObjectSerializer.Context ctxt, final Externalizable ext) throws IOException {
        serializedClass = (SerializedExternalizableClass) ctxt.serialize(ext.getClass());
        ctxt.preSetSerialized(this);
        try (CapturingObjectOutput oo = new CapturingObjectOutput(ctxt)) {
            ext.writeExternal(oo);
            oo.close();
            data = oo.streamData();
        }
    }

    /**
     * {@return the class descriptor for the externalizable object (not {@code null})}
     */
    public SerializedExternalizableClass serializedClass() {
        return serializedClass;
    }

    /**
     * {@return the stream data for this object (not {@code null})}
     */
    public List<StreamData> data() {
        return data;
    }
}
