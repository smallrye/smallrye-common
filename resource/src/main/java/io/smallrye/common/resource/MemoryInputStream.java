package io.smallrye.common.resource;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * An input stream over a segment of memory.
 */
public final class MemoryInputStream extends InputStream {
    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final ByteBuffer CLOSED = ByteBuffer.allocateDirect(0);

    ByteBuffer buf;
    int mark = -1;

    /**
     * Construct a new instance for a byte buffer.
     * The given buffer contents are not copied.
     * Consuming the stream will not affect the buffer's position or limit.
     * Modifying the buffer's position or limit will not affect operation of the stream.
     *
     * @param buffer the byte buffer containing the stream data (must not be {@code null})
     */
    public MemoryInputStream(final ByteBuffer buffer) {
        buf = buffer.duplicate();
    }

    /**
     * Construct a new instance for a byte array.
     * The byte array is not copied.
     *
     * @param bytes the byte array (must not be {@code null})
     */
    public MemoryInputStream(final byte[] bytes) {
        this(ByteBuffer.wrap(bytes));
    }

    // todo: MemorySegment variation

    public int read() throws IOException {
        ByteBuffer buf = this.buf;
        checkClosed(buf);
        return buf.hasRemaining() ? Byte.toUnsignedInt(buf.get()) : -1;
    }

    public int read(final byte[] b, final int off, final int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        ByteBuffer buf = this.buf;
        checkClosed(buf);
        int rem = buf.remaining();
        if (rem == 0) {
            return -1;
        }
        int cnt = Math.min(len, rem);
        buf.get(b, off, cnt);
        return cnt;
    }

    public byte[] readAllBytes() throws IOException {
        ByteBuffer buf = this.buf;
        checkClosed(buf);
        int rem = buf.remaining();
        if (rem == 0) {
            return EMPTY_BYTES;
        }
        byte[] bytes = new byte[rem];
        buf.get(bytes);
        return bytes;
    }

    public long skip(final long n) throws IOException {
        ByteBuffer buf = this.buf;
        checkClosed(buf);
        int pos = buf.position();
        int lim = buf.limit();
        int cnt = (int) Math.min(n, lim - pos);
        if (cnt > 0) {
            buf.position(pos + cnt);
        }
        return cnt;
    }

    public long transferTo(final OutputStream out) throws IOException {
        ByteBuffer buf = this.buf;
        checkClosed(buf);
        int pos = buf.position();
        int lim = buf.limit();
        if (pos == lim) {
            return 0;
        } else if (out instanceof FileOutputStream) {
            FileOutputStream fos = (FileOutputStream) out;
            return fos.getChannel().write(buf);
        }
        int rem = lim - pos;
        if (buf.hasArray()) {
            // shortcut
            int offs = buf.arrayOffset() + pos;
            out.write(buf.array(), offs, rem);
            buf.position(lim);
            return rem;
        } else if (rem <= 8192) {
            byte[] b = readAllBytes();
            out.write(b);
            return b.length;
        } else {
            // not much else we can do to improve on the default case
            return super.transferTo(out);
        }
    }

    public void mark(final int bytes) {
        mark = buf.position();
    }

    public void reset() throws IOException {
        ByteBuffer buf = this.buf;
        checkClosed(buf);
        int mark = this.mark;
        if (mark == -1) {
            throw new IOException("No mark set");
        }
        buf.position(mark);
    }

    public boolean markSupported() {
        return true;
    }

    public int available() {
        return buf.remaining();
    }

    public void close() {
        buf = CLOSED;
    }

    private static void checkClosed(final ByteBuffer buf) throws IOException {
        if (buf == CLOSED) {
            throw new IOException("Stream closed");
        }
    }
}
