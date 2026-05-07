package io.smallrye.common.serial.impl;

import static io.smallrye.common.serial.impl.Util.*;

import java.io.IOException;
import java.io.ObjectOutput;
import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.smallrye.common.serial.Serialized;
import io.smallrye.common.serial.StreamData;
import io.smallrye.common.serial.spi.ObjectSerializer;

/**
 * An implementation of {@code ObjectOutput} which captures the stream data as it is written.
 */
public final class CapturingObjectOutput implements ObjectOutput {

    /**
     * Idle state (neither objects nor binary data has been written).
     */
    private static final int ST_IDLE = 0;
    /**
     * Bytes are being written to the buffer.
     */
    private static final int ST_BYTES = 1;
    /**
     * Objects are being written to the object list.
     */
    private static final int ST_OBJECTS = 2;
    /**
     * The stream is finished.
     */
    private static final int ST_CLOSED = 3;

    private final ObjectSerializer.Context context;

    /**
     * The current state (one of the {@code ST_} constants).
     */
    private int state;
    /**
     * The stream data which has been captured.
     * Initially empty, uses small immutable lists until it is no longer practical to continue doing so,
     * then switches to an {@code ArrayList}.
     */
    private List<StreamData> streamData = List.of();

    /**
     * The temporary byte buffer for capturing binary data.
     * Initially {@code null}, lazily created and grown as needed.
     */
    private byte[] buffer;
    /**
     * The current buffer size, which is always less than or equal to {@code buffer.length}.
     */
    private int bufferSize;

    /**
     * The temporary object list buffer for capturing nested serialized objects.
     * Initially {@code null}, lazily created and grown as needed.
     */
    private List<Serialized> objects;

    /**
     * Construct a new instance.
     *
     * @param context the serialization context for writing nested objects (must not be {@code null})
     */
    public CapturingObjectOutput(final ObjectSerializer.Context context) {
        this.context = context;
    }

    public void writeObject(final Object obj) throws IOException {
        enterObjectsState();
        if (objects == null) {
            objects = new ArrayList<>();
        }
        objects.add(context.serialize(obj));
    }

    public void write(final int val) throws IOException {
        ensureSpace(1);
        buffer[bufferSize++] = (byte) val;
    }

    public void write(final byte[] buf) throws IOException {
        ensureSpace(buf.length);
        System.arraycopy(buf, 0, buffer, bufferSize, buf.length);
        bufferSize += buf.length;
    }

    public void write(final byte[] buf, final int off, final int len) throws IOException {
        Objects.checkFromIndexSize(off, len, buf.length);
        ensureSpace(len);
        System.arraycopy(buf, off, buffer, bufferSize, len);
        bufferSize += len;
    }

    public void writeBoolean(final boolean val) throws IOException {
        ensureSpace(1);
        buffer[bufferSize++] = (byte) (val ? 1 : 0);
    }

    public void writeByte(final int val) throws IOException {
        ensureSpace(1);
        buffer[bufferSize++] = (byte) val;
    }

    public void writeShort(final int val) throws IOException {
        ensureSpace(2);
        BE16.set(buffer, bufferSize, (short) val);
        bufferSize += 2;
    }

    public void writeChar(final int val) throws IOException {
        writeShort(val);
    }

    public void writeInt(final int val) throws IOException {
        ensureSpace(4);
        BE32.set(buffer, bufferSize, val);
        bufferSize += 4;
    }

    public void writeLong(final long val) throws IOException {
        ensureSpace(8);
        BE64.set(buffer, bufferSize, val);
        bufferSize += 8;
    }

    public void writeFloat(final float val) throws IOException {
        writeInt(Float.floatToRawIntBits(val));
    }

    public void writeDouble(final double val) throws IOException {
        writeLong(Double.doubleToRawLongBits(val));
    }

    public void writeBytes(final String str) throws IOException {
        ensureSpace(str.length());
        for (int i = 0; i < str.length(); i++) {
            writeByte(str.charAt(i));
        }
    }

    public void writeChars(final String str) throws IOException {
        ensureSpace(str.length() << 1);
        for (int i = 0; i < str.length(); i++) {
            writeChar(str.charAt(i));
        }
    }

