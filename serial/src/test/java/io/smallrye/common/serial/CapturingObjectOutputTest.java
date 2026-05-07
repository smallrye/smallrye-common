package io.smallrye.common.serial;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.junit.jupiter.api.Test;

import io.smallrye.common.serial.impl.CapturingObjectOutput;

/**
 * Tests for {@link CapturingObjectOutput}, verifying byte operations, state transitions,
 * and correct grouping of stream data into alternating {@link StreamData.OfBytes} and
 * {@link StreamData.OfObjects} blocks.
 * <p>
 * Byte-only tests pass {@code null} as the serialization context since
 * only {@link CapturingObjectOutput#writeObject} touches the context.
 * Mixed byte/object tests use {@link Externalizable} round trips to exercise
 * state transitions with a real context.
 */
class CapturingObjectOutputTest {

    // ---- Byte-only tests (null context) ----

    /**
     * An immediately-closed stream produces no data blocks.
     */
    @Test
    void emptyStream() throws IOException {
        var out = new CapturingObjectOutput(null);
        out.close();
        assertTrue(out.streamData().isEmpty());
    }

    /**
     * A single {@code writeInt} produces one {@link StreamData.OfBytes} block of 4 bytes.
     */
    @Test
    void singleIntWrite() throws IOException {
        var out = new CapturingObjectOutput(null);
        out.writeInt(42);
        out.close();
        var data = out.streamData();
        assertEquals(1, data.size());
        var bytes = assertInstanceOf(StreamData.OfBytes.class, data.get(0));
        assertEquals(4, bytes.size());
        assertEquals(42, bytes.getInt(0));
    }

    /**
     * Multiple consecutive primitive writes coalesce into a single {@link StreamData.OfBytes} block.
     */
    @Test
    void multiplePrimitiveWritesCoalesce() throws IOException {
        var out = new CapturingObjectOutput(null);
        out.writeInt(1);
        out.writeLong(2L);
        out.writeShort(3);
        out.writeByte(4);
        out.writeBoolean(true);
        out.writeFloat(5.0f);
        out.writeDouble(6.0);
        out.writeChar('A');
        out.close();
        var data = out.streamData();
        assertEquals(1, data.size());
        var bytes = assertInstanceOf(StreamData.OfBytes.class, data.get(0));
        // 4 + 8 + 2 + 1 + 1 + 4 + 8 + 2 = 30
        assertEquals(30, bytes.size());
        assertEquals(1, bytes.getInt(0));
        assertEquals(2L, bytes.getLong(4));
        assertEquals((short) 3, bytes.getShort(12));
        assertEquals((byte) 4, bytes.getByte(14));
        assertTrue(bytes.getBoolean(15));
        assertEquals(5.0f, bytes.getFloat(16));
        assertEquals(6.0, bytes.getDouble(20));
        assertEquals('A', bytes.getChar(28));
    }

    /**
     * {@code writeUTF} produces a 2-byte length prefix followed by the encoded string.
     */
    @Test
    void writeUTFAscii() throws IOException {
        var out = new CapturingObjectOutput(null);
        out.writeUTF("hello");
        out.close();
        var data = out.streamData();
        assertEquals(1, data.size());
        var bytes = assertInstanceOf(StreamData.OfBytes.class, data.get(0));
        // 2 bytes length + 5 bytes "hello" = 7
        assertEquals(7, bytes.size());
        assertEquals(5, bytes.getUnsignedShort(0));
    }

    /**
     * {@code writeUTF} with multi-byte characters encodes correctly.
     */
    @Test
    void writeUTFMultiByte() throws IOException {
        var out = new CapturingObjectOutput(null);
        // U+00E9 (2-byte modified UTF-8) and U+2603 (3-byte modified UTF-8)
        out.writeUTF("é☃");
        out.close();
        var data = out.streamData();
        assertEquals(1, data.size());
        var bytes = assertInstanceOf(StreamData.OfBytes.class, data.get(0));
        // 2 bytes length prefix + 2 bytes for U+00E9 + 3 bytes for U+2603 = 7
        assertEquals(7, bytes.size());
        assertEquals(5, bytes.getUnsignedShort(0));
    }

