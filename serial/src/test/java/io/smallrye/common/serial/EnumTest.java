package io.smallrye.common.serial;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * Tests for round-trip serialization and deserialization of enum constants.
 */
class EnumTest {

    private final SerialContext ctx = SerialContext.builder().addDefaultProviders().build();

    @Test
    void enumRoundTrip() throws IOException, ClassNotFoundException {
        Thread.State original = Thread.State.RUNNABLE;
        Serialized serialized = ctx.serialize(original);
        assertInstanceOf(SerializedEnum.class, serialized);
        Object result = ctx.deserialize(serialized);
        assertSame(original, result);
    }

    @Test
    void checkIntermediateStructure() throws IOException {
        Serialized serialized = ctx.serialize(Thread.State.BLOCKED);
        SerializedEnum se = (SerializedEnum) serialized;
        assertInstanceOf(SerializedEnumClass.class, se.enumClass());
        assertEquals("java.lang.Thread$State", se.enumClass().name());
        assertInstanceOf(SerializedString.class, se.constantName());
        assertEquals("BLOCKED", ((SerializedString) se.constantName()).string());
    }
}
