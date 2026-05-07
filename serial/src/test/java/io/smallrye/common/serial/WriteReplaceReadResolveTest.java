package io.smallrye.common.serial;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.Serializable;

import org.junit.jupiter.api.Test;

/**
 * Tests for round-trip serialization involving {@code writeReplace} and {@code readResolve} methods.
 */
class WriteReplaceReadResolveTest {

    private final SerialContext ctx = SerialContext.builder().addDefaultProviders().build();

    /**
     * A serializable class whose {@code writeReplace} method substitutes a {@link Replacement}.
     */
    public static class Replaceable implements Serializable {
        private static final long serialVersionUID = 1L;
        final int value;

        Replaceable(int value) {
            this.value = value;
        }

        private Object writeReplace() {
            return new Replacement(value);
        }
    }

    /**
     * The replacement object written in place of {@link Replaceable}.
     * Its {@code readResolve} method produces a {@link Resolved} instance.
     */
    public static class Replacement implements Serializable {
        private static final long serialVersionUID = 1L;
        int value;

        /** No-arg constructor. */
        public Replacement() {
        }

        Replacement(int value) {
            this.value = value;
        }

        private Object readResolve() {
            return new Resolved(value);
        }
    }

    /**
     * The final resolved object after deserialization of a {@link Replaceable}.
     */
    public static class Resolved implements Serializable {
        private static final long serialVersionUID = 1L;
        int value;

        /** No-arg constructor. */
        public Resolved() {
        }

        Resolved(int value) {
            this.value = value;
        }
    }

    @Test
    void writeReplaceRoundTrip() throws IOException, ClassNotFoundException {
        Replaceable original = new Replaceable(99);
        Serialized serialized = ctx.serialize(original);
        // the intermediate should be for Replacement, not Replaceable
        assertInstanceOf(SerializedSerializable.class, serialized);
        SerializedSerializable ss = (SerializedSerializable) serialized;
        assertEquals(Replacement.class.getName(), ss.serializedClass().name());

        Object result = ctx.deserialize(serialized);
        assertInstanceOf(Resolved.class, result);
        assertEquals(99, ((Resolved) result).value);
    }
}
