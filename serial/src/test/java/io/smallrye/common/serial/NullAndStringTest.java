package io.smallrye.common.serial;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * Tests for round-trip serialization and deserialization of {@code null} and {@link String} values.
 */
class NullAndStringTest {

    private final SerialContext ctx = SerialContext.builder().addDefaultProviders().build();

    @Test
    void nullRoundTrip() throws IOException, ClassNotFoundException {
        Serialized serialized = ctx.serialize(null);
        assertSame(SerializedNull.INSTANCE, serialized);
        Object result = ctx.deserialize(serialized);
        assertNull(result);
    }

    @Test
    void stringRoundTrip() throws IOException, ClassNotFoundException {
        Serialized serialized = ctx.serialize("hello");
        assertInstanceOf(SerializedString.class, serialized);
        assertEquals("hello", ((SerializedString) serialized).string());
        Object result = ctx.deserialize(serialized);
        assertEquals("hello", result);
    }

    @Test
    void emptyStringRoundTrip() throws IOException, ClassNotFoundException {
        Serialized serialized = ctx.serialize("");
        assertInstanceOf(SerializedString.class, serialized);
        assertEquals("", ((SerializedString) serialized).string());
        Object result = ctx.deserialize(serialized);
        assertEquals("", result);
    }
}