    /**
     * {@code write(byte[])} captures the full array.
     */
    @Test
    void writeByteArray() throws IOException {
        var out = new CapturingObjectOutput(null);
        out.write(new byte[] { 10, 20, 30, 40, 50 });
        out.close();
        var data = out.streamData();
        assertEquals(1, data.size());
        var bytes = assertInstanceOf(StreamData.OfBytes.class, data.get(0));
        assertEquals(5, bytes.size());
        assertEquals(10, bytes.getByte(0));
        assertEquals(50, bytes.getByte(4));
    }

    /**
     * {@code write(byte[], off, len)} captures only the specified range.
     */
    @Test
    void writeByteArrayRange() throws IOException {
        var out = new CapturingObjectOutput(null);
        out.write(new byte[] { 1, 2, 3, 4, 5 }, 1, 3);
        out.close();
        var data = out.streamData();
        assertEquals(1, data.size());
        var bytes = assertInstanceOf(StreamData.OfBytes.class, data.get(0));
        assertEquals(3, bytes.size());
        assertEquals(2, bytes.getByte(0));
        assertEquals(4, bytes.getByte(2));
    }

    /**
     * {@code writeBytes(String)} writes each character's low byte.
     */
    @Test
    void writeBytes() throws IOException {
        var out = new CapturingObjectOutput(null);
        out.writeBytes("AB");
        out.close();
        var data = out.streamData();
        assertEquals(1, data.size());
        var bytes = assertInstanceOf(StreamData.OfBytes.class, data.get(0));
        assertEquals(2, bytes.size());
        assertEquals('A', bytes.getByte(0));
        assertEquals('B', bytes.getByte(1));
    }

    /**
     * {@code writeChars(String)} writes each character as a 2-byte big-endian value.
     */
    @Test
    void writeChars() throws IOException {
        var out = new CapturingObjectOutput(null);
        out.writeChars("AB");
        out.close();
        var data = out.streamData();
        assertEquals(1, data.size());
        var bytes = assertInstanceOf(StreamData.OfBytes.class, data.get(0));
        assertEquals(4, bytes.size());
        assertEquals('A', bytes.getChar(0));
        assertEquals('B', bytes.getChar(2));
    }

    /**
     * A {@code flush()} between byte writes creates separate {@link StreamData.OfBytes} blocks.
     */
    @Test
    void flushCreatesSeparateBlocks() throws IOException {
        var out = new CapturingObjectOutput(null);
        out.writeInt(1);
        out.flush();
        out.writeInt(2);
        out.close();
        var data = out.streamData();
        assertEquals(2, data.size());
        var b1 = assertInstanceOf(StreamData.OfBytes.class, data.get(0));
        var b2 = assertInstanceOf(StreamData.OfBytes.class, data.get(1));
        assertEquals(1, b1.getInt(0));
        assertEquals(2, b2.getInt(0));
    }

    /**
     * Writing to a closed stream throws {@link IOException}.
     */
    @Test
    void closedStreamWriteThrows() throws IOException {
        var out = new CapturingObjectOutput(null);
        out.close();
        assertThrows(IOException.class, () -> out.writeInt(1));
        assertThrows(IOException.class, () -> out.writeByte(1));
        assertThrows(IOException.class, () -> out.writeObject("x"));
    }

    /**
     * Calling {@code streamData()} before {@code close()} throws {@link IllegalStateException}.
     */
    @Test
    void streamDataBeforeCloseThrows() {
        var out = new CapturingObjectOutput(null);
        assertThrows(IllegalStateException.class, out::streamData);
    }

    /**
     * Closing an already-closed stream is a no-op.
     */
    @Test
    void doubleCloseIsIdempotent() throws IOException {
        var out = new CapturingObjectOutput(null);
        out.writeInt(1);
        out.close();
        out.close();
        var data = out.streamData();
        assertEquals(1, data.size());
    }

