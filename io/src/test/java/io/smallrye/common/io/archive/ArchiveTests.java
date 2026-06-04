package io.smallrye.common.io.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.common.io.archive.Archive;
import io.smallrye.common.io.archive.ArchiveBuilder;
import io.smallrye.common.io.archive.ZipOption;

/**
 * Tests for the {@link Archive} class.
 */
public class ArchiveTests {

    @TempDir
    Path tempDir;

    /**
     * Test that {@link Archive#modifiedTime(long)} and {@link Archive#creationTime(long)}
     * correctly parse timestamps from ZIP entries when falling back to DOS time fields.
     * Uses a timestamp with 44 seconds to detect the DOS seconds/2 decoding bug.
     * <p>
     * This test intentionally uses {@link ZipOutputStream} rather than {@link ArchiveBuilder}
     * because {@code ArchiveBuilder} always writes NTFS extra fields; this test exercises the
     * DOS-only fallback path in the {@link Archive} reader.
     */
    @Test
    public void testModifiedAndCreationTime() throws Exception {
        Instant inputTime = Instant.parse("2024-06-15T10:30:44Z");
        byte[] zipBytes = createZipWithDosTimestamp("test.txt", "hello", inputTime);

        // Both ZipOutputStream and Archive interpret DOS time as local time,
        // so the round-trip result is the input truncated to 2-second DOS granularity.
        Instant expectedDos = inputTime.minusNanos(inputTime.getNano())
                .minusSeconds(inputTime.getEpochSecond() % 2);

        Archive archive = Archive.open(zipBytes);
        try {
            long idx = archive.findEntry("test.txt");
            assertTrue(idx >= 0, "Entry 'test.txt' should be found");

            Instant mtime = archive.modifiedTime(idx);
            assertEquals(expectedDos.getEpochSecond(), mtime.getEpochSecond(),
                    "Modified time seconds should match DOS round-trip");

            Instant ctime = archive.creationTime(idx);
            assertEquals(expectedDos.getEpochSecond(), ctime.getEpochSecond(),
                    "Creation time seconds should match DOS round-trip");
        } finally {
            archive.release();
        }
    }

    /**
     * Test that {@link Archive#modifiedTime(long)} and {@link Archive#creationTime(long)}
     * correctly parse NTFS extended timestamp attributes from ZIP entries.
     * NTFS timestamps are stored as 100-nanosecond intervals since 1601-01-01 UTC.
     */
    @Test
    public void testNtfsTimestamps() throws Exception {
        Instant expectedMtime = Instant.parse("2024-06-15T10:30:44.1234567Z");
        Instant expectedCtime = Instant.parse("2024-01-01T00:00:00.9999999Z");

        Path file = tempDir.resolve("ntfs-timestamps.zip");
        FileAttribute<?> modAttr = new SimpleFileAttribute<>("basic:lastModifiedTime", FileTime.from(expectedMtime));
        FileAttribute<?> createAttr = new SimpleFileAttribute<>("basic:creationTime", FileTime.from(expectedCtime));

        try (ArchiveBuilder builder = ArchiveBuilder.open(file)) {
            try (OutputStream os = builder.addEntry("ntfs.txt", java.util.Set.of(ZipOption.STORED), modAttr, createAttr)) {
                os.write("data".getBytes(StandardCharsets.UTF_8));
            }
        }

        Archive archive = Archive.of(file);
        try {
            long idx = archive.findEntry("ntfs.txt");
            assertTrue(idx >= 0, "Entry 'ntfs.txt' should be found");

            Instant mtime = archive.modifiedTime(idx);
            assertTrue(Duration.between(expectedMtime, mtime).abs().toNanos() < 200,
                    "Modified time should match within 200ns, got " + mtime + " expected " + expectedMtime);

            Instant ctime = archive.creationTime(idx);
            assertTrue(Duration.between(expectedCtime, ctime).abs().toNanos() < 200,
                    "Creation time should match within 200ns, got " + ctime + " expected " + expectedCtime);
        } finally {
            archive.release();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /**
     * Create a minimal ZIP archive with a single STORED entry having only DOS time fields.
     * Uses {@link ZipEntry#setTime(long)} which does not produce NTFS extra fields.
     *
     * @param name the entry name
     * @param content the entry content
     * @param mtime the modification time to set
     * @return the ZIP file bytes
     */
    private static byte[] createZipWithDosTimestamp(String name, String content, Instant mtime) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(name);
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(data.length);
            entry.setCompressedSize(data.length);
            CRC32 crc32 = new CRC32();
            crc32.update(data);
            entry.setCrc(crc32.getValue());
            entry.setTime(mtime.toEpochMilli());
            zos.putNextEntry(entry);
            zos.write(data);
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    /**
     * A simple {@link FileAttribute} implementation for testing.
     *
     * @param <T> the attribute value type
     */
    private record SimpleFileAttribute<T>(String name, T value) implements FileAttribute<T> {
    }
}
