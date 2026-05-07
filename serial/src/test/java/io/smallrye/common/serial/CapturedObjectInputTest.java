package io.smallrye.common.serial;

import static org.junit.jupiter.api.Assertions.*;

import java.io.EOFException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.smallrye.common.serial.impl.CapturedObjectInput;

/**
 * Tests for {@link CapturedObjectInput}, verifying byte read operations, object reads,
 * and correct navigation through interleaved {@link StreamData.OfBytes} and
 * {@link StreamData.OfObjects} blocks.
 * <p>
 * Byte-only tests pass {@code null} as the deserialization context and use
 * hand-built {@link StreamData} lists. Mixed tests use {@link Externalizable}
 * round trips to exercise reading from captured data with a real context.
 */
class CapturedObjectInputTest {

    // ---- Helper ----

    /**
     * Build a big-endian int as a 4-byte array.
     */
    private static byte[] intBytes(int value) {
        return new byte[] {
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }

    /**
     * Build a big-endian long as an 8-byte array.
     */
    private static byte[] longBytes(long value) {
        return new byte[] {
                (byte) (value >>> 56),
                (byte) (value >>> 48),
                (byte) (value >>> 40),
                (byte) (value >>> 32),
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }

    /**
     * Build a big-endian short as a 2-byte array.
     */
    private static byte[] shortBytes(short value) {
        return new byte[] {
                (byte) (value >>> 8),
                (byte) value
        };
    }

    // ---- Byte reading tests (null context) ----

    /**
     * Read an int from a single {@link StreamData.OfBytes} block.
     */
    @Test
    void readIntFromSingleBlock() throws IOException {
        var input = new CapturedObjectInput(null, List.of(StreamData.of(intBytes(42))));
        assertEquals(42, input.readInt());
    }

    /**
     * Read a long from a single {@link StreamData.OfBytes} block.
     */
    @Test
    void readLongFromSingleBlock() throws IOException {
        var input = new CapturedObjectInput(null, List.of(StreamData.of(longBytes(123456789L))));
        assertEquals(123456789L, input.readLong());
    }

    /**
     * Read a short from a single {@link StreamData.OfBytes} block.
     */
    @Test
    void readShortFromSingleBlock() throws IOException {
        var input = new CapturedObjectInput(null, List.of(StreamData.of(shortBytes((short) 1000))));
        assertEquals((short) 1000, input.readShort());
    }

    /**
     * Read a boolean from a single-byte block.
     */
    @Test
    void readBoolean() throws IOException {
        var input = new CapturedObjectInput(null, List.of(StreamData.of(new byte[] { 1 })));
        assertTrue(input.readBoolean());
    }

    /**
     * Read a byte from a single-byte block.
     */
    @Test
    void readByte() throws IOException {
        var input = new CapturedObjectInput(null, List.of(StreamData.of(new byte[] { (byte) 0xAB })));
        assertEquals((byte) 0xAB, input.readByte());
    }

    /**
     * Read an unsigned byte from a single-byte block.
     */
    @Test
    void readUnsignedByte() throws IOException {
        var input = new CapturedObjectInput(null, List.of(StreamData.of(new byte[] { (byte) 0xFF })));
        assertEquals(255, input.readUnsignedByte());
    }

    /**
     * Read a char from a 2-byte block.
     */
    @Test
    void readChar() throws IOException {
        var input = new CapturedObjectInput(null, List.of(StreamData.of(shortBytes((short) 'Z'))));
        assertEquals('Z', input.readChar());
    }

    /**
     * Read a float from a 4-byte block.
     */
    @Test
    void readFloat() throws IOException {
        var input = new CapturedObjectInput(null, List.of(StreamData.of(intBytes(Float.floatToRawIntBits(3.14f)))));
        assertEquals(3.14f, input.readFloat());
    }

    /**
     * Read a double from an 8-byte block.
     */
    @Test
    void readDouble() throws IOException {
        var input = new CapturedObjectInput(null,
                List.of(StreamData.of(longBytes(Double.doubleToRawLongBits(2.718)))));
        assertEquals(2.718, input.readDouble());
    }

    /**
     * {@code readByte()} at EOF throws {@link EOFException}.
     */
    @Test
    void readByteAtEofThrows() {
        var input = new CapturedObjectInput(null, List.of());
        assertThrows(EOFException.class, input::readByte);
    }

    /**
     * {@code readInt()} at EOF throws {@link EOFException}.
     */
    @Test
    void readIntAtEofThrows() {
        var input = new CapturedObjectInput(null, List.of());
        assertThrows(EOFException.class, input::readInt);
    }

    /**
     * {@code read()} at EOF returns -1.
     */
    @Test
    void readSingleByteAtEofReturnsMinusOne() throws IOException {
        var input = new CapturedObjectInput(null, List.of());
        assertEquals(-1, input.read());
    }

    /**
     * {@code read(byte[])} at EOF returns -1.
     */
    @Test
    void readArrayAtEofReturnsMinusOne() throws IOException {
        var input = new CapturedObjectInput(null, List.of());
        assertEquals(-1, input.read(new byte[4]));
    }

    /**
     * Multiple values can be read sequentially from a single block.
     */
    @Test
    void sequentialReadsFromSingleBlock() throws IOException {
        // 4 bytes (int) + 2 bytes (short) + 1 byte (boolean) = 7 bytes
        byte[] buf = new byte[7];
        System.arraycopy(intBytes(100), 0, buf, 0, 4);
        System.arraycopy(shortBytes((short) 200), 0, buf, 4, 2);
        buf[6] = 1;
        var input = new CapturedObjectInput(null, List.of(StreamData.of(buf)));
        assertEquals(100, input.readInt());
        assertEquals((short) 200, input.readShort());
        assertTrue(input.readBoolean());
    }

    /**
     * An int that spans two consecutive {@link StreamData.OfBytes} blocks is read correctly.
     */
    @Test
    void readIntSpanningTwoBlocks() throws IOException {
        // Split the 4 bytes of int 0x01020304 across two blocks
        var input = new CapturedObjectInput(null, List.of(
                StreamData.of(new byte[] { 0x01, 0x02 }),
                StreamData.of(new byte[] { 0x03, 0x04 })));
        assertEquals(0x01020304, input.readInt());
    }

    /**
     * A long that spans two consecutive {@link StreamData.OfBytes} blocks is read correctly.
     */
    @Test
    void readLongSpanningTwoBlocks() throws IOException {
        var input = new CapturedObjectInput(null, List.of(
                StreamData.of(intBytes(0x01020304)),
                StreamData.of(intBytes(0x05060708))));
        assertEquals(0x0102030405060708L, input.readLong());
    }

    /**
     * {@code readFully} fills the destination array completely.
     */
    @Test
    void readFully() throws IOException {
        var input = new CapturedObjectInput(null, List.of(StreamData.of(new byte[] { 1, 2, 3, 4, 5 })));
        byte[] dest = new byte[5];
        input.readFully(dest);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, dest);
    }

    /**
     * {@code readFully} at EOF throws {@link EOFException}.
     */
    @Test
    void readFullyAtEofThrows() {
        var input = new CapturedObjectInput(null, List.of(StreamData.of(new byte[] { 1, 2 })));
        assertThrows(EOFException.class, () -> input.readFully(new byte[5]));
    }

    /**
     * {@code skipBytes} advances past data.
     */
    @Test
    void skipBytesAdvances() throws IOException {
        var input = new CapturedObjectInput(null, List.of(StreamData.of(new byte[] { 0, 0, 0, 0, 0, 0, 0, 42 })));
        assertEquals(4, input.skipBytes(4));
        // Now 4 bytes remain; the next int should be 42
        assertEquals(42, input.readInt());
    }

    /**
     * {@code available()} returns the remaining bytes in the current block.
     */
    @Test
    void availableReportsRemainingBytes() throws IOException {
        var input = new CapturedObjectInput(null, List.of(StreamData.of(new byte[10])));
        assertEquals(10, input.available());
        input.readInt();
        assertEquals(6, input.available());
    }

    /**
     * {@code available()} returns 0 when no data remains.
     */
    @Test
    void availableReturnsZeroAtEof() throws IOException {
        var input = new CapturedObjectInput(null, List.of());
        assertEquals(0, input.available());
    }

    // ---- Error case: reading bytes at an object position ----

    /**
     * Attempting to read bytes when the next block is {@link StreamData.OfObjects}
     * throws {@link IOException}.
     */
    @Test
    void readBytesAtObjectPositionThrows() {
        var input = new CapturedObjectInput(null,
                List.of(StreamData.of(new Serialized[] { SerializedNull.INSTANCE })));
        assertThrows(IOException.class, input::readInt);
    }

    /**
     * Attempting to read an object when the next block is {@link StreamData.OfBytes}
     * throws {@link NotActiveException}.
     */
    @Test
    void readObjectAtBytesPositionThrows() {
        var input = new CapturedObjectInput(null, List.of(StreamData.of(new byte[] { 0, 0, 0, 0 })));
        assertThrows(NotActiveException.class, input::readObject);
    }

    // ---- UTF reading ----

    /**
     * {@code readUTF} reads a modified-UTF8-encoded ASCII string.
     */
    @Test
    void readUTFAscii() throws IOException {
        // Manually encode "hi" in modified UTF-8: length prefix (2) + 'h' + 'i'
        byte[] buf = new byte[] { 0, 2, 'h', 'i' };
        var input = new CapturedObjectInput(null, List.of(StreamData.of(buf)));
        assertEquals("hi", input.readUTF());
    }

    // ---- Interleaved block navigation ----

    /**
     * Reads skip over exhausted blocks to find the next block of the correct type.
     */
    @Test
    void readBytesSkipsExhaustedBlocks() throws IOException {
        // Two byte blocks with one empty object block between them
        var input = new CapturedObjectInput(null, List.of(
                StreamData.of(intBytes(1)),
                StreamData.of(intBytes(2))));
        assertEquals(1, input.readInt());
        assertEquals(2, input.readInt());
    }

    /**
     * After closing, the input is at EOF.
     */
    @Test
    void closeAdvancesToEof() throws IOException {
        var input = new CapturedObjectInput(null, List.of(StreamData.of(new byte[100])));
        input.close();
        assertEquals(-1, input.read());
    }

    // ---- Mixed byte/object reading via Externalizable round trip ----

    private final SerialContext ctx = SerialContext.builder().addDefaultProviders().build();

    /**
     * An {@link Externalizable} that reads from an objects→bytes→objects interleaving.
     */
    public static class ObjBytesObj implements Externalizable {
        private static final long serialVersionUID = 1L;
        String first;
        int middle;
        String last;

        /** Required no-arg constructor. */
        public ObjBytesObj() {
        }

        ObjBytesObj(String first, int middle, String last) {
            this.first = first;
            this.middle = middle;
            this.last = last;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(first);
            out.writeInt(middle);
            out.writeObject(last);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            first = (String) in.readObject();
            middle = in.readInt();
            last = (String) in.readObject();
        }
    }

    /**
     * Round-trips through objects→bytes→objects interleaving.
     */
    @Test
    void objectsBytesObjectsRoundTrip() throws Exception {
        var original = new ObjBytesObj("alpha", 999, "omega");
        Serialized serialized = ctx.serialize(original);
        var result = (ObjBytesObj) ctx.deserialize(serialized);
        assertEquals("alpha", result.first);
        assertEquals(999, result.middle);
        assertEquals("omega", result.last);
    }

    /**
     * An {@link Externalizable} that reads from a bytes→objects→bytes interleaving.
     */
    public static class BytesObjBytes implements Externalizable {
        private static final long serialVersionUID = 1L;
        int first;
        String middle;
        long last;

        /** Required no-arg constructor. */
        public BytesObjBytes() {
        }

        BytesObjBytes(int first, String middle, long last) {
            this.first = first;
            this.middle = middle;
            this.last = last;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(first);
            out.writeObject(middle);
            out.writeLong(last);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            first = in.readInt();
            middle = (String) in.readObject();
            last = in.readLong();
        }
    }

    /**
     * Round-trips through bytes→objects→bytes interleaving.
     */
    @Test
    void bytesObjectsBytesRoundTrip() throws Exception {
        var original = new BytesObjBytes(7, "center", Long.MIN_VALUE);
        Serialized serialized = ctx.serialize(original);
        var result = (BytesObjBytes) ctx.deserialize(serialized);
        assertEquals(7, result.first);
        assertEquals("center", result.middle);
        assertEquals(Long.MIN_VALUE, result.last);
    }

    /**
     * An {@link Externalizable} with many alternations to stress-test block navigation.
     */
    public static class ManyAlternations implements Externalizable {
        private static final long serialVersionUID = 1L;
        int a;
        String b;
        long c;
        String d;
        short e;

        /** Required no-arg constructor. */
        public ManyAlternations() {
        }

        ManyAlternations(int a, String b, long c, String d, short e) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(a);
            out.writeObject(b);
            out.writeLong(c);
            out.writeObject(d);
            out.writeShort(e);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            a = in.readInt();
            b = (String) in.readObject();
            c = in.readLong();
            d = (String) in.readObject();
            e = in.readShort();
        }
    }

    /**
     * Round-trips through many byte↔object alternations.
     */
    @Test
    void manyAlternationsRoundTrip() throws Exception {
        var original = new ManyAlternations(10, "twenty", 30L, "forty", (short) 50);
        Serialized serialized = ctx.serialize(original);
        var result = (ManyAlternations) ctx.deserialize(serialized);
        assertEquals(10, result.a);
        assertEquals("twenty", result.b);
        assertEquals(30L, result.c);
        assertEquals("forty", result.d);
        assertEquals((short) 50, result.e);
    }
}
