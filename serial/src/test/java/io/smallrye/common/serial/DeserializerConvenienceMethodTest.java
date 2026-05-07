package io.smallrye.common.serial;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * Tests for the convenience methods on {@link Deserializer}:
 * {@link Deserializer#deserialize(Serialized, Class)},
 * {@link Deserializer#deserializeClass(Serialized)}, and
 * {@link Deserializer#deserializeClass(Serialized, Class)}.
 */
class DeserializerConvenienceMethodTest {

    private final SerialContext ctx = SerialContext.builder().addDefaultProviders().build();

    @Test
    void deserializeWithType() throws IOException, ClassNotFoundException {
        Serialized serialized = ctx.serialize("hello");
        String result = ctx.deserialize(serialized, String.class);
        assertEquals("hello", result);
    }

    @Test
    void deserializeWithWrongTypeThrows() throws IOException {
        Serialized serialized = ctx.serialize("hello");
        assertThrows(ClassCastException.class, () -> ctx.deserialize(serialized, Integer.class));
    }

    @Test
    void deserializeClass() throws IOException, ClassNotFoundException {
        Serialized serialized = ctx.serialize(String.class);
        Class<?> result = ctx.deserializeClass(serialized);
        assertSame(String.class, result);
    }

    @Test
    void deserializeClassWithBound() throws IOException, ClassNotFoundException {
        Serialized serialized = ctx.serialize(Integer.class);
        Class<? extends Number> result = ctx.deserializeClass(serialized, Number.class);
        assertSame(Integer.class, result);
    }

    @Test
    void deserializeClassWithWrongBoundThrows() throws IOException {
        Serialized serialized = ctx.serialize(Integer.class);
        assertThrows(ClassCastException.class, () -> ctx.deserializeClass(serialized, String.class));
    }
}
