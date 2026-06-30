package io.smallrye.common.io.archive;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.common.io.BufferedFile;
import io.smallrye.common.io.FileAttributes;
import io.smallrye.common.io.Files2;

/**
 * Tests for {@link ArchiveBuilder}.
 */
public class ArchiveBuilderTests {

    @TempDir
    Path tempDir;

    /**
     * Test that an empty archive can be created and read by both {@link Archive} and {@link ZipFile}.
     */
    @Test
    public void testEmptyArchive() throws Exception {
        Path file = tempDir.resolve("empty.zip");
        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            // no entries
        }
        assertTrue(Files.exists(file));
        assertTrue(Files.size(file) > 0);

        // verify with JDK ZipFile
        try (ZipFile zf = new ZipFile(file.toFile())) {
            assertEquals(0, zf.size());
        }

        // verify with Archive
        Archive archive = Archive.open(Files.readAllBytes(file));
        assertEquals(0, archive.entryCount());
    }

    /**
     * Test adding a STORED entry and reading it back.
     */
    @Test
    public void testStoredEntry() throws Exception {
        Path file = tempDir.resolve("stored.zip");
        byte[] content = "Hello, World!".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (OutputStream os = builder.addEntry("hello.txt", ZipOption.STORED)) {
                os.write(content);
            }
        }

        // verify with JDK ZipFile
        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("hello.txt");
            assertNotNull(entry);
            assertEquals(ZipEntry.STORED, entry.getMethod());
            assertEquals(content.length, entry.getSize());
            assertEquals(content.length, entry.getCompressedSize());
            try (InputStream is = zf.getInputStream(entry)) {
                assertArrayEquals(content, is.readAllBytes());
            }
        }

        // verify with Archive
        Archive archive = Archive.open(Files.readAllBytes(file));
        assertEquals(1, archive.entryCount());
        long idx = archive.findEntry("hello.txt");
        assertTrue(idx >= 0);
        assertTrue(archive.isStored(idx));
        try (InputStream is = archive.openEntry(idx)) {
            assertArrayEquals(content, is.readAllBytes());
        }
    }

    /**
     * Test adding a DEFLATED entry and reading it back.
     */
    @Test
    public void testDeflatedEntry() throws Exception {
        Path file = tempDir.resolve("deflated.zip");
        byte[] content = "This is some text content that should be compressed.".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (OutputStream os = builder.addEntry("data.txt", ZipOption.DEFLATED)) {
                os.write(content);
            }
        }

        // verify with JDK ZipFile
        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("data.txt");
            assertNotNull(entry);
            assertEquals(ZipEntry.DEFLATED, entry.getMethod());
            assertEquals(content.length, entry.getSize());
            try (InputStream is = zf.getInputStream(entry)) {
                assertArrayEquals(content, is.readAllBytes());
            }
        }

        // verify with Archive
        Archive archive = Archive.open(Files.readAllBytes(file));
        long idx = archive.findEntry("data.txt");
        assertTrue(idx >= 0);
        assertFalse(archive.isStored(idx));
        try (InputStream is = archive.openEntry(idx)) {
            assertArrayEquals(content, is.readAllBytes());
        }
    }

    /**
     * Test that the default compression method is DEFLATED.
     */
    @Test
    public void testDefaultCompressionIsDeflated() throws Exception {
        Path file = tempDir.resolve("default-method.zip");
        byte[] content = "test".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (OutputStream os = builder.addEntry("test.txt")) {
                os.write(content);
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("test.txt");
            assertNotNull(entry);
            assertEquals(ZipEntry.DEFLATED, entry.getMethod());
        }
    }

    /**
     * Test adding a directory entry.
     */
    @Test
    public void testDirectoryEntry() throws Exception {
        Path file = tempDir.resolve("dir.zip");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addDirectory("META-INF");
            builder.addDirectory("META-INF/services/");
        }

        // verify with JDK ZipFile
        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry metaInf = zf.getEntry("META-INF/");
            assertNotNull(metaInf, "Directory with auto-appended / should exist");
            assertTrue(metaInf.isDirectory());

            ZipEntry services = zf.getEntry("META-INF/services/");
            assertNotNull(services, "Directory with explicit / should exist");
            assertTrue(services.isDirectory());
        }

        // verify with Archive
        Archive archive = Archive.open(Files.readAllBytes(file));
        assertEquals(2, archive.entryCount());
        long idx = archive.findEntry("META-INF/");
        assertTrue(idx >= 0);
        assertTrue(archive.isDirectory(idx));
    }

    /**
     * Test adding multiple entries and reading them all back.
     */
    @Test
    public void testMultipleEntries() throws Exception {
        Path file = tempDir.resolve("multi.zip");
        byte[] content1 = "file one".getBytes(StandardCharsets.UTF_8);
        byte[] content2 = "file two with more content for deflation testing: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                .getBytes(StandardCharsets.UTF_8);
        byte[] content3 = "three".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addDirectory("dir/");
            try (OutputStream os = builder.addEntry("dir/file1.txt", ZipOption.STORED)) {
                os.write(content1);
            }
            try (OutputStream os = builder.addEntry("dir/file2.txt", ZipOption.DEFLATED)) {
                os.write(content2);
            }
            try (OutputStream os = builder.addEntry("file3.txt", ZipOption.STORED)) {
                os.write(content3);
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            assertEquals(4, zf.size());
            try (InputStream is = zf.getInputStream(zf.getEntry("dir/file1.txt"))) {
                assertArrayEquals(content1, is.readAllBytes());
            }
            try (InputStream is = zf.getInputStream(zf.getEntry("dir/file2.txt"))) {
                assertArrayEquals(content2, is.readAllBytes());
            }
            try (InputStream is = zf.getInputStream(zf.getEntry("file3.txt"))) {
                assertArrayEquals(content3, is.readAllBytes());
            }
        }
    }

    /**
     * Test that UTF-8 file names are correctly stored and retrieved.
     */
    @Test
    public void testUtf8FileNames() throws Exception {
        Path file = tempDir.resolve("utf8.zip");
        String name = "données/résumé.txt";
        byte[] content = "unicode content".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (OutputStream os = builder.addEntry(name, ZipOption.STORED)) {
                os.write(content);
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry(name);
            assertNotNull(entry, "UTF-8 entry name should be found");
            try (InputStream is = zf.getInputStream(entry)) {
                assertArrayEquals(content, is.readAllBytes());
            }
        }

        Archive archive = Archive.open(Files.readAllBytes(file));
        long idx = archive.findEntry(name);
        assertTrue(idx >= 0, "Archive should find UTF-8 named entry");
        assertEquals(name, archive.entryName(idx));
    }

    /**
     * Test that closing the builder with an open entry stream throws.
     */
    @Test
    public void testCloseWithOpenStreamThrows() throws Exception {
        Path file = tempDir.resolve("open-stream.zip");
        ArchiveBuilder builder = ArchiveBuilder.open(file);
        OutputStream os = builder.addEntry("test.txt", ZipOption.STORED);
        os.write(42);

        assertThrows(IllegalStateException.class, builder::close);
        // clean up
        os.close();
        builder.close();
    }

    /**
     * Test that adding an entry while another stream is open throws.
     */
    @Test
    public void testAddEntryWhileStreamOpenThrows() throws Exception {
        Path file = tempDir.resolve("concurrent-entry.zip");
        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            OutputStream os = builder.addEntry("first.txt", ZipOption.STORED);
            assertThrows(IllegalStateException.class,
                    () -> builder.addEntry("second.txt", ZipOption.STORED));
            assertThrows(IllegalStateException.class,
                    () -> builder.addDirectory("dir/"));
            os.close();
        }
    }

    /**
     * Test that timestamps round-trip through NTFS extra fields with high precision.
     */
    @Test
    public void testTimestampRoundTrip() throws Exception {
        Path file = tempDir.resolve("timestamps.zip");
        Instant mtime = Instant.parse("2024-06-15T10:30:44.123456700Z");
        Instant ctime = Instant.parse("2024-01-01T00:00:00Z");

        FileAttribute<?> modAttr = FileAttributes.lastModifiedTime(mtime);
        FileAttribute<?> createAttr = FileAttributes.creationTime(ctime);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (OutputStream os = builder.addEntry("test.txt", Set.of(ZipOption.STORED), modAttr, createAttr)) {
                os.write("content".getBytes(StandardCharsets.UTF_8));
            }
        }

        // verify via Archive
        Archive archive = Archive.open(Files.readAllBytes(file));
        long idx = archive.findEntry("test.txt");
        assertTrue(idx >= 0);

        Instant readMtime = archive.modifiedTime(idx);
        Instant readCtime = archive.creationTime(idx);

        // NTFS has 100ns precision, so allow 200ns tolerance
        assertTrue(Duration.between(mtime, readMtime).abs().toNanos() <= 200,
                "Modified time should round-trip within 200ns: expected " + mtime + ", got " + readMtime);
        assertTrue(Duration.between(ctime, readCtime).abs().toNanos() <= 200,
                "Creation time should round-trip within 200ns: expected " + ctime + ", got " + readCtime);
    }

    /**
     * Test that default timestamps are populated when no time attributes are given.
     */
    @Test
    public void testDefaultTimestamps() throws Exception {
        Path file = tempDir.resolve("default-time.zip");
        Instant before = Instant.now();

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (OutputStream os = builder.addEntry("test.txt", ZipOption.STORED)) {
                os.write("data".getBytes(StandardCharsets.UTF_8));
            }
        }

        Instant after = Instant.now();

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("test.txt");
            assertNotNull(entry);
            // DOS time has 2-second resolution; the time should be close to "now"
            long entryTime = entry.getTime();
            assertTrue(entryTime >= before.toEpochMilli() - 2000,
                    "Entry time should be >= before - 2s");
            assertTrue(entryTime <= after.toEpochMilli() + 2000,
                    "Entry time should be <= after + 2s");
        }
    }

    /**
     * Test that {@link ZipOption#ZIP64} produces valid ZIP64 archives.
     */
    @Test
    public void testZip64Option() throws Exception {
        Path file = tempDir.resolve("zip64.zip");
        byte[] content = "zip64 content".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file, Set.of(ZipOption.ZIP64))) {
            try (OutputStream os = builder.addEntry("test.txt", ZipOption.STORED)) {
                os.write(content);
            }
            builder.addDirectory("dir/");
        }

        // verify readable by JDK
        try (ZipFile zf = new ZipFile(file.toFile())) {
            assertEquals(2, zf.size());
            ZipEntry entry = zf.getEntry("test.txt");
            assertNotNull(entry);
            try (InputStream is = zf.getInputStream(entry)) {
                assertArrayEquals(content, is.readAllBytes());
            }
        }

        // verify readable by Archive
        Archive archive = Archive.open(Files.readAllBytes(file));
        assertEquals(2, archive.entryCount());
        long idx = archive.findEntry("test.txt");
        assertTrue(idx >= 0);
        try (InputStream is = archive.openEntry(idx)) {
            assertArrayEquals(content, is.readAllBytes());
        }
    }

    /**
     * Test writing an empty STORED entry.
     */
    @Test
    public void testEmptyStoredEntry() throws Exception {
        Path file = tempDir.resolve("empty-entry.zip");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (OutputStream os = builder.addEntry("empty.txt", ZipOption.STORED)) {
                // write nothing
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("empty.txt");
            assertNotNull(entry);
            assertEquals(0, entry.getSize());
            try (InputStream is = zf.getInputStream(entry)) {
                assertEquals(0, is.readAllBytes().length);
            }
        }
    }

    /**
     * Test writing an empty DEFLATED entry.
     */
    @Test
    public void testEmptyDeflatedEntry() throws Exception {
        Path file = tempDir.resolve("empty-deflated.zip");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (OutputStream os = builder.addEntry("empty.txt", ZipOption.DEFLATED)) {
                // write nothing
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("empty.txt");
            assertNotNull(entry);
            assertEquals(0, entry.getSize());
            try (InputStream is = zf.getInputStream(entry)) {
                assertEquals(0, is.readAllBytes().length);
            }
        }
    }

    /**
     * Test writing a large DEFLATED entry in multiple chunks.
     */
    @Test
    public void testLargeDeflatedEntry() throws Exception {
        Path file = tempDir.resolve("large.zip");
        // ~100KB of compressible data
        byte[] chunk = "abcdefghijklmnopqrstuvwxyz0123456789\n".getBytes(StandardCharsets.UTF_8);
        int totalSize = chunk.length * 3000;

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (OutputStream os = builder.addEntry("large.txt", ZipOption.DEFLATED)) {
                for (int i = 0; i < 3000; i++) {
                    os.write(chunk);
                }
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("large.txt");
            assertNotNull(entry);
            assertEquals(totalSize, entry.getSize());
            assertTrue(entry.getCompressedSize() < totalSize, "Compressed size should be smaller");
            try (InputStream is = zf.getInputStream(entry)) {
                byte[] read = is.readAllBytes();
                assertEquals(totalSize, read.length);
                // verify first and last chunks
                for (int i = 0; i < chunk.length; i++) {
                    assertEquals(chunk[i], read[i], "First chunk mismatch at byte " + i);
                    assertEquals(chunk[i], read[totalSize - chunk.length + i], "Last chunk mismatch at byte " + i);
                }
            }
        }
    }

    /**
     * Test that closing an already-closed builder is a no-op.
     */
    @Test
    public void testDoubleClose() throws Exception {
        Path file = tempDir.resolve("double-close.zip");
        ArchiveBuilder builder = ArchiveBuilder.open(file);
        builder.close();
        assertDoesNotThrow(builder::close);
    }

    // ── Archive.of(Path) tests ───────────────────────────────────────────

    /**
     * Test that {@link Archive#of(Path)} can read an archive with STORED and DEFLATED entries.
     */
    @Test
    public void testArchiveOfPathRoundTrip() throws Exception {
        Path file = tempDir.resolve("of-path.zip");
        byte[] storedContent = "stored data".getBytes(StandardCharsets.UTF_8);
        byte[] deflatedContent = "deflated data that is a bit longer for compression to matter"
                .getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addDirectory("dir/");
            try (OutputStream os = builder.addEntry("dir/stored.txt", ZipOption.STORED)) {
                os.write(storedContent);
            }
            try (OutputStream os = builder.addEntry("dir/deflated.txt", ZipOption.DEFLATED)) {
                os.write(deflatedContent);
            }
        }

        Archive archive = Archive.of(file);
        assertEquals(3, archive.entryCount());

        long dirIdx = archive.findEntry("dir/");
        assertTrue(dirIdx >= 0);
        assertTrue(archive.isDirectory(dirIdx));

        long storedIdx = archive.findEntry("dir/stored.txt");
        assertTrue(storedIdx >= 0);
        assertTrue(archive.isStored(storedIdx));
        try (InputStream is = archive.openEntry(storedIdx)) {
            assertArrayEquals(storedContent, is.readAllBytes());
        }

        long deflatedIdx = archive.findEntry("dir/deflated.txt");
        assertTrue(deflatedIdx >= 0);
        assertFalse(archive.isStored(deflatedIdx));
        try (InputStream is = archive.openEntry(deflatedIdx)) {
            assertArrayEquals(deflatedContent, is.readAllBytes());
        }
    }

    /**
     * Test that {@link Archive#of(Path)} can read a ZIP64 archive.
     */
    @Test
    public void testArchiveOfPathZip64() throws Exception {
        Path file = tempDir.resolve("of-path-zip64.zip");
        byte[] content = "zip64 via path".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file, Set.of(ZipOption.ZIP64))) {
            try (OutputStream os = builder.addEntry("test.txt", ZipOption.STORED)) {
                os.write(content);
            }
        }

        Archive archive = Archive.of(file);
        assertEquals(1, archive.entryCount());
        long idx = archive.findEntry("test.txt");
        assertTrue(idx >= 0);
        try (InputStream is = archive.openEntry(idx)) {
            assertArrayEquals(content, is.readAllBytes());
        }
    }

    /**
     * Test that {@link Archive#of(Path)} can read an empty archive.
     */
    @Test
    public void testArchiveOfPathEmpty() throws Exception {
        Path file = tempDir.resolve("of-path-empty.zip");
        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            // no entries
        }

        Archive archive = Archive.of(file);
        assertEquals(0, archive.entryCount());
    }

    /**
     * Test that {@link Archive#of(Path)} correctly reads timestamps written by {@link ArchiveBuilder}.
     */
    @Test
    public void testArchiveOfPathTimestamps() throws Exception {
        Path file = tempDir.resolve("of-path-timestamps.zip");
        Instant mtime = Instant.parse("2025-03-15T14:30:00.123456700Z");
        Instant ctime = Instant.parse("2025-01-01T00:00:00Z");

        FileAttribute<?> modAttr = FileAttributes.lastModifiedTime(mtime);
        FileAttribute<?> createAttr = FileAttributes.creationTime(ctime);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (OutputStream os = builder.addEntry("test.txt", Set.of(ZipOption.STORED), modAttr, createAttr)) {
                os.write("timestamped".getBytes(StandardCharsets.UTF_8));
            }
        }

        Archive archive = Archive.of(file);
        long idx = archive.findEntry("test.txt");
        assertTrue(idx >= 0);

        Instant readMtime = archive.modifiedTime(idx);
        Instant readCtime = archive.creationTime(idx);

        assertTrue(Duration.between(mtime, readMtime).abs().toNanos() <= 200,
                "Modified time should round-trip within 200ns via of(Path)");
        assertTrue(Duration.between(ctime, readCtime).abs().toNanos() <= 200,
                "Creation time should round-trip within 200ns via of(Path)");
    }

    /**
     * Test that {@link Archive#of(Path)} throws on a non-existent file.
     */
    @Test
    public void testArchiveOfPathNonExistent() {
        Path file = tempDir.resolve("does-not-exist.zip");
        assertThrows(IOException.class, () -> Archive.of(file));
    }

    /**
     * Test that {@link Archive#of(Path)} throws on an invalid (non-ZIP) file.
     */
    @Test
    public void testArchiveOfPathInvalidFile() throws Exception {
        Path file = tempDir.resolve("not-a-zip.bin");
        Files.write(file, "this is not a zip file".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> Archive.of(file));
    }

    // ── Buffered entry tests ─────────────────────────────────────────────

    /**
     * Test that {@link ArchiveBuilder#addBufferedEntry(String, java.nio.file.attribute.FileAttribute[])}
     * creates a valid STORED entry readable by the JDK {@link ZipFile}.
     */
    @Test
    public void testBufferedEntryReadableByZipFile() throws Exception {
        Path file = tempDir.resolve("buffered.zip");
        byte[] content = "Buffered entry content".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (BufferedFile be = builder.addBufferedEntry("buffered.txt")) {
                be.write(content);
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("buffered.txt");
            assertNotNull(entry);
            assertEquals(ZipEntry.STORED, entry.getMethod());
            assertEquals(content.length, entry.getSize());
            assertEquals(content.length, entry.getCompressedSize());
            try (InputStream is = zf.getInputStream(entry)) {
                assertArrayEquals(content, is.readAllBytes());
            }
        }
    }

    /**
     * Test that a buffered entry supports random-access writing and produces correct CRC/sizes.
     */
    @Test
    public void testBufferedEntryRandomAccessWrite() throws Exception {
        Path file = tempDir.resolve("buffered-random.zip");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (BufferedFile be = builder.addBufferedEntry("random.bin")) {
                // write some data sequentially, then overwrite part with random access
                be.writeIntLE(0xAAAAAAAA);
                be.writeIntLE(0xBBBBBBBB);
                be.writeIntLE(0xCCCCCCCC);
                // overwrite the middle int
                be.writeIntLE(4, 0xDDDDDDDD);
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("random.bin");
            assertNotNull(entry);
            assertEquals(12, entry.getSize());
            try (InputStream is = zf.getInputStream(entry)) {
                byte[] data = is.readAllBytes();
                assertEquals(12, data.length);
                // verify the random-access overwrite took effect
                assertEquals((byte) 0xAA, data[0]);
                assertEquals((byte) 0xDD, data[4]);
                assertEquals((byte) 0xCC, data[8]);
            }
        }
    }

    /**
     * Test that a buffered entry can coexist with regular stream entries in the same archive.
     */
    @Test
    public void testBufferedEntryMixedWithStreamEntries() throws Exception {
        Path file = tempDir.resolve("mixed.zip");
        byte[] streamContent = "stream data".getBytes(StandardCharsets.UTF_8);
        byte[] bufferedContent = "buffered data".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (OutputStream os = builder.addEntry("stream.txt", ZipOption.STORED)) {
                os.write(streamContent);
            }
            try (BufferedFile be = builder.addBufferedEntry("buffered.txt")) {
                be.write(bufferedContent);
            }
            try (OutputStream os = builder.addEntry("stream2.txt", ZipOption.DEFLATED)) {
                os.write(streamContent);
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            assertEquals(3, zf.size());
            try (InputStream is = zf.getInputStream(zf.getEntry("stream.txt"))) {
                assertArrayEquals(streamContent, is.readAllBytes());
            }
            try (InputStream is = zf.getInputStream(zf.getEntry("buffered.txt"))) {
                assertArrayEquals(bufferedContent, is.readAllBytes());
            }
            try (InputStream is = zf.getInputStream(zf.getEntry("stream2.txt"))) {
                assertArrayEquals(streamContent, is.readAllBytes());
            }
        }
    }

    /**
     * Test that specifying DEFLATED for a buffered entry throws.
     */
    @Test
    public void testBufferedEntryRejectsDeflated() throws Exception {
        Path file = tempDir.resolve("buffered-deflated.zip");
        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            assertThrows(IllegalArgumentException.class,
                    () -> builder.addBufferedEntry("test.txt", ZipOption.DEFLATED));
        }
    }

    /**
     * Test that an empty buffered entry produces a valid archive.
     */
    @Test
    public void testEmptyBufferedEntry() throws Exception {
        Path file = tempDir.resolve("empty-buffered.zip");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (BufferedFile be = builder.addBufferedEntry("empty.txt")) {
                // write nothing
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("empty.txt");
            assertNotNull(entry);
            assertEquals(0, entry.getSize());
            try (InputStream is = zf.getInputStream(entry)) {
                assertEquals(0, is.readAllBytes().length);
            }
        }
    }

    /**
     * Test that a buffered entry with ZIP64 option works correctly.
     */
    @Test
    public void testBufferedEntryZip64() throws Exception {
        Path file = tempDir.resolve("buffered-zip64.zip");
        byte[] content = "zip64 buffered".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (BufferedFile be = builder.addBufferedEntry("test.txt", ZipOption.ZIP64)) {
                be.write(content);
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("test.txt");
            assertNotNull(entry);
            assertEquals(content.length, entry.getSize());
            try (InputStream is = zf.getInputStream(entry)) {
                assertArrayEquals(content, is.readAllBytes());
            }
        }
    }

    // ── addArchive() tests ───────────────────────────────────────────────

    /**
     * Test that {@link ArchiveBuilder#addArchive(String)} creates a nested archive
     * readable by the JDK {@link ZipFile} and by {@link Archive}.
     */
    @Test
    public void testAddArchiveReadable() throws Exception {
        Path file = tempDir.resolve("nested.zip");
        byte[] innerContent = "inner content".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder outer = ArchiveBuilder.open(file)) {
            try (ArchiveBuilder inner = outer.addArchive("lib/nested.jar")) {
                try (OutputStream os = inner.addEntry("hello.txt", ZipOption.STORED)) {
                    os.write(innerContent);
                }
            }
        }

        // verify outer archive is readable by JDK
        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry outerEntry = zf.getEntry("lib/nested.jar");
            assertNotNull(outerEntry);
            assertEquals(ZipEntry.STORED, outerEntry.getMethod());
            assertTrue(outerEntry.getSize() > 0);

            // read the nested JAR bytes and verify the inner archive
            byte[] nestedBytes;
            try (InputStream is = zf.getInputStream(outerEntry)) {
                nestedBytes = is.readAllBytes();
            }
            Archive innerArchive = Archive.open(nestedBytes);
            assertEquals(1, innerArchive.entryCount());
            long idx = innerArchive.findEntry("hello.txt");
            assertTrue(idx >= 0);
            try (InputStream is = innerArchive.openEntry(idx)) {
                assertArrayEquals(innerContent, is.readAllBytes());
            }
        }

        // verify outer archive is readable by Archive
        Archive outerArchive = Archive.open(Files.readAllBytes(file));
        assertEquals(1, outerArchive.entryCount());
        long outerIdx = outerArchive.findEntry("lib/nested.jar");
        assertTrue(outerIdx >= 0);
        assertTrue(outerArchive.isStored(outerIdx));
    }

    /**
     * Test that multiple nested archives can coexist in the same outer archive.
     */
    @Test
    public void testMultipleNestedArchives() throws Exception {
        Path file = tempDir.resolve("multi-nested.zip");
        byte[] content1 = "one".getBytes(StandardCharsets.UTF_8);
        byte[] content2 = "two".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder outer = ArchiveBuilder.open(file)) {
            try (ArchiveBuilder inner1 = outer.addArchive("a.jar")) {
                try (OutputStream os = inner1.addEntry("a.txt", ZipOption.STORED)) {
                    os.write(content1);
                }
            }
            try (ArchiveBuilder inner2 = outer.addArchive("b.jar")) {
                try (OutputStream os = inner2.addEntry("b.txt", ZipOption.STORED)) {
                    os.write(content2);
                }
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            assertEquals(2, zf.size());

            byte[] nested1;
            try (InputStream is = zf.getInputStream(zf.getEntry("a.jar"))) {
                nested1 = is.readAllBytes();
            }
            Archive a = Archive.open(nested1);
            long aIdx = a.findEntry("a.txt");
            assertTrue(aIdx >= 0);
            try (InputStream is = a.openEntry(aIdx)) {
                assertArrayEquals(content1, is.readAllBytes());
            }

            byte[] nested2;
            try (InputStream is = zf.getInputStream(zf.getEntry("b.jar"))) {
                nested2 = is.readAllBytes();
            }
            Archive b = Archive.open(nested2);
            long bIdx = b.findEntry("b.txt");
            assertTrue(bIdx >= 0);
            try (InputStream is = b.openEntry(bIdx)) {
                assertArrayEquals(content2, is.readAllBytes());
            }
        }
    }

    /**
     * Test that a nested archive can contain multiple entries including directories.
     */
    @Test
    public void testNestedArchiveMultipleEntries() throws Exception {
        Path file = tempDir.resolve("nested-multi.zip");
        byte[] classData = "fake class bytes".getBytes(StandardCharsets.UTF_8);
        byte[] manifest = "Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder outer = ArchiveBuilder.open(file)) {
            try (ArchiveBuilder inner = outer.addArchive("lib.jar")) {
                inner.addDirectory("META-INF/");
                try (OutputStream os = inner.addEntry("META-INF/MANIFEST.MF", ZipOption.STORED)) {
                    os.write(manifest);
                }
                try (OutputStream os = inner.addEntry("com/example/Main.class")) {
                    os.write(classData);
                }
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            byte[] nestedBytes;
            try (InputStream is = zf.getInputStream(zf.getEntry("lib.jar"))) {
                nestedBytes = is.readAllBytes();
            }
            Archive inner = Archive.open(nestedBytes);
            assertEquals(3, inner.entryCount());
            assertTrue(inner.findEntry("META-INF/") >= 0);
            assertTrue(inner.findEntry("META-INF/MANIFEST.MF") >= 0);
            assertTrue(inner.findEntry("com/example/Main.class") >= 0);
        }
    }

    /**
     * Test nested archive with mixed regular and nested entries in the outer archive.
     */
    @Test
    public void testNestedArchiveMixedWithRegularEntries() throws Exception {
        Path file = tempDir.resolve("mixed-nested.zip");
        byte[] regularContent = "regular".getBytes(StandardCharsets.UTF_8);
        byte[] innerContent = "inner".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder outer = ArchiveBuilder.open(file)) {
            try (OutputStream os = outer.addEntry("readme.txt", ZipOption.STORED)) {
                os.write(regularContent);
            }
            try (ArchiveBuilder inner = outer.addArchive("lib/nested.jar")) {
                try (OutputStream os = inner.addEntry("data.txt", ZipOption.STORED)) {
                    os.write(innerContent);
                }
            }
            outer.addDirectory("empty/");
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            assertEquals(3, zf.size());
            try (InputStream is = zf.getInputStream(zf.getEntry("readme.txt"))) {
                assertArrayEquals(regularContent, is.readAllBytes());
            }
            assertNotNull(zf.getEntry("lib/nested.jar"));
            assertNotNull(zf.getEntry("empty/"));
        }
    }

    // ── open(BufferedFile) tests ──────────────────────────────────────────

    /**
     * Test that {@link ArchiveBuilder#open(BufferedFile, java.nio.file.OpenOption...)} creates
     * a valid archive and does not close the underlying file.
     */
    @Test
    public void testOpenBufferedFile() throws Exception {
        Path file = tempDir.resolve("bf-open.zip");
        byte[] content = "via BufferedFile".getBytes(StandardCharsets.UTF_8);

        try (BufferedFile bf = Files2.openBuffered(file,
                StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {
            try (ArchiveBuilder builder = ArchiveBuilder.open(bf)) {
                try (OutputStream os = builder.addEntry("test.txt", ZipOption.STORED)) {
                    os.write(content);
                }
            }
            // builder is closed but BufferedFile should still be open
            assertTrue(bf.length() > 0);
        }

        // verify the archive is valid
        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("test.txt");
            assertNotNull(entry);
            try (InputStream is = zf.getInputStream(entry)) {
                assertArrayEquals(content, is.readAllBytes());
            }
        }
    }

    /**
     * Test that {@link ArchiveBuilder#open(BufferedFile, java.nio.file.OpenOption...)} respects
     * the builder-level default compression and ZIP64 options.
     */
    @Test
    public void testOpenBufferedFileWithOptions() throws Exception {
        Path file = tempDir.resolve("bf-options.zip");
        byte[] content = "zip64 via bf".getBytes(StandardCharsets.UTF_8);

        try (BufferedFile bf = Files2.openBuffered(file,
                StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {
            try (ArchiveBuilder builder = ArchiveBuilder.open(bf, ZipOption.ZIP64, ZipOption.STORED)) {
                try (OutputStream os = builder.addEntry("test.txt")) {
                    os.write(content);
                }
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("test.txt");
            assertNotNull(entry);
            assertEquals(ZipEntry.STORED, entry.getMethod());
            try (InputStream is = zf.getInputStream(entry)) {
                assertArrayEquals(content, is.readAllBytes());
            }
        }
    }

    // ── StandardOpenOption handling tests ────────────────────────────────

    /**
     * Test that opening an archive with default options creates the file if absent
     * and overwrites it if present.
     */
    @Test
    public void testOpenDefaultOverwritesExisting() throws Exception {
        Path file = tempDir.resolve("overwrite.zip");
        byte[] first = "first".getBytes(StandardCharsets.UTF_8);
        byte[] second = "second".getBytes(StandardCharsets.UTF_8);

        // create the archive once
        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (OutputStream os = builder.addEntry("a.txt")) {
                os.write(first);
            }
        }

        // open again at the same path — should overwrite, not fail
        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (OutputStream os = builder.addEntry("b.txt")) {
                os.write(second);
            }
        }

        // verify only the second archive's entry is present
        try (ZipFile zf = new ZipFile(file.toFile())) {
            assertNotNull(zf.getEntry("b.txt"));
            assertEquals(1, zf.size());
            try (InputStream is = zf.getInputStream(zf.getEntry("b.txt"))) {
                assertArrayEquals(second, is.readAllBytes());
            }
        }
    }

    /**
     * Test that {@link StandardOpenOption#CREATE_NEW} causes a failure when the file already exists.
     */
    @Test
    public void testOpenCreateNewFailsIfExists() throws Exception {
        Path file = tempDir.resolve("create-new.zip");

        // create the file first
        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            // empty archive
        }
        assertTrue(Files.exists(file));

        // opening with CREATE_NEW should fail
        assertThrows(FileAlreadyExistsException.class,
                () -> ArchiveBuilder.open(file, StandardOpenOption.CREATE_NEW));
    }

    /**
     * Test that {@link StandardOpenOption#CREATE_NEW} succeeds when the file does not exist.
     */
    @Test
    public void testOpenCreateNewSucceeds() throws Exception {
        Path file = tempDir.resolve("create-new-ok.zip");
        byte[] content = "create-new".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file, StandardOpenOption.CREATE_NEW)) {
            try (OutputStream os = builder.addEntry("entry.txt")) {
                os.write(content);
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            assertNotNull(zf.getEntry("entry.txt"));
            try (InputStream is = zf.getInputStream(zf.getEntry("entry.txt"))) {
                assertArrayEquals(content, is.readAllBytes());
            }
        }
    }

    /**
     * Test that {@link StandardOpenOption#TRUNCATE_EXISTING} is accepted
     * and truncates an existing file.
     */
    @Test
    public void testOpenTruncateExisting() throws Exception {
        Path file = tempDir.resolve("truncate.zip");
        byte[] first = "first-content".getBytes(StandardCharsets.UTF_8);
        byte[] second = "second".getBytes(StandardCharsets.UTF_8);

        // create a first archive
        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (OutputStream os = builder.addEntry("a.txt")) {
                os.write(first);
            }
        }
        long firstSize = Files.size(file);

        // reopen with explicit TRUNCATE_EXISTING
        try (ArchiveBuilder builder = ArchiveBuilder.open(file, StandardOpenOption.TRUNCATE_EXISTING)) {
            try (OutputStream os = builder.addEntry("b.txt")) {
                os.write(second);
            }
        }

        // verify it's a valid archive with only the second entry
        try (ZipFile zf = new ZipFile(file.toFile())) {
            assertNotNull(zf.getEntry("b.txt"));
            assertEquals(1, zf.size());
        }
    }

    /**
     * Test that {@link StandardOpenOption#READ}, {@link StandardOpenOption#WRITE},
     * and {@link StandardOpenOption#CREATE} are silently accepted by {@code open}.
     */
    @Test
    public void testOpenImpliedOptionsAccepted() throws Exception {
        Path file = tempDir.resolve("implied.zip");
        byte[] content = "implied".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file,
                StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            try (OutputStream os = builder.addEntry("entry.txt")) {
                os.write(content);
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            assertNotNull(zf.getEntry("entry.txt"));
        }
    }

    /**
     * Test that entry-creation methods silently accept {@link StandardOpenOption#CREATE},
     * {@link StandardOpenOption#READ}, {@link StandardOpenOption#WRITE}, and
     * {@link StandardOpenOption#TRUNCATE_EXISTING}.
     */
    @Test
    public void testEntryMethodsAcceptImpliedOptions() throws Exception {
        Path file = tempDir.resolve("entry-implied.zip");
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            // addEntry with implied options
            try (OutputStream os = builder.addEntry("a.txt",
                    List.of(StandardOpenOption.CREATE, StandardOpenOption.READ,
                            StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
                            ZipOption.STORED))) {
                os.write(content);
            }

            // addBufferedEntry with implied options
            try (BufferedFile bf = builder.addBufferedEntry("b.txt",
                    List.of(StandardOpenOption.CREATE, StandardOpenOption.CREATE_NEW,
                            StandardOpenOption.READ, StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING))) {
                bf.write(content);
            }

            // addArchive with implied options
            try (ArchiveBuilder inner = builder.addArchive("inner.zip",
                    List.of(StandardOpenOption.CREATE, StandardOpenOption.READ,
                            StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
                            ZipOption.STORED))) {
                try (OutputStream os = inner.addEntry("nested.txt")) {
                    os.write(content);
                }
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            assertNotNull(zf.getEntry("a.txt"));
            assertNotNull(zf.getEntry("b.txt"));
            assertNotNull(zf.getEntry("inner.zip"));
            assertEquals(3, zf.size());
        }
    }

    /**
     * Test that unsupported {@link StandardOpenOption} values are still rejected.
     */
    @Test
    public void testOpenRejectsUnsupportedOption() {
        Path file = tempDir.resolve("unsupported.zip");
        assertThrows(UnsupportedOperationException.class,
                () -> ArchiveBuilder.open(file, StandardOpenOption.APPEND));
    }

    /**
     * Test that entry-creation methods reject unsupported {@link StandardOpenOption} values.
     */
    @Test
    public void testEntryRejectsUnsupportedOption() throws Exception {
        Path file = tempDir.resolve("entry-unsupported.zip");
        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            assertThrows(UnsupportedOperationException.class,
                    () -> builder.addEntry("a.txt", StandardOpenOption.APPEND));
            assertThrows(UnsupportedOperationException.class,
                    () -> builder.addBufferedEntry("b.txt", StandardOpenOption.APPEND));
            assertThrows(UnsupportedOperationException.class,
                    () -> builder.addArchive("c.zip", StandardOpenOption.APPEND));
        }
    }

    // ── addEntry(InputStream) tests ─────────────────────────────────────

    /**
     * Test adding a STORED entry from an {@link InputStream}.
     */
    @Test
    public void testAddEntryFromInputStreamStored() throws Exception {
        Path file = tempDir.resolve("is-stored.zip");
        byte[] content = "input stream content".getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("test.txt", new ByteArrayInputStream(content), ZipOption.STORED);
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("test.txt");
            assertNotNull(entry);
            assertEquals(ZipEntry.STORED, entry.getMethod());
            assertEquals(content.length, entry.getSize());
            try (InputStream is = zf.getInputStream(entry)) {
                assertArrayEquals(content, is.readAllBytes());
            }
        }
    }

    /**
     * Test adding a DEFLATED entry from an {@link InputStream}.
     */
    @Test
    public void testAddEntryFromInputStreamDeflated() throws Exception {
        Path file = tempDir.resolve("is-deflated.zip");
        byte[] content = "input stream deflated content that should compress well aaaaaaaaaaaaa"
                .getBytes(StandardCharsets.UTF_8);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("test.txt", new ByteArrayInputStream(content), ZipOption.DEFLATED);
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("test.txt");
            assertNotNull(entry);
            assertEquals(ZipEntry.DEFLATED, entry.getMethod());
            assertEquals(content.length, entry.getSize());
            try (InputStream is = zf.getInputStream(entry)) {
                assertArrayEquals(content, is.readAllBytes());
            }
        }
    }

    /**
     * Test adding an entry from an empty {@link InputStream}.
     */
    @Test
    public void testAddEntryFromEmptyInputStream() throws Exception {
        Path file = tempDir.resolve("is-empty.zip");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("empty.txt", new ByteArrayInputStream(new byte[0]), ZipOption.STORED);
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("empty.txt");
            assertNotNull(entry);
            assertEquals(0, entry.getSize());
        }
    }

    /**
     * Test that the {@link InputStream} is not closed by the builder.
     */
    @Test
    public void testAddEntryFromInputStreamDoesNotCloseStream() throws Exception {
        Path file = tempDir.resolve("is-no-close.zip");
        byte[] content = "test".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream bais = new ByteArrayInputStream(content);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("test.txt", bais, ZipOption.STORED);
        }

        // reset and read again to prove the stream was not closed
        bais.reset();
        assertArrayEquals(content, bais.readAllBytes());
    }

    /**
     * Test adding an entry from an {@link InputStream} with file attributes.
     */
    @Test
    public void testAddEntryFromInputStreamWithAttributes() throws Exception {
        Path file = tempDir.resolve("is-attrs.zip");
        byte[] content = "with attrs".getBytes(StandardCharsets.UTF_8);
        Instant mtime = Instant.parse("2024-06-15T10:30:44.123456700Z");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("test.txt", new ByteArrayInputStream(content),
                    FileAttributes.lastModifiedTime(mtime));
        }

        Archive archive = Archive.open(Files.readAllBytes(file));
        long idx = archive.findEntry("test.txt");
        assertTrue(idx >= 0);
        assertTrue(Duration.between(mtime, archive.modifiedTime(idx)).abs().toNanos() <= 200);
        try (InputStream is = archive.openEntry(idx)) {
            assertArrayEquals(content, is.readAllBytes());
        }
    }

    /**
     * Test adding an entry from an {@link InputStream} with options and file attributes.
     */
    @Test
    public void testAddEntryFromInputStreamWithOptionsAndAttributes() throws Exception {
        Path file = tempDir.resolve("is-opts-attrs.zip");
        byte[] content = "options and attrs".getBytes(StandardCharsets.UTF_8);
        Instant mtime = Instant.parse("2025-01-01T00:00:00Z");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("test.txt", new ByteArrayInputStream(content),
                    Set.of(ZipOption.STORED), FileAttributes.lastModifiedTime(mtime));
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("test.txt");
            assertNotNull(entry);
            assertEquals(ZipEntry.STORED, entry.getMethod());
            try (InputStream is = zf.getInputStream(entry)) {
                assertArrayEquals(content, is.readAllBytes());
            }
        }
    }

    // ── addEntry(Charset) [Writer] tests ────────────────────────────────

    /**
     * Test adding a text entry via a {@link Writer} with UTF-8 charset.
     */
    @Test
    public void testAddEntryWriterUtf8() throws Exception {
        Path file = tempDir.resolve("writer-utf8.zip");
        String text = "Hello, world! Ünïcödé.";

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (Writer w = builder.addEntry("text.txt", StandardCharsets.UTF_8)) {
                w.write(text);
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("text.txt");
            assertNotNull(entry);
            try (InputStream is = zf.getInputStream(entry)) {
                assertEquals(text, new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * Test adding a text entry via a {@link Writer} with ISO-8859-1 charset.
     */
    @Test
    public void testAddEntryWriterLatin1() throws Exception {
        Path file = tempDir.resolve("writer-latin1.zip");
        String text = "café";
        Charset latin1 = StandardCharsets.ISO_8859_1;

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (Writer w = builder.addEntry("text.txt", latin1, ZipOption.STORED)) {
                w.write(text);
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("text.txt");
            assertNotNull(entry);
            try (InputStream is = zf.getInputStream(entry)) {
                assertEquals(text, new String(is.readAllBytes(), latin1));
            }
        }
    }

    /**
     * Test adding a text entry via a {@link Writer} with file attributes.
     */
    @Test
    public void testAddEntryWriterWithAttributes() throws Exception {
        Path file = tempDir.resolve("writer-attrs.zip");
        Instant mtime = Instant.parse("2024-06-15T10:30:44.123456700Z");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (Writer w = builder.addEntry("text.txt", StandardCharsets.UTF_8,
                    FileAttributes.lastModifiedTime(mtime))) {
                w.write("with attrs");
            }
        }

        Archive archive = Archive.open(Files.readAllBytes(file));
        long idx = archive.findEntry("text.txt");
        assertTrue(idx >= 0);
        assertTrue(Duration.between(mtime, archive.modifiedTime(idx)).abs().toNanos() <= 200);
    }

    /**
     * Test adding a text entry via a {@link Writer} with options and file attributes.
     */
    @Test
    public void testAddEntryWriterWithOptionsAndAttributes() throws Exception {
        Path file = tempDir.resolve("writer-opts-attrs.zip");
        String text = "options and attrs";
        Instant mtime = Instant.parse("2025-01-01T00:00:00Z");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (Writer w = builder.addEntry("text.txt", StandardCharsets.UTF_8,
                    Set.of(ZipOption.STORED), FileAttributes.lastModifiedTime(mtime))) {
                w.write(text);
            }
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("text.txt");
            assertNotNull(entry);
            assertEquals(ZipEntry.STORED, entry.getMethod());
            try (InputStream is = zf.getInputStream(entry)) {
                assertEquals(text, new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    // ── addEntry(CharSequence, Charset) tests ───────────────────────────

    /**
     * Test adding an entry from a {@link CharSequence} with UTF-8 charset.
     */
    @Test
    public void testAddEntryFromCharSequenceUtf8() throws Exception {
        Path file = tempDir.resolve("cs-utf8.zip");
        String text = "CharSequence content with Ünïcödé";

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("text.txt", text, StandardCharsets.UTF_8);
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("text.txt");
            assertNotNull(entry);
            try (InputStream is = zf.getInputStream(entry)) {
                assertEquals(text, new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * Test adding an entry from a {@link StringBuilder} (non-String CharSequence).
     */
    @Test
    public void testAddEntryFromStringBuilder() throws Exception {
        Path file = tempDir.resolve("cs-sb.zip");
        StringBuilder sb = new StringBuilder("built by ");
        sb.append("StringBuilder");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("text.txt", sb, StandardCharsets.UTF_8, ZipOption.STORED);
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("text.txt");
            assertNotNull(entry);
            try (InputStream is = zf.getInputStream(entry)) {
                assertEquals(sb.toString(), new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * Test adding an empty {@link CharSequence} entry.
     */
    @Test
    public void testAddEntryFromEmptyCharSequence() throws Exception {
        Path file = tempDir.resolve("cs-empty.zip");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("empty.txt", "", StandardCharsets.UTF_8, ZipOption.STORED);
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("empty.txt");
            assertNotNull(entry);
            assertEquals(0, entry.getSize());
        }
    }

    /**
     * Test adding a {@link CharSequence} entry with ISO-8859-1 charset.
     */
    @Test
    public void testAddEntryFromCharSequenceLatin1() throws Exception {
        Path file = tempDir.resolve("cs-latin1.zip");
        String text = "café";
        Charset latin1 = StandardCharsets.ISO_8859_1;

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("text.txt", text, latin1, ZipOption.STORED);
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("text.txt");
            assertNotNull(entry);
            try (InputStream is = zf.getInputStream(entry)) {
                assertEquals(text, new String(is.readAllBytes(), latin1));
            }
        }
    }

    /**
     * Test adding a {@link CharSequence} entry with file attributes.
     */
    @Test
    public void testAddEntryFromCharSequenceWithAttributes() throws Exception {
        Path file = tempDir.resolve("cs-attrs.zip");
        Instant mtime = Instant.parse("2024-06-15T10:30:44.123456700Z");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("text.txt", "with attrs", StandardCharsets.UTF_8,
                    FileAttributes.lastModifiedTime(mtime));
        }

        Archive archive = Archive.open(Files.readAllBytes(file));
        long idx = archive.findEntry("text.txt");
        assertTrue(idx >= 0);
        assertTrue(Duration.between(mtime, archive.modifiedTime(idx)).abs().toNanos() <= 200);
    }

    /**
     * Test adding a {@link CharSequence} entry with options and file attributes.
     */
    @Test
    public void testAddEntryFromCharSequenceWithOptionsAndAttributes() throws Exception {
        Path file = tempDir.resolve("cs-opts-attrs.zip");
        String text = "options and attrs";
        Instant mtime = Instant.parse("2025-01-01T00:00:00Z");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("text.txt", text, StandardCharsets.UTF_8,
                    Set.of(ZipOption.STORED), FileAttributes.lastModifiedTime(mtime));
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("text.txt");
            assertNotNull(entry);
            assertEquals(ZipEntry.STORED, entry.getMethod());
            try (InputStream is = zf.getInputStream(entry)) {
                assertEquals(text, new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    // ── addEntry(Reader, Charset) tests ─────────────────────────────────

    /**
     * Test adding an entry from a {@link java.io.Reader} with UTF-8 charset.
     */
    @Test
    public void testAddEntryFromReaderUtf8() throws Exception {
        Path file = tempDir.resolve("reader-utf8.zip");
        String text = "Reader content with Ünïcödé";

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("text.txt", new StringReader(text), StandardCharsets.UTF_8);
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("text.txt");
            assertNotNull(entry);
            try (InputStream is = zf.getInputStream(entry)) {
                assertEquals(text, new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * Test adding a STORED entry from a {@link java.io.Reader}.
     */
    @Test
    public void testAddEntryFromReaderStored() throws Exception {
        Path file = tempDir.resolve("reader-stored.zip");
        String text = "stored reader content";

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("text.txt", new StringReader(text), StandardCharsets.UTF_8, ZipOption.STORED);
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("text.txt");
            assertNotNull(entry);
            assertEquals(ZipEntry.STORED, entry.getMethod());
            try (InputStream is = zf.getInputStream(entry)) {
                assertEquals(text, new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * Test adding an entry from an empty {@link java.io.Reader}.
     */
    @Test
    public void testAddEntryFromEmptyReader() throws Exception {
        Path file = tempDir.resolve("reader-empty.zip");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("empty.txt", new StringReader(""), StandardCharsets.UTF_8, ZipOption.STORED);
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("empty.txt");
            assertNotNull(entry);
            assertEquals(0, entry.getSize());
        }
    }

    /**
     * Test that the {@link java.io.Reader} is not closed by the builder.
     */
    @Test
    public void testAddEntryFromReaderDoesNotCloseReader() throws Exception {
        Path file = tempDir.resolve("reader-no-close.zip");
        String text = "test";
        StringReader reader = new StringReader(text);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("test.txt", reader, StandardCharsets.UTF_8, ZipOption.STORED);
        }

        // reset and read again to prove the reader was not closed
        reader.reset();
        char[] buf = new char[text.length()];
        assertEquals(text.length(), reader.read(buf));
        assertEquals(text, new String(buf));
    }

    /**
     * Test adding an entry from a {@link java.io.Reader} with file attributes.
     */
    @Test
    public void testAddEntryFromReaderWithAttributes() throws Exception {
        Path file = tempDir.resolve("reader-attrs.zip");
        Instant mtime = Instant.parse("2024-06-15T10:30:44.123456700Z");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("text.txt", new StringReader("with attrs"), StandardCharsets.UTF_8,
                    FileAttributes.lastModifiedTime(mtime));
        }

        Archive archive = Archive.open(Files.readAllBytes(file));
        long idx = archive.findEntry("text.txt");
        assertTrue(idx >= 0);
        assertTrue(Duration.between(mtime, archive.modifiedTime(idx)).abs().toNanos() <= 200);
    }

    /**
     * Test adding an entry from a {@link java.io.Reader} with options and file attributes.
     */
    @Test
    public void testAddEntryFromReaderWithOptionsAndAttributes() throws Exception {
        Path file = tempDir.resolve("reader-opts-attrs.zip");
        String text = "options and attrs";
        Instant mtime = Instant.parse("2025-01-01T00:00:00Z");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("text.txt", new StringReader(text), StandardCharsets.UTF_8,
                    Set.of(ZipOption.STORED), FileAttributes.lastModifiedTime(mtime));
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("text.txt");
            assertNotNull(entry);
            assertEquals(ZipEntry.STORED, entry.getMethod());
            try (InputStream is = zf.getInputStream(entry)) {
                assertEquals(text, new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    // ── addEntry(Path) tests ────────────────────────────────────────────

    /**
     * Test adding a STORED entry from a {@link Path}.
     */
    @Test
    public void testAddEntryFromPathStored() throws Exception {
        Path file = tempDir.resolve("path-stored.zip");
        Path source = tempDir.resolve("source.txt");
        byte[] content = "file content from path".getBytes(StandardCharsets.UTF_8);
        Files.write(source, content);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("test.txt", source, ZipOption.STORED);
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("test.txt");
            assertNotNull(entry);
            assertEquals(ZipEntry.STORED, entry.getMethod());
            assertEquals(content.length, entry.getSize());
            try (InputStream is = zf.getInputStream(entry)) {
                assertArrayEquals(content, is.readAllBytes());
            }
        }
    }

    /**
     * Test adding a DEFLATED entry from a {@link Path}.
     */
    @Test
    public void testAddEntryFromPathDeflated() throws Exception {
        Path file = tempDir.resolve("path-deflated.zip");
        Path source = tempDir.resolve("source-deflated.txt");
        byte[] content = "path deflated content that should compress well aaaaaaaaaaaaa"
                .getBytes(StandardCharsets.UTF_8);
        Files.write(source, content);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("test.txt", source, ZipOption.DEFLATED);
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("test.txt");
            assertNotNull(entry);
            assertEquals(ZipEntry.DEFLATED, entry.getMethod());
            assertEquals(content.length, entry.getSize());
            try (InputStream is = zf.getInputStream(entry)) {
                assertArrayEquals(content, is.readAllBytes());
            }
        }
    }

    /**
     * Test adding an entry from an empty file {@link Path}.
     */
    @Test
    public void testAddEntryFromEmptyPath() throws Exception {
        Path file = tempDir.resolve("path-empty.zip");
        Path source = tempDir.resolve("empty-source.txt");
        Files.write(source, new byte[0]);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("empty.txt", source, ZipOption.STORED);
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("empty.txt");
            assertNotNull(entry);
            assertEquals(0, entry.getSize());
        }
    }

    /**
     * Test adding an entry from a {@link Path} with default options (DEFLATED).
     */
    @Test
    public void testAddEntryFromPathDefaultOptions() throws Exception {
        Path file = tempDir.resolve("path-default.zip");
        Path source = tempDir.resolve("source-default.txt");
        byte[] content = "default options".getBytes(StandardCharsets.UTF_8);
        Files.write(source, content);

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("test.txt", source);
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("test.txt");
            assertNotNull(entry);
            assertEquals(ZipEntry.DEFLATED, entry.getMethod());
            try (InputStream is = zf.getInputStream(entry)) {
                assertArrayEquals(content, is.readAllBytes());
            }
        }
    }

    /**
     * Test adding an entry from a {@link Path} with file attributes.
     */
    @Test
    public void testAddEntryFromPathWithAttributes() throws Exception {
        Path file = tempDir.resolve("path-attrs.zip");
        Path source = tempDir.resolve("source-attrs.txt");
        byte[] content = "with attrs".getBytes(StandardCharsets.UTF_8);
        Files.write(source, content);
        Instant mtime = Instant.parse("2024-06-15T10:30:44.123456700Z");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("test.txt", source, FileAttributes.lastModifiedTime(mtime));
        }

        Archive archive = Archive.open(Files.readAllBytes(file));
        long idx = archive.findEntry("test.txt");
        assertTrue(idx >= 0);
        assertTrue(Duration.between(mtime, archive.modifiedTime(idx)).abs().toNanos() <= 200);
        try (InputStream is = archive.openEntry(idx)) {
            assertArrayEquals(content, is.readAllBytes());
        }
    }

    /**
     * Test adding an entry from a {@link Path} with options and file attributes.
     */
    @Test
    public void testAddEntryFromPathWithOptionsAndAttributes() throws Exception {
        Path file = tempDir.resolve("path-opts-attrs.zip");
        Path source = tempDir.resolve("source-opts-attrs.txt");
        byte[] content = "options and attrs".getBytes(StandardCharsets.UTF_8);
        Files.write(source, content);
        Instant mtime = Instant.parse("2025-01-01T00:00:00Z");

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("test.txt", source,
                    Set.of(ZipOption.STORED), FileAttributes.lastModifiedTime(mtime));
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry entry = zf.getEntry("test.txt");
            assertNotNull(entry);
            assertEquals(ZipEntry.STORED, entry.getMethod());
            try (InputStream is = zf.getInputStream(entry)) {
                assertArrayEquals(content, is.readAllBytes());
            }
        }
    }

    /**
     * Test that all convenience entry methods work together in a single archive.
     */
    @Test
    public void testAllConvenienceMethodsMixed() throws Exception {
        Path file = tempDir.resolve("all-methods.zip");
        Path sourceFile = tempDir.resolve("source-mixed.dat");
        byte[] binaryContent = { 0x00, 0x01, 0x02, 0x03, (byte) 0xFF };
        Files.write(sourceFile, binaryContent);
        byte[] streamContent = "from input stream".getBytes(StandardCharsets.UTF_8);
        String textContent = "from CharSequence";
        String readerContent = "from Reader";

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            // OutputStream (existing)
            try (OutputStream os = builder.addEntry("a-stream.txt", ZipOption.STORED)) {
                os.write("from OutputStream".getBytes(StandardCharsets.UTF_8));
            }
            // InputStream
            builder.addEntry("b-inputstream.txt", new ByteArrayInputStream(streamContent), ZipOption.STORED);
            // Writer
            try (Writer w = builder.addEntry("c-writer.txt", StandardCharsets.UTF_8, ZipOption.STORED)) {
                w.write("from Writer");
            }
            // CharSequence
            builder.addEntry("d-charsequence.txt", textContent, StandardCharsets.UTF_8, ZipOption.STORED);
            // Reader
            builder.addEntry("e-reader.txt", new StringReader(readerContent), StandardCharsets.UTF_8, ZipOption.STORED);
            // Path
            builder.addEntry("f-path.dat", sourceFile, ZipOption.STORED);
        }

        try (ZipFile zf = new ZipFile(file.toFile())) {
            assertEquals(6, zf.size());
            try (InputStream is = zf.getInputStream(zf.getEntry("a-stream.txt"))) {
                assertEquals("from OutputStream", new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
            try (InputStream is = zf.getInputStream(zf.getEntry("b-inputstream.txt"))) {
                assertArrayEquals(streamContent, is.readAllBytes());
            }
            try (InputStream is = zf.getInputStream(zf.getEntry("c-writer.txt"))) {
                assertEquals("from Writer", new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
            try (InputStream is = zf.getInputStream(zf.getEntry("d-charsequence.txt"))) {
                assertEquals(textContent, new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
            try (InputStream is = zf.getInputStream(zf.getEntry("e-reader.txt"))) {
                assertEquals(readerContent, new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
            try (InputStream is = zf.getInputStream(zf.getEntry("f-path.dat"))) {
                assertArrayEquals(binaryContent, is.readAllBytes());
            }
        }
    }

    // ── lastEntry*() query method tests ─────────────────────────────────

    /**
     * Test that {@code lastEntry*()} methods return correct values for a STORED entry.
     */
    @Test
    public void testLastEntryStoredEntry() throws Exception {
        byte[] data = "hello, stored!".getBytes(StandardCharsets.UTF_8);
        CRC32 expectedCrc = new CRC32();
        expectedCrc.update(data);

        Path file = tempDir.resolve("last-stored.zip");
        long dataOffset;
        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("stored.txt", data, ZipOption.STORED);

            assertEquals("stored.txt", builder.lastEntryName());
            dataOffset = builder.lastEntryDataOffset();
            assertEquals(data.length, builder.lastEntryCompressedSize());
            assertEquals(data.length, builder.lastEntryUncompressedSize());
            assertEquals((int) expectedCrc.getValue(), builder.lastEntryCrc32());
        }

        // verify the data offset is correct by reading the raw file
        byte[] raw = Files.readAllBytes(file);
        byte[] atOffset = new byte[data.length];
        System.arraycopy(raw, (int) dataOffset, atOffset, 0, data.length);
        assertArrayEquals(data, atOffset);
    }

    /**
     * Test that {@code lastEntry*()} methods return correct values for a DEFLATED entry.
     */
    @Test
    public void testLastEntryDeflatedEntry() throws Exception {
        byte[] data = "hello, deflated! repeat repeat repeat repeat repeat".getBytes(StandardCharsets.UTF_8);
        CRC32 expectedCrc = new CRC32();
        expectedCrc.update(data);

        Path file = tempDir.resolve("last-deflated.zip");
        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("deflated.txt", data, ZipOption.DEFLATED);

            assertEquals("deflated.txt", builder.lastEntryName());
            assertEquals(data.length, builder.lastEntryUncompressedSize());
            assertTrue(builder.lastEntryCompressedSize() < data.length,
                    "compressed size should be smaller than uncompressed for repetitive data");
            assertEquals((int) expectedCrc.getValue(), builder.lastEntryCrc32());
        }
    }

    /**
     * Test that {@code lastEntry*()} methods return correct values for a directory entry.
     */
    @Test
    public void testLastEntryDirectoryEntry() throws Exception {
        Path file = tempDir.resolve("last-dir.zip");
        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addDirectory("META-INF/");

            assertEquals("META-INF/", builder.lastEntryName());
            assertEquals(0, builder.lastEntryCompressedSize());
            assertEquals(0, builder.lastEntryUncompressedSize());
            assertEquals(0, builder.lastEntryCrc32());
        }
    }

    /**
     * Test that {@code lastEntry*()} throws {@link IllegalStateException}
     * before any entry has been written.
     */
    @Test
    public void testLastEntryBeforeAnyEntry() throws Exception {
        Path file = tempDir.resolve("last-none.zip");
        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            assertThrows(IllegalStateException.class, builder::lastEntryName);
            assertThrows(IllegalStateException.class, builder::lastEntryDataOffset);
            assertThrows(IllegalStateException.class, builder::lastEntryCompressedSize);
            assertThrows(IllegalStateException.class, builder::lastEntryUncompressedSize);
            assertThrows(IllegalStateException.class, builder::lastEntryCrc32);
        }
    }

    /**
     * Test that {@code lastEntry*()} throws {@link IllegalStateException}
     * while an entry output stream is still open.
     */
    @Test
    public void testLastEntryWhileStreamOpen() throws Exception {
        Path file = tempDir.resolve("last-open.zip");
        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            OutputStream os = builder.addEntry("open.txt");
            assertThrows(IllegalStateException.class, builder::lastEntryName);
            assertThrows(IllegalStateException.class, builder::lastEntryDataOffset);
            assertThrows(IllegalStateException.class, builder::lastEntryCompressedSize);
            assertThrows(IllegalStateException.class, builder::lastEntryUncompressedSize);
            assertThrows(IllegalStateException.class, builder::lastEntryCrc32);
            os.close();
        }
    }

    /**
     * Test that {@code lastEntry*()} reflects the most recently completed entry
     * when multiple entries are written.
     */
    @Test
    public void testLastEntryReflectsSecondEntry() throws Exception {
        byte[] first = "first".getBytes(StandardCharsets.UTF_8);
        byte[] second = "second entry data".getBytes(StandardCharsets.UTF_8);
        CRC32 secondCrc = new CRC32();
        secondCrc.update(second);

        Path file = tempDir.resolve("last-two.zip");
        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            builder.addEntry("first.txt", first, ZipOption.STORED);
            assertEquals("first.txt", builder.lastEntryName());

            builder.addEntry("second.txt", second, ZipOption.STORED);
            assertEquals("second.txt", builder.lastEntryName());
            assertEquals(second.length, builder.lastEntryCompressedSize());
            assertEquals(second.length, builder.lastEntryUncompressedSize());
            assertEquals((int) secondCrc.getValue(), builder.lastEntryCrc32());
        }
    }
}
