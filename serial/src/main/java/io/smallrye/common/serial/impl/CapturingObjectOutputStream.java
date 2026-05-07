package io.smallrye.common.serial.impl;

import static io.smallrye.common.serial.impl.Util.*;

import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import io.smallrye.common.serial.Serialized;
import io.smallrye.common.serial.StreamData;
import io.smallrye.common.serial.spi.ObjectSerializer;

public final class CapturingObjectOutputStream extends ObjectOutputStream {

    private static final int ST_INITIAL = 0;
    private static final int ST_STREAM = 1;
    private static final int ST_CLOSED = 2;

    private final ObjectSerializer.Context context;
    private final Class<?> serialClass;
    private final Object serialObject;
    private final Map<String, ObjectStreamField> fields;

    /**
     * The capturing stream for data written after the field data is written.
     */
    private CapturingObjectOutput objectOutput;
    /**
     * The current state; one of the {@code ST_} constants in this class.
     */
    private int state;

    /**
     * The lazily created cached {@code PutField} instance.
     * It can be acquired as many times as desired and has no impact on the stream state.
     */
    private PutField putField;

    /**
     * The array of bytes which holds the primitive field values.
     */
    private final byte[] primFields;
    /**
     * The array which holds the pre-serialized object field values.
     */
    private final Object[] objFields;
    /**
     * The array which holds the post-serialized object field values.
     */
    private final Serialized[] serializedFields;

    public CapturingObjectOutputStream(final ObjectSerializer.Context context, final Class<?> serialClass,
            final Object serialObject,
            final Map<String, ObjectStreamField> fields) throws IOException {
        super();
        this.context = context;
        this.serialClass = serialClass;
        this.serialObject = serialObject;
        this.fields = fields;
        // compute the data sizes
        int pds = 0, ods = 0;
        for (ObjectStreamField field : fields.values()) {
            int size = switch (field.getTypeCode()) {
                case '[', 'L' -> 0;
                case 'B', 'Z' -> 1;
                case 'C', 'S' -> 2;
                case 'I', 'F' -> 4;
                case 'J', 'D' -> 8;
                default -> throw new IllegalStateException();
            };
            int end = size + field.getOffset();
            if (end > pds) {
                pds = end;
            }
            size = switch (field.getTypeCode()) {
                case '[', 'L' -> 1;
                case 'B', 'Z', 'C', 'S', 'I', 'F', 'J', 'D' -> 0;
                default -> throw new IllegalStateException();
            };
            end = size + field.getOffset();
            if (end > ods) {
                ods = end;
            }
        }
        primFields = pds == 0 ? null : new byte[pds];
        objFields = ods == 0 ? null : new Object[ods];
        serializedFields = ods == 0 ? null : new Serialized[ods];
    }

    public void useProtocolVersion(final int version) throws IOException {
        if (state != ST_INITIAL) {
            throw fieldsAlreadyWritten();
        }
        // otherwise ignored
    }

    protected void writeObjectOverride(final Object obj) throws IOException {
        objectOutput().writeObject(obj);
    }

    public void writeUnshared(final Object obj) {
        throw new UnsupportedOperationException("Unshared object writes are not supported");
    }

    public void defaultWriteObject() throws IOException {
        if (state != ST_INITIAL) {
            throw fieldsAlreadyWritten();
        }
        WriteUtil.defaultWriteObject(serialClass, serialObject, this);
        // should have done something
        if (state == ST_INITIAL) {
            throw noObjectStreamFields();
        }
    }

    public PutField putFields() {
        PutField putField = this.putField;
        if (putField == null) {
            putField = this.putField = new PutFieldImpl();
        }
        return putField;
    }

    public void writeFields() throws IOException {
        if (state != ST_INITIAL) {
            throw fieldsAlreadyWritten();
        }
        try {
            if (objFields != null) {
                for (int i = 0; i < objFields.length; i++) {
                    serializedFields[i] = context.serialize(objFields[i]);
                }
            }
        } catch (Throwable t) {
            state = ST_CLOSED;
            throw t;
        }
        state = ST_STREAM;
    }

    public void reset() throws IOException {
        throw new IOException("Cannot call reset() while serializing an object");
    }

    public void write(final int val) throws IOException {
        objectOutput().write(val);
    }

    public void write(final byte[] buf) throws IOException {
        objectOutput().write(buf);
    }

    public void write(final byte[] buf, final int off, final int len) throws IOException {
        objectOutput().write(buf, off, len);
    }

