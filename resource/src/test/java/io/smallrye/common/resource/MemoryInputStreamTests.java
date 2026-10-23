package io.smallrye.common.resource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

public final class MemoryInputStreamTests {
    @Test
    public void zeroLengthReadsAtEndOfStream() throws IOException {
        byte[] destination = { 9, 9 };
        try (MemoryInputStream stream = new MemoryInputStream(new byte[] { 1 })) {
            assertEquals(0, stream.read(destination, 1, 0));
            assertEquals(1, stream.read());
            assertEquals(0, stream.read(destination, 2, 0));
            assertEquals(0, stream.read(new byte[0]));
            assertEquals(-1, stream.read(destination, 0, 1));
            assertArrayEquals(new byte[] { 9, 9 }, destination);
        }
    }

    @Test
    public void zeroLengthReadOfEmptyStream() throws IOException {
        try (MemoryInputStream stream = new MemoryInputStream(new byte[0])) {
            assertEquals(0, stream.read(new byte[0]));
            assertEquals(-1, stream.read());
        }
    }

    @Test
    public void negativeSkipsDoNotConsumeData() throws IOException {
        for (long count : new long[] { -1, Integer.MIN_VALUE, -0xffff_ffffL, Long.MIN_VALUE }) {
            try (MemoryInputStream stream = new MemoryInputStream(new byte[] { 1, 2, 3 })) {
                assertEquals(0, stream.skip(count), "skip count: " + count);
                assertArrayEquals(new byte[] { 1, 2, 3 }, stream.readAllBytes());
                assertEquals(0, stream.skip(count));
            }
        }
    }

    @Test
    public void negativeSkipCannotWrapToPositiveCount() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(5);
        buffer.put(new byte[] { 9, 1, 2, 3, 9 }).flip().position(1).limit(4);
        try (MemoryInputStream stream = new MemoryInputStream(buffer.asReadOnlyBuffer())) {
            assertEquals(0, stream.skip(-0xffff_ffffL));
            assertArrayEquals(new byte[] { 1, 2, 3 }, stream.readAllBytes());
            assertEquals(1, buffer.position());
            assertEquals(4, buffer.limit());
        }
    }

    @Test
    public void positiveSkipsAreLimitedToRemainingData() throws IOException {
        try (MemoryInputStream stream = new MemoryInputStream(new byte[] { 1, 2, 3 })) {
            assertEquals(0, stream.skip(0));
            assertEquals(1, stream.skip(1));
            assertEquals(2, stream.read());
            assertEquals(1, stream.skip(Long.MAX_VALUE));
            assertEquals(0, stream.skip(1));
            assertEquals(-1, stream.read());
        }
    }

    @Test
    public void emptyReadsStillValidateArguments() throws IOException {
        try (MemoryInputStream stream = new MemoryInputStream(new byte[0])) {
            assertThrows(NullPointerException.class, () -> stream.read(null, 0, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> stream.read(new byte[0], 1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> stream.read(new byte[0], 0, -1));
        }
    }

    @Test
    public void noOpOperationsStillCheckForClosedStream() {
        MemoryInputStream stream = new MemoryInputStream(new byte[0]);
        stream.close();
        assertThrows(IOException.class, () -> stream.read(new byte[0]));
        assertThrows(IOException.class, () -> stream.skip(-1));
        assertThrows(IOException.class, () -> stream.skip(0));
    }
}
