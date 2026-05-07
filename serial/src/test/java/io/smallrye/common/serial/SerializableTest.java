package io.smallrye.common.serial;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.jupiter.api.Test;

/**
 * Tests for round-trip serialization and deserialization of {@link Serializable} objects,
 * including inheritance hierarchies and custom {@code writeObject}/{@code readObject} methods.
 */
class SerializableTest {

    private final SerialContext ctx = SerialContext.builder().addDefaultProviders().build();

    /**
     * A simple serializable point with two int fields.
     */
    public static class SimplePoint implements Serializable {
        private static final long serialVersionUID = 1L;
        int x;
        int y;

        /** No-arg constructor. */
        public SimplePoint() {
        }

        SimplePoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * A named point that extends {@link SimplePoint}, adding a {@code String} field
     * to test superclass chain serialization.
     */
    public static class NamedPoint extends SimplePoint {
        private static final long serialVersionUID = 1L;
        String name;

        /** No-arg constructor. */
        public NamedPoint() {
        }

        NamedPoint(int x, int y, String name) {
            super(x, y);
            this.name = name;
        }
    }

    /**
     * A serializable class that uses custom {@code writeObject} and {@code readObject} methods
     * to write ancillary stream data beyond the default fields.
     */
    public static class CustomWriteObject implements Serializable {
        private static final long serialVersionUID = 1L;
        int value;
        transient int extra;

        /** No-arg constructor. */
        public CustomWriteObject() {
        }

        CustomWriteObject(int value, int extra) {
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
     * A serializable class with an object field that may be {@code null}.
     */
    public static class NullableField implements Serializable {
        private static final long serialVersionUID = 1L;
        String label;

        /** No-arg constructor. */
        public NullableField() {
        }

        NullableField(String label) {
            this.label = label;
        }
    }

    @Test
    void simpleSerializableRoundTrip() throws IOException, ClassNotFoundException {
        SimplePoint original = new SimplePoint(42, -7);
        Serialized serialized = ctx.serialize(original);
        assertInstanceOf(SerializedSerializable.class, serialized);
        SimplePoint result = (SimplePoint) ctx.deserialize(serialized);
        assertEquals(original.x, result.x);
        assertEquals(original.y, result.y);
    }

    @Test
    void inheritanceRoundTrip() throws IOException, ClassNotFoundException {
        NamedPoint original = new NamedPoint(1, 2, "origin");
        Serialized serialized = ctx.serialize(original);
        assertInstanceOf(SerializedSerializable.class, serialized);
        SerializedSerializable ss = (SerializedSerializable) serialized;
        assertEquals(2, ss.data().size(), "expected two levels: SimplePoint + NamedPoint");
        assertNotNull(ss.serializedClass().superClass(), "NamedPoint should have a superclass descriptor");
        NamedPoint result = (NamedPoint) ctx.deserialize(serialized);
        assertEquals(original.x, result.x);
        assertEquals(original.y, result.y);
        assertEquals(original.name, result.name);
    }

    @Test
    void customWriteObjectRoundTrip() throws IOException, ClassNotFoundException {
        CustomWriteObject original = new CustomWriteObject(100, 200);
        Serialized serialized = ctx.serialize(original);
        assertInstanceOf(SerializedSerializable.class, serialized);
        CustomWriteObject result = (CustomWriteObject) ctx.deserialize(serialized);
        assertEquals(original.value, result.value);
        assertEquals(original.extra, result.extra);
    }

    @Test
    void nullFieldRoundTrip() throws IOException, ClassNotFoundException {
        NullableField original = new NullableField(null);
        Serialized serialized = ctx.serialize(original);
        assertInstanceOf(SerializedSerializable.class, serialized);
        NullableField result = (NullableField) ctx.deserialize(serialized);
        assertNull(result.label);
    }

    @Test
    void checkIntermediateStructure() throws IOException {
        SimplePoint original = new SimplePoint(5, 10);
        Serialized serialized = ctx.serialize(original);
        SerializedSerializable ss = (SerializedSerializable) serialized;
        SerializedSerializableClass sc = ss.serializedClass();
        assertInstanceOf(SerializedSerializableClass.class, sc);
        assertEquals(SimplePoint.class.getName(), sc.name());
        assertTrue(sc.streamFields().containsKey("x"));
        assertTrue(sc.streamFields().containsKey("y"));
        assertEquals(1, ss.data().size());
    }
}