    /**
     * A flush on a fresh (idle) stream is a no-op.
     */
    @Test
    void flushOnIdleStream() throws IOException {
        var out = new CapturingObjectOutput(null);
        out.flush();
        out.close();
        assertTrue(out.streamData().isEmpty());
    }

    // ---- Mixed byte/object state transition tests via Externalizable round trip ----

    private final SerialContext ctx = SerialContext.builder().addDefaultProviders().build();

    /**
     * An {@link Externalizable} that writes objects, bytes, and objects (the TreeSet pattern).
     * This exercises the objects → bytes → objects state transition.
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
     * Verifies that the objects→bytes→objects pattern produces three correctly-typed blocks
     * and round-trips correctly.
     */
    @Test
    void objectsBytesObjectsPattern() throws Exception {
        var original = new ObjBytesObj("hello", 42, "world");
        Serialized serialized = ctx.serialize(original);
        var ext = assertInstanceOf(SerializedExternalizable.class, serialized);
        var data = ext.data();
        assertEquals(3, data.size());
        assertInstanceOf(StreamData.OfObjects.class, data.get(0));
        assertInstanceOf(StreamData.OfBytes.class, data.get(1));
        assertInstanceOf(StreamData.OfObjects.class, data.get(2));
        // verify byte block contains the int
        assertEquals(42, ((StreamData.OfBytes) data.get(1)).getInt(0));

        var result = (ObjBytesObj) ctx.deserialize(serialized);
        assertEquals("hello", result.first);
        assertEquals(42, result.middle);
        assertEquals("world", result.last);
    }

    /**
     * An {@link Externalizable} that writes bytes, objects, and bytes.
     * This exercises the bytes → objects → bytes state transition.
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
     * Verifies that the bytes→objects→bytes pattern produces three correctly-typed blocks
     * and round-trips correctly.
     */
    @Test
    void bytesObjectsBytesPattern() throws Exception {
        var original = new BytesObjBytes(99, "middle", Long.MAX_VALUE);
        Serialized serialized = ctx.serialize(original);
        var ext = assertInstanceOf(SerializedExternalizable.class, serialized);
        var data = ext.data();
        assertEquals(3, data.size());
        assertInstanceOf(StreamData.OfBytes.class, data.get(0));
        assertInstanceOf(StreamData.OfObjects.class, data.get(1));
        assertInstanceOf(StreamData.OfBytes.class, data.get(2));
        assertEquals(99, ((StreamData.OfBytes) data.get(0)).getInt(0));
        assertEquals(Long.MAX_VALUE, ((StreamData.OfBytes) data.get(2)).getLong(0));

        var result = (BytesObjBytes) ctx.deserialize(serialized);
        assertEquals(99, result.first);
        assertEquals("middle", result.middle);
        assertEquals(Long.MAX_VALUE, result.last);
    }

    /**
     * An {@link Externalizable} that writes only objects.
     */
    public static class ObjectsOnly implements Externalizable {
        private static final long serialVersionUID = 1L;
        String a;
        String b;

        /** Required no-arg constructor. */
        public ObjectsOnly() {
        }

        ObjectsOnly(String a, String b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(a);
            out.writeObject(b);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            a = (String) in.readObject();
            b = (String) in.readObject();
        }
    }

    /**
     * Consecutive object writes produce a single {@link StreamData.OfObjects} block.
     */
    @Test
    void objectsOnlyCoalesce() throws Exception {
        var original = new ObjectsOnly("alpha", "beta");
        Serialized serialized = ctx.serialize(original);
        var ext = assertInstanceOf(SerializedExternalizable.class, serialized);
        var data = ext.data();
        assertEquals(1, data.size());
        var objs = assertInstanceOf(StreamData.OfObjects.class, data.get(0));
        assertEquals(2, objs.size());

        var result = (ObjectsOnly) ctx.deserialize(serialized);
        assertEquals("alpha", result.a);
        assertEquals("beta", result.b);
    }

