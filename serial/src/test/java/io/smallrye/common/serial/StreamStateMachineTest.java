package io.smallrye.common.serial;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@code CapturingObjectOutputStream} / {@code CapturedObjectInputStream}
 * state machines, exercised through {@link Serializable} classes with custom
 * {@code writeObject} and {@code readObject} methods.
 * <p>
 * These tests verify that the field-capture stage ({@code defaultWriteObject}/{@code writeFields})
 * and the subsequent stream-data stage (primitive writes, object writes) interact correctly
 * with the underlying {@code CapturingObjectOutput}/{@code CapturedObjectInput} block grouping.
 */
class StreamStateMachineTest {

    private final SerialContext ctx = SerialContext.builder().addDefaultProviders().build();

    // ---- No-field classes with custom writeObject (TreeSet/TreeMap pattern) ----

    /**
     * A class with no serializable fields and a custom {@code writeObject} that writes
     * objects, bytes, and objects — mimicking the {@link java.util.TreeSet} serialization pattern.
     */
    public static class NoFieldsObjBytesObj implements Serializable {
        private static final long serialVersionUID = 1L;
        transient String comparator;
        transient int size;
        transient String[] elements;

        /** No-arg constructor. */
        public NoFieldsObjBytesObj() {
        }

        NoFieldsObjBytesObj(String comparator, String... elements) {
            this.comparator = comparator;
            this.size = elements.length;
            this.elements = elements;
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
            oos.writeObject(comparator);
            oos.writeInt(size);
            for (String e : elements) {
                oos.writeObject(e);
            }
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            comparator = (String) ois.readObject();
            size = ois.readInt();
            elements = new String[size];
            for (int i = 0; i < size; i++) {
                elements[i] = (String) ois.readObject();
            }
        }
    }

    /**
     * Regression test for the state machine bug: a class with no serializable fields
     * and a custom writeObject that writes objects→bytes→objects must produce correctly-ordered
     * stream data blocks and round-trip successfully.
     */
    @Test
    void noFieldsObjectsBytesObjectsRoundTrip() throws Exception {
        var original = new NoFieldsObjBytesObj("natural", "apple", "banana", "cherry");
        Serialized serialized = ctx.serialize(original);
        var ss = assertInstanceOf(SerializedSerializable.class, serialized);
        var data = ss.data();
        assertEquals(1, data.size());
        var serialData = data.get(0);

        // No serializable fields → empty primitive and object field data
        assertTrue(serialData.primitiveFieldData().isEmpty());
        assertTrue(serialData.objectFieldData().isEmpty());

        // Stream data: [OfObjects(comparator), OfBytes(int), OfObjects(elements)]
        var streamData = serialData.streamData();
        assertEquals(3, streamData.size());
        assertInstanceOf(StreamData.OfObjects.class, streamData.get(0));
        assertInstanceOf(StreamData.OfBytes.class, streamData.get(1));
        assertInstanceOf(StreamData.OfObjects.class, streamData.get(2));

        // Verify the byte block contains the correct size
        assertEquals(3, ((StreamData.OfBytes) streamData.get(1)).getInt(0));

        var result = (NoFieldsObjBytesObj) ctx.deserialize(serialized);
        assertEquals("natural", result.comparator);
        assertEquals(3, result.size);
        assertArrayEquals(new String[] { "apple", "banana", "cherry" }, result.elements);
    }

    /**
     * Same pattern with zero elements — tests empty element loop.
     */
    @Test
    void noFieldsObjectsBytesNoElements() throws Exception {
        var original = new NoFieldsObjBytesObj("natural");
        Serialized serialized = ctx.serialize(original);
        var ss = assertInstanceOf(SerializedSerializable.class, serialized);
        var streamData = ss.data().get(0).streamData();

        // [OfObjects(comparator), OfBytes(int=0)], no trailing objects block
        assertEquals(2, streamData.size());
        assertInstanceOf(StreamData.OfObjects.class, streamData.get(0));
        assertInstanceOf(StreamData.OfBytes.class, streamData.get(1));
        assertEquals(0, ((StreamData.OfBytes) streamData.get(1)).getInt(0));

        var result = (NoFieldsObjBytesObj) ctx.deserialize(serialized);
        assertEquals("natural", result.comparator);
        assertEquals(0, result.size);
        assertEquals(0, result.elements.length);
    }

    /**
     * Same pattern with null comparator — exercises null object writes in the pattern.
     */
    @Test
    void noFieldsNullComparator() throws Exception {
        var original = new NoFieldsObjBytesObj(null, "x", "y");
        Serialized serialized = ctx.serialize(original);
        var result = (NoFieldsObjBytesObj) ctx.deserialize(serialized);
        assertNull(result.comparator);
        assertEquals(2, result.size);
        assertArrayEquals(new String[] { "x", "y" }, result.elements);
    }

    // ---- Classes with fields AND stream data ----

