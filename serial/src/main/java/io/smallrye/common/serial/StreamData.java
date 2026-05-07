package io.smallrye.common.serial;

import static io.smallrye.common.serial.impl.Util.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * A unit of stream data.
 */
public abstract class StreamData {
    private StreamData() {
    }

    /**
     * {@return {@code true} if this is an empty block, or {@code false} if it contains data}
     */
    public final boolean isEmpty() {
        return size() == 0;
    }

    /**
     * {@return the number of items (bytes or objects) in this unit of data}
     */
    public abstract int size();

    /**
     * Create a byte data block from the given byte array.
     *
     * @param bytes the byte array (must not be {@code null})
     * @return the byte data block (not {@code null})
     */
    public static OfBytes of(byte[] bytes) {
        return bytes.length == 0 ? OfBytes.EMPTY : new OfBytes(bytes.clone());
    }

    /**
     * Create a byte data block from a range within the given byte array.
     *
     * @param bytes the source byte array (must not be {@code null})
     * @param off the starting offset within the array
     * @param len the number of bytes to copy
     * @return the byte data block (not {@code null})
     * @throws IndexOutOfBoundsException if the specified range is out of bounds
     */
    public static OfBytes of(byte[] bytes, int off, int len) {
        Objects.checkFromIndexSize(off, len, bytes.length);
        return len == 0 ? OfBytes.EMPTY : new OfBytes(Arrays.copyOfRange(bytes, off, off + len));
    }

    /**
     * Create a byte data block from the remaining bytes in the given buffer.
     * The buffer's position is advanced to its limit.
     *
     * @param buf the byte buffer (must not be {@code null})
     * @return the byte data block (not {@code null})
     */
    public static OfBytes of(ByteBuffer buf) {
        byte[] b = new byte[buf.remaining()];
        buf.get(b);
        return new OfBytes(b);
    }

    // todo: JDK 24+ accept MemorySegment

    /**
     * Create an object data block from the given serialized objects.
     *
     * @param objects the serialized objects (must not be {@code null})
     * @return the object data block (not {@code null})
     */
    public static OfObjects of(Serialized... objects) {
        return objects.length == 0 ? OfObjects.EMPTY : new OfObjects(objects.clone());
    }

    /**
     * Create an object data block from a collection of serialized objects.
     *
     * @param objects the serialized objects (must not be {@code null})
     * @return the object data block (not {@code null})
     */
    public static OfObjects of(Collection<? extends Serialized> objects) {
        Serialized[] array = objects.toArray(Serialized[]::new);
        return array.length == 0 ? OfObjects.EMPTY : new OfObjects(array);
    }

    /**
     * A block of binary stream data.
     */
    public static final class OfBytes extends StreamData {
        /**
         * An empty byte data block.
         */
        public static final OfBytes EMPTY = new OfBytes(new byte[0]);

        private final byte[] bytes;

        private OfBytes(final byte[] bytes) {
            this.bytes = bytes;
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            return bytes.length;
        }

        /**
         * {@return a copy of the byte data (not {@code null})}
         */
        public byte[] getBytes() {
            return bytes.length == 0 ? bytes : bytes.clone();
        }

        /**
         * Get a {@code byte} value at the given offset.
         *
         * @param offset the byte offset
         * @return the byte value
         */
        public byte getByte(int offset) {
            return bytes[offset];
        }

        /**
         * Get an unsigned {@code byte} value at the given offset.
         *
         * @param offset the byte offset
         * @return the unsigned byte value as an {@code int}
         */
        public int getUnsignedByte(int offset) {
            return Byte.toUnsignedInt(getByte(offset));
        }

        /**
         * Get a big-endian {@code short} value at the given offset.
         *
         * @param offset the byte offset
         * @return the short value
         */
        public short getShort(int offset) {
            return (short) BE16.get(bytes, offset);
        }

        /**
         * Get an unsigned big-endian {@code short} value at the given offset.
         *
         * @param offset the byte offset
         * @return the unsigned short value as an {@code int}
         */
        public int getUnsignedShort(int offset) {
            return getChar(offset);
        }

        /**
         * Get a big-endian {@code int} value at the given offset.
         *
         * @param offset the byte offset
         * @return the int value
         */
        public int getInt(int offset) {
            return (int) BE32.get(bytes, offset);
        }

        /**
         * Get an unsigned big-endian {@code int} value at the given offset.
         *
         * @param offset the byte offset
         * @return the unsigned int value as a {@code long}
         */
        public long getUnsignedInt(int offset) {
            return Integer.toUnsignedLong(getInt(offset));
        }

        /**
         * Get a big-endian {@code long} value at the given offset.
         *
         * @param offset the byte offset
         * @return the long value
         */
        public long getLong(int offset) {
            return (long) BE64.get(bytes, offset);
        }

        /**
         * Get a big-endian {@code char} value at the given offset.
         *
         * @param offset the byte offset
         * @return the char value
         */
        public char getChar(int offset) {
            return (char) getShort(offset);
        }

        /**
         * Get a big-endian {@code float} value at the given offset.
         *
         * @param offset the byte offset
         * @return the float value
         */
        public float getFloat(int offset) {
            return Float.intBitsToFloat(getInt(offset));
        }

        /**
         * Get a big-endian {@code double} value at the given offset.
         *
         * @param offset the byte offset
         * @return the double value
         */
        public double getDouble(int offset) {
            return Double.longBitsToDouble(getLong(offset));
        }

        /**
         * Get a {@code boolean} value at the given offset.
         *
         * @param offset the byte offset
         * @return the boolean value
         */
        public boolean getBoolean(int offset) {
            return getByte(offset) != 0;
        }

        /**
         * Copy bytes from this data block into the given destination array.
         *
         * @param start the starting offset within this data block
         * @param dest the destination array (must not be {@code null})
         * @param offset the starting offset within the destination array
         * @param length the number of bytes to copy
         */
        public void get(int start, byte[] dest, int offset, int length) {
            System.arraycopy(bytes, start, dest, offset, length);
        }

        /**
         * Copy bytes from this data block into the given destination array, filling it completely.
         *
         * @param start the starting offset within this data block
         * @param dest the destination array (must not be {@code null})
         */
        public void get(int start, byte[] dest) {
            get(start, dest, 0, dest.length);
        }
    }

    /**
     * A block of object stream data.
     */
    public static final class OfObjects extends StreamData {
        /**
         * An empty object data block.
         */
        public static final OfObjects EMPTY = new OfObjects(new Serialized[0]);

        private final Serialized[] objects;

        private OfObjects(final Serialized[] objects) {
            this.objects = objects.length == 0 ? objects : objects.clone();
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            return objects.length;
        }

        /**
         * {@return a copy of the serialized object array (not {@code null})}
         */
        public Serialized[] getObjects() {
            return objects.length == 0 ? objects : objects.clone();
        }

        /**
         * Get a serialized object at the given index.
         *
         * @param index the object index
         * @return the serialized object (not {@code null})
         */
        public Serialized getObject(int index) {
            return objects[index];
        }
    }
}