    public void writeUTF(final String str) throws IOException {
        // estimate
        if (str.length() > 0xffff) {
            // we can tell right away
            throw new UTFDataFormatException("String is too long");
        }
        // rough guess to avoid lots of small reallocations
        ensureSpace(2 + str.length());
        int len = 0;
        // placeholder to hold the actual length
        int lenIdx = bufferSize;
        bufferSize += 2;
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if (c > 0 && c <= 0x7f) {
                ensureSpace(1);
                len++;
                buffer[bufferSize++] = (byte) c;
            } else if (c <= 0x7ff) {
                ensureSpace(2);
                len += 2;
                // todo: JDK 19+ use Integer.expand + BE16
                buffer[bufferSize++] = (byte) (0xc0 | 0x1f & c >> 6);
                buffer[bufferSize++] = (byte) (0x80 | 0x3f & c);
            } else {
                ensureSpace(3);
                len += 3;
                // todo: JDK 19+ use Integer.expand + BE32 + ensureSpace(4)
                buffer[bufferSize++] = (byte) (0xe0 | 0x0f & c >> 12);
                buffer[bufferSize++] = (byte) (0x80 | 0x3f & c >> 6);
                buffer[bufferSize++] = (byte) (0x80 | 0x3f & c);
            }
            if (len > 0xffff) {
                throw new UTFDataFormatException("String is too long");
            }
        }
        // update with the actual length
        BE16.set(buffer, lenIdx, (short) len);
    }

    public void flush() {
        switch (state) {
            case ST_BYTES -> exitBytesState();
            case ST_OBJECTS -> exitObjectsState();
        }
        assert state == ST_IDLE || state == ST_CLOSED;
    }

    public void close() {
        if (state != ST_CLOSED) {
            flush();
            // release temporary storage
            buffer = null;
            bufferSize = 0;
            objects = null;
            state = ST_CLOSED;
        }
    }

    // -- package-private --

    /**
     * {@return the captured stream data (not {@code null})}
     */
    public List<StreamData> streamData() {
        if (state != ST_CLOSED) {
            throw new IllegalStateException("Stream is not closed");
        }
        return streamData == null ? List.of() : List.copyOf(streamData);
    }

    // -- private --

    /**
     * Bytes state only.
     * Ensure that there is at least {@code size} bytes of space at {@code bufferSize} before {@code buffer.length}.
     * Grow the array as needed.
     *
     * @param size the desired size
     * @throws IOException if the stream is closed
     */
    private void ensureSpace(final int size) throws IOException {
        assert size >= 0;
        enterBytesState();
        int proposedSize = bufferSize + size;
        if (proposedSize < 0) {
            // probably OOME by now, but let's be gracious about it
            exitBytesState();
            ensureSpace(size);
            return;
        }
        if (buffer == null) {
            buffer = new byte[Math.max(64, proposedSize)];
        } else if (proposedSize > buffer.length) {
            // grow by 50% or by 1MB, whichever is less (!)
            int growSize = Math.min(buffer.length >> 1, 1 << 20) + buffer.length;
            if (growSize < 0) {
                // the largest possible size! (roughly)
                growSize = Integer.MAX_VALUE - 16;
            }
            buffer = Arrays.copyOf(buffer, Math.max(growSize, bufferSize + size));
        }
        // otherwise, buffer is OK
    }

    private void enterBytesState() throws IOException {
        switch (state) {
            case ST_IDLE -> state = ST_BYTES;
            case ST_BYTES -> {
            }
            case ST_OBJECTS -> {
                exitObjectsState();
                state = ST_BYTES;
            }
            case ST_CLOSED -> throw closed();
        }
    }

    private void exitBytesState() {
        if (bufferSize > 0) {
            addStreamData(StreamData.of(buffer, 0, bufferSize));
            // save the buffer for future data
            bufferSize = 0;
        }
        state = ST_IDLE;
    }

    private void enterObjectsState() throws IOException {
        switch (state) {
            case ST_IDLE -> state = ST_OBJECTS;
            case ST_BYTES -> {
                exitBytesState();
                state = ST_OBJECTS;
            }
            case ST_OBJECTS -> {
            }
            case ST_CLOSED -> throw closed();
        }
    }

    private void exitObjectsState() {
        if (objects != null && !objects.isEmpty()) {
            addStreamData(StreamData.of(objects));
            // save the list for future items
            objects.clear();
        }
        state = ST_IDLE;
    }

    private void addStreamData(final StreamData streamData) {
        switch (this.streamData.size()) {
            case 0 -> this.streamData = List.of(streamData);
            case 1 -> this.streamData = List.of(this.streamData.get(0), streamData);
            case 2 -> {
                ArrayList<StreamData> newList = new ArrayList<>(16);
                newList.addAll(this.streamData);
                newList.add(streamData);
                this.streamData = newList;
            }
            default -> this.streamData.add(streamData);
        }
    }

    private IOException closed() {
        return new IOException("Stream closed");
    }
}
