package io.smallrye.common.io.archive;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.common.io.BufferedFile;
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

        FileAttribute<?> modAttr = new SimpleFileAttribute<>("basic:lastModifiedTime", FileTime.from(mtime));
        FileAttribute<?> createAttr = new SimpleFileAttribute<>("basic:creationTime", FileTime.from(ctime));

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

        FileAttribute<?> modAttr = new SimpleFileAttribute<>("basic:lastModifiedTime", FileTime.from(mtime));
        FileAttribute<?> createAttr = new SimpleFileAttribute<>("basic:creationTime", FileTime.from(ctime));

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

    // ── Helper ──────────────────────────────────────────────────────────

    /**
     * A simple {@link FileAttribute} implementation for testing.
     *
     * @param <T> the attribute value type
     */
    private record SimpleFileAttribute<T>(String name, T value) implements FileAttribute<T> {
    }
}
