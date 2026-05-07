package io.smallrye.common.serial.impl;

import java.io.EOFException;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInput;
import java.io.UTFDataFormatException;
import java.util.List;
import java.util.Objects;

import io.smallrye.common.serial.StreamData;
import io.smallrye.common.serial.spi.ObjectDeserializer;

public final class CapturedObjectInput implements ObjectInput {
    private final ObjectDeserializer.Context ctxt;
    private final List<StreamData> data;
    private int listIndex;
    private int index;

    public CapturedObjectInput(final ObjectDeserializer.Context ctxt, final List<StreamData> data) {
        this.ctxt = ctxt;
        this.data = data;
    }

    private StreamData current() {
        return listIndex < data.size() ? data.get(listIndex) : null;
    }

    private StreamData next() {
        int size = data.size();
        if (listIndex == size) {
            return null;
        } else {
            index = 0;
            if (++listIndex == size) {
                return null;
            } else {
                return data.get(listIndex);
            }
        }
    }

    private StreamData.OfObjects objects() throws IOException {
        StreamData current = current();
        for (;;) {
            if (current == null) {
                throw new EOFException();
            } else if (current instanceof StreamData.OfObjects o && index < o.size()) {
                return o;
            } else if (current instanceof StreamData.OfBytes b && index < b.size()) {
                throw new NotActiveException("No object at this point in the stream");
            }
            current = next();
        }
    }

    private StreamData.OfBytes bytesOrNonBytes() {
        StreamData current = current();
        for (;;) {
            if (current == null) {
                return null;
            } else if (current instanceof StreamData.OfBytes b && index < b.size()) {
                return b;
            } else if (current instanceof StreamData.OfObjects o && index < o.size()) {
                return null;
            }
            current = next();
        }
    }

    private StreamData.OfBytes bytesOrEof() throws IOException {
        StreamData current = current();
        for (;;) {
            if (current == null) {
                return null;
            } else if (current instanceof StreamData.OfBytes b && index < b.size()) {
                return b;
            } else if (current instanceof StreamData.OfObjects o && index < o.size()) {
                throw new IOException("No data at this point in the stream");
            }
            current = next();
        }
    }

    private StreamData.OfBytes bytes() throws IOException {
        StreamData.OfBytes b = bytesOrEof();
        if (b == null) {
            throw new EOFException();
        }
        return b;
    }

    public Object readObject() throws ClassNotFoundException, IOException {
        return ctxt.deserialize(objects().getObject(index++));
    }

    public int read() throws IOException {
        StreamData.OfBytes b = bytesOrEof();
        if (b == null) {
            return -1;
        }
        return Byte.toUnsignedInt(b.getByte(index++));
    }

    public int read(final byte[] array) throws IOException {
        return read(array, 0, array.length);
    }

    public int read(final byte[] array, final int off, final int len) throws IOException {
        Objects.checkFromIndexSize(off, len, array.length);
        StreamData.OfBytes b = bytesOrEof();
        if (b == null) {
            return -1;
        }
        int cnt = 0;
        while (cnt < len && b != null) {
            int copyLen = Math.min(len - cnt, b.size() - index);
            b.get(index, array, off + cnt, copyLen);
            cnt += copyLen;
            index += copyLen;
            b = bytesOrEof();
        }
        return cnt;
    }

    public long skip(final long n) throws IOException {
        long cnt = 0;
        StreamData.OfBytes b = bytesOrNonBytes();
        while (b != null && cnt < n) {
            int skip = (int) Math.min(n - cnt, b.size() - index);
            index += skip;
            cnt += skip;
            b = bytesOrNonBytes();
        }
        return cnt;
    }

    public int available() throws IOException {
        StreamData.OfBytes b = bytesOrNonBytes();
        return b == null ? 0 : b.size() - index;
    }

    public void close() throws IOException {
        listIndex = data.size();
        index = 0;
    }

    public void readFully(final byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    public void readFully(final byte[] b, final int off, final int len) throws IOException {
        if (len == 0) {
            return;
        }
        int t = read(b, off, len);
        if (t < len) {
            throw new EOFException();
        }
    }

    public int skipBytes(final int n) throws IOException {
        return (int) skip(n);
    }

    public boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    public byte readByte() throws IOException {
        int v = read();
        if (v == -1) {
            throw new EOFException();
        }
        return (byte) v;
    }

    public int readUnsignedByte() throws IOException {
        return Byte.toUnsignedInt(readByte());
    }

    public short readShort() throws IOException {
        return (short) readUnsignedShort();
    }

    public int readUnsignedShort() throws IOException {
        StreamData.OfBytes b = bytes();
        int index = this.index;
        if (index <= b.size() - 2) {
            this.index += 2;
            return b.getChar(index);
        } else {
            return readUnsignedByte() << 8 | readUnsignedByte();
        }
    }

    public char readChar() throws IOException {
        return (char) readUnsignedShort();
    }

    public int readInt() throws IOException {
        StreamData.OfBytes b = bytes();
        int index = this.index;
        if (index <= b.size() - 4) {
            this.index += 4;
            return b.getInt(index);
        } else {
            return readShort() << 16 | readUnsignedShort();
        }
    }

    public long readUnsignedInt() throws IOException {
        return Integer.toUnsignedLong(readInt());
    }

    public long readLong() throws IOException {
        StreamData.OfBytes b = bytes();
        int index = this.index;
        if (index <= b.size() - 8) {
            this.index += 8;
            return b.getLong(index);
        } else {
            return (long) readInt() << 32 | readUnsignedInt();
        }
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public String readLine() throws IOException {
        StreamData.OfBytes b = bytesOrEof();
        if (b == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int c = b.getByte(index++);
        for (;;) {
            switch (c) {
                case '\r' -> {
                    // peek at the next byte
                    b = bytesOrNonBytes();
                    if (b != null && b.getByte(index) == '\n') {
                        index++;
                    }
                    return sb.toString();
                }
                case '\n' -> {
                    return sb.toString();
                }
                default -> sb.append((char) c);
            }
            if (index == b.size()) {
                b = bytesOrEof();
            }
            if (b == null) {
                return sb.toString();
            }
            c = b.getByte(index++);
        }
    }

    public String readUTF() throws IOException {
        int len = readUnsignedShort();
        StringBuilder sb = new StringBuilder(len >> 1);
        int i = 0;
        while (i < len) {
            int a = readUnsignedByte();
            // count leading ones
            switch (Integer.numberOfLeadingZeros(~a)) {
                // one-byte character
                case 0 -> {
                    sb.append((char) a);
                    i++;
                }
                // two-byte character
                case 2 -> {
                    if (i + 1 >= len) {
                        throw new UTFDataFormatException();
                    }
                    int b = readUnsignedByte();
                    if ((b & 0xc0) != 0x80) {
                        throw new UTFDataFormatException();
                    }
                    sb.append((char) ((a & 0x1F) << 6 | b & 0x3F));
                    i += 2;
                }
                // three-byte character
                case 3 -> {
                    if (i + 2 >= len) {
                        throw new UTFDataFormatException();
                    }
                    int b = readUnsignedShort();
                    if ((b & 0xc0c0) != 0x8080) {
                        throw new UTFDataFormatException();
                    }
                    // todo: JDK 19+ Integer.compress(b, 0x3F3F)
                    sb.append((char) ((a & 0x0F) << 12 | (b & 0x3F00) >> 2 | b & 0x3F));
                    i += 3;
                }
                default -> throw new UTFDataFormatException();
            }
        }
        return sb.toString();
    }
}
