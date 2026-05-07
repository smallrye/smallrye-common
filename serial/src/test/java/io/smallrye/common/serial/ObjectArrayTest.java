package io.smallrye.common.serial;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * Tests for round-trip serialization and deserialization of object arrays.
 */
class ObjectArrayTest {

    private final SerialContext ctx = SerialContext.builder().addDefaultProviders().build();

    @Test
    void stringArrayRoundTrip() throws IOException, ClassNotFoundException {
        String[] original = { "alpha", "beta", "gamma" };
        Serialized serialized = ctx.serialize(original);
        assertInstanceOf(SerializedObjectArray.class, serialized);
        SerializedObjectArray soa = (SerializedObjectArray) serialized;
        assertInstanceOf(SerializedArrayClass.class, soa.arrayType());
        assertEquals("[Ljava.lang.String;", soa.arrayType().name());
        String[] result = (String[]) ctx.deserialize(serialized);
        assertArrayEquals(original, result);
    }

    @Test
    void nestedObjectArrayRoundTrip() throws IOException, ClassNotFoundException {
        String[][] original = { { "a", "b" }, { "c" } };
        Serialized serialized = ctx.serialize(original);
        assertInstanceOf(SerializedObjectArray.class, serialized);
        SerializedObjectArray soa = (SerializedObjectArray) serialized;
        SerializedArrayClass arrayClass = soa.arrayType();
        assertInstanceOf(SerializedArrayClass.class, arrayClass.componentType());
        String[][] result = (String[][]) ctx.deserialize(serialized);
        assertArrayEquals(original[0], result[0]);
        assertArrayEquals(original[1], result[1]);
    }

    @Test
    void arrayWithNullElementsRoundTrip() throws IOException, ClassNotFoundException {
        Object[] original = { "hello", null, "world" };
        Serialized serialized = ctx.serialize(original);
        assertInstanceOf(SerializedObjectArray.class, serialized);
        SerializedObjectArray soa = (SerializedObjectArray) serialized;
        assertInstanceOf(SerializedNull.class, soa.get(1));
        Object[] result = (Object[]) ctx.deserialize(serialized);
        assertEquals("hello", result[0]);
        assertNull(result[1]);
        assertEquals("world", result[2]);
    }

    @Test
    void emptyObjectArrayRoundTrip() throws IOException, ClassNotFoundException {
        String[] original = new String[0];
        Serialized serialized = ctx.serialize(original);
        assertInstanceOf(SerializedObjectArray.class, serialized);
        assertEquals(0, ((SerializedObjectArray) serialized).length());
        String[] result = (String[]) ctx.deserialize(serialized);
        assertEquals(0, result.length);
    }
}