    /**
     * A class with serializable fields and custom writeObject that writes additional bytes
     * after defaultWriteObject.
     */
    public static class FieldsAndBytes implements Serializable {
        private static final long serialVersionUID = 1L;
        int value;
        transient int extra;

        /** No-arg constructor. */
        public FieldsAndBytes() {
        }

        FieldsAndBytes(int value, int extra) {
            this.value = value;
            this.extra = extra;
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
            oos.writeInt(extra);
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            extra = ois.readInt();
        }
    }

    /**
     * Fields are captured in the primitive/object field data; additional bytes go into stream data.
     */
    @Test
    void fieldsAndBytesStreamData() throws Exception {
        var original = new FieldsAndBytes(10, 20);
        Serialized serialized = ctx.serialize(original);
        var ss = assertInstanceOf(SerializedSerializable.class, serialized);
        var serialData = ss.data().get(0);

        // The 'value' field should be in primitive field data
        assertFalse(serialData.primitiveFieldData().isEmpty());

        // The extra int should be in stream data
        var streamData = serialData.streamData();
        assertEquals(1, streamData.size());
        var bytes = assertInstanceOf(StreamData.OfBytes.class, streamData.get(0));
        assertEquals(20, bytes.getInt(0));

        var result = (FieldsAndBytes) ctx.deserialize(serialized);
        assertEquals(10, result.value);
        assertEquals(20, result.extra);
    }

    /**
     * A class with both primitive and object fields, plus mixed stream data.
     */
    public static class FieldsAndMixedStream implements Serializable {
        private static final long serialVersionUID = 1L;
        int id;
        String name;
        transient String tag;
        transient int count;

        /** No-arg constructor. */
        public FieldsAndMixedStream() {
        }

        FieldsAndMixedStream(int id, String name, String tag, int count) {
            this.id = id;
            this.name = name;
            this.tag = tag;
            this.count = count;
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
            oos.writeObject(tag);
            oos.writeInt(count);
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            tag = (String) ois.readObject();
            count = ois.readInt();
        }
    }

    /**
     * Fields go to field data; additional object + bytes go to stream data blocks.
     */
    @Test
    void fieldsAndMixedStreamData() throws Exception {
        var original = new FieldsAndMixedStream(1, "test", "extra-tag", 42);
        Serialized serialized = ctx.serialize(original);
        var ss = assertInstanceOf(SerializedSerializable.class, serialized);
        var serialData = ss.data().get(0);

        // Should have non-empty field data (id is primitive, name is object)
        assertFalse(serialData.primitiveFieldData().isEmpty());
        assertFalse(serialData.objectFieldData().isEmpty());

        // Stream data: [OfObjects(tag), OfBytes(count)]
        var streamData = serialData.streamData();
        assertEquals(2, streamData.size());
        assertInstanceOf(StreamData.OfObjects.class, streamData.get(0));
        assertInstanceOf(StreamData.OfBytes.class, streamData.get(1));
        assertEquals(42, ((StreamData.OfBytes) streamData.get(1)).getInt(0));

        var result = (FieldsAndMixedStream) ctx.deserialize(serialized);
        assertEquals(1, result.id);
        assertEquals("test", result.name);
        assertEquals("extra-tag", result.tag);
        assertEquals(42, result.count);
    }

    // ---- defaultWriteObject only (no additional stream data) ----

    /**
     * A class with custom writeObject that only calls defaultWriteObject.
     */
    public static class DefaultOnly implements Serializable {
        private static final long serialVersionUID = 1L;
        int x;
        int y;

        /** No-arg constructor. */
        public DefaultOnly() {
        }

        DefaultOnly(int x, int y) {
            this.x = x;
            this.y = y;
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
        }
    }

    /**
     * defaultWriteObject with no additional writes produces empty stream data.
     */
    @Test
    void defaultOnlyProducesNoStreamData() throws Exception {
        var original = new DefaultOnly(5, 10);
        Serialized serialized = ctx.serialize(original);
        var ss = assertInstanceOf(SerializedSerializable.class, serialized);
        var serialData = ss.data().get(0);

        assertFalse(serialData.primitiveFieldData().isEmpty());
        assertTrue(serialData.streamData().isEmpty());

        var result = (DefaultOnly) ctx.deserialize(serialized);
        assertEquals(5, result.x);
        assertEquals(10, result.y);
    }

    // ---- No-field class with defaultWriteObject only ----

    /**
     * A class with no fields whose custom writeObject only calls defaultWriteObject.
     * This exercises the edge case where writeFields is called with nothing to write.
     */
    public static class NoFieldsDefaultOnly implements Serializable {
        private static final long serialVersionUID = 1L;

        /** No-arg constructor. */
        public NoFieldsDefaultOnly() {
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
        }
    }

