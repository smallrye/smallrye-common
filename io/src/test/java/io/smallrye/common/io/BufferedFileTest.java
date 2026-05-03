package io.smallrye.common.io;

import static java.nio.file.StandardOpenOption.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import java.nio.channels.Pipe;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BufferedFileTest {

    @TempDir
    Path tempDir;

    private Path testFile() {
        return tempDir.resolve("test.bin");
    }

    // ── Construction / Open Options ─────────────────────────────────────

    @Test
    void openForReadOnExistingFile() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2, 3 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertEquals(0, bf.filePosition());
                assertEquals(3, bf.length());
            }
        }
    }

    @Test
    void openWriteCreate() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, WRITE, CREATE)) {
                bf.writeByte(42);
            }
            assertTrue(Files.exists(f));
        }
    }

    @Test
    void openCreateNewFailsOnExisting() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[0]);
            assertThrows(FileAlreadyExistsException.class, () -> {
                try (BufferedFile ignored1 = Files2.openBuffered(f, WRITE, CREATE_NEW)) {
                    fail("unreachable");
                }
            });
        }
    }

    @Test
    void openTruncateExisting() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2, 3 });
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, TRUNCATE_EXISTING)) {
                assertEquals(0, bf.length());
            }
        }
    }

    @Test
    void openAppendMode() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2, 3 });
            assertThrows(UnsupportedOperationException.class, () -> {
                try (BufferedFile ignored2 = Files2.openBuffered(f, READ, WRITE, APPEND)) {
                    fail("unreachable");
                }
            });
        }
    }

    @Test
    void customBufferSize() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[100]);
            try (BufferedFile bf = Files2.openBuffered(f, READ, BufferSizeOption.of(32))) {
                assertEquals(32, bf.fill(32));
            }
        }
    }

    @Test
    void customByteOrder() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.LITTLE_ENDIAN)) {
                assertEquals(ByteOrder.LITTLE_ENDIAN, bf.byteOrder());
            }
        }
    }

    @Test
    void defaultByteOrderIsNative() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                assertEquals(ByteOrder.nativeOrder(), bf.byteOrder());
            }
        }
    }

    @Test
    void conflictingByteOrderThrows() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            assertThrows(IllegalArgumentException.class,
                    () -> {
                        try (BufferedFile ignored1 = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN,
                                ByteOrderOption.LITTLE_ENDIAN)) {
                            fail("unreachable");
                        }
                    });
        }
    }

    @Test
    void nullOptionThrows() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            assertThrows(NullPointerException.class, () -> {
                try (BufferedFile ignored1 = Files2.openBuffered(f, (OpenOption) null)) {
                    fail("unreachable");
                }
            });
        }
    }

    @Test
    void collectionOverload() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2 });
            try (BufferedFile bf = Files2.openBuffered(f, List.of(READ))) {
                assertEquals(1, bf.readByte());
            }
        }
    }

    // ── Sequential byte read/write ──────────────────────────────────────

    @Test
    void writeAndReadByte() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeByte(0xAB);
                bf.seek(0);
                assertEquals(0xAB, bf.readUnsignedByte());
            }
        }
    }

    @Test
    void readByteReturnsMinusOneAtEof() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[0]);
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertEquals(-1, bf.read());
            }
        }
    }

    // ── Sequential short read/write (both byte orders) ──────────────────

    @Test
    void writeAndReadShortBE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeShort(0x1234);
                bf.seek(0);
                assertEquals((short) 0x1234, bf.readShort());
            }
        }
    }

    @Test
    void writeAndReadShortLE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeShortLE(0x1234);
                bf.seek(0);
                assertEquals((short) 0x1234, bf.readShortLE());
            }
        }
    }

    // ── Sequential int read/write ───────────────────────────────────────

    @Test
    void writeAndReadIntBE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeInt(0x12345678);
                bf.seek(0);
                assertEquals(0x12345678, bf.readInt());
            }
        }
    }

    @Test
    void writeAndReadIntLE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeIntLE(0x12345678);
                bf.seek(0);
                assertEquals(0x12345678, bf.readIntLE());
            }
        }
    }

    // ── Sequential long read/write ──────────────────────────────────────

    @Test
    void writeAndReadLongBE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeLong(0x123456789ABCDEF0L);
                bf.seek(0);
                assertEquals(0x123456789ABCDEF0L, bf.readLong());
            }
        }
    }

    @Test
    void writeAndReadLongLE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeLongLE(0x123456789ABCDEF0L);
                bf.seek(0);
                assertEquals(0x123456789ABCDEF0L, bf.readLongLE());
            }
        }
    }

    // ── Sequential float/double/boolean/char ────────────────────────────

    @Test
    void writeAndReadFloat() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeFloat(3.14f);
                bf.seek(0);
                assertEquals(3.14f, bf.readFloat());
            }
        }
    }

    @Test
    void writeAndReadFloatPreservesNaN() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                float nanWithPayload = Float.intBitsToFloat(0x7FC01234);
                bf.writeFloat(nanWithPayload);
                bf.seek(0);
                assertEquals(Float.floatToRawIntBits(nanWithPayload), Float.floatToRawIntBits(bf.readFloat()));
            }
        }
    }

    @Test
    void writeAndReadDouble() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeDouble(2.718281828);
                bf.seek(0);
                assertEquals(2.718281828, bf.readDouble());
            }
        }
    }

    @Test
    void writeAndReadBoolean() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeBoolean(true);
                bf.writeBoolean(false);
                bf.seek(0);
                assertTrue(bf.readBoolean());
                assertFalse(bf.readBoolean());
            }
        }
    }

    @Test
    void writeAndReadChar() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeChar('A');
                bf.seek(0);
                assertEquals('A', bf.readChar());
            }
        }
    }

    // ── Random-access primitives ────────────────────────────────────────

    @Test
    void randomAccessByteReadWrite() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(16);
                bf.writeByte(5L, 0xDD);
                assertEquals(0xDD, bf.readUnsignedByte(5L));
                // Position unchanged
                assertEquals(16, bf.filePosition());
            }
        }
    }

    @Test
    void randomAccessShortBE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(16);
                bf.writeShortBE(4L, 0x1234);
                assertEquals((short) 0x1234, bf.readShortBE(4L));
            }
        }
    }

    @Test
    void randomAccessIntLE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(16);
                bf.writeIntLE(8L, 0xDEADBEEF);
                assertEquals(0xDEADBEEF, bf.readIntLE(8L));
            }
        }
    }

    @Test
    void randomAccessLongBE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(16);
                bf.writeLongBE(0L, 0xCAFEBABEDEADFACEL);
                assertEquals(0xCAFEBABEDEADFACEL, bf.readLongBE(0L));
            }
        }
    }

    @Test
    void randomAccessPositionUnchanged() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(32);
                long before = bf.filePosition();
                bf.writeInt(16L, 42);
                assertEquals(before, bf.filePosition());
                bf.readInt(16L);
                assertEquals(before, bf.filePosition());
            }
        }
    }

    @Test
    void randomAccessReadBeyondEofThrows() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertThrows(EOFException.class, () -> bf.readByte(100L));
            }
        }
    }

    @Test
    void randomAccessWriteBeyondEofExtendsFile() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeByte(100L, 0xFF);
                assertTrue(bf.length() > 100);
            }
        }
    }

    // ── Buffer boundary spanning ────────────────────────────────────────

    @Test
    void intSpanningBufferBoundary() throws IOException {
        // Use a tiny buffer so we can easily span the boundary
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, BufferSizeOption.of(16),
                    ByteOrderOption.BIG_ENDIAN)) {
                // Write 14 bytes to nearly fill the 16-byte buffer
                bf.writeZeros(14);
                // Write a 4-byte int that spans bytes 14-17 (crosses buffer boundary)
                bf.writeInt(0xDEADBEEF);
                bf.flush();
                // Read back at the boundary-spanning position
                assertEquals(0xDEADBEEF, bf.readInt(14L));
            }
        }
    }

    @Test
    void longSpanningBufferBoundary() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, BufferSizeOption.of(16),
                    ByteOrderOption.LITTLE_ENDIAN)) {
                bf.writeZeros(12);
                bf.writeLong(0x0102030405060708L);
                bf.flush();
                assertEquals(0x0102030405060708L, bf.readLong(12L));
            }
        }
    }

    @Test
    void shortSpanningBufferBoundary() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, BufferSizeOption.of(16),
                    ByteOrderOption.BIG_ENDIAN)) {
                bf.writeZeros(15);
                bf.writeShort(0xABCD);
                bf.flush();
                assertEquals((short) 0xABCD, bf.readShort(15L));
            }
        }
    }

    // ── Buffer: fully inside, fully outside, no flush on outside ────────

    @Test
    void randomWriteInsideBufferNoRafAccess() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, BufferSizeOption.of(64))) {
                bf.writeZeros(32); // buffer now has 32 bytes
                // Write inside the buffer window
                bf.writeInt(4L, 0x12345678);
                // Read back from buffer without flush
                assertEquals(0x12345678, bf.readInt(4L));
            }
        }
    }

    @Test
    void randomWriteOutsideBufferDoesNotFlush() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, BufferSizeOption.of(16))) {
                bf.writeZeros(16); // fills the buffer
                bf.flush();
                // Write 4 more bytes (sequential, buffer repositioned)
                bf.writeInt(0xAAAAAAAA);
                // Now random-write outside the buffer window (at position 0)
                // This should NOT flush the buffer
                bf.writeByte(0L, 0x99);
                // The buffered data should still be readable
                assertEquals(0xAAAAAAAA, bf.readInt(16L));
            }
        }
    }

    // ── Byte array operations ───────────────────────────────────────────

    @Test
    void writeAndReadByteArray() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            byte[] data = { 1, 2, 3, 4, 5, 6, 7, 8 };
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.write(data);
                bf.seek(0);
                byte[] result = new byte[8];
                int n = bf.read(result);
                assertEquals(8, n);
                assertArrayEquals(data, result);
            }
        }
    }

    @Test
    void writeAndReadByteArrayLargerThanBuffer() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            byte[] data = new byte[256];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) i;
            }
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, BufferSizeOption.of(32))) {
                bf.write(data);
                bf.seek(0);
                byte[] result = new byte[256];
                bf.readFully(result);
                assertArrayEquals(data, result);
            }
        }
    }

    @Test
    void readFullyThrowsOnShortRead() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertThrows(EOFException.class, () -> bf.readFully(new byte[10]));
            }
        }
    }

    // ── ByteBuffer operations ───────────────────────────────────────────

    @Test
    void writeAndReadHeapByteBuffer() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            ByteBuffer src = ByteBuffer.wrap(new byte[] { 10, 20, 30, 40 });
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.write(src);
                bf.seek(0);
                ByteBuffer dst = ByteBuffer.allocate(4);
                bf.read(dst);
                dst.flip();
                assertEquals(10, dst.get());
                assertEquals(20, dst.get());
            }
        }
    }

    @Test
    void writeAndReadDirectByteBuffer() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            ByteBuffer src = ByteBuffer.allocateDirect(4);
            src.put(new byte[] { 5, 6, 7, 8 });
            src.flip();
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.write(src);
                bf.seek(0);
                ByteBuffer dst = ByteBuffer.allocateDirect(4);
                bf.read(dst);
                dst.flip();
                assertEquals(5, dst.get());
                assertEquals(8, dst.get(3));
            }
        }
    }

    // ── String operations ───────────────────────────────────────────────

    @Test
    void writeAndReadZeroTerminatedString() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeroTerminatedString("hello");
                bf.seek(0);
                assertEquals("hello", bf.readZeroTerminatedString());
            }
        }
    }

    @Test
    void writeAndReadZeroTerminatedStringWithCharset() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeroTerminatedString("café", StandardCharsets.UTF_8);
                bf.seek(0);
                assertEquals("café", bf.readZeroTerminatedString(StandardCharsets.UTF_8));
            }
        }
    }

    @Test
    void writeAndReadFixedLengthString() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeString("test");
                bf.seek(0);
                assertEquals("test", bf.readString(4));
            }
        }
    }

    @Test
    void writeNonTerminatedString() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeString("abc");
                bf.writeString("def");
                bf.seek(0);
                assertEquals("abcdef", bf.readString(6));
            }
        }
    }

    // ── Stream views ────────────────────────────────────────────────────

    @Test
    void inputStreamIsFileInputStream() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2, 3 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertInstanceOf(FileInputStream.class, bf.inputStream());
            }
        }
    }

    @Test
    void outputStreamIsFileOutputStream() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                assertInstanceOf(FileOutputStream.class, bf.outputStream());
            }
        }
    }

    @Test
    void inputStreamDelegates() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 10, 20, 30 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                InputStream is = bf.inputStream();
                assertEquals(10, is.read());
                assertEquals(20, is.read());
                assertEquals(30, is.read());
                assertEquals(-1, is.read());
            }
        }
    }

    @Test
    void outputStreamDelegates() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                OutputStream os = bf.outputStream();
                os.write(new byte[] { 1, 2, 3 });
                os.flush();
                bf.seek(0);
                assertEquals(1, bf.readByte());
            }
        }
    }

    @Test
    void inputStreamMarkReset() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2, 3, 4, 5 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                InputStream is = bf.inputStream();
                assertTrue(is.markSupported());
                assertEquals(1, is.read());
                is.mark(100);
                assertEquals(2, is.read());
                assertEquals(3, is.read());
                is.reset();
                assertEquals(2, is.read()); // back to mark position
            }
        }
    }

    @Test
    void viewCloseIsNoOp() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1 });
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.inputStream().close();
                bf.outputStream().close();
                // BufferedFile still works
                // make sure it's still open
                bf.length();
                bf.seek(0);
                assertEquals(1, bf.readByte());
            }
        }
    }

    @Test
    void viewsAreCached() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1 });
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                assertSame(bf.inputStream(), bf.inputStream());
                assertSame(bf.outputStream(), bf.outputStream());
                assertSame(bf.channel(), bf.channel());
            }
        }
    }

    @Test
    void inputStreamThrowsIfNotReadable() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, WRITE, CREATE)) {
                assertThrows(IllegalStateException.class, bf::inputStream);
            }
        }
    }

    @Test
    void outputStreamThrowsIfNotWritable() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertThrows(IllegalStateException.class, bf::outputStream);
            }
        }
    }

    // ── DataInput/DataOutput on views (BE semantics) ────────────────────

    @Test
    void dataInputReadsBE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.LITTLE_ENDIAN)) {
                // Write via DataOutput (always BE)
                DataOutput dout = bf.outputStream();
                dout.writeInt(0x12345678);
                bf.seek(0);
                // Read via DataInput (always BE)
                DataInput din = bf.inputStream();
                assertEquals(0x12345678, din.readInt());
            }
        }
    }

    @Test
    void dataOutputWritesBE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.LITTLE_ENDIAN)) {
                DataOutput dout = bf.outputStream();
                dout.writeShort(0x1234);
                bf.seek(0);
                // The bytes should be in BE order regardless of file byte order
                assertEquals(0x12, bf.readByte());
                assertEquals(0x34, bf.readByte());
            }
        }
    }

    @Test
    void dataInputReadUTF() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                DataOutput dout = bf.outputStream();
                dout.writeUTF("hello");
                bf.seek(0);
                DataInput din = bf.inputStream();
                assertEquals("hello", din.readUTF());
            }
        }
    }

    @Test
    void dataInputReadLine() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.write("line1\nline2\r\nline3".getBytes(StandardCharsets.US_ASCII));
                bf.seek(0);
                DataInput din = bf.inputStream();
                assertEquals("line1", din.readLine());
                assertEquals("line2", din.readLine());
                assertEquals("line3", din.readLine());
            }
        }
    }

    @Test
    void dataInputReadFully() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.write(new byte[] { 1, 2, 3, 4 });
                bf.seek(0);
                byte[] buf = new byte[4];
                DataInput din = bf.inputStream();
                din.readFully(buf);
                assertArrayEquals(new byte[] { 1, 2, 3, 4 }, buf);
            }
        }
    }

    @Test
    void dataInputReadBoolean() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                DataOutput dout = bf.outputStream();
                dout.writeBoolean(true);
                dout.writeBoolean(false);
                bf.seek(0);
                DataInput din = bf.inputStream();
                assertTrue(din.readBoolean());
                assertFalse(din.readBoolean());
            }
        }
    }

    @Test
    void dataInputReadFloat() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                DataOutput dout = bf.outputStream();
                dout.writeFloat(1.5f);
                bf.seek(0);
                DataInput din = bf.inputStream();
                assertEquals(1.5f, din.readFloat());
            }
        }
    }

    @Test
    void dataInputReadDouble() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                DataOutput dout = bf.outputStream();
                dout.writeDouble(2.5);
                bf.seek(0);
                DataInput din = bf.inputStream();
                assertEquals(2.5, din.readDouble());
            }
        }
    }

    @Test
    void dataInputReadChar() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                DataOutput dout = bf.outputStream();
                dout.writeChar('Z');
                bf.seek(0);
                DataInput din = bf.inputStream();
                assertEquals('Z', din.readChar());
            }
        }
    }

    @Test
    void dataOutputWriteBytes() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                DataOutput dout = bf.outputStream();
                dout.writeBytes("AB");
                bf.seek(0);
                assertEquals('A', bf.readByte());
                assertEquals('B', bf.readByte());
            }
        }
    }

    @Test
    void dataOutputWriteChars() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                DataOutput dout = bf.outputStream();
                dout.writeChars("AB");
                bf.seek(0);
                // Each char is 2 bytes in BE
                assertEquals(0, bf.readByte());
                assertEquals('A', bf.readByte());
                assertEquals(0, bf.readByte());
                assertEquals('B', bf.readByte());
            }
        }
    }

    // ── FileChannel view ────────────────────────────────────────────────

    @Test
    void fileChannelViewReadWrite() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                FileChannel ch = bf.channel();
                ByteBuffer src = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 });
                ch.write(src);
                assertEquals(4, ch.position());
                ch.position(0);
                ByteBuffer dst = ByteBuffer.allocate(4);
                ch.read(dst);
                dst.flip();
                assertEquals(1, dst.get());
                assertEquals(4, dst.get(3));
            }
        }
    }

    @Test
    void fileChannelViewSize() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(100);
                assertEquals(100, bf.channel().size());
            }
        }
    }

    @Test
    void fileChannelViewCloseIsNoOp() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.channel().close();
                // make sure it's still open
                bf.length();
            }
        }
    }

    // ── Transfer operations ─────────────────────────────────────────────

    @Test
    void transferToOutputStream() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2, 3, 4, 5 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                long n = bf.transferTo(baos);
                assertEquals(5, n);
                assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, baos.toByteArray());
            }
        }
    }

    @Test
    void transferToWithLength() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2, 3, 4, 5 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                long n = bf.transferTo(baos, 3);
                assertEquals(3, n);
                assertArrayEquals(new byte[] { 1, 2, 3 }, baos.toByteArray());
            }
        }
    }

    @Test
    void transferToFromPosition() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2, 3, 4, 5 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                long n = bf.transferTo(2, baos, 2);
                assertEquals(2, n);
                assertArrayEquals(new byte[] { 3, 4 }, baos.toByteArray());
                // Position should be unchanged
                assertEquals(0, bf.filePosition());
            }
        }
    }

    @Test
    void transferFromInputStream() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                ByteArrayInputStream bais = new ByteArrayInputStream(new byte[] { 10, 20, 30 });
                long n = bf.transferFrom(bais);
                assertEquals(3, n);
                bf.seek(0);
                assertEquals(10, bf.readByte());
            }
        }
    }

    @Test
    void transferFromWithLength() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                ByteArrayInputStream bais = new ByteArrayInputStream(new byte[] { 10, 20, 30 });
                long n = bf.transferFrom(bais, 2);
                assertEquals(2, n);
                assertEquals(2, bf.filePosition());
            }
        }
    }

    @Test
    void transferFromAtPosition() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(10);
                ByteArrayInputStream bais = new ByteArrayInputStream(new byte[] { 0x42, 0x43 });
                long n = bf.transferFrom(5L, bais, 2);
                assertEquals(2, n);
                assertEquals(10, bf.filePosition()); // unchanged
                assertEquals(0x42, bf.readByte(5L));
                assertEquals(0x43, bf.readByte(6L));
            }
        }
    }

    // ── Write zeros ─────────────────────────────────────────────────────

    @Test
    void writeZerosSequential() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(100);
                assertEquals(100, bf.filePosition());
                bf.seek(0);
                for (int i = 0; i < 100; i++) {
                    assertEquals(0, bf.readByte());
                }
            }
        }
    }

    @Test
    void writeZerosRandomAccess() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(10);
                bf.writeByte(5L, 0xFF);
                bf.writeZeros(3L, 4); // overwrite bytes 3-6 with zeros
                assertEquals(0, bf.readByte(5L)); // was overwritten
                assertEquals(10, bf.filePosition()); // unchanged by random write
            }
        }
    }

    // ── Fill behavior ───────────────────────────────────────────────────

    @Test
    void fillReturnsAvailableBytes() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[100]);
            try (BufferedFile bf = Files2.openBuffered(f, READ, BufferSizeOption.of(32))) {
                int avail = bf.fill(32);
                assertTrue(avail > 0 && avail <= 32);
            }
        }
    }

    @Test
    void fillReturnsMinusOneAtEof() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[0]);
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertEquals(-1, bf.fill(10));
            }
        }
    }

    // ── Length includes unflushed data ───────────────────────────────────

    @Test
    void lengthIncludesBufferedData() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeByte(1);
                bf.writeByte(2);
                // Not flushed, but length should reflect it
                assertEquals(2, bf.length());
            }
        }
    }

    // ── setLength ───────────────────────────────────────────────────────

    @Test
    void setLengthTruncates() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(100);
                bf.flush();
                bf.setLength(50);
                assertEquals(50, bf.length());
            }
        }
    }

    @Test
    void setLengthExtends() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeByte(1);
                bf.flush();
                bf.setLength(100);
                assertEquals(100, bf.length());
            }
        }
    }

    @Test
    void setLengthTrimsBuffer() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(50);
                // Buffer holds data [0, 50). Truncate to 20.
                bf.setLength(20);
                // Position unchanged
                assertEquals(50, bf.filePosition());
                // But length is 20
                assertEquals(20, bf.length());
            }
        }
    }

    // ── Skip ────────────────────────────────────────────────────────────

    @Test
    void skipAdvancesPosition() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[100]);
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                long skipped = bf.skip(50);
                assertEquals(50, skipped);
                assertEquals(50, bf.filePosition());
            }
        }
    }

    // ── Remaining ───────────────────────────────────────────────────────

    @Test
    void remainingReportsCorrectly() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[100]);
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertEquals(100, bf.remaining());
                bf.skip(30);
                assertEquals(70, bf.remaining());
            }
        }
    }

    // ── Close behavior ──────────────────────────────────────────────────

    @Test
    void closeFlushesData() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeByte(42);
            }
            // Data should be persisted
            byte[] data = Files.readAllBytes(f);
            assertEquals(1, data.length);
            assertEquals(42, data[0] & 0xFF);
        }
    }

    @Test
    void operationAfterCloseThrows() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE);
            bf.close();
            assertThrows(IOException.class, () -> bf.writeByte(1));
            assertThrows(IOException.class, bf::readByte);
        }
    }

    @Test
    void doubleCloseIsSafe() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE);
            bf.close();
            assertDoesNotThrow(bf::close);
        }
    }

    // ── Dirty tracking ──────────────────────────────────────────────────

    @Test
    void readOnlyDoesNotDirtyBuffer() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2, 3 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                bf.readByte();
                // Flush should be a no-op (not dirty)
                bf.flush(); // should not throw
            }
        }
    }

    // ── Readable/writable checks ────────────────────────────────────────

    @Test
    void readOnFileOpenedForWriteOnlyThrows() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, WRITE, CREATE)) {
                assertThrows(IOException.class, bf::readByte);
            }
        }
    }

    @Test
    void writeOnFileOpenedForReadOnlyThrows() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertThrows(IOException.class, () -> bf.writeByte(1));
            }
        }
    }

    // ── BufferSizeOption ────────────────────────────────────────────────

    @Test
    void bufferSizeOptionOfDefaultReturnsConstant() {
        //noinspection EqualsWithItself
        assertSame(BufferSizeOption.ofDefault(), BufferSizeOption.ofDefault());
        assertSame(BufferSizeOption.ofDefault(), BufferSizeOption.of(8192));
    }

    @Test
    void bufferSizeOptionRejectsZero() {
        assertThrows(IllegalArgumentException.class, () -> BufferSizeOption.of(0));
    }

    @Test
    void bufferSizeOptionRejectsNegative() {
        assertThrows(IllegalArgumentException.class, () -> BufferSizeOption.of(-1));
    }

    @Test
    void bufferSizeOptionEquals() {
        assertEquals(BufferSizeOption.of(64), BufferSizeOption.of(64));
        assertNotEquals(BufferSizeOption.of(32), BufferSizeOption.of(64));
    }

    @Test
    void bufferSizeOptionToString() {
        assertEquals("BufferSizeOption[8192]", BufferSizeOption.ofDefault().toString());
    }

    // ── ByteOrderOption ─────────────────────────────────────────────────

    @Test
    void byteOrderOptionValues() {
        assertEquals(ByteOrder.BIG_ENDIAN, ByteOrderOption.BIG_ENDIAN.byteOrder());
        assertEquals(ByteOrder.LITTLE_ENDIAN, ByteOrderOption.LITTLE_ENDIAN.byteOrder());
    }

    // ── Interleaved access via views and direct ─────────────────────────

    @Test
    void interleavedViewAndDirectAccess() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                // Write via output stream view
                bf.outputStream().write(new byte[] { 1, 2, 3, 4 });
                // Write directly
                bf.writeByte(5);
                bf.seek(0);
                // Read via input stream view
                assertEquals(1, bf.inputStream().read());
                // Read directly
                assertEquals(2, bf.readByte());
                // Continue via input stream
                assertEquals(3, bf.inputStream().read());
            }
        }
    }

    @Test
    void streamViewsShareFilePosition() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.outputStream().write(new byte[] { 10, 20, 30, 40 });
                // Position is now at 4 due to output stream write
                assertEquals(4, bf.filePosition());
                bf.seek(0);
                // Read two bytes via input stream
                bf.inputStream().read();
                bf.inputStream().read();
                // Position advanced by input stream reads
                assertEquals(2, bf.filePosition());
            }
        }
    }

    // ── Byte order can be changed at runtime ────────────────────────────

    @Test
    void byteOrderChangeDuringLifetime() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeShort(0x1234); // written as BE
                bf.byteOrder(ByteOrder.LITTLE_ENDIAN);
                bf.writeShort(0x5678); // written as LE
                bf.seek(0);
                bf.byteOrder(ByteOrder.BIG_ENDIAN);
                assertEquals((short) 0x1234, bf.readShort());
                bf.byteOrder(ByteOrder.LITTLE_ENDIAN);
                assertEquals((short) 0x5678, bf.readShort());
            }
        }
    }

    // ── Random-access float/double/boolean/char ─────────────────────────

    @Test
    void randomAccessFloat() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(16);
                bf.writeFloat(4L, 1.5f);
                assertEquals(1.5f, bf.readFloat(4L));
            }
        }
    }

    @Test
    void randomAccessDouble() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(16);
                bf.writeDouble(0L, Math.PI);
                assertEquals(Math.PI, bf.readDouble(0L));
            }
        }
    }

    @Test
    void randomAccessBoolean() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(4);
                bf.writeBoolean(1L, true);
                bf.writeBoolean(2L, false);
                assertTrue(bf.readBoolean(1L));
                assertFalse(bf.readBoolean(2L));
            }
        }
    }

    @Test
    void randomAccessChar() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeZeros(8);
                bf.writeChar(2L, 'X');
                assertEquals('X', bf.readChar(2L));
            }
        }
    }

    // ── fileDescriptor ──────────────────────────────────────────────────

    @Test
    void fileDescriptorIsValid() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertNotNull(bf.fileDescriptor());
                assertTrue(bf.fileDescriptor().valid());
            }
        }
    }

    // ── Random-access byte array ────────────────────────────────────────

    @Test
    void randomAccessByteArrayReadWrite() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(20);
                byte[] src = { 10, 20, 30 };
                bf.write(5L, src, 0, 3);
                byte[] dst = new byte[3];
                int n = bf.read(5L, dst, 0, 3);
                assertEquals(3, n);
                assertArrayEquals(src, dst);
                // Position unchanged
                assertEquals(20, bf.filePosition());
            }
        }
    }

    // ── Random-access ByteBuffer ────────────────────────────────────────

    @Test
    void randomAccessByteBuffer() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(20);
                ByteBuffer src = ByteBuffer.wrap(new byte[] { 7, 8, 9 });
                bf.write(10L, src);
                ByteBuffer dst = ByteBuffer.allocate(3);
                bf.read(10L, dst);
                dst.flip();
                assertEquals(7, dst.get());
                assertEquals(8, dst.get());
                assertEquals(9, dst.get());
            }
        }
    }

    // ── Peek operations ────────────────────────────────────────────────

    @Test
    void peekByteDoesNotAdvance() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 0x42, 0x43 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertEquals(0x42, bf.peekByte());
                assertEquals(0x42, bf.peekByte()); // still at same position
                assertEquals(0, bf.filePosition());
            }
        }
    }

    @Test
    void peekByteAtEofThrows() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[0]);
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertThrows(EOFException.class, bf::peekByte);
            }
        }
    }

    @Test
    void peekUnsignedByteWorks() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { (byte) 0xFF });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertEquals(255, bf.peekUnsignedByte());
                assertEquals(0, bf.filePosition());
            }
        }
    }

    @Test
    void peekAtEofReturnsMinusOne() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[0]);
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertEquals(-1, bf.peek());
            }
        }
    }

    @Test
    void peekShortBEDoesNotAdvance() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeShort(0x1234);
                bf.seek(0);
                assertEquals((short) 0x1234, bf.peekShortBE());
                assertEquals(0, bf.filePosition());
            }
        }
    }

    @Test
    void peekShortLEDoesNotAdvance() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.LITTLE_ENDIAN)) {
                bf.writeShortLE(0x5678);
                bf.seek(0);
                assertEquals((short) 0x5678, bf.peekShortLE());
                assertEquals(0, bf.filePosition());
            }
        }
    }

    @Test
    void peekShortUsesDefaultByteOrder() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeShortBE(0xABCD);
                bf.seek(0);
                assertEquals((short) 0xABCD, bf.peekShort());
                assertEquals(0, bf.filePosition());
            }
        }
    }

    @Test
    void peekIntDoesNotAdvance() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeInt(0xDEADBEEF);
                bf.seek(0);
                assertEquals(0xDEADBEEF, bf.peekInt());
                assertEquals(0, bf.filePosition());
                assertEquals(0xDEADBEEF, bf.peekIntBE());
                assertEquals(0, bf.filePosition());
            }
        }
    }

    @Test
    void peekIntLEDoesNotAdvance() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.LITTLE_ENDIAN)) {
                bf.writeIntLE(0x12345678);
                bf.seek(0);
                assertEquals(0x12345678, bf.peekIntLE());
                assertEquals(0, bf.filePosition());
            }
        }
    }

    @Test
    void peekLongDoesNotAdvance() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeLong(0xCAFEBABEDEADFACEL);
                bf.seek(0);
                assertEquals(0xCAFEBABEDEADFACEL, bf.peekLong());
                assertEquals(0, bf.filePosition());
                assertEquals(0xCAFEBABEDEADFACEL, bf.peekLongBE());
                assertEquals(0, bf.filePosition());
            }
        }
    }

    @Test
    void peekLongLEDoesNotAdvance() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.LITTLE_ENDIAN)) {
                bf.writeLongLE(0x0102030405060708L);
                bf.seek(0);
                assertEquals(0x0102030405060708L, bf.peekLongLE());
                assertEquals(0, bf.filePosition());
            }
        }
    }

    @Test
    void peekShortBEAtEofThrows() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[1]);
            try (BufferedFile bf = Files2.openBuffered(f, READ, ByteOrderOption.BIG_ENDIAN)) {
                assertThrows(EOFException.class, bf::peekShortBE);
            }
        }
    }

    @Test
    void peekShortLEAtEofThrows() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[1]);
            try (BufferedFile bf = Files2.openBuffered(f, READ, ByteOrderOption.LITTLE_ENDIAN)) {
                assertThrows(EOFException.class, bf::peekShortLE);
            }
        }
    }

    @Test
    void peekIntBEAtEofThrows() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[3]);
            try (BufferedFile bf = Files2.openBuffered(f, READ, ByteOrderOption.BIG_ENDIAN)) {
                assertThrows(EOFException.class, bf::peekIntBE);
            }
        }
    }

    @Test
    void peekIntLEAtEofThrows() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[3]);
            try (BufferedFile bf = Files2.openBuffered(f, READ, ByteOrderOption.LITTLE_ENDIAN)) {
                assertThrows(EOFException.class, bf::peekIntLE);
            }
        }
    }

    @Test
    void peekLongBEAtEofThrows() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[7]);
            try (BufferedFile bf = Files2.openBuffered(f, READ, ByteOrderOption.BIG_ENDIAN)) {
                assertThrows(EOFException.class, bf::peekLongBE);
            }
        }
    }

    @Test
    void peekLongLEAtEofThrows() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[7]);
            try (BufferedFile bf = Files2.openBuffered(f, READ, ByteOrderOption.LITTLE_ENDIAN)) {
                assertThrows(EOFException.class, bf::peekLongLE);
            }
        }
    }

    // ── Unsigned read variants ─────────────────────────────────────────

    @Test
    void readUnsignedShortVariants() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeShortBE(0xFFFF);
                bf.writeShortLE(0xFFFF);
                bf.writeShortBE(0xFFFF);
                bf.seek(0);
                assertEquals(0xFFFF, bf.readUnsignedShort());
                assertEquals(0xFFFF, bf.readUnsignedShortLE());
                assertEquals(0xFFFF, bf.readUnsignedShortBE());
            }
        }
    }

    @Test
    void peekUnsignedShortVariants() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeShortBE((short) 0x8000);
                bf.seek(0);
                assertEquals(0x8000, bf.peekUnsignedShort());
                assertEquals(0x8000, bf.peekUnsignedShortBE());
                assertEquals(0, bf.filePosition());
            }
        }
    }

    @Test
    void peekUnsignedShortLEWorks() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.LITTLE_ENDIAN)) {
                bf.writeShortLE((short) 0x8000);
                bf.seek(0);
                assertEquals(0x8000, bf.peekUnsignedShortLE());
                assertEquals(0, bf.filePosition());
            }
        }
    }

    @Test
    void readUnsignedIntVariants() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeIntBE(0x80000000);
                bf.writeIntLE(0x80000000);
                bf.writeIntBE(0x80000000);
                bf.seek(0);
                assertEquals(0x80000000L, bf.readUnsignedInt());
                assertEquals(0x80000000L, bf.readUnsignedIntLE());
                assertEquals(0x80000000L, bf.readUnsignedIntBE());
            }
        }
    }

    @Test
    void peekUnsignedIntVariants() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeIntBE(0xFFFFFFFF);
                bf.seek(0);
                assertEquals(0xFFFFFFFFL, bf.peekUnsignedInt());
                assertEquals(0xFFFFFFFFL, bf.peekUnsignedIntBE());
                assertEquals(0, bf.filePosition());
            }
        }
    }

    @Test
    void peekUnsignedIntLEWorks() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.LITTLE_ENDIAN)) {
                bf.writeIntLE(0xFFFFFFFF);
                bf.seek(0);
                assertEquals(0xFFFFFFFFL, bf.peekUnsignedIntLE());
                assertEquals(0, bf.filePosition());
            }
        }
    }

    // ── Random-access unsigned int/short variants ──────────────────────

    @Test
    void randomAccessUnsignedIntVariants() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeZeros(24);
                bf.writeIntBE(0L, 0x80000000);
                bf.writeIntLE(4L, 0x80000000);
                bf.writeIntBE(8L, 0x80000000);
                assertEquals(0x80000000L, bf.readUnsignedInt(0L));
                assertEquals(0x80000000L, bf.readUnsignedIntLE(4L));
                assertEquals(0x80000000L, bf.readUnsignedIntBE(8L));
            }
        }
    }

    @Test
    void randomAccessUnsignedShortVariants() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, ByteOrderOption.BIG_ENDIAN)) {
                bf.writeZeros(12);
                bf.writeShortBE(0L, (short) 0x8000);
                bf.writeShortLE(2L, (short) 0x8000);
                bf.writeShortBE(4L, (short) 0x8000);
                assertEquals(0x8000, bf.readUnsignedShort(0L));
                assertEquals(0x8000, bf.readUnsignedShortLE(2L));
                assertEquals(0x8000, bf.readUnsignedShortBE(4L));
            }
        }
    }

    // ── Explicit LE/BE float/double sequential ─────────────────────────

    @Test
    void writeAndReadFloatLE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeFloatLE(1.5f);
                bf.seek(0);
                assertEquals(1.5f, bf.readFloatLE());
            }
        }
    }

    @Test
    void writeAndReadFloatBE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeFloatBE(2.5f);
                bf.seek(0);
                assertEquals(2.5f, bf.readFloatBE());
            }
        }
    }

    @Test
    void writeAndReadDoubleLE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeDoubleLE(Math.E);
                bf.seek(0);
                assertEquals(Math.E, bf.readDoubleLE());
            }
        }
    }

    @Test
    void writeAndReadDoubleBE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeDoubleBE(Math.PI);
                bf.seek(0);
                assertEquals(Math.PI, bf.readDoubleBE());
            }
        }
    }

    // ── Random-access explicit LE/BE float/double ──────────────────────

    @Test
    void randomAccessFloatLEAndBE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(16);
                bf.writeFloatLE(0L, 1.25f);
                bf.writeFloatBE(4L, 2.75f);
                assertEquals(1.25f, bf.readFloatLE(0L));
                assertEquals(2.75f, bf.readFloatBE(4L));
            }
        }
    }

    @Test
    void randomAccessDoubleLEAndBE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(24);
                bf.writeDoubleLE(0L, 1.125);
                bf.writeDoubleBE(8L, 2.875);
                assertEquals(1.125, bf.readDoubleLE(0L));
                assertEquals(2.875, bf.readDoubleBE(8L));
            }
        }
    }

    // ── Explicit LE/BE char sequential and random-access ───────────────

    @Test
    void writeAndReadCharLE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeCharLE('Z');
                bf.seek(0);
                assertEquals('Z', bf.readCharLE());
            }
        }
    }

    @Test
    void writeAndReadCharBE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeCharBE('A');
                bf.seek(0);
                assertEquals('A', bf.readCharBE());
            }
        }
    }

    @Test
    void randomAccessCharLEAndBE() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(8);
                bf.writeCharLE(0L, 'Q');
                bf.writeCharBE(2L, 'R');
                assertEquals('Q', bf.readCharLE(0L));
                assertEquals('R', bf.readCharBE(2L));
            }
        }
    }

    // ── Random-access short/int/long LE that go through direct RAF ─────

    @Test
    void randomAccessShortLEOutsideBuffer() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, BufferSizeOption.of(16))) {
                bf.writeZeros(32);
                bf.flush();
                bf.writeShortLE(28L, 0x5678);
                assertEquals((short) 0x5678, bf.readShortLE(28L));
            }
        }
    }

    @Test
    void randomAccessIntLEOutsideBuffer() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, BufferSizeOption.of(16))) {
                bf.writeZeros(32);
                bf.flush();
                bf.writeIntLE(24L, 0xCAFEBABE);
                assertEquals(0xCAFEBABE, bf.readIntLE(24L));
            }
        }
    }

    @Test
    void randomAccessLongLEOutsideBuffer() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, BufferSizeOption.of(16))) {
                bf.writeZeros(32);
                bf.flush();
                bf.writeLongLE(24L, 0x0102030405060708L);
                assertEquals(0x0102030405060708L, bf.readLongLE(24L));
            }
        }
    }

    // ── Skip edge cases ────────────────────────────────────────────────

    @Test
    void skipZeroReturnsZero() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[10]);
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertEquals(0, bf.skip(0));
                assertEquals(0, bf.filePosition());
            }
        }
    }

    @Test
    void skipNegativeReturnsZero() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[10]);
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                bf.skip(5);
                assertEquals(0, bf.skip(-1));
                assertEquals(5, bf.filePosition());
            }
        }
    }

    // ── read/write byte arrays with offset/length ──────────────────────

    @Test
    void readByteArrayWithOffset() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2, 3, 4, 5 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                byte[] result = new byte[8];
                int n = bf.read(result, 2, 3);
                assertEquals(3, n);
                assertEquals(0, result[0]);
                assertEquals(0, result[1]);
                assertEquals(1, result[2]);
                assertEquals(2, result[3]);
                assertEquals(3, result[4]);
            }
        }
    }

    @Test
    void readByteArrayZeroLength() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertEquals(0, bf.read(new byte[5], 0, 0));
            }
        }
    }

    @Test
    void readFullyWithOffsetAndLength() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 10, 20, 30, 40 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                byte[] result = new byte[6];
                bf.readFully(result, 1, 4);
                assertEquals(0, result[0]);
                assertEquals(10, result[1]);
                assertEquals(20, result[2]);
                assertEquals(30, result[3]);
                assertEquals(40, result[4]);
                assertEquals(0, result[5]);
            }
        }
    }

    @Test
    void writeByteArrayWithOffset() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                byte[] src = { 0, 0, 10, 20, 30, 0 };
                bf.write(src, 2, 3);
                bf.seek(0);
                assertEquals(10, bf.readByte());
                assertEquals(20, bf.readByte());
                assertEquals(30, bf.readByte());
                assertEquals(3, bf.length());
            }
        }
    }

    @Test
    void readByteArrayAtEofReturnsMinusOne() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[0]);
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertEquals(-1, bf.read(new byte[5]));
            }
        }
    }

    @Test
    void readByteArrayPartialFromEof() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2, 3 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                byte[] buf = new byte[10];
                int n = bf.read(buf);
                assertEquals(3, n);
                assertEquals(1, buf[0]);
                assertEquals(2, buf[1]);
                assertEquals(3, buf[2]);
            }
        }
    }

    // ── String with explicit charset ───────────────────────────────────

    @Test
    void writeAndReadStringWithCharset() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeString("héllo", StandardCharsets.UTF_8);
                bf.seek(0);
                assertEquals("héllo", bf.readString(6, StandardCharsets.UTF_8));
            }
        }
    }

    // ── ReadOnlyBufferException ────────────────────────────────────────

    @Test
    void readIntoReadOnlyByteBufferThrows() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2, 3 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                ByteBuffer readOnly = ByteBuffer.allocate(3).asReadOnlyBuffer();
                assertThrows(ReadOnlyBufferException.class, () -> bf.read(readOnly));
            }
        }
    }

    // ── writeZeros spanning multiple buffers ───────────────────────────

    @Test
    void writeZerosLargerThanBuffer() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, BufferSizeOption.of(16))) {
                bf.writeZeros(50);
                assertEquals(50, bf.filePosition());
                assertEquals(50, bf.length());
            }
            // Verify on disk: all bytes should be zero
            byte[] data = Files.readAllBytes(f);
            assertEquals(50, data.length);
            for (int i = 0; i < 50; i++) {
                assertEquals(0, data[i], "Non-zero at position " + i);
            }
        }
    }

    // ── setLength invalidates buffer entirely ──────────────────────────

    @Test
    void setLengthInvalidatesEntireBuffer() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(100);
                bf.seek(50);
                bf.setLength(0);
                assertEquals(0, bf.length());
            }
        }
    }

    // ── InputStream view: available(), skipBytes(), reset ──────────────

    @Test
    void inputStreamAvailable() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[100]);
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                InputStream is = bf.inputStream();
                assertEquals(100, is.available());
                is.read();
                assertEquals(99, is.available());
            }
        }
    }

    @Test
    void inputStreamSkipBytes() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2, 3, 4, 5 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                BufferedFile.BufferedFileInputStream is = bf.inputStream();
                assertEquals(2, is.skipBytes(2));
                assertEquals(3, is.read());
            }
        }
    }

    @Test
    void inputStreamResetWithoutMarkThrows() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertThrows(IOException.class, () -> bf.inputStream().reset());
            }
        }
    }

    @Test
    void inputStreamGetChannelReturnsSameAsBufferedFile() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1 });
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                assertSame(bf.channel(), bf.inputStream().getChannel());
                assertSame(bf.channel(), bf.outputStream().getChannel());
            }
        }
    }

    // ── DataInput/DataOutput via stream views ──────────────────────────

    @Test
    void dataInputReadLong() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.outputStream().writeLong(0x0102030405060708L);
                bf.seek(0);
                assertEquals(0x0102030405060708L, bf.inputStream().readLong());
            }
        }
    }

    @Test
    void dataInputReadShort() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.outputStream().writeShort(0x1234);
                bf.seek(0);
                assertEquals((short) 0x1234, bf.inputStream().readShort());
            }
        }
    }

    @Test
    void dataInputReadUnsignedByte() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeByte(0xFF);
                bf.seek(0);
                assertEquals(0xFF, bf.inputStream().readUnsignedByte() & 0xFF);
            }
        }
    }

    @Test
    void dataInputReadUnsignedShort() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.outputStream().writeShort(0x8000);
                bf.seek(0);
                int val = bf.inputStream().readUnsignedShort();
                assertEquals((short) 0x8000, (short) val);
            }
        }
    }

    @Test
    void dataInputReadCharViaStream() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.outputStream().writeChar('W');
                bf.seek(0);
                assertEquals('W', bf.inputStream().readChar());
            }
        }
    }

    @Test
    void dataOutputWriteUTF() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.outputStream().writeUTF("world");
                bf.seek(0);
                assertEquals("world", bf.inputStream().readUTF());
            }
        }
    }

    @Test
    void dataInputReadFullyWithOffset() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.write(new byte[] { 1, 2, 3, 4, 5 });
                bf.seek(0);
                byte[] buf = new byte[7];
                bf.inputStream().readFully(buf, 1, 5);
                assertEquals(0, buf[0]);
                assertEquals(1, buf[1]);
                assertEquals(5, buf[5]);
                assertEquals(0, buf[6]);
            }
        }
    }

    @Test
    void dataInputReadLineReturnsNullAtEof() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[0]);
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertNull(bf.inputStream().readLine());
            }
        }
    }

    @Test
    void dataInputReadLineWithCarriageReturnOnly() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.write("abc\rdef".getBytes(StandardCharsets.US_ASCII));
                bf.seek(0);
                assertEquals("abc", bf.inputStream().readLine());
                assertEquals("def", bf.inputStream().readLine());
            }
        }
    }

    @Test
    void dataInputReadByteAtEofThrows() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[0]);
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertThrows(EOFException.class, () -> bf.inputStream().readByte());
            }
        }
    }

    // ── FileChannel view: scatter/gather, truncate, force, transferTo/From

    @Test
    void channelScatterRead() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.write(new byte[] { 1, 2, 3, 4, 5, 6 });
                bf.seek(0);
                ByteBuffer b1 = ByteBuffer.allocate(2);
                ByteBuffer b2 = ByteBuffer.allocate(3);
                long n = bf.channel().read(new ByteBuffer[] { b1, b2 }, 0, 2);
                assertEquals(5, n);
                b1.flip();
                assertEquals(1, b1.get());
                assertEquals(2, b1.get());
                b2.flip();
                assertEquals(3, b2.get());
                assertEquals(4, b2.get());
                assertEquals(5, b2.get());
            }
        }
    }

    @Test
    void channelGatherWrite() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                ByteBuffer b1 = ByteBuffer.wrap(new byte[] { 10, 20 });
                ByteBuffer b2 = ByteBuffer.wrap(new byte[] { 30, 40 });
                long n = bf.channel().write(new ByteBuffer[] { b1, b2 }, 0, 2);
                assertEquals(4, n);
                bf.seek(0);
                assertEquals(10, bf.readByte());
                assertEquals(20, bf.readByte());
                assertEquals(30, bf.readByte());
                assertEquals(40, bf.readByte());
            }
        }
    }

    @Test
    void channelTruncate() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(100);
                bf.flush();
                FileChannel ch = bf.channel();
                ch.truncate(50);
                assertEquals(50, ch.size());
            }
        }
    }

    @Test
    void channelForce() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeByte(42);
                bf.channel().force(true);
                assertEquals(1, Files.size(f));
            }
        }
    }

    @Test
    void channelTransferToWritableChannel() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.write(new byte[] { 1, 2, 3, 4, 5 });
                bf.flush();
                FileChannel ch = bf.channel();

                Pipe pipe = Pipe.open();
                try (Pipe.SinkChannel sink = pipe.sink()) {
                    try (Pipe.SourceChannel source = pipe.source()) {
                        long transferred = ch.transferTo(1, 3, sink);
                        assertEquals(3, transferred);
                        ByteBuffer dst = ByteBuffer.allocate(3);
                        source.read(dst);
                        dst.flip();
                        assertEquals(2, dst.get());
                        assertEquals(3, dst.get());
                        assertEquals(4, dst.get());
                    }
                }
            }
        }
    }

    @Test
    void channelTransferFromReadableChannel() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(10);
                bf.flush();
                FileChannel ch = bf.channel();

                Pipe pipe = Pipe.open();
                ByteBuffer src = ByteBuffer.wrap(new byte[] { 10, 20, 30 });
                try (Pipe.SinkChannel sink = pipe.sink()) {
                    try (Pipe.SourceChannel source = pipe.source()) {
                        sink.write(src);
                        long transferred = ch.transferFrom(source, 2, 3);
                        assertEquals(3, transferred);
                        assertEquals(10, bf.readByte(2L));
                        assertEquals(20, bf.readByte(3L));
                        assertEquals(30, bf.readByte(4L));
                    }
                }
            }
        }
    }

    @Test
    void channelReadAtPosition() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.write(new byte[] { 1, 2, 3, 4, 5 });
                ByteBuffer dst = ByteBuffer.allocate(2);
                int n = bf.channel().read(dst, 2);
                assertEquals(2, n);
                dst.flip();
                assertEquals(3, dst.get());
                assertEquals(4, dst.get());
            }
        }
    }

    @Test
    void channelWriteAtPosition() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(10);
                ByteBuffer src = ByteBuffer.wrap(new byte[] { 99, 88 });
                int n = bf.channel().write(src, 3);
                assertEquals(2, n);
                assertEquals(99, bf.readByte(3L));
                assertEquals(88, bf.readByte(4L));
            }
        }
    }

    // ── BufferSizeOption: small values, hashCode, equality ─────────────

    @Test
    void bufferSizeOptionSmallValueRoundedUp() {
        BufferSizeOption opt = BufferSizeOption.of(3);
        assertEquals(BufferSizeOption.MIN_BUFFER_SIZE, opt.bufferSize());
    }

    @Test
    void bufferSizeOptionHashCode() {
        assertEquals(BufferSizeOption.of(64).hashCode(), BufferSizeOption.of(64).hashCode());
        assertNotEquals(BufferSizeOption.of(32).hashCode(), BufferSizeOption.of(64).hashCode());
    }

    @Test
    void bufferSizeOptionNotEqualToNull() {
        assertNotEquals(null, BufferSizeOption.of(64));
    }

    @Test
    void bufferSizeOptionNotEqualToOtherType() {
        assertNotEquals("not a BufferSizeOption", BufferSizeOption.of(64));
    }

    // ── ByteOrderOption toString ───────────────────────────────────────

    @Test
    void byteOrderOptionToString() {
        assertEquals("BIG_ENDIAN", ByteOrderOption.BIG_ENDIAN.toString());
        assertEquals("LITTLE_ENDIAN", ByteOrderOption.LITTLE_ENDIAN.toString());
    }

    // ── openBuffered with unsupported options ──────────────────────────

    @Test
    void unsupportedStandardOptionThrows() {
        Path f = testFile();
        assertThrows(UnsupportedOperationException.class,
                () -> Files2.openBuffered(f, WRITE, CREATE, DELETE_ON_CLOSE));
    }

    @Test
    void unsupportedCustomOptionThrows() {
        Path f = testFile();
        assertThrows(UnsupportedOperationException.class,
                () -> Files2.openBuffered(f, READ, new java.nio.file.OpenOption() {
                }));
    }

    @Test
    void openWithNoOptionsDefaultsToRead() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 42 });
            try (BufferedFile bf = Files2.openBuffered(f, List.of())) {
                assertEquals(42, bf.readByte());
                assertThrows(IOException.class, () -> bf.writeByte(1));
            }
        }
    }

    // ── Random-access byte array read from buffer ──────────────────────

    @Test
    void randomAccessReadByteArrayFromBuffer() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
                byte[] dst = new byte[3];
                int n = bf.read(2L, dst);
                assertEquals(3, n);
                assertArrayEquals(new byte[] { 3, 4, 5 }, dst);
                assertEquals(8, bf.filePosition());
            }
        }
    }

    @Test
    void randomAccessReadByteArrayZeroLength() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                assertEquals(0, bf.read(0L, new byte[0], 0, 0));
            }
        }
    }

    @Test
    void randomAccessWriteByteArrayZeroLength() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(4);
                bf.write(0L, new byte[] { 99 }, 0, 0);
                assertEquals(0, bf.readByte(0L));
            }
        }
    }

    // ── Random-access ByteBuffer ───────────────────────────────────────

    @Test
    void randomAccessDirectByteBuffer() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(20);
                ByteBuffer src = ByteBuffer.allocateDirect(3);
                src.put(new byte[] { 11, 22, 33 });
                src.flip();
                int written = bf.write(5L, src);
                assertEquals(3, written);

                ByteBuffer dst = ByteBuffer.allocateDirect(3);
                int read = bf.read(5L, dst);
                assertEquals(3, read);
                dst.flip();
                assertEquals(11, dst.get());
                assertEquals(22, dst.get());
                assertEquals(33, dst.get());
            }
        }
    }

    @Test
    void randomAccessByteBufferZeroRemaining() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.writeZeros(4);
                ByteBuffer empty = ByteBuffer.allocate(0);
                assertEquals(0, bf.read(0L, empty));
                assertEquals(0, bf.write(0L, empty));
            }
        }
    }

    @Test
    void writeByteBufferZeroRemaining() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                ByteBuffer empty = ByteBuffer.allocate(0);
                bf.write(empty);
                assertEquals(0, bf.length());
            }
        }
    }

    // ── Random-access byte array outside buffer ────────────────────────

    @Test
    void randomAccessByteArrayOutsideBuffer() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE, BufferSizeOption.of(16))) {
                bf.writeZeros(64);
                bf.flush();
                byte[] src = { 0x11, 0x22, 0x33 };
                bf.write(48L, src, 0, 3);
                byte[] dst = new byte[3];
                bf.read(48L, dst, 0, 3);
                assertArrayEquals(src, dst);
            }
        }
    }

    // ── Sequential read spanning buffer and RAF ────────────────────────

    @Test
    void readArraySplitBetweenBufferAndRaf() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            byte[] data = new byte[20];
            for (int i = 0; i < 20; i++) {
                data[i] = (byte) (i + 1);
            }
            Files.write(f, data);
            try (BufferedFile bf = Files2.openBuffered(f, READ, BufferSizeOption.of(8))) {
                bf.fill(8);
                byte[] result = new byte[15];
                int n = bf.read(result);
                assertEquals(15, n);
                for (int i = 0; i < 15; i++) {
                    assertEquals(i + 1, result[i], "Mismatch at index " + i);
                }
            }
        }
    }

    // ── Transfer edge cases ────────────────────────────────────────────

    @Test
    void transferToFromMidFile() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2, 3, 4, 5 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                bf.skip(2);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                long n = bf.transferTo(baos);
                assertEquals(3, n);
                assertArrayEquals(new byte[] { 3, 4, 5 }, baos.toByteArray());
            }
        }
    }

    @Test
    void transferToWithLengthZero() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            Files.write(f, new byte[] { 1, 2, 3 });
            try (BufferedFile bf = Files2.openBuffered(f, READ)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                long n = bf.transferTo(baos, 0);
                assertEquals(0, n);
                assertEquals(0, baos.size());
            }
        }
    }

    @Test
    void transferFromEmptyStream() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
                long n = bf.transferFrom(bais);
                assertEquals(0, n);
                assertEquals(0, bf.filePosition());
            }
        }
    }

    // ── Flush on non-dirty file ────────────────────────────────────────

    @Test
    void flushOnCleanFileIsNoOp() throws IOException {
        Path f = testFile();
        try (Closeable ignored = tempFile(f)) {
            try (BufferedFile bf = Files2.openBuffered(f, READ, WRITE, CREATE)) {
                bf.flush();
            }
        }
    }

    // test utilities

    Closeable tempFile(Path path) {
        return () -> Files.deleteIfExists(path);
    }
}
