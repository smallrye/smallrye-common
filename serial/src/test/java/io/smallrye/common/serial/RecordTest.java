package io.smallrye.common.serial;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.Serializable;

import org.junit.jupiter.api.Test;

/**
 * Tests for round-trip serialization and deserialization of {@code record} types.
 */
class RecordTest {

    private final SerialContext ctx = SerialContext.builder().addDefaultProviders().build();

    /**
     * A simple record with two primitive fields.
     */
    public record SimpleRecord(int x, int y) implements Serializable {
    }

    /**
     * A record with mixed primitive and object fields.
     */
    public record MixedRecord(String name, int value) implements Serializable {
    }

    /**
     * A record with no components.
     */
    public record EmptyRecord() implements Serializable {
    }

    @Test
    void simpleRecordRoundTrip() throws IOException, ClassNotFoundException {
        SimpleRecord original = new SimpleRecord(3, 4);
        Serialized serialized = ctx.serialize(original);
        assertInstanceOf(SerializedRecord.class, serialized);
        SimpleRecord result = (SimpleRecord) ctx.deserialize(serialized);
        assertEquals(original, result);
    }

    @Test
    void mixedRecordRoundTrip() throws IOException, ClassNotFoundException {
        MixedRecord original = new MixedRecord("test", 42);
        Serialized serialized = ctx.serialize(original);
        assertInstanceOf(SerializedRecord.class, serialized);
        MixedRecord result = (MixedRecord) ctx.deserialize(serialized);
        assertEquals(original, result);
    }

    @Test
    void emptyRecordRoundTrip() throws IOException, ClassNotFoundException {
        EmptyRecord original = new EmptyRecord();
        Serialized serialized = ctx.serialize(original);
        assertInstanceOf(SerializedRecord.class, serialized);
        EmptyRecord result = (EmptyRecord) ctx.deserialize(serialized);
        assertEquals(original, result);
    }

    @Test
    void checkIntermediateStructure() throws IOException {
        Serialized serialized = ctx.serialize(new SimpleRecord(1, 2));
        SerializedRecord sr = (SerializedRecord) serialized;
        SerializedRecordClass rc = sr.recordClass();
        assertInstanceOf(SerializedRecordClass.class, rc);
        assertEquals(SimpleRecord.class.getName(), rc.name());
        assertTrue(rc.streamFields().containsKey("x"));
        assertTrue(rc.streamFields().containsKey("y"));
    }
}