    /**
     * No fields and no stream data produces completely empty SerialData.
     */
    @Test
    void noFieldsDefaultOnlyRoundTrip() throws Exception {
        var original = new NoFieldsDefaultOnly();
        Serialized serialized = ctx.serialize(original);
        var ss = assertInstanceOf(SerializedSerializable.class, serialized);
        var serialData = ss.data().get(0);

        assertTrue(serialData.primitiveFieldData().isEmpty());
        assertTrue(serialData.objectFieldData().isEmpty());
        assertTrue(serialData.streamData().isEmpty());

        var result = assertInstanceOf(NoFieldsDefaultOnly.class, ctx.deserialize(serialized));
        assertNotNull(result);
    }

    // ---- Bytes-only stream data after defaultWriteObject ----

    /**
     * A class with no fields whose custom writeObject calls defaultWriteObject
     * then writes only byte data.
     */
    public static class NoFieldsBytesOnly implements Serializable {
        private static final long serialVersionUID = 1L;
        transient int a;
        transient long b;

        /** No-arg constructor. */
        public NoFieldsBytesOnly() {
        }

        NoFieldsBytesOnly(int a, long b) {
            this.a = a;
            this.b = b;
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
            oos.writeInt(a);
            oos.writeLong(b);
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            a = ois.readInt();
            b = ois.readLong();
        }
    }

    /**
     * Bytes-only stream data after defaultWriteObject produces a single OfBytes block.
     */
    @Test
    void noFieldsBytesOnlyStreamData() throws Exception {
        var original = new NoFieldsBytesOnly(99, Long.MAX_VALUE);
        Serialized serialized = ctx.serialize(original);
        var ss = assertInstanceOf(SerializedSerializable.class, serialized);
        var streamData = ss.data().get(0).streamData();

        assertEquals(1, streamData.size());
        var bytes = assertInstanceOf(StreamData.OfBytes.class, streamData.get(0));
        assertEquals(99, bytes.getInt(0));
        assertEquals(Long.MAX_VALUE, bytes.getLong(4));

        var result = (NoFieldsBytesOnly) ctx.deserialize(serialized);
        assertEquals(99, result.a);
        assertEquals(Long.MAX_VALUE, result.b);
    }

    // ---- Superclass chain with mixed patterns ----

    /**
     * Base class with fields and custom writeObject that writes a transient value.
     */
    public static class Base implements Serializable {
        private static final long serialVersionUID = 1L;
        int baseField;
        transient int baseExtra;

        /** No-arg constructor. */
        public Base() {
        }

        Base(int baseField, int baseExtra) {
            this.baseField = baseField;
            this.baseExtra = baseExtra;
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
            oos.writeInt(baseExtra);
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            baseExtra = ois.readInt();
        }
    }

    /**
     * Subclass that adds its own custom writeObject with a different pattern.
     */
    public static class Sub extends Base {
        private static final long serialVersionUID = 1L;
        String subField;
        transient String subTag;
        transient int subCount;

        /** No-arg constructor. */
        public Sub() {
        }

        Sub(int baseField, int baseExtra, String subField, String subTag, int subCount) {
            super(baseField, baseExtra);
            this.subField = subField;
            this.subTag = subTag;
            this.subCount = subCount;
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
            oos.writeObject(subTag);
            oos.writeInt(subCount);
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            subTag = (String) ois.readObject();
            subCount = ois.readInt();
        }
    }

    /**
     * Each class level in the superclass chain has its own SerialData entry with
     * independently correct field and stream data.
     */
    @Test
    void superclassChainMixedPatterns() throws Exception {
        var original = new Sub(1, 2, "subval", "tag", 77);
        Serialized serialized = ctx.serialize(original);
        var ss = assertInstanceOf(SerializedSerializable.class, serialized);
        var allData = ss.data();
        assertEquals(2, allData.size());

        // Index 0: Base level
        var baseData = allData.get(0);
        assertFalse(baseData.primitiveFieldData().isEmpty());
        var baseStream = baseData.streamData();
        assertEquals(1, baseStream.size());
        var baseBytes = assertInstanceOf(StreamData.OfBytes.class, baseStream.get(0));
        assertEquals(2, baseBytes.getInt(0));

        // Index 1: Sub level — only has String subField (object field, no primitives)
        var subData = allData.get(1);
        assertTrue(subData.primitiveFieldData().isEmpty());
        var subStream = subData.streamData();
        assertEquals(2, subStream.size());
        assertInstanceOf(StreamData.OfObjects.class, subStream.get(0));
        assertInstanceOf(StreamData.OfBytes.class, subStream.get(1));
        assertEquals(77, ((StreamData.OfBytes) subStream.get(1)).getInt(0));

        var result = (Sub) ctx.deserialize(serialized);
        assertEquals(1, result.baseField);
        assertEquals(2, result.baseExtra);
        assertEquals("subval", result.subField);
        assertEquals("tag", result.subTag);
        assertEquals(77, result.subCount);
    }
}