    public void writeBoolean(final boolean val) throws IOException {
        objectOutput().writeBoolean(val);
    }

    public void writeByte(final int val) throws IOException {
        objectOutput().writeByte(val);
    }

    public void writeShort(final int val) throws IOException {
        objectOutput().writeShort(val);
    }

    public void writeChar(final int val) throws IOException {
        objectOutput().writeChar(val);
    }

    public void writeInt(final int val) throws IOException {
        objectOutput().writeInt(val);
    }

    public void writeLong(final long val) throws IOException {
        objectOutput().writeLong(val);
    }

    public void writeFloat(final float val) throws IOException {
        objectOutput().writeFloat(val);
    }

    public void writeDouble(final double val) throws IOException {
        objectOutput().writeDouble(val);
    }

    public void writeBytes(final String str) throws IOException {
        objectOutput().writeBytes(str);
    }

    public void writeChars(final String str) throws IOException {
        objectOutput().writeChars(str);
    }

    public void writeUTF(final String str) throws IOException {
        objectOutput().writeUTF(str);
    }

    public void flush() {
        CapturingObjectOutput objectOutput = this.objectOutput;
        if (objectOutput != null) {
            objectOutput.flush();
        }
    }

    protected void drain() {
        flush();
    }

    public void close() throws IOException {
        CapturingObjectOutput objectOutput = this.objectOutput;
        if (objectOutput != null) {
            objectOutput.close();
        }
    }

    /**
     * {@return the primitive field data captured during serialization (not {@code null})}
     */
    public StreamData.OfBytes primitiveFieldData() {
        return primFields == null ? StreamData.OfBytes.EMPTY : StreamData.of(primFields);
    }

    /**
     * {@return the object field data captured during serialization (not {@code null})}
     */
    public StreamData.OfObjects objectFieldData() {
        return serializedFields == null ? StreamData.OfObjects.EMPTY : StreamData.of(serializedFields);
    }

    /**
     * {@return the additional stream data captured during serialization (not {@code null})}
     */
    public List<StreamData> streamData() {
        return objectOutput == null ? List.of() : objectOutput.streamData();
    }

    // -- private --

    private CapturingObjectOutput objectOutput() throws IOException {
        switch (state) {
            case ST_INITIAL -> throw noObjectStreamFields();
            case ST_CLOSED -> throw closed();
        }
        CapturingObjectOutput objectOutput = this.objectOutput;
        if (objectOutput == null) {
            objectOutput = this.objectOutput = new CapturingObjectOutput(context);
        }
        return objectOutput;
    }

    private static IOException noObjectStreamFields() {
        return new IOException("Object stream fields were not written");
    }

    private static IOException fieldsAlreadyWritten() {
        return new IOException("Object stream fields were already written");
    }

    private static IOException closed() {
        return new IOException("Stream closed");
    }

    private final class PutFieldImpl extends PutField {
        public void put(final String name, final boolean val) {
            primFields[offs(name, true)] = (byte) (val ? 1 : 0);
        }

        public void put(final String name, final byte val) {
            primFields[offs(name, true)] = val;
        }

        public void put(final String name, final char val) {
            BE16.set(primFields, offs(name, true), (short) val);
        }

        public void put(final String name, final short val) {
            BE16.set(primFields, offs(name, true), val);
        }

        public void put(final String name, final int val) {
            BE32.set(primFields, offs(name, true), val);
        }

        public void put(final String name, final long val) {
            BE64.set(primFields, offs(name, true), val);
        }

        public void put(final String name, final float val) {
            BE32.set(primFields, offs(name, true), Float.floatToRawIntBits(val));
        }

        public void put(final String name, final double val) {
            BE64.set(primFields, offs(name, true), Double.doubleToRawLongBits(val));
        }

        public void put(final String name, final Object val) {
            objFields[offs(name, false)] = val;
        }

        @SuppressWarnings("removal")
        public void write(final ObjectOutput out) {
            if (out == CapturingObjectOutputStream.this) {
                try {
                    writeFields();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                throw new IllegalArgumentException(
                        "The given stream must match the stream which produced this PutField instance");
            }
        }
    }

    private int offs(String name, boolean primitive) {
        ObjectStreamField field = fields.get(name);
        if (field == null) {
            throw new IllegalArgumentException("The given stream field name " + name + " does not exist");
        }
        if (field.isPrimitive() == primitive) {
            return field.getOffset();
        } else {
            throw new IllegalArgumentException("Field " + name + " has an unexpected kind");
        }
    }
}