    /**
     * An {@link Externalizable} that writes nothing.
     */
    public static class EmptyExternalizable implements Externalizable {
        private static final long serialVersionUID = 1L;

        /** Required no-arg constructor. */
        public EmptyExternalizable() {
        }

        @Override
        public void writeExternal(ObjectOutput out) {
        }

        @Override
        public void readExternal(ObjectInput in) {
        }
    }

    /**
     * An {@link Externalizable} that writes nothing produces empty stream data.
     */
    @Test
    void emptyExternalizableProducesNoData() throws Exception {
        var original = new EmptyExternalizable();
        Serialized serialized = ctx.serialize(original);
        var ext = assertInstanceOf(SerializedExternalizable.class, serialized);
        assertTrue(ext.data().isEmpty());

        var result = assertInstanceOf(EmptyExternalizable.class, ctx.deserialize(serialized));
        assertNotNull(result);
    }

    /**
     * An {@link Externalizable} that alternates between bytes and objects multiple times.
     * Exercises repeated state transitions.
     */
    public static class MultiAlternation implements Externalizable {
        private static final long serialVersionUID = 1L;
        int a;
        String b;
        long c;
        String d;
        short e;

        /** Required no-arg constructor. */
        public MultiAlternation() {
        }

        MultiAlternation(int a, String b, long c, String d, short e) {
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
     * Multiple byte→object alternations produce the expected sequence of blocks.
     */
    @Test
    void multipleAlternationsPattern() throws Exception {
        var original = new MultiAlternation(1, "two", 3L, "four", (short) 5);
        Serialized serialized = ctx.serialize(original);
        var ext = assertInstanceOf(SerializedExternalizable.class, serialized);
        var data = ext.data();
        // bytes(int) → objects(String) → bytes(long) → objects(String) → bytes(short)
        assertEquals(5, data.size());
        assertInstanceOf(StreamData.OfBytes.class, data.get(0));
        assertInstanceOf(StreamData.OfObjects.class, data.get(1));
        assertInstanceOf(StreamData.OfBytes.class, data.get(2));
        assertInstanceOf(StreamData.OfObjects.class, data.get(3));
        assertInstanceOf(StreamData.OfBytes.class, data.get(4));
        assertEquals(1, ((StreamData.OfBytes) data.get(0)).getInt(0));
        assertEquals(3L, ((StreamData.OfBytes) data.get(2)).getLong(0));
        assertEquals((short) 5, ((StreamData.OfBytes) data.get(4)).getShort(0));

        var result = (MultiAlternation) ctx.deserialize(serialized);
        assertEquals(1, result.a);
        assertEquals("two", result.b);
        assertEquals(3L, result.c);
        assertEquals("four", result.d);
        assertEquals((short) 5, result.e);
    }

    /**
     * An {@link Externalizable} with null object writes (null is serializable).
     */
    public static class NullObjects implements Externalizable {
        private static final long serialVersionUID = 1L;
        String value;

        /** Required no-arg constructor. */
        public NullObjects() {
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(null);
            out.writeInt(7);
            out.writeObject(null);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            assertNull(in.readObject());
            assertEquals(7, in.readInt());
            assertNull(in.readObject());
        }
    }

    /**
     * Null object writes produce the correct object→bytes→object transition
     * (regression test for the state machine bug where null comparators were involved).
     */
    @Test
    void nullObjectsBytesNullObjects() throws Exception {
        var original = new NullObjects();
        Serialized serialized = ctx.serialize(original);
        var ext = assertInstanceOf(SerializedExternalizable.class, serialized);
        var data = ext.data();
        assertEquals(3, data.size());
        assertInstanceOf(StreamData.OfObjects.class, data.get(0));
        assertInstanceOf(StreamData.OfBytes.class, data.get(1));
        assertInstanceOf(StreamData.OfObjects.class, data.get(2));

        assertDoesNotThrow(() -> ctx.deserialize(serialized));
    }
}
