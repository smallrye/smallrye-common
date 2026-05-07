package io.smallrye.common.io;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import io.smallrye.common.constraint.Assert;

/**
 * A buffered random-access file that supports reading and writing variously-sized integers,
 * floating-point values, byte arrays, NIO {@link ByteBuffer}s, and strings at both the current
 * file position and at explicit (random-access) file positions.
 * <p>
 * The buffer is used for both reading and writing. Sequential read and write operations advance
 * the file position; random-access operations (those accepting an explicit position parameter)
 * do not. The default byte order is configurable and may be changed during the lifetime of
 * the file; explicit little-endian and big-endian method variants are also provided.
 *
 * <h2>Thread safety</h2>
 * This class uses a lightweight compare-and-swap lock to detect illegal concurrent access.
 * If two threads attempt to use the same instance concurrently, the second thread will receive
 * an {@link IllegalStateException}. Re-entrant calls from the same thread are permitted
 * (the lock is recursive within a single thread).
 *
 * <h2>Stream and channel views</h2>
 * The {@link #inputStream()} and {@link #outputStream()} methods return cached views that
 * extend {@link FileInputStream} and {@link FileOutputStream} respectively and also implement
 * {@link DataInput} and {@link DataOutput} (with the big-endian semantics mandated by those
 * interfaces, independent of this file's configured byte order). The {@link #channel()} method
 * returns a cached {@link FileChannel} view.
 * <p>
 * <strong>The input and output stream views share a single file position with this
 * {@code BufferedFile}, so mixing their usage may produce unexpected results unless the
 * shared position is explicitly considered.</strong>
 * <p>
 * Calling {@link Closeable#close() close()} on any view is a no-op; closing the
 * {@code BufferedFile} itself closes all views.
 *
 * <h2>Append mode</h2>
 * Append mode is not supported by this API.
 *
 * <h2>File descriptor and buffering caveats</h2>
 * The {@linkplain #fileDescriptor() file descriptor} is exposed for advanced use, but
 * <strong>directly accessing or modifying the file position through the file descriptor will
 * result in inconsistent behavior</strong>. Furthermore, the backing file may not reflect
 * buffered data that has not yet been {@linkplain #flush() flushed}.
 *
 * <h2>Byte order</h2>
 * The byte order used by methods on this class is the configurable default (or the explicit
 * LE/BE variant). This differs from the {@link DataInput}/{@link DataOutput} methods on the
 * stream views, which always use big-endian per their contract. The
 * {@link #writeFloat(float)}/{@link #writeDouble(double)} methods use raw bit patterns
 * ({@link Float#floatToRawIntBits}, {@link Double#doubleToRawLongBits}), preserving NaN
 * payloads.
 *
 * <h2>Buffer boundary behavior</h2>
 * Random-access operations that partially overlap the buffer boundary are decomposed into
 * smaller sub-operations; this is a rare edge case and not a performance concern in practice.
 *
 * @see Files2#openBuffered(Path, OpenOption...)
 */
public final class BufferedFile implements Closeable, Flushable {

    // ── VarHandles for byte-order-aware buffer access ───────────────────

