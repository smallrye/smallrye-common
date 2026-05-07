package io.smallrye.common.serial.impl;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectInputValidation;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.util.List;
import java.util.Map;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.serial.StreamData;
import io.smallrye.common.serial.spi.ObjectDeserializer;

public final class CapturedObjectInputStream extends ObjectInputStream {
    private static final int ST_INITIAL = 0;
    private static final int ST_STREAM = 1;
    private static final int ST_CLOSED = 2;

    private final ObjectDeserializer.Context context;
    private final Class<?> serialClass;
    private final Object serialObject;
    private final Map<String, ObjectStreamField> fields;
    private final StreamData.OfBytes primData;
    private final StreamData.OfObjects objectData;
    private final List<StreamData> streamData;
    private GetFieldImpl getField;
    private CapturedObjectInput objectInput;
    private int state = ST_INITIAL;

    public CapturedObjectInputStream(final ObjectDeserializer.Context context, final Class<?> serialClass,
            final Object serialObject,
            final Map<String, ObjectStreamField> fields, final StreamData.OfBytes primData,
            final StreamData.OfObjects objectData, final List<StreamData> streamData) throws IOException {
        this.context = context;
        this.serialClass = serialClass;
        this.serialObject = serialObject;
        this.fields = Map.copyOf(fields);
        this.primData = primData;
        this.objectData = objectData;
        this.streamData = List.copyOf(streamData);
    }

    protected Object readObjectOverride() throws IOException, ClassNotFoundException {
        return objectInput().readObject();
    }

    public Object readUnshared() throws IOException, ClassNotFoundException {
        throw new UnsupportedOperationException();
    }

    public void defaultReadObject() throws IOException, ClassNotFoundException {
        if (state != ST_INITIAL) {
            throw fieldsAlreadyRead();
        }
        ReadUtil.defaultReadObject(serialClass, serialObject, this);
        // should have done something
        if (state == ST_INITIAL) {
            throw fieldsNotRead();
        }
    }

    public GetField readFields() throws IOException {
        switch (state) {
            case ST_INITIAL -> {
                getField = new GetFieldImpl();
                state = ST_STREAM;
                return getField;
            }
            case ST_STREAM -> throw fieldsAlreadyRead();
            case ST_CLOSED -> throw closed();
            default -> throw Assert.impossibleSwitchCase(state);
        }
    }

    public void registerValidation(final ObjectInputValidation obj, final int prio)
            throws NotActiveException, InvalidObjectException {
        throw new UnsupportedOperationException();
    }

    public int read() throws IOException {
        return objectInput().read();
    }

    public int read(final byte[] b) throws IOException {
        return objectInput().read(b);
    }

    public int read(final byte[] buf, final int off, final int len) throws IOException {
        return objectInput().read(buf, off, len);
    }

    public int available() throws IOException {
        return objectInput().available();
    }

    public boolean readBoolean() throws IOException {
        return objectInput().readBoolean();
    }

    public byte readByte() throws IOException {
        return objectInput().readByte();
    }

    public int readUnsignedByte() throws IOException {
        return objectInput().readUnsignedByte();
    }

    public char readChar() throws IOException {
        return objectInput().readChar();
    }

    public short readShort() throws IOException {
        return objectInput().readShort();
    }

    public int readUnsignedShort() throws IOException {
        return objectInput().readUnsignedShort();
    }

    public int readInt() throws IOException {
        return objectInput().readInt();
    }

    public long readLong() throws IOException {
        return objectInput().readLong();
    }

    public float readFloat() throws IOException {
        return objectInput().readFloat();
    }

    public double readDouble() throws IOException {
        return objectInput().readDouble();
    }

    public void readFully(final byte[] buf) throws IOException {
        objectInput().readFully(buf);
    }

    public void readFully(final byte[] buf, final int off, final int len) throws IOException {
        objectInput().readFully(buf, off, len);
    }

    public int skipBytes(final int len) throws IOException {
        return objectInput().skipBytes(len);
    }

    @Deprecated
    public String readLine() throws IOException {
        return objectInput().readLine();
    }

    public String readUTF() throws IOException {
        return objectInput().readUTF();
    }

    public long skip(final long n) throws IOException {
        return objectInput().skip(n);
    }

    public void close() throws IOException {
    }

    private CapturedObjectInput objectInput() throws IOException {
        switch (state) {
            case ST_INITIAL -> throw fieldsNotRead();
            case ST_STREAM -> {
                CapturedObjectInput objectInput = this.objectInput;
                if (objectInput == null) {
                    this.objectInput = objectInput = new CapturedObjectInput(context, streamData);
                }
                return objectInput;
            }
            case ST_CLOSED -> throw closed();
            default -> throw Assert.impossibleSwitchCase(state);
        }
    }

    private static NotActiveException fieldsAlreadyRead() {
        return new NotActiveException("Object stream fields were already read");
    }

    private static NotActiveException fieldsNotRead() {
        return new NotActiveException("Object stream fields were not read");
    }

    private static IOException closed() {
        return new IOException("Stream closed");
    }

    private class GetFieldImpl extends GetField {
        public ObjectStreamClass getObjectStreamClass() {
            throw new UnsupportedOperationException();
        }

        public boolean defaulted(final String name) {
            return !fields.containsKey(name);
        }

        public boolean get(final String name, final boolean val) {
            return fields.containsKey(name) ? primData.getBoolean(offs(name, true)) : val;
        }

        public byte get(final String name, final byte val) {
            return fields.containsKey(name) ? primData.getByte(offs(name, true)) : val;
        }

        public char get(final String name, final char val) {
            return fields.containsKey(name) ? primData.getChar(offs(name, true)) : val;
        }

        public short get(final String name, final short val) {
            return fields.containsKey(name) ? primData.getShort(offs(name, true)) : val;
        }

        public int get(final String name, final int val) {
            return fields.containsKey(name) ? primData.getInt(offs(name, true)) : val;
        }

        public long get(final String name, final long val) {
            return fields.containsKey(name) ? primData.getLong(offs(name, true)) : val;
        }

        public float get(final String name, final float val) {
            return fields.containsKey(name) ? primData.getFloat(offs(name, true)) : val;
        }

        public double get(final String name, final double val) {
            return fields.containsKey(name) ? primData.getDouble(offs(name, true)) : val;
        }

        public Object get(final String name, final Object val) throws /* TODO: JDK 18+ ClassNotFoundException, */ IOException {
            try {
                return fields.containsKey(name) ? context.deserialize(objectData.getObject(offs(name, false))) : val;
            } catch (ClassNotFoundException e) {
                throw Util.sneak(e);
            }
        }
    }

    private int offs(String name, boolean primitive) {
        ObjectStreamField field = fields.get(name);
        if (field.isPrimitive() == primitive) {
            return field.getOffset();
        } else {
            throw new IllegalArgumentException("Field " + name + " has an unexpected kind");
        }
    }
}