    private static final VarHandle SHORT_LE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN)
            .withInvokeExactBehavior();
    private static final VarHandle SHORT_BE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN)
            .withInvokeExactBehavior();
    private static final VarHandle INT_LE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN)
            .withInvokeExactBehavior();
    private static final VarHandle INT_BE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN)
            .withInvokeExactBehavior();
    private static final VarHandle LONG_LE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN)
            .withInvokeExactBehavior();
    private static final VarHandle LONG_BE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN)
            .withInvokeExactBehavior();

    // ── VarHandle for lightweight lock ──────────────────────────────────

    private static final VarHandle OWNER = ConstantBootstraps.fieldVarHandle(
            MethodHandles.lookup(), "owner", VarHandle.class, BufferedFile.class, Thread.class).withInvokeExactBehavior();

    // ── Instance fields ─────────────────────────────────────────────────

    private final RandomAccessFile raf;
    private final byte[] buffer;
    private final boolean writeMode;
    private final boolean readMode;

    private long bufferPosition;
    private int bufferDataSize;
    private boolean dirty;
    private long position;
    private ByteOrder byteOrder;
    private boolean closed;

    @SuppressWarnings("unused") // accessed via OWNER VarHandle
    private volatile Thread owner;

    // cached views
    private BufferedFileInputStream inputStream;
    private BufferedFileOutputStream outputStream;
    private BufferedFileChannel channel;

    // ── Constructor (package-private) ───────────────────────────────────

    BufferedFile(RandomAccessFile raf, int bufferSize, ByteOrder byteOrder, boolean readMode, boolean writeMode)
            throws IOException {
        this.raf = raf;
        this.buffer = new byte[bufferSize];
        this.byteOrder = byteOrder;
        this.readMode = readMode;
        this.writeMode = writeMode;
        long fp = raf.getFilePointer();
        this.bufferPosition = fp;
        this.bufferDataSize = 0;
        this.position = fp;
    }

    // ── Locking ─────────────────────────────────────────────────────────

    /**
     * Attempt to acquire the lightweight lock.
     *
     * @return {@code true} if the lock was acquired, {@code false} if it was already held
     *         by the current thread (re-entrant)
     * @throws IllegalStateException if the lock is held by another thread
     */
    private boolean lock() {
        Thread current = Thread.currentThread();
        Thread existing = (Thread) OWNER.compareAndExchangeAcquire(this, (Thread) null, current);
        if (existing == null) {
            return true;
        }
        if (existing == current) {
            return false;
        }
        throw new IllegalStateException("Illegal concurrent access from " + current + "; owner is " + owner);
    }

    /**
     * Release the lightweight lock. Must only be called if {@link #lock()} returned {@code true}.
     */
    private void unlock() {
        OWNER.setRelease(this, (Thread) null);
    }

    /**
     * {@return {@code true} if the current thread holds the lightweight lock}
     */
    private boolean holdsLock() {
        return (Thread) OWNER.getOpaque(this) == Thread.currentThread();
    }

    private void checkOpen() throws IOException {
        assert holdsLock();
        if (closed) {
            throw new IOException("File is closed");
        }
    }

    private void checkReadable() throws IOException {
        assert holdsLock();
        checkOpen();
        if (!readMode) {
            throw new IOException("File is not open for reading");
        }
    }

    private void checkWritable() throws IOException {
        assert holdsLock();
        checkOpen();
        if (!writeMode) {
            throw new IOException("File is not open for writing");
        }
    }

    // ── Buffer management ───────────────────────────────────────────────

    /**
     * Flush dirty buffer contents to the file without invalidating the buffer.
     */
    private void flushDirty() throws IOException {
        assert holdsLock();
        if (dirty) {
            if (bufferDataSize > 0) {
                raf.seek(bufferPosition);
                raf.write(buffer, 0, bufferDataSize);
            }
            dirty = false;
        }
    }

    /**
     * Ensure there is space in the buffer for a sequential write of {@code needed} bytes,
     * and adjust the buffer data size appropriately.
     * The buffer is positioned so that the next write appends at the current position.
     * The caller is expected to overwrite {@code needed} bytes.
     *
     * @return the index of the write
     */
    private int ensureSpace(int needed) throws IOException {
        assert holdsLock();
        assert needed <= buffer.length;
        // find out where the current position is in relation to the buffer
        long relStart = position - bufferPosition;
        if (relStart >= 0 && relStart + needed <= buffer.length) {
            // OK (there is space; either overwriting existing, or appending to buffer, or a mix of the two)
            if (bufferDataSize < relStart + needed) {
                bufferDataSize = (int) relStart + needed;
            }
            return (int) relStart;
        }
        // reposition and empty the buffer
        flushDirty();
        bufferPosition = position;
        bufferDataSize = needed;
        return 0;
    }

    /**
     * Attempt to fill the buffer with at least {@code bytes} bytes of data.
     * If a value less than {@code bytes} is returned, then that is the number
     * of bytes that can be read before reaching EOF (at the time of the call).
     * <p>
     * If the buffer has remaining capacity, data is read into the buffer at the current
     * end of valid data, extending the valid region without affecting the dirty state.
     * If the buffer does not have enough remaining space, it is flushed (if dirty) and
     * compacted before reading.
     *
     * @param bytes the minimum number of bytes to attempt to read (must not be negative, and must be less than
     *        the buffer size)
     * @return the actual number of bytes currently available in the buffer, or -1 if end-of-file
     *         was reached and no bytes could be read
     * @throws IOException if an I/O error occurs
     */
    public int fill(int bytes) throws IOException {
        Assert.checkMinimumParameter("bytes", 0, bytes);
        Assert.checkMaximumParameter("bytes", buffer.length, bytes);
        boolean locked = lock();
        try {
            checkReadable();
            // compute the number of readable bytes
            long relStart = position - bufferPosition;
            if (relStart < 0 || relStart > bufferDataSize) {
                // the current position is outside the buffer; we must discard the buffer
                flushDirty();
                raf.seek(position);
                bufferPosition = position;
                int n = raf.read(buffer, bufferDataSize, buffer.length - bufferDataSize);
                if (n == -1) {
                    bufferDataSize = 0;
                    return -1;
                }
                bufferDataSize += n;
                return n;
            }
            // the current position is inside the buffer
            int readable = (int) (bufferDataSize - relStart);
            if (readable >= bytes) {
                return readable;
            }
            // compute how many more bytes can fit into the buffer without compaction
            int space = buffer.length - bufferDataSize;
            if (space + readable < bytes) {
                // not enough space left to satisfy the request; we need to flush it out & compact
                compact();
                space = buffer.length - bufferDataSize;
            }
            raf.seek(position);
            // there's enough space left (now) to fill it the rest of the way
            int n = raf.read(buffer, bufferDataSize, space);
            if (n == -1) {
                return readable == 0 ? -1 : readable;
            }
            bufferDataSize += n;
            return readable + n;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    private void compact() throws IOException {
        int readPos = (int) (position - bufferPosition);
        int readable = bufferDataSize - readPos;
        flushDirty();
        // compact the data in the buffer to the beginning of the buffer
        System.arraycopy(buffer, readPos, buffer, 0, readable);
        bufferPosition = position;
    }

    // ── Position / Size ─────────────────────────────────────────────────

    /**
     * {@return the current file position}
     *
     * @throws IOException if an I/O error occurs
     */
    public long filePosition() throws IOException {
        boolean locked = lock();
        try {
            checkOpen();
            return position;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Set the file position.
     * <p>
     * This method does not flush or invalidate the buffer. If the file has been
     * {@linkplain #setLength(long) truncated} to a length smaller than the given position,
     * subsequent sequential reads will see EOF and subsequent sequential writes will extend
     * the file (with a zero-filled gap, per {@link RandomAccessFile} semantics).
     *
     * @param position the new file position
     * @throws IOException if the file is in append mode, or if an I/O error occurs
     */
    public void seek(long position) throws IOException {
        Assert.checkMinimumParameter("position", 0, position);
        boolean locked = lock();
        try {
            checkOpen();
            this.position = position;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * {@return the current file length, including any buffered but unflushed data}
     *
     * @throws IOException if an I/O error occurs
     */
    public long length() throws IOException {
        boolean locked = lock();
        try {
            checkOpen();
            long rafLen = raf.length();
            if (dirty) {
                long bufferedEnd = bufferPosition + bufferDataSize;
                return Math.max(rafLen, bufferedEnd);
            }
            return rafLen;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * {@return the number of bytes between the current position and the end of the file}
     *
     * @throws IOException if an I/O error occurs
     */
    public long remaining() throws IOException {
        boolean locked = lock();
        try {
            checkOpen();
            return lengthInternal() - position;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    private long lengthInternal() throws IOException {
        assert holdsLock();
        long rafLen = raf.length();
        if (dirty) {
            long bufferedEnd = bufferPosition + bufferDataSize;
            return Math.max(rafLen, bufferedEnd);
        }
        return rafLen;
    }

    /**
     * Set the length of the file.
     * <p>
     * If {@code newLength} is less than the current length, the file is truncated.
     * The current {@linkplain #filePosition() file position} is <em>not</em> automatically
     * adjusted; if it exceeds the new length, subsequent sequential reads will see EOF
     * and sequential writes will extend the file with a zero-filled gap.
     * Call {@link #seek(long)} to reposition if needed.
     * <p>
     * If the buffer window extends beyond the new length, the valid portion of the buffer
     * is trimmed. If the entire buffer lies beyond the new length, it is invalidated.
     * <p>
     * If {@code newLength} is greater than the current length, the file is extended with
     * zero bytes (per {@link RandomAccessFile} semantics).
     *
     * @param newLength the desired file length
     * @throws IOException if an I/O error occurs
     */
    public void setLength(long newLength) throws IOException {
        Assert.checkMinimumParameter("newLength", 0, newLength);
        boolean locked = lock();
        try {
            checkWritable();
            // Trim or invalidate buffer if it extends beyond the new length
            if (bufferDataSize > 0) {
                long bufferEnd = bufferPosition + bufferDataSize;
                if (bufferPosition >= newLength) {
                    // Entire buffer is beyond new length; invalidate
                    bufferDataSize = 0;
                    dirty = false;
                } else if (bufferEnd > newLength) {
                    // Buffer partially extends beyond new length; trim
                    bufferDataSize = (int) (newLength - bufferPosition);
                    // dirty status is preserved for the remaining portion
                }
            }
            raf.setLength(newLength);
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Skip forward by the given number of bytes.
     *
     * @param count the number of bytes to skip
     * @return the number of bytes actually skipped
     * @throws IOException if an I/O error occurs
     */
    public long skip(long count) throws IOException {
        boolean locked = lock();
        try {
            checkOpen();
            if (count <= 0) {
                return 0;
            }
            return relSeekInternal(count);
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Safely update the current {@code position} with a relative offset.
     * Seeks past the beginning of the file or past {@code Long.MAX_VALUE}
     * are forbidden.
     *
     * @param offset the file position offset
     * @return the byte offset
     * @throws IOException if the seek is invalid
     */
    private long relSeekInternal(final long offset) throws IOException {
        assert holdsLock();
        long oldPos = position;
        long newPos = oldPos + offset;
        if (newPos < 0) {
            if (offset < 0) {
                throw new IOException("Seek before beginning of file");
            } else {
                throw new IOException("Seek position is too large");
            }
        }
        if (offset != 0) {
            // update the position
            position = newPos;
            return newPos - oldPos;
        } else {
            return 0;
        }
    }

    /**
     * {@return the file descriptor for this file}
     * <p>
     * <strong>Warning:</strong> directly accessing or modifying the file position through
     * this descriptor will result in inconsistent behavior. The backing file may not
     * reflect buffered data that has not yet been flushed, among other potential issues.
     *
     * @throws IOException if an I/O error occurs
     */
    public FileDescriptor fileDescriptor() throws IOException {
        return raf.getFD();
    }

    // ── Byte order ──────────────────────────────────────────────────────

    /**
     * {@return the current default byte order}
     */
    public ByteOrder byteOrder() {
        return byteOrder;
    }

    /**
     * Set the default byte order.
     *
     * @param byteOrder the new byte order (must not be {@code null})
     */
    public void byteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    // ── Sequential write: byte ──────────────────────────────────────────

    /**
     * Write a single byte at the current position and advance.
     *
     * @param value the byte value (only the low 8 bits are used)
     * @throws IOException if an I/O error occurs
     */
    public void writeByte(int value) throws IOException {
        boolean locked = lock();
        try {
            checkWritable();
            buffer[ensureSpace(1)] = (byte) value;
            dirty = true;
            relSeekInternal(1);
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    // ── Sequential read: byte ───────────────────────────────────────────

    /**
     * Read a single byte at the current position and advance.
     *
     * @return the byte value (sign-extended to {@code int}), or -1 at end-of-file
     * @throws IOException if an I/O error occurs
     */
    public int read() throws IOException {
        boolean locked = lock();
        try {
            int res = peek();
            if (res != -1) {
                relSeekInternal(1);
            }
            return res;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a single byte at the current position without advancing.
     *
     * @return the byte value (sign-extended to {@code int}), or -1 at end-of-file
     * @throws IOException if an I/O error occurs
     */
    public int peek() throws IOException {
        boolean locked = lock();
        try {
            //checkReadable(); // this is done inside of fill(xx)
            assert holdsLock();
            int avail = fill(1);
            if (avail < 1) {
                return -1;
            }
            int relStart = (int) (position - bufferPosition);
            return Byte.toUnsignedInt(buffer[relStart]);
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a single byte at the current position and advance.
     *
     * @return the byte value
     * @throws EOFException if end-of-file is reached
     * @throws IOException if an I/O error occurs
     */
    public byte readByte() throws IOException {
        int res = read();
        if (res == -1) {
            throw new EOFException();
        }
        return (byte) res;
    }

    /**
     * Read a single byte at the current position without advancing.
     *
     * @return the byte value
     * @throws EOFException if end-of-file is reached
     * @throws IOException if an I/O error occurs
     */
    public byte peekByte() throws IOException {
        int res = peek();
        if (res == -1) {
            throw new EOFException();
        }
        return (byte) res;
    }

    /**
     * Read a single unsigned byte at the current position and advance.
     *
     * @return the byte value as an unsigned {@code int} (in the range 0-255)
     * @throws EOFException if end-of-file is reached
     * @throws IOException if an I/O error occurs
     */
    public int readUnsignedByte() throws IOException {
        return Byte.toUnsignedInt(readByte());
    }

    /**
     * Read a single unsigned byte at the current position without advancing.
     *
     * @return the byte value as an unsigned {@code int} (in the range 0-255)
     * @throws EOFException if end-of-file is reached
     * @throws IOException if an I/O error occurs
     */
    public int peekUnsignedByte() throws IOException {
        return Byte.toUnsignedInt(peekByte());
    }

    // ── Sequential write: short ─────────────────────────────────────────

    /**
     * Write a 16-bit value at the current position using the default byte order and advance.
     *
     * @param value the value (only the low 16 bits are used)
     * @throws IOException if an I/O error occurs
     */
    public void writeShort(int value) throws IOException {
        boolean locked = lock();
        try {
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                writeShortLE(value);
            } else {
                writeShortBE(value);
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Write a 16-bit value at the current position using little-endian byte order and advance.
     *
     * @param value the value (only the low 16 bits are used)
     * @throws IOException if an I/O error occurs
     */
    public void writeShortLE(int value) throws IOException {
        boolean locked = lock();
        try {
            checkWritable();
            SHORT_LE.set(buffer, ensureSpace(2), (short) value);
            dirty = true;
            relSeekInternal(2);
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Write a 16-bit value at the current position using big-endian byte order and advance.
     *
     * @param value the value (only the low 16 bits are used)
     * @throws IOException if an I/O error occurs
     */
    public void writeShortBE(int value) throws IOException {
        boolean locked = lock();
        try {
            checkWritable();
            SHORT_BE.set(buffer, ensureSpace(2), (short) value);
            dirty = true;
            relSeekInternal(2);
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    // ── Sequential read: short ──────────────────────────────────────────

    /**
     * Read a 16-bit value at the current position using the default byte order and advance.
     *
     * @return the value (sign-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public short readShort() throws IOException {
        boolean locked = lock();
        try {
            short res = peekShort();
            relSeekInternal(2);
            return res;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 16-bit value at the current position using the default byte order without advancing.
     *
     * @return the value (sign-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public short peekShort() throws IOException {
        boolean locked = lock();
        try {
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                return peekShortLE();
            } else {
                return peekShortBE();
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 16-bit value at the current position using little-endian byte order and advance.
     *
     * @return the value (sign-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public short readShortLE() throws IOException {
        boolean locked = lock();
        try {
            short res = peekShortLE();
            relSeekInternal(2);
            return res;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 16-bit value at the current position using little-endian byte order without advancing.
     *
     * @return the value (sign-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public short peekShortLE() throws IOException {
        boolean locked = lock();
        try {
            checkReadable();
            if (fill(2) < 2) {
                throw new EOFException();
            }
            return (short) SHORT_LE.get(buffer, (int) (position - bufferPosition));
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 16-bit value at the current position using big-endian byte order and advance.
     *
     * @return the value (sign-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public short readShortBE() throws IOException {
        boolean locked = lock();
        try {
            short res = peekShortBE();
            relSeekInternal(2);
            return res;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 16-bit value at the current position using big-endian byte order without advancing.
     *
     * @return the value (sign-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public short peekShortBE() throws IOException {
        boolean locked = lock();
        try {
            checkReadable();
            if (fill(2) < 2) {
                throw new EOFException();
            }
            int relStart = (int) (position - bufferPosition);
            return (short) SHORT_BE.get(buffer, relStart);
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read an unsigned 16-bit value at the current position using the default byte order and advance.
     *
     * @return the value (zero-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int readUnsignedShort() throws IOException {
        return Short.toUnsignedInt(readShort());
    }

    /**
     * Read an unsigned 16-bit value at the current position using the default byte order without advancing.
     *
     * @return the value (zero-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int peekUnsignedShort() throws IOException {
        return Short.toUnsignedInt(peekShort());
    }

    /**
     * Read an unsigned 16-bit value at the current position using little-endian byte order and advance.
     *
     * @return the value (zero-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int readUnsignedShortLE() throws IOException {
        return Short.toUnsignedInt(readShortLE());
    }

    /**
     * Read an unsigned 16-bit value at the current position using little-endian byte order without advancing.
     *
     * @return the value (zero-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int peekUnsignedShortLE() throws IOException {
        return Short.toUnsignedInt(peekShortLE());
    }

    /**
     * Read an unsigned 16-bit value at the current position using big-endian byte order and advance.
     *
     * @return the value (zero-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int readUnsignedShortBE() throws IOException {
        return Short.toUnsignedInt(readShortBE());
    }

    /**
     * Read an unsigned 16-bit value at the current position using big-endian byte order without advancing.
     *
     * @return the value (zero-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int peekUnsignedShortBE() throws IOException {
        return Short.toUnsignedInt(peekShortBE());
    }

    // ── Sequential write: int ───────────────────────────────────────────

    /**
     * Write a 32-bit value at the current position using the default byte order and advance.
     *
     * @param value the value
     * @throws IOException if an I/O error occurs
     */
    public void writeInt(int value) throws IOException {
        boolean locked = lock();
        try {
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                writeIntLE(value);
            } else {
                writeIntBE(value);
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Write a 32-bit value at the current position using little-endian byte order and advance.
     *
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeIntLE(int value) throws IOException {
        boolean locked = lock();
        try {
            checkWritable();
            INT_LE.set(buffer, ensureSpace(4), value);
            dirty = true;
            relSeekInternal(4);
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Write a 32-bit value at the current position using big-endian byte order and advance.
     *
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeIntBE(int value) throws IOException {
        boolean locked = lock();
        try {
            checkWritable();
            INT_BE.set(buffer, ensureSpace(4), value);
            dirty = true;
            relSeekInternal(4);
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    // ── Sequential read: int ────────────────────────────────────────────

    /**
     * Read a 32-bit value at the current position using the default byte order and advance.
     *
     * @return the value
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int readInt() throws IOException {
        boolean locked = lock();
        try {
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                return readIntLE();
            } else {
                return readIntBE();
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 32-bit value at the current position using the default byte order without advancing.
     *
     * @return the value
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int peekInt() throws IOException {
        boolean locked = lock();
        try {
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                return peekIntLE();
            } else {
                return peekIntBE();
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 32-bit value at the current position using little-endian byte order and advance.
     *
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int readIntLE() throws IOException {
        boolean locked = lock();
        try {
            int res = peekIntLE();
            relSeekInternal(4);
            return res;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 32-bit value at the current position using little-endian byte order without advancing.
     *
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int peekIntLE() throws IOException {
        boolean locked = lock();
        try {
            checkReadable();
            if (fill(4) < 4) {
                throw new EOFException();
            }
            return (int) INT_LE.get(buffer, (int) (position - bufferPosition));
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 32-bit value at the current position using big-endian byte order and advance.
     *
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int readIntBE() throws IOException {
        boolean locked = lock();
        try {
            int res = peekIntBE();
            relSeekInternal(4);
            return res;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 32-bit value at the current position using big-endian byte order without advancing.
     *
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int peekIntBE() throws IOException {
        boolean locked = lock();
        try {
            checkReadable();
            if (fill(4) < 4) {
                throw new EOFException();
            }
            return (int) INT_BE.get(buffer, (int) (position - bufferPosition));
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 32-bit unsigned value at the current position using the default byte order and advance.
     *
     * @return the unsigned value
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long readUnsignedInt() throws IOException {
        return Integer.toUnsignedLong(readInt());
    }

    /**
     * Read a 32-bit unsigned value at the current position using the default byte order without advancing.
     *
     * @return the unsigned value
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long peekUnsignedInt() throws IOException {
        return Integer.toUnsignedLong(peekInt());
    }

    /**
     * Read a 32-bit unsigned value at the current position using little-endian byte order and advance.
     *
     * @return the unsigned value
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long readUnsignedIntLE() throws IOException {
        return Integer.toUnsignedLong(readIntLE());
    }

    /**
     * Read a 32-bit unsigned value at the current position using little-endian byte order without advancing.
     *
     * @return the unsigned value
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long peekUnsignedIntLE() throws IOException {
        return Integer.toUnsignedLong(peekIntLE());
    }

    /**
     * Read a 32-bit unsigned value at the current position using big-endian byte order and advance.
     *
     * @return the unsigned value
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long readUnsignedIntBE() throws IOException {
        return Integer.toUnsignedLong(readIntBE());
    }

    /**
     * Read a 32-bit unsigned value at the current position using big-endian byte order without advancing.
     *
     * @return the unsigned value
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long peekUnsignedIntBE() throws IOException {
        return Integer.toUnsignedLong(peekIntBE());
    }

    // ── Sequential write: long ──────────────────────────────────────────

    /**
     * Write a 64-bit value at the current position using the default byte order and advance.
     *
     * @param value the value
     * @throws IOException if an I/O error occurs
     */
    public void writeLong(long value) throws IOException {
        boolean locked = lock();
        try {
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                writeLongLE(value);
            } else {
                writeLongBE(value);
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Write a 64-bit value at the current position using little-endian byte order and advance.
     *
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeLongLE(long value) throws IOException {
        boolean locked = lock();
        try {
            checkWritable();
            LONG_LE.set(buffer, ensureSpace(8), value);
            dirty = true;
            relSeekInternal(8);
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Write a 64-bit value at the current position using big-endian byte order and advance.
     *
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeLongBE(long value) throws IOException {
        boolean locked = lock();
        try {
            checkWritable();
            LONG_BE.set(buffer, ensureSpace(8), value);
            dirty = true;
            relSeekInternal(8);
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    // ── Sequential read: long ───────────────────────────────────────────

    /**
     * Read a 64-bit value at the current position using the default byte order and advance.
     *
     * @return the value
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long readLong() throws IOException {
        boolean locked = lock();
        try {
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                return readLongLE();
            } else {
                return readLongBE();
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 64-bit value at the current position using the default byte order without advancing.
     *
     * @return the value
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long peekLong() throws IOException {
        boolean locked = lock();
        try {
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                return peekLongLE();
            } else {
                return peekLongBE();
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 64-bit value at the current position using little-endian byte order and advance.
     *
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long readLongLE() throws IOException {
        boolean locked = lock();
        try {
            long res = peekLongLE();
            relSeekInternal(8);
            return res;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 64-bit value at the current position using little-endian byte order without advancing.
     *
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long peekLongLE() throws IOException {
        boolean locked = lock();
        try {
            checkReadable();
            if (fill(8) < 8) {
                throw new EOFException();
            }
            return (long) LONG_LE.get(buffer, (int) (position - bufferPosition));
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 64-bit value at the current position using big-endian byte order and advance.
     *
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long readLongBE() throws IOException {
        boolean locked = lock();
        try {
            long res = peekLongBE();
            relSeekInternal(8);
            return res;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 64-bit value at the current position using big-endian byte order without advancing.
     *
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long peekLongBE() throws IOException {
        boolean locked = lock();
        try {
            checkReadable();
            if (fill(8) < 8) {
                throw new EOFException();
            }
            return (long) LONG_BE.get(buffer, (int) (position - bufferPosition));
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    // ── Sequential: float, double, boolean, char ────────────────────────

    /**
     * Write a 32-bit floating-point value using raw bit patterns at the current position using the default byte order and
     * advance.
     *
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeFloat(float value) throws IOException {
        writeInt(Float.floatToRawIntBits(value));
    }

    /**
     * Write a 32-bit floating-point value using raw bit patterns at the current position using little-endian byte order and
     * advance.
     *
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeFloatLE(float value) throws IOException {
        writeIntLE(Float.floatToRawIntBits(value));
    }

    /**
     * Write a 32-bit floating-point value using raw bit patterns at the current position using big-endian byte order and
     * advance.
     *
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeFloatBE(float value) throws IOException {
        writeIntBE(Float.floatToRawIntBits(value));
    }

    /**
     * Read a 32-bit floating-point value using raw bit patterns at the current position using the default byte order and
     * advance.
     *
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * Read a 32-bit floating-point value using raw bit patterns at the current position using little-endian byte order and
     * advance.
     *
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public float readFloatLE() throws IOException {
        return Float.intBitsToFloat(readIntLE());
    }

    /**
     * Read a 32-bit floating-point value using raw bit patterns at the current position using big-endian byte order and
     * advance.
     *
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public float readFloatBE() throws IOException {
        return Float.intBitsToFloat(readIntBE());
    }

    /**
     * Write a 64-bit floating-point value using raw bit patterns at the current position using the default byte order and
     * advance.
     *
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeDouble(double value) throws IOException {
        writeLong(Double.doubleToRawLongBits(value));
    }

    /**
     * Write a 64-bit floating-point value using raw bit patterns at the current position using little-endian byte order and
     * advance.
     *
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeDoubleLE(double value) throws IOException {
        writeLongLE(Double.doubleToRawLongBits(value));
    }

    /**
     * Write a 64-bit floating-point value using raw bit patterns at the current position using big-endian byte order and
     * advance.
     *
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeDoubleBE(double value) throws IOException {
        writeLongBE(Double.doubleToRawLongBits(value));
    }

    /**
     * Read a 64-bit floating-point value using raw bit patterns at the current position using the default byte order and
     * advance.
     *
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Read a 64-bit floating-point value using raw bit patterns at the current position using little-endian byte order and
     * advance.
     *
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public double readDoubleLE() throws IOException {
        return Double.longBitsToDouble(readLongLE());
    }

    /**
     * Read a 64-bit floating-point value using raw bit patterns at the current position using big-endian byte order and
     * advance.
     *
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public double readDoubleBE() throws IOException {
        return Double.longBitsToDouble(readLongBE());
    }

    /**
     * Write a boolean value (1 byte: 0 for false, 1 for true) and advance.
     *
     * @param value the boolean value
     * @throws IOException if an I/O error occurs
     */
    public void writeBoolean(boolean value) throws IOException {
        writeByte(value ? 1 : 0);
    }

    /**
     * Read a boolean value (1 byte: 0 for false, non-zero for true) and advance.
     *
     * @return the boolean value
     * @throws EOFException if end-of-file is reached
     * @throws IOException if an I/O error occurs
     */
    public boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    /**
     * Write a 16-bit char at the current position using the default byte order and advance.
     *
     * @param value the char value (only the low 16 bits of the {@code int} are used)
     * @throws IOException if an I/O error occurs
     */
    public void writeChar(int value) throws IOException {
        writeShort(value);
    }

    /**
     * Write a 16-bit character value at the current position using little-endian byte order and advance.
     *
     * @param value the value (only the low 16 bits are used)
     * @throws IOException if an I/O error occurs
     */
    public void writeCharLE(int value) throws IOException {
        writeShortLE(value);
    }

    /**
     * Write a 16-bit character value at the current position using big-endian byte order and advance.
     *
     * @param value the value (only the low 16 bits are used)
     * @throws IOException if an I/O error occurs
     */
    public void writeCharBE(int value) throws IOException {
        writeShortBE(value);
    }

    /**
     * Read a 16-bit char at the current position using the default byte order and advance.
     *
     * @return the char value
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public char readChar() throws IOException {
        return (char) readShort();
    }

    /**
     * Read a 16-bit character value at the current position using little-endian byte order and advance.
     *
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public char readCharLE() throws IOException {
        return (char) readShortLE();
    }

    /**
     * Read a 16-bit character value at the current position using big-endian byte order and advance.
     *
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public char readCharBE() throws IOException {
        return (char) readShortBE();
    }

    // ── Random-access write: byte ───────────────────────────────────────

    /**
     * Write a single byte at the given file position without changing the current position.
     *
     * @param pos the file position
     * @param value the byte value (only the low 8 bits are used)
     * @throws IOException if an I/O error occurs
     */
    public void writeByte(long pos, int value) throws IOException {
        Assert.checkMinimumParameter("pos", 0, pos);
        boolean locked = lock();
        try {
            checkWritable();
            long relStart = pos - bufferPosition;
            if (relStart >= 0 && relStart < buffer.length - 1) {
                // it'll fit in the buffer (we might have to extend it a little)
                buffer[(int) relStart] = (byte) value;
                dirty = true;
                if (relStart > bufferDataSize - 1) {
                    bufferDataSize = (int) (relStart + 1);
                }
            } else {
                // write the value directly
                raf.seek(pos);
                raf.writeByte(value);
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    // ── Random-access read: byte ────────────────────────────────────────

    /**
     * Read a single byte at the given file position without changing the current position.
     *
     * @param pos the file position
     * @return the byte value
     * @throws EOFException if end-of-file is reached
     * @throws IOException if an I/O error occurs
     */
    public byte readByte(long pos) throws IOException {
        Assert.checkMinimumParameter("pos", 0, pos);
        boolean locked = lock();
        try {
            checkReadable();
            long relStart = pos - bufferPosition;
            if (relStart >= 0 && relStart < bufferDataSize) {
                return buffer[(int) relStart];
            }
            // not in buffer
            raf.seek(pos);
            return raf.readByte();
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a single unsigned byte at the given file position without changing the current position.
     *
     * @param pos the file position
     * @return the unsigned byte value
     * @throws EOFException if end-of-file is reached
     * @throws IOException if an I/O error occurs
     */
    public int readUnsignedByte(long pos) throws IOException {
        return Byte.toUnsignedInt(readByte(pos));
    }

    // ── Random-access write: short ──────────────────────────────────────

    /**
     * Write a 16-bit value at the given file position using the default byte order
     * without changing the current position.
     *
     * @param pos the file position
     * @param value the value (only the low 16 bits are used)
     * @throws IOException if an I/O error occurs
     */
    public void writeShort(long pos, int value) throws IOException {
        boolean locked = lock();
        try {
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                writeShortLE(pos, value);
            } else {
                writeShortBE(pos, value);
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Write a 16-bit value at the given file position using little-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @param value the value (only the low 16 bits are used)
     * @throws IOException if an I/O error occurs
     */
    public void writeShortLE(long pos, int value) throws IOException {
        Assert.checkMinimumParameter("pos", 0, pos);
        Assert.checkMaximumParameter("pos", Long.MAX_VALUE - 1, pos);
        boolean locked = lock();
        try {
            checkWritable();
            long relStart = pos - bufferPosition;
            if (relStart >= 0 && relStart <= buffer.length - 2 && relStart <= bufferDataSize) {
                // it'll fit in the buffer (we might have to extend it a little)
                SHORT_LE.set(buffer, (int) relStart, (short) value);
                dirty = true;
                if (relStart > bufferDataSize - 2) {
                    bufferDataSize = (int) (relStart + 2);
                }
            } else {
                if (relStart >= -1 && relStart < bufferDataSize) {
                    // try hard to make the write be atomic, but to do so, we must flush
                    flushDirty();
                    bufferDataSize = 0;
                }
                // write the value directly
                raf.seek(pos);
                raf.writeShort(Short.reverseBytes((short) value));
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Write a 16-bit value at the given file position using big-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @param value the value (only the low 16 bits are used)
     * @throws IOException if an I/O error occurs
     */
    public void writeShortBE(long pos, int value) throws IOException {
        Assert.checkMinimumParameter("pos", 0, pos);
        Assert.checkMaximumParameter("pos", Long.MAX_VALUE - 1, pos);
        boolean locked = lock();
        try {
            long relStart = pos - bufferPosition;
            if (relStart >= 0 && relStart <= buffer.length - 2 && relStart <= bufferDataSize) {
                // it'll fit in the buffer (we might have to extend it a little)
                SHORT_BE.set(buffer, (int) relStart, (short) value);
                dirty = true;
                if (relStart > bufferDataSize - 2) {
                    bufferDataSize = (int) (relStart + 2);
                }
            } else {
                if (relStart >= -1 && relStart < bufferDataSize) {
                    // try hard to make the write be atomic, but to do so, we must flush
                    flushDirty();
                    bufferDataSize = 0;
                }
                // write the value directly
                raf.seek(pos);
                raf.writeShort(value);
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    // ── Random-access read: short ───────────────────────────────────────

    /**
     * Read a 16-bit value at the given file position using the default byte order
     * without changing the current position.
     *
     * @param pos the file position
     * @return the value (sign-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public short readShort(long pos) throws IOException {
        boolean locked = lock();
        try {
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                return readShortLE(pos);
            } else {
                return readShortBE(pos);
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 16-bit value at the given file position using little-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public short readShortLE(long pos) throws IOException {
        Assert.checkMinimumParameter("pos", 0, pos);
        Assert.checkMaximumParameter("pos", Long.MAX_VALUE - 1, pos);
        boolean locked = lock();
        try {
            checkReadable();
            assert holdsLock();
            long relStart = pos - bufferPosition;
            if (relStart >= 0 && relStart < bufferDataSize - 1) {
                return (short) SHORT_LE.get(buffer, (int) relStart);
            }
            // not in buffer
            if (relStart >= -1 && relStart < bufferDataSize) {
                // flush if the data spans the edge of the buffer so we read atomically
                flushDirty();
            }
            raf.seek(pos);
            return Short.reverseBytes(raf.readShort());
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 16-bit value at the given file position using big-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public short readShortBE(long pos) throws IOException {
        Assert.checkMinimumParameter("pos", 0, pos);
        Assert.checkMaximumParameter("pos", Long.MAX_VALUE - 1, pos);
        boolean locked = lock();
        try {
            checkReadable();
            assert holdsLock();
            long relStart = pos - bufferPosition;
            if (relStart >= 0 && relStart < bufferDataSize - 1) {
                return (short) SHORT_BE.get(buffer, (int) relStart);
            }
            // not in buffer
            if (relStart >= -1 && relStart < bufferDataSize) {
                // flush if the data spans the edge of the buffer so we read atomically
                flushDirty();
            }
            raf.seek(pos);
            return raf.readShort();
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read an unsigned 16-bit value at the given file position using the default byte order
     * without changing the current position.
     *
     * @param pos the file position
     * @return the value (zero-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int readUnsignedShort(long pos) throws IOException {
        return readChar(pos);
    }

    /**
     * Read an unsigned 16-bit value at the given file position using little-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value (zero-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int readUnsignedShortLE(long pos) throws IOException {
        return readCharLE(pos);
    }

    /**
     * Read an unsigned 16-bit value at the given file position using big-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value (zero-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int readUnsignedShortBE(long pos) throws IOException {
        return readCharBE(pos);
    }

    // ── Random-access write: int ────────────────────────────────────────

    /**
     * Write a 32-bit value at the given file position using the default byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeInt(long pos, int value) throws IOException {
        boolean locked = lock();
        try {
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                writeIntLE(pos, value);
            } else {
                writeIntBE(pos, value);
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Write a 32-bit value at the given file position using little-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeIntLE(long pos, int value) throws IOException {
        Assert.checkMinimumParameter("pos", 0, pos);
        Assert.checkMaximumParameter("pos", Long.MAX_VALUE - 3, pos);
        boolean locked = lock();
        try {
            checkWritable();
            long relStart = pos - bufferPosition;
            if (relStart >= 0 && relStart <= buffer.length - 4 && relStart <= bufferDataSize) {
                // it'll fit in the buffer (we might have to extend it a little)
                INT_LE.set(buffer, (int) relStart, value);
                dirty = true;
                if (relStart > bufferDataSize - 4) {
                    bufferDataSize = (int) (relStart + 4);
                }
            } else {
                if (relStart >= -3 && relStart < bufferDataSize) {
                    // try hard to make the write be atomic, but to do so, we must flush
                    flushDirty();
                    bufferDataSize = 0;
                }
                // write it directly
                raf.seek(pos);
                raf.writeInt(Integer.reverseBytes(value));
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Write a 32-bit value at the given file position using big-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeIntBE(long pos, int value) throws IOException {
        Assert.checkMinimumParameter("pos", 0, pos);
        Assert.checkMaximumParameter("pos", Long.MAX_VALUE - 3, pos);
        boolean locked = lock();
        try {
            checkWritable();
            long relStart = pos - bufferPosition;
            if (relStart >= 0 && relStart <= buffer.length - 4 && relStart <= bufferDataSize) {
                // it'll fit in the buffer (we might have to extend it a little)
                INT_BE.set(buffer, (int) relStart, value);
                dirty = true;
                if (relStart > bufferDataSize - 4) {
                    bufferDataSize = (int) (relStart + 4);
                }
            } else {
                if (relStart >= -3 && relStart < bufferDataSize) {
                    // try hard to make the write be atomic, but to do so, we must flush
                    flushDirty();
                    bufferDataSize = 0;
                }
                // write it directly
                raf.seek(pos);
                raf.writeInt(value);
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    // ── Random-access read: int ─────────────────────────────────────────

    /**
     * Read a 32-bit value at the given file position using the default byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int readInt(long pos) throws IOException {
        boolean locked = lock();
        try {
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                return readIntLE(pos);
            } else {
                return readIntBE(pos);
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 32-bit value at the given file position using little-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int readIntLE(long pos) throws IOException {
        Assert.checkMinimumParameter("pos", 0, pos);
        Assert.checkMaximumParameter("pos", Long.MAX_VALUE - 3, pos);
        boolean locked = lock();
        try {
            checkReadable();
            long relStart = pos - bufferPosition;
            if (relStart >= 0 && relStart < bufferDataSize - 3) {
                return (int) INT_LE.get(buffer, (int) relStart);
            }
            // not in buffer
            if (relStart >= -3 && relStart < bufferDataSize - 3) {
                // flush if the data spans the edge of the buffer so we read atomically
                flushDirty();
            }
            raf.seek(pos);
            return Integer.reverseBytes(raf.readInt());
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 32-bit value at the given file position using big-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public int readIntBE(long pos) throws IOException {
        Assert.checkMinimumParameter("pos", 0, pos);
        Assert.checkMaximumParameter("pos", Long.MAX_VALUE - 3, pos);
        boolean locked = lock();
        try {
            checkReadable();
            long relStart = pos - bufferPosition;
            if (relStart >= 0 && relStart < bufferDataSize - 3) {
                return (int) INT_BE.get(buffer, (int) relStart);
            }
            // not in buffer
            if (relStart >= -3 && relStart < bufferDataSize - 3) {
                // flush if the data spans the edge of the buffer so we read atomically
                flushDirty();
            }
            raf.seek(pos);
            return raf.readInt();
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read an unsigned 32-bit value at the given file position using the default byte order
     * without changing the current position.
     *
     * @param pos the file position
     * @return the value (zero-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long readUnsignedInt(long pos) throws IOException {
        return Integer.toUnsignedLong(readInt(pos));
    }

    /**
     * Read an unsigned 32-bit value at the given file position using little-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value (zero-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long readUnsignedIntLE(long pos) throws IOException {
        return Integer.toUnsignedLong(readIntLE(pos));
    }

    /**
     * Read an unsigned 32-bit value at the given file position using big-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value (zero-extended)
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long readUnsignedIntBE(long pos) throws IOException {
        return Integer.toUnsignedLong(readIntBE(pos));
    }

    // ── Random-access write: long ───────────────────────────────────────

    /**
     * Write a 64-bit value at the given file position using the default byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeLong(long pos, long value) throws IOException {
        boolean locked = lock();
        try {
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                writeLongLE(pos, value);
            } else {
                writeLongBE(pos, value);
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Write a 64-bit value at the given file position using little-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeLongLE(long pos, long value) throws IOException {
        Assert.checkMinimumParameter("pos", 0, pos);
        Assert.checkMaximumParameter("pos", Long.MAX_VALUE - 7, pos);
        boolean locked = lock();
        try {
            checkWritable();
            long relStart = pos - bufferPosition;
            if (relStart >= 0 && relStart <= buffer.length - 8 && relStart <= bufferDataSize) {
                // it'll fit in the buffer (we might have to extend it a little)
                LONG_LE.set(buffer, (int) relStart, value);
                dirty = true;
                if (relStart > bufferDataSize - 8) {
                    bufferDataSize = (int) (relStart + 8);
                }
            } else {
                if (relStart >= -7 && relStart < bufferDataSize) {
                    // try hard to make the write be atomic, but to do so, we must flush
                    flushDirty();
                    bufferDataSize = 0;
                }
                // write it directly
                raf.seek(pos);
                raf.writeLong(Long.reverseBytes(value));
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Write a 64-bit value at the given file position using big-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeLongBE(long pos, long value) throws IOException {
        Assert.checkMinimumParameter("pos", 0, pos);
        Assert.checkMaximumParameter("pos", Long.MAX_VALUE - 7, pos);
        boolean locked = lock();
        try {
            checkWritable();
            long relStart = pos - bufferPosition;
            if (relStart >= 0 && relStart <= buffer.length - 8 && relStart <= bufferDataSize) {
                // it'll fit in the buffer (we might have to extend it a little)
                LONG_BE.set(buffer, (int) relStart, value);
                dirty = true;
                if (relStart > bufferDataSize - 8) {
                    bufferDataSize = (int) (relStart + 8);
                }
            } else {
                if (relStart >= -7 && relStart < bufferDataSize) {
                    // try hard to make the write be atomic, but to do so, we must flush
                    flushDirty();
                    bufferDataSize = 0;
                }
                // write it directly
                raf.seek(pos);
                raf.writeLong(value);
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    // ── Random-access read: long ────────────────────────────────────────

    /**
     * Read a 64-bit value at the given file position using the default byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long readLong(long pos) throws IOException {
        boolean locked = lock();
        try {
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                return readLongLE(pos);
            } else {
                return readLongBE(pos);
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 64-bit value at the given file position using little-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long readLongLE(long pos) throws IOException {
        Assert.checkMinimumParameter("pos", 0, pos);
        Assert.checkMaximumParameter("pos", Long.MAX_VALUE - 7, pos);
        boolean locked = lock();
        try {
            checkReadable();
            long relStart = pos - bufferPosition;
            if (relStart >= 0 && relStart < bufferDataSize - 7) {
                return (long) LONG_LE.get(buffer, (int) relStart);
            }
            // not in buffer
            if (relStart >= -7 && relStart < bufferDataSize - 7) {
                // flush if the data spans the edge of the buffer so we read atomically
                flushDirty();
            }
            raf.seek(pos);
            return Long.reverseBytes(raf.readLong());
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a 64-bit value at the given file position using big-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public long readLongBE(long pos) throws IOException {
        Assert.checkMinimumParameter("pos", 0, pos);
        boolean locked = lock();
        try {
            checkReadable();
            long relStart = pos - bufferPosition;
            if (relStart >= 0 && relStart < bufferDataSize - 7) {
                return (long) LONG_BE.get(buffer, (int) relStart);
            }
            // not in buffer
            if (relStart >= -7 && relStart < bufferDataSize - 7) {
                // flush if the data spans the edge of the buffer so we read atomically
                flushDirty();
            }
            raf.seek(pos);
            return raf.readLong();
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    // ── Random-access: float, double, boolean, char ─────────────────────

    /**
     * Write a 32-bit floating-point value using raw bit patterns at the given file position using the default byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeFloat(long pos, float value) throws IOException {
        writeInt(pos, Float.floatToRawIntBits(value));
    }

    /**
     * Write a 32-bit floating-point value using raw bit patterns at the given file position using little-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeFloatLE(long pos, float value) throws IOException {
        writeIntLE(pos, Float.floatToRawIntBits(value));
    }

    /**
     * Write a 32-bit floating-point value using raw bit patterns at the given file position using big-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeFloatBE(long pos, float value) throws IOException {
        writeIntBE(pos, Float.floatToRawIntBits(value));
    }

    /**
     * Read a 32-bit floating-point value using raw bit patterns at the given file position using the default byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public float readFloat(long pos) throws IOException {
        return Float.intBitsToFloat(readInt(pos));
    }

    /**
     * Read a 32-bit floating-point value using raw bit patterns at the given file position using little-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public float readFloatLE(long pos) throws IOException {
        return Float.intBitsToFloat(readIntLE(pos));
    }

    /**
     * Read a 32-bit floating-point value using raw bit patterns at the given file position using big-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public float readFloatBE(long pos) throws IOException {
        return Float.intBitsToFloat(readIntBE(pos));
    }

    /**
     * Write a 64-bit floating-point value using raw bit patterns at the given file position using the default byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeDouble(long pos, double value) throws IOException {
        writeLong(pos, Double.doubleToRawLongBits(value));
    }

    /**
     * Write a 64-bit floating-point value using raw bit patterns at the given file position using little-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeDoubleLE(long pos, double value) throws IOException {
        writeLongLE(pos, Double.doubleToRawLongBits(value));
    }

    /**
     * Write a 64-bit floating-point value using raw bit patterns at the given file position using big-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeDoubleBE(long pos, double value) throws IOException {
        writeLongBE(pos, Double.doubleToRawLongBits(value));
    }

    /**
     * Read a 64-bit floating-point value using raw bit patterns at the given file position using the default byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public double readDouble(long pos) throws IOException {
        return Double.longBitsToDouble(readLong(pos));
    }

    /**
     * Read a 64-bit floating-point value using raw bit patterns at the given file position using little-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public double readDoubleLE(long pos) throws IOException {
        return Double.longBitsToDouble(readLongLE(pos));
    }

    /**
     * Read a 64-bit floating-point value using raw bit patterns at the given file position using big-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public double readDoubleBE(long pos) throws IOException {
        return Double.longBitsToDouble(readLongBE(pos));
    }

    /**
     * Write a boolean value at the given file position without advancing the current position.
     *
     * @param pos the file position
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeBoolean(long pos, boolean value) throws IOException {
        writeByte(pos, value ? 1 : 0);
    }

    /**
     * Read a boolean value at the given file position without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public boolean readBoolean(long pos) throws IOException {
        return readByte(pos) != 0;
    }

    /**
     * Write a 16-bit character value at the given file position using the default byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @param value the value (only the low 16 bits are used)
     * @throws IOException if an I/O error occurs
     */
    public void writeChar(long pos, int value) throws IOException {
        writeShort(pos, value);
    }

    /**
     * Write a 16-bit character value at the given file position using little-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @param value the value (only the low 16 bits are used)
     * @throws IOException if an I/O error occurs
     */
    public void writeCharLE(long pos, int value) throws IOException {
        writeShortLE(pos, value);
    }

    /**
     * Write a 16-bit character value at the given file position using big-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @param value the value (only the low 16 bits are used)
     * @throws IOException if an I/O error occurs
     */
    public void writeCharBE(long pos, int value) throws IOException {
        writeShortBE(pos, value);
    }

    /**
     * Read a 16-bit character value at the given file position using the default byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public char readChar(long pos) throws IOException {
        return (char) readShort(pos);
    }

    /**
     * Read a 16-bit character value at the given file position using little-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public char readCharLE(long pos) throws IOException {
        return (char) readShortLE(pos);
    }

    /**
     * Read a 16-bit character value at the given file position using big-endian byte order
     * without advancing the current position.
     *
     * @param pos the file position
     * @return the value read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public char readCharBE(long pos) throws IOException {
        return (char) readShortBE(pos);
    }

    // ── Sequential byte array operations ────────────────────────────────

    /**
     * Read bytes into the given array at the current position and advance.
     *
     * @param dest the destination array
     * @param offset the start offset in the array
     * @param length the maximum number of bytes to read
     * @return the number of bytes actually read, or -1 at end-of-file
     * @throws IOException if an I/O error occurs
     */
    public int read(byte[] dest, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, dest.length);
        boolean locked = lock();
        try {
            checkReadable();
            assert holdsLock();
            if (length == 0) {
                return 0;
            }
            int cnt = 0;
            // first, fill the dest buffer from our buffer as much as possible (regardless of buffer type)
            long relStart = position - bufferPosition;
            if (relStart >= 0 && relStart < bufferDataSize) {
                // some of the data is in-buffer
                cnt = bufferDataSize - (int) relStart;
                if (cnt >= length) {
                    // buffer contains all the desired data to read
                    System.arraycopy(buffer, (int) relStart, dest, offset, length);
                    relSeekInternal(length);
                    return length;
                }
                // the target is bigger than our buffered data, so copy partial & continue
                System.arraycopy(buffer, (int) relStart, dest, offset, cnt);
                relSeekInternal(cnt);
                offset += cnt;
                length -= cnt;
            } else if (relStart >= -length && relStart < bufferDataSize) {
                // partial overlap; flush for read consistency
                flushDirty();
            }
            // now read the rest directly from the underlying channel
            raf.seek(position);
            int n = raf.read(dest, offset, length);
            if (n > 0) {
                relSeekInternal(n);
                cnt += n;
            } else {
                if (cnt == 0) {
                    return -1;
                }
            }
            return cnt;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read bytes into the given array at the current position and advance.
     *
     * @param dest the destination array
     * @return the number of bytes actually read, or -1 at end-of-file
     * @throws IOException if an I/O error occurs
     */
    public int read(byte[] dest) throws IOException {
        return read(dest, 0, dest.length);
    }

    /**
     * Read exactly {@code length} bytes at the current position and advance.
     * If {@code EOFException} is thrown, the buffer may have been partially populated.
     *
     * @param dest the destination array
     * @param offset the start offset in the array
     * @param length the number of bytes to read
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public void readFully(byte[] dest, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, dest.length);
        boolean locked = lock();
        try {
            int cnt = 0;
            while (cnt < length) {
                int n = read(dest, offset + cnt, length - cnt);
                if (n <= 0) {
                    throw new EOFException();
                }
                cnt += n;
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read exactly {@code dest.length} bytes from the current position into the given array and advance.
     *
     * @param dest the destination array
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public void readFully(byte[] dest) throws IOException {
        readFully(dest, 0, dest.length);
    }

    /**
     * Write bytes from the given array at the current position and advance.
     *
     * @param src the source array
     * @param offset the start offset in the array
     * @param length the number of bytes to write
     * @throws IOException if an I/O error occurs
     */
    public void write(byte[] src, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, src.length);
        boolean locked = lock();
        try {
            checkWritable();
            assert holdsLock();
            if (length <= buffer.length) {
                // we'll buffer all of it
                int cnt = ensureSpace(length);
                System.arraycopy(src, offset, buffer, cnt, length);
                dirty = true;
                relSeekInternal(length);
            } else {
                // flush any dirty data, reset the buffer, and write (most of) the data directly to the backing file
                // `rem` is the number of bytes that we'll put into the buffer at the end (maybe 0)
                int rem = length % buffer.length;
                // `cnt` is the amount of data that we're going to write directly (a multiple of the buffer size)
                int cnt = length - rem;
                // clear the buffer
                flushDirty();
                bufferDataSize = 0;
                // perform the backend write
                raf.seek(position);
                raf.write(src, offset, cnt);
                relSeekInternal(cnt);
                // position the buffer
                bufferPosition = position + cnt;
                // put the rest in the buffer
                System.arraycopy(src, offset + cnt, buffer, ensureSpace(rem), rem);
                bufferDataSize = rem;
                dirty = true;
                relSeekInternal(rem);
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Write all bytes from the given array to the current position and advance.
     *
     * @param src the source byte array
     * @throws IOException if an I/O error occurs
     */
    public void write(byte[] src) throws IOException {
        write(src, 0, src.length);
    }

    // ── Random-access byte array operations ─────────────────────────────

    /**
     * Read bytes at the given file position without changing the current position.
     *
     * @param pos the file position
     * @param dest the destination array
     * @return the number of bytes actually read, or -1 at end-of-file
     * @throws IOException if an I/O error occurs
     */
    public int read(long pos, byte[] dest) throws IOException {
        return read(pos, dest, 0, dest.length);
    }

    /**
     * Read bytes at the given file position without changing the current position.
     *
     * @param pos the file position
     * @param dest the destination array
     * @param offset the start offset in the array
     * @param length the maximum number of bytes to read
     * @return the number of bytes actually read, or -1 at end-of-file
     * @throws IOException if an I/O error occurs
     */
    public int read(long pos, byte[] dest, int offset, int length) throws IOException {
        Assert.checkMinimumParameter("pos", 0, pos);
        Objects.checkFromIndexSize(offset, length, dest.length);
        boolean locked = lock();
        try {
            checkReadable();
            if (length == 0) {
                return 0;
            }
            assert holdsLock();
            long relStart = pos - bufferPosition;
            if (relStart >= 0 && relStart + length <= bufferDataSize) {
                // Fully inside buffer
                System.arraycopy(buffer, (int) relStart, dest, offset, length);
                return length;
            } else {
                if (length + relStart > 0 && relStart < bufferDataSize) {
                    // Partial overlap
                    flushDirty();
                }
                raf.seek(pos);
                return raf.read(dest, offset, length);
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Write bytes at the given file position without changing the current position.
     *
     * @param pos the file position
     * @param src the source array
     * @param offset the start offset in the array
     * @param length the number of bytes to write
     * @throws IOException if an I/O error occurs
     */
    public void write(long pos, byte[] src, int offset, int length) throws IOException {
        Assert.checkMinimumParameter("pos", 0, pos);
        Objects.checkFromIndexSize(offset, length, src.length);
        boolean locked = lock();
        try {
            checkWritable();
            if (length == 0) {
                return;
            }
            assert holdsLock();
            long relStart = pos - bufferPosition;
            if (relStart >= 0 && relStart + length <= bufferDataSize) {
                System.arraycopy(src, offset, buffer, (int) relStart, length);
                dirty = true;
            } else {
                if (length + relStart > 0 && relStart < bufferDataSize) {
                    // Partial overlap
                    flushDirty();
                }
                raf.seek(pos);
                raf.write(src, offset, length);
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    // ── ByteBuffer operations ───────────────────────────────────────────

    /**
     * Read into the given ByteBuffer at the current position and advance.
     *
     * @param dest the destination buffer
     * @return the number of bytes read, or -1 at end-of-file
     * @throws IOException if an I/O error occurs
     */
    public int read(ByteBuffer dest) throws IOException {
        boolean locked = lock();
        try {
            checkReadable();
            if (dest.isReadOnly()) {
                // fail fast
                throw new ReadOnlyBufferException();
            }
            int pos = dest.position();
            int lim = dest.limit();
            int remaining = lim - pos;
            if (dest.hasArray()) {
                // delegate to array-based I/O
                int n = read(dest.array(), dest.arrayOffset() + pos, remaining);
                if (n > 0) {
                    dest.position(pos + n);
                }
                return n;
            }
            int cnt = 0;
            while (remaining > 0) {
                // first, fill the dest buffer from our buffer as much as possible (regardless of buffer type)
                long relStart = position - bufferPosition;
                if (relStart >= 0 && relStart < bufferDataSize) {
                    // some of the data is in-buffer
                    cnt = bufferDataSize - (int) relStart;
                    if (cnt < remaining) {
                        // the target is bigger than our buffered data, so copy & continue
                        dest.put(buffer, (int) relStart, cnt);
                        relSeekInternal(cnt);
                    } else {
                        // we've filled the target buffer, so we're done
                        dest.put(buffer, (int) relStart, remaining);
                        relSeekInternal(remaining);
                        return remaining;
                    }
                    pos += cnt;
                    remaining -= cnt;
                }
                // now we may be able to read the rest from the underlying channel
                if (dest.isDirect()) {
                    // read to direct buffer using FC
                    int n = raf.getChannel().read(dest, position);
                    if (n > 0) {
                        cnt += n;
                        relSeekInternal(n);
                    }
                    return cnt == 0 ? -1 : cnt;
                } else {
                    // continue to transfer slowly through our own buffer
                    cnt = Math.min(remaining, buffer.length);
                    fill(cnt);
                }
            }
            return cnt;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Write from the given ByteBuffer at the current position and advance.
     *
     * @param src the source buffer
     * @throws IOException if an I/O error occurs
     */
    public void write(ByteBuffer src) throws IOException {
        boolean locked = lock();
        try {
            checkWritable();
            int pos = src.position();
            int lim = src.limit();
            int remaining = lim - pos;
            if (remaining <= 0) {
                return;
            }
            if (src.hasArray()) {
                // delegate to array writer
                write(src.array(), src.arrayOffset() + pos, remaining);
                src.position(pos + remaining);
            } else if (remaining <= buffer.length) {
                // we'll buffer all of it
                int index = ensureSpace(remaining);
                src.get(buffer, index, remaining);
            } else if (src.isDirect()) {
                // flush any dirty data, reset the buffer, and write (most of) the data directly to the backing file
                // `rem` is the number of bytes that we'll put into the buffer at the end (maybe 0)
                int rem = remaining % buffer.length;
                // `cnt` is the amount of data that we're going to write directly (a multiple of the buffer size)
                int cnt = remaining - rem;
                // clear the buffer
                flushDirty();
                bufferDataSize = 0;
                // perform the backend write
                raf.seek(position);
                FileChannel ch = raf.getChannel();
                while (cnt > 0) {
                    int n = ch.write(src, position);
                    if (n > 0) {
                        relSeekInternal(n);
                        cnt -= n;
                    } else {
                        throw new IOException("Unexpected partial transfer");
                    }
                }
                // position the buffer
                bufferPosition = position + cnt;
                // put the rest in the buffer
                src.get(buffer, ensureSpace(rem), rem);
                bufferDataSize = rem;
                dirty = true;
                relSeekInternal(rem);
            } else {
                // write all through the buffer
                while (remaining > 0) {
                    int cnt = Math.min(remaining, buffer.length);
                    src.get(buffer, ensureSpace(cnt), cnt);
                    relSeekInternal(cnt);
                    remaining -= cnt;
                }
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read into the given ByteBuffer at the given file position without changing the current position.
     *
     * @param pos the file position
     * @param dest the destination buffer
     * @return the number of bytes read, or -1 at end-of-file
     * @throws IOException if an I/O error occurs
     */
    public int read(long pos, ByteBuffer dest) throws IOException {
        boolean locked = lock();
        try {
            checkReadable();
            int remaining = dest.remaining();
            if (remaining == 0) {
                return 0;
            }
            if (dest.hasArray()) {
                int n = read(pos, dest.array(), dest.arrayOffset() + dest.position(), remaining);
                if (n > 0) {
                    dest.position(dest.position() + n);
                }
                return n;
            } else {
                byte[] tmp = new byte[Math.min(remaining, buffer.length)];
                int totalRead = 0;
                long curPos = pos;
                while (totalRead < remaining) {
                    int toRead = Math.min(remaining - totalRead, tmp.length);
                    int n = read(curPos, tmp, 0, toRead);
                    if (n < 0) {
                        break;
                    }
                    dest.put(tmp, 0, n);
                    totalRead += n;
                    curPos += n;
                    if (n < toRead) {
                        break;
                    }
                }
                return totalRead == 0 ? -1 : totalRead;
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Write from the given ByteBuffer at the given file position without changing the current position.
     *
     * @param pos the file position
     * @param src the source buffer
     * @return the number of bytes written
     * @throws IOException if an I/O error occurs
     */
    public int write(long pos, ByteBuffer src) throws IOException {
        boolean locked = lock();
        try {
            checkWritable();
            int remaining = src.remaining();
            if (remaining == 0) {
                return 0;
            }
            if (src.hasArray()) {
                write(pos, src.array(), src.arrayOffset() + src.position(), remaining);
                src.position(src.position() + remaining);
                return remaining;
            } else {
                byte[] tmp = new byte[Math.min(remaining, buffer.length)];
                int written = 0;
                long curPos = pos;
                while (src.hasRemaining()) {
                    int toRead = Math.min(src.remaining(), tmp.length);
                    src.get(tmp, 0, toRead);
                    write(curPos, tmp, 0, toRead);
                    written += toRead;
                    curPos += toRead;
                }
                return written;
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    // ── String operations ───────────────────────────────────────────────

    /**
     * Write a string encoded with the given charset, followed by a zero byte, and advance.
     *
     * @param s the string to write
     * @param charset the charset to use for encoding
     * @throws IOException if an I/O error occurs
     */
    public void writeZeroTerminatedString(String s, Charset charset) throws IOException {
        byte[] bytes = s.getBytes(charset);
        write(bytes);
        writeByte(0);
    }

    /**
     * Write a UTF-8-encoded string followed by a zero byte and advance.
     *
     * @param s the string to write
     * @throws IOException if an I/O error occurs
     */
    public void writeZeroTerminatedString(String s) throws IOException {
        writeZeroTerminatedString(s, StandardCharsets.UTF_8);
    }

    /**
     * Write a string encoded with the given charset (no terminator) and advance.
     *
     * @param s the string to write
     * @param charset the charset to use for encoding
     * @throws IOException if an I/O error occurs
     */
    public void writeString(String s, Charset charset) throws IOException {
        write(s.getBytes(charset));
    }

    /**
     * Write a UTF-8-encoded string (no terminator) and advance.
     *
     * @param s the string to write
     * @throws IOException if an I/O error occurs
     */
    public void writeString(String s) throws IOException {
        writeString(s, StandardCharsets.UTF_8);
    }

    /**
     * Read a zero-terminated string using the given charset and advance past the terminator.
     *
     * @param charset the charset to use for decoding
     * @return the string (not including the zero terminator)
     * @throws EOFException if end-of-file is reached before a zero byte is found
     * @throws IOException if an I/O error occurs
     */
    public String readZeroTerminatedString(Charset charset) throws IOException {
        boolean locked = lock();
        try {
            checkReadable();
            // Read bytes until we find a zero byte
            byte[] buf = new byte[64];
            int len = 0;
            while (true) {
                int b = readUnsignedByte();
                if (b == 0) {
                    break;
                }
                if (len == buf.length) {
                    buf = Arrays.copyOf(buf, buf.length * 2);
                }
                buf[len++] = (byte) b;
            }
            return new String(buf, 0, len, charset);
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Read a zero-terminated UTF-8 string and advance past the terminator.
     *
     * @return the string
     * @throws EOFException if end-of-file is reached before a zero byte is found
     * @throws IOException if an I/O error occurs
     */
    public String readZeroTerminatedString() throws IOException {
        return readZeroTerminatedString(StandardCharsets.UTF_8);
    }

    /**
     * Read a fixed number of bytes and decode as a string using the given charset.
     *
     * @param byteLength the number of bytes to read
     * @param charset the charset to use for decoding
     * @return the decoded string
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public String readString(int byteLength, Charset charset) throws IOException {
        byte[] bytes = new byte[byteLength];
        readFully(bytes);
        return new String(bytes, charset);
    }

    /**
     * Read a fixed number of bytes and decode as a UTF-8 string.
     *
     * @param byteLength the number of bytes to read
     * @return the decoded string
     * @throws EOFException if end-of-file is reached before all bytes are read
     * @throws IOException if an I/O error occurs
     */
    public String readString(int byteLength) throws IOException {
        return readString(byteLength, StandardCharsets.UTF_8);
    }

    // ── Write zeros ─────────────────────────────────────────────────────

    /**
     * Write the given number of zero bytes at the current position and advance.
     *
     * @param count the number of zero bytes to write
     * @throws IOException if an I/O error occurs
     */
    public void writeZeros(long count) throws IOException {
        boolean locked = lock();
        try {
            checkWritable();
            long remaining = count;
            while (remaining > 0) {
                ensureSpace(0);
                int space = buffer.length - bufferDataSize;
                if (space == 0) {
                    flushDirty();
                    bufferPosition = position;
                    bufferDataSize = 0;
                    dirty = false;
                    space = buffer.length;
                }
                int toWrite = (int) Math.min(remaining, space);
                Arrays.fill(buffer, bufferDataSize, bufferDataSize + toWrite, (byte) 0);
                bufferDataSize += toWrite;
                position += toWrite;
                remaining -= toWrite;
                dirty = true;
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Write zero bytes at the given file position without changing the current position.
     *
     * @param pos the file position
     * @param count the number of zero bytes to write
     * @throws IOException if an I/O error occurs
     */
    public void writeZeros(long pos, long count) throws IOException {
        boolean locked = lock();
        try {
            checkWritable();
            // Use a zeroed temp array for efficiency
            byte[] zeros = new byte[(int) Math.min(count, buffer.length)];
            long remaining = count;
            long curPos = pos;
            while (remaining > 0) {
                int toWrite = (int) Math.min(remaining, zeros.length);
                write(curPos, zeros, 0, toWrite);
                curPos += toWrite;
                remaining -= toWrite;
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    // ── Transfer operations ─────────────────────────────────────────────

    /**
     * Transfer all remaining bytes from the current position to the given output stream.
     *
     * @param out the output stream
     * @return the number of bytes transferred
     * @throws IOException if an I/O error occurs
     */
    public long transferTo(OutputStream out) throws IOException {
        boolean locked = lock();
        try {
            checkReadable();
            long total = 0;
            // first, copy buffered data out (doesn't matter if it's dirty)
            long relStart = position - bufferPosition;
            if (0 <= relStart && relStart < bufferDataSize) {
                // write & advance
                int cnt = bufferDataSize - (int) relStart;
                if (cnt > 0) {
                    out.write(buffer, (int) relStart, cnt);
                    relSeekInternal(cnt);
                    total += cnt;
                }
            }
            // now copy the remaining file content
            if (out instanceof FileOutputStream fos) {
                long cnt = fos.getChannel().transferFrom(raf.getChannel(), position, Long.MAX_VALUE);
                while (cnt > 0) {
                    total += cnt;
                    relSeekInternal(cnt);
                    cnt = fos.getChannel().transferFrom(raf.getChannel(), position, Long.MAX_VALUE);
                }
            } else {
                // slow path
                int n = fill(buffer.length);
                while (n > 0) {
                    relStart = position - bufferPosition;
                    // write it
                    out.write(buffer, (int) relStart, bufferDataSize - (int) relStart);
                    relSeekInternal(n);
                    total += n;
                    n = fill(buffer.length);
                }
            }
            return total;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Transfer up to {@code length} bytes from the current position to the given output stream.
     *
     * @param out the output stream
     * @param length the maximum number of bytes to transfer
     * @return the number of bytes transferred
     * @throws IOException if an I/O error occurs
     */
    public long transferTo(OutputStream out, long length) throws IOException {
        Assert.checkMinimumParameter("length", 0, length);
        boolean locked = lock();
        try {
            checkReadable();
            long total = 0;
            // first, copy buffered data out (doesn't matter if it's dirty)
            long relStart = position - bufferPosition;
            if (0 <= relStart && relStart < bufferDataSize) {
                // write & advance
                int cnt = (int) Math.min(bufferDataSize - (int) relStart, length - total);
                if (cnt > 0) {
                    out.write(buffer, (int) relStart, cnt);
                    relSeekInternal(cnt);
                    total += cnt;
                }
            }
            // now copy the remaining file content
            if (out instanceof FileOutputStream fos) {
                long cnt = fos.getChannel().transferFrom(raf.getChannel(), position, length - total);
                while (cnt > 0) {
                    total += cnt;
                    relSeekInternal(cnt);
                    cnt = fos.getChannel().transferFrom(raf.getChannel(), position, length - total);
                }
            } else {
                // slow path
                int n = fill((int) Math.min(buffer.length, length - total));
                while (n > 0 && total < length) {
                    relStart = position - bufferPosition;
                    // write it
                    int cnt = (int) Math.min(bufferDataSize - (int) relStart, length - total);
                    if (cnt > 0) {
                        out.write(buffer, (int) relStart, cnt);
                        relSeekInternal(cnt);
                        total += cnt;
                        n = fill((int) Math.min(buffer.length, length - total));
                    }
                }
            }
            return total;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Transfer bytes from the given file position to the output stream without
     * changing the current position.
     *
     * @param pos the file position to read from
     * @param out the output stream
     * @param length the number of bytes to transfer
     * @return the number of bytes transferred
     * @throws IOException if an I/O error occurs
     */
    public long transferTo(long pos, OutputStream out, long length) throws IOException {
        Assert.checkMinimumParameter("pos", 0, pos);
        Assert.checkMinimumParameter("length", 0, length);
        if (length == 0) {
            return 0;
        }
        boolean locked = lock();
        try {
            checkReadable();
            if (pos + length < 0) {
                throw new IOException("Seek past end of file");
            }
            // if we have buffered data overlapping the position + length, we must flush it out
            if (position <= bufferPosition + bufferDataSize && position + length >= bufferPosition) {
                flushDirty();
            }
            long total = 0;
            // now copy the file content
            if (out instanceof FileOutputStream fos) {
                long cnt = fos.getChannel().transferFrom(raf.getChannel(), pos, length - total);
                while (cnt > 0) {
                    total += cnt;
                    pos += cnt;
                    cnt = fos.getChannel().transferFrom(raf.getChannel(), pos, length - total);
                }
            } else {
                // slow path; we must allocate a buffer
                byte[] buffer = new byte[(int) Math.min(length, this.buffer.length)];
                while (total < length) {
                    raf.seek(pos + total);
                    int n = raf.read(buffer, 0, (int) Math.min(buffer.length, length - total));
                    if (n > 0) {
                        out.write(buffer, 0, n);
                        total += n;
                    } else {
                        break;
                    }
                }
                return total;
            }
            return total;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Transfer all bytes from the given input stream to the current position.
     *
     * @param in the input stream
     * @return the number of bytes transferred
     * @throws IOException if an I/O error occurs
     */
    public long transferFrom(InputStream in) throws IOException {
        boolean locked = lock();
        try {
            checkWritable();
            long total = 0;
            for (;;) {
                int idx = ensureSpace(buffer.length);
                int n = in.read(buffer, idx, buffer.length - idx);
                if (n > 0) {
                    dirty = true;
                    bufferDataSize = n;
                    relSeekInternal(n);
                    total += n;
                } else {
                    bufferDataSize = 0;
                    break;
                }
            }
            return total;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Transfer up to {@code length} bytes from the given input stream to the current position.
     *
     * @param in the input stream
     * @param length the maximum number of bytes to transfer
     * @return the number of bytes transferred
     * @throws IOException if an I/O error occurs
     */
    public long transferFrom(InputStream in, long length) throws IOException {
        Assert.checkMinimumParameter("length", 0, length);
        boolean locked = lock();
        try {
            checkWritable();
            long total = 0;
            while (total < length) {
                int idx = ensureSpace(buffer.length);
                int n = in.read(buffer, idx, (int) Math.min(buffer.length - idx, length - total));
                if (n > 0) {
                    dirty = true;
                    relSeekInternal(n);
                    total += n;
                } else {
                    break;
                }
            }
            return total;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Transfer bytes from the given input stream to the given file position without
     * changing the current position.
     *
     * @param pos the file position to write to
     * @param in the input stream
     * @param length the maximum number of bytes to transfer
     * @return the number of bytes transferred
     * @throws IOException if an I/O error occurs
     */
    public long transferFrom(long pos, InputStream in, long length) throws IOException {
        boolean locked = lock();
        try {
            checkWritable();
            flushDirty();
            bufferDataSize = 0;
            // todo: optimize for FileInputStream
            long total = 0;
            byte[] tmp = new byte[(int) Math.min(length, buffer.length)];
            long curPos = pos;
            while (total < length) {
                int toRead = (int) Math.min(length - total, tmp.length);
                int n = in.read(tmp, 0, toRead);
                if (n < 0) {
                    break;
                }
                write(curPos, tmp, 0, n);
                total += n;
                curPos += n;
            }
            return total;
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    // ── Flush / Close ───────────────────────────────────────────────────

    /**
     * Flush any buffered data to the file.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void flush() throws IOException {
        boolean locked = lock();
        try {
            checkOpen();
            flushDirty();
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    /**
     * Flush any buffered data and close this file.
     * Subsequent operations (except further calls to {@code close()}) will throw
     * {@link IOException}.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        boolean locked = lock();
        try {
            if (closed) {
                return;
            }
            try {
                flushDirty();
            } finally {
                closed = true;
                try {
                    raf.close();
                } finally {
                    // Clean up cached views
                    BufferedFileInputStream is = this.inputStream;
                    if (is != null) {
                        is.closeSuper();
                    }
                    BufferedFileOutputStream os = this.outputStream;
                    if (os != null) {
                        os.closeSuper();
                    }
                }
            }
        } finally {
            if (locked) {
                unlock();
            }
        }
    }

    // ── Stream views ────────────────────────────────────────────────────

    /**
     * {@return a cached {@link FileInputStream} view of this file}
     * <p>
     * The returned stream also implements {@link DataInput} (with big-endian semantics).
     * Calling {@link InputStream#close() close()} on the returned stream is a no-op.
     *
     * @throws IllegalStateException if this file was not opened for reading
     */
    public BufferedFileInputStream inputStream() {
        if (!readMode) {
            throw new IllegalStateException("File is not open for reading");
        }
        BufferedFileInputStream is = this.inputStream;
        if (is == null) {
            try {
                is = new BufferedFileInputStream();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create input stream view", e);
            }
            this.inputStream = is;
        }
        return is;
    }

    /**
     * {@return a cached {@link FileOutputStream} view of this file}
     * <p>
     * The returned stream also implements {@link DataOutput} (with big-endian semantics).
     * Calling {@link OutputStream#close() close()} on the returned stream is a no-op.
     *
     * @throws IllegalStateException if this file was not opened for writing
     */
    public BufferedFileOutputStream outputStream() {
        if (!writeMode) {
            throw new IllegalStateException("File is not open for writing");
        }
        BufferedFileOutputStream os = this.outputStream;
        if (os == null) {
            try {
                os = new BufferedFileOutputStream();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create output stream view", e);
            }
            this.outputStream = os;
        }
        return os;
    }

    /**
     * {@return a cached {@link FileChannel} view of this file}
     * <p>
     * All read/write/position operations on the returned channel delegate to this
     * {@code BufferedFile}, maintaining buffer consistency. Calling
     * {@link FileChannel#close() close()} on the returned channel is a no-op.
     */
    public BufferedFileChannel channel() {
        BufferedFileChannel ch = this.channel;
        if (ch == null) {
            ch = new BufferedFileChannel();
            this.channel = ch;
        }
        return ch;
    }

    // ── Inner class: InputStream view ───────────────────────────────────

    /**
     * An {@link InputStream} view that extends {@link FileInputStream} and implements
     * {@link DataInput} with big-endian semantics. Delegates to the enclosing
     * {@link BufferedFile}.
     */
    public final class BufferedFileInputStream extends FileInputStream implements DataInput {
        private long markPosition = -1;

        private BufferedFileInputStream() throws IOException {
            super(raf.getFD());
        }

        @Override
        public int read() throws IOException {
            return BufferedFile.this.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return BufferedFile.this.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return BufferedFile.this.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return BufferedFile.this.skip(n);
        }

        @Override
        public int available() throws IOException {
            long rem = BufferedFile.this.remaining();
            return (int) Math.min(rem, Integer.MAX_VALUE);
        }

        @Override
        public void close() {
            // no-op
        }

        void closeSuper() throws IOException {
            super.close();
        }

        @Override
        public FileChannel getChannel() {
            return BufferedFile.this.channel();
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public void mark(int readLimit) {
            boolean locked = lock();
            try {
                markPosition = BufferedFile.this.filePosition();
            } catch (IOException e) {
                markPosition = -1;
            } finally {
                if (locked) {
                    unlock();
                }
            }
        }

        @Override
        public void reset() throws IOException {
            boolean locked = lock();
            try {
                if (markPosition < 0) {
                    throw new IOException("Mark not set or invalidated");
                }
                BufferedFile.this.seek(markPosition);
            } finally {
                if (locked) {
                    unlock();
                }
            }
        }

        // ── DataInput (big-endian) ──────────────────────────────────

        @Override
        public void readFully(byte[] b) throws IOException {
            BufferedFile.this.readFully(b);
        }

        @Override
        public void readFully(byte[] b, int off, int len) throws IOException {
            BufferedFile.this.readFully(b, off, len);
        }

        @Override
        public int skipBytes(int n) throws IOException {
            return (int) BufferedFile.this.skip(n);
        }

        @Override
        public boolean readBoolean() throws IOException {
            return BufferedFile.this.readBoolean();
        }

        @Override
        public byte readByte() throws IOException {
            int b = BufferedFile.this.read();
            if (b < 0) {
                throw new EOFException();
            }
            return (byte) b;
        }

        @Override
        public int readUnsignedByte() throws IOException {
            return BufferedFile.this.readByte();
        }

        @Override
        public short readShort() throws IOException {
            return BufferedFile.this.readShortBE();
        }

        @Override
        public int readUnsignedShort() throws IOException {
            return BufferedFile.this.readShortBE();
        }

        @Override
        public char readChar() throws IOException {
            return BufferedFile.this.readCharBE();
        }

        @Override
        public int readInt() throws IOException {
            return BufferedFile.this.readIntBE();
        }

        @Override
        public long readLong() throws IOException {
            return BufferedFile.this.readLongBE();
        }

        @Override
        public float readFloat() throws IOException {
            return Float.intBitsToFloat(BufferedFile.this.readIntBE());
        }

        @Override
        public double readDouble() throws IOException {
            return Double.longBitsToDouble(BufferedFile.this.readLongBE());
        }

        @Override
        public String readLine() throws IOException {
            boolean locked = BufferedFile.this.lock();
            try {
                BufferedFile.this.checkOpen();
                StringBuilder sb = new StringBuilder();
                int c;
                while ((c = read()) >= 0) {
                    if (c == '\n') {
                        break;
                    }
                    if (c == '\r') {
                        // Peek at next byte
                        int next = peek();
                        if (next == '\n') {
                            // consume it
                            skipBytes(1);
                        }
                        break;
                    }
                    sb.append((char) c);
                }
                if (c < 0 && sb.isEmpty()) {
                    return null;
                }
                return sb.toString();
            } finally {
                if (locked) {
                    BufferedFile.this.unlock();
                }
            }
        }

        @Override
        public String readUTF() throws IOException {
            return DataInputStream.readUTF(this);
        }
    }

    // ── Inner class: OutputStream view ──────────────────────────────────

    /**
     * An {@link OutputStream} view that extends {@link FileOutputStream} and implements
     * {@link DataOutput} with big-endian semantics. Delegates to the enclosing
     * {@link BufferedFile}.
     */
    public final class BufferedFileOutputStream extends FileOutputStream implements DataOutput {

        private BufferedFileOutputStream() throws IOException {
            super(raf.getFD());
        }

        @Override
        public void write(int b) throws IOException {
            BufferedFile.this.writeByte(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            BufferedFile.this.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            BufferedFile.this.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            BufferedFile.this.flush();
        }

        @Override
        public void close() {
            // no-op
        }

        void closeSuper() throws IOException {
            super.close();
        }

        @Override
        public FileChannel getChannel() {
            return BufferedFile.this.channel();
        }

        // ── DataOutput (big-endian) ─────────────────────────────────

        @Override
        public void writeBoolean(boolean v) throws IOException {
            BufferedFile.this.writeBoolean(v);
        }

        @Override
        public void writeByte(int v) throws IOException {
            BufferedFile.this.writeByte(v);
        }

        @Override
        public void writeShort(int v) throws IOException {
            BufferedFile.this.writeShortBE(v);
        }

        @Override
        public void writeChar(int v) throws IOException {
            BufferedFile.this.writeCharBE(v);
        }

        @Override
        public void writeInt(int v) throws IOException {
            BufferedFile.this.writeIntBE(v);
        }

        @Override
        public void writeLong(long v) throws IOException {
            BufferedFile.this.writeLongBE(v);
        }

        @Override
        public void writeFloat(float v) throws IOException {
            BufferedFile.this.writeIntBE(Float.floatToIntBits(v));
        }

        @Override
        public void writeDouble(double v) throws IOException {
            BufferedFile.this.writeLongBE(Double.doubleToLongBits(v));
        }

        @Override
        public void writeBytes(String s) throws IOException {
            int len = s.length();
            for (int i = 0; i < len; i++) {
                BufferedFile.this.writeByte(s.charAt(i));
            }
        }

        @Override
        public void writeChars(String s) throws IOException {
            int len = s.length();
            for (int i = 0; i < len; i++) {
                BufferedFile.this.writeShortBE(s.charAt(i));
            }
        }

        @Override
        public void writeUTF(String s) throws IOException {
            DataOutputStream dos = new DataOutputStream(this);
            dos.writeUTF(s);
            dos.flush();
        }
    }

    // ── Inner class: FileChannel view ───────────────────────────────────

    /**
     * A {@link FileChannel} view that delegates to the enclosing {@link BufferedFile},
     * maintaining buffer consistency for all read/write/position operations.
     * Calling {@link #close()} is a no-op.
     */
    public final class BufferedFileChannel extends FileChannel {
        private BufferedFileChannel() {
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            return BufferedFile.this.read(dst);
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            long total = 0;
            for (int i = offset; i < offset + length; i++) {
                int n = read(dsts[i]);
                if (n < 0) {
                    return total == 0 ? -1 : total;
                }
                total += n;
                if (dsts[i].hasRemaining()) {
                    break;
                }
            }
            return total;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            int rem = src.remaining();
            BufferedFile.this.write(src);
            return rem - src.remaining();
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            long total = 0;
            for (int i = offset; i < offset + length; i++) {
                total += write(srcs[i]);
            }
            return total;
        }

        @Override
        public long position() throws IOException {
            return BufferedFile.this.filePosition();
        }

        @Override
        public FileChannel position(long newPosition) throws IOException {
            BufferedFile.this.seek(newPosition);
            return this;
        }

        @Override
        public long size() throws IOException {
            return BufferedFile.this.length();
        }

        @Override
        public FileChannel truncate(long size) throws IOException {
            BufferedFile.this.setLength(size);
            return this;
        }

        @Override
        public void force(boolean metaData) throws IOException {
            BufferedFile.this.flush();
            raf.getChannel().force(metaData);
        }

        @Override
        public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
            // Read through our buffer
            ByteBuffer buf = ByteBuffer.allocate((int) Math.min(count, buffer.length));
            long total = 0;
            long curPos = position;
            while (total < count) {
                buf.clear();
                int toRead = (int) Math.min(count - total, buf.capacity());
                buf.limit(toRead);
                int n = BufferedFile.this.read(curPos, buf);
                if (n <= 0) {
                    break;
                }
                buf.flip();
                while (buf.hasRemaining()) {
                    total += target.write(buf);
                }
                curPos += n;
            }
            return total;
        }

        @Override
        public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
            ByteBuffer buf = ByteBuffer.allocate((int) Math.min(count, buffer.length));
            long total = 0;
            long curPos = position;
            while (total < count) {
                buf.clear();
                int toRead = (int) Math.min(count - total, buf.capacity());
                buf.limit(toRead);
                int n = src.read(buf);
                if (n <= 0) {
                    break;
                }
                buf.flip();
                BufferedFile.this.write(curPos, buf);
                total += n;
                curPos += n;
            }
            return total;
        }

        @Override
        public int read(ByteBuffer dst, long position) throws IOException {
            return BufferedFile.this.read(position, dst);
        }

        @Override
        public int write(ByteBuffer src, long position) throws IOException {
            return BufferedFile.this.write(position, src);
        }

        @Override
        public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
            BufferedFile.this.flush();
            return raf.getChannel().map(mode, position, size);
        }

        @Override
        public FileLock lock(long position, long size, boolean shared) throws IOException {
            BufferedFile.this.flush();
            return raf.getChannel().lock(position, size, shared);
        }

        @Override
        public FileLock tryLock(long position, long size, boolean shared) throws IOException {
            BufferedFile.this.flush();
            return raf.getChannel().tryLock(position, size, shared);
        }

        @Override
        protected void implCloseChannel() {
            // no-op; closing is managed by BufferedFile.close()
        }
    }
}
