package io.smallrye.common.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

import io.smallrye.common.io.archive.Archive;

/**
 * Tests for the {@link Archive} class.
 */
public class ArchiveTests {

    /**
     * Test that {@link Archive#modifiedTime(long)} and {@link Archive#creationTime(long)}
     * correctly parse timestamps from ZIP entries when falling back to DOS time fields.
     * Uses a timestamp with 44 seconds to detect the DOS seconds/2 decoding bug.
     */
    @Test
    public void testModifiedAndCreationTime() throws Exception {
        Instant inputTime = Instant.parse("2024-06-15T10:30:44Z");
        byte[] zipBytes = createZipWithTimestamp("test.txt", "hello", inputTime);

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
        byte[] zipBytes = createZipWithNtfsTimestamps("ntfs.txt", "data", expectedMtime, expectedCtime);
        Archive archive = Archive.open(zipBytes);
        try {
            long idx = archive.findEntry("ntfs.txt");
            assertTrue(idx >= 0, "Entry 'ntfs.txt' should be found");

            Instant mtime = archive.modifiedTime(idx);
            // NTFS has 100ns resolution; compare at that granularity
            assertTrue(Duration.between(expectedMtime, mtime).abs().toNanos() < 200,
                    "Modified time should match within 200ns, got " + mtime + " expected " + expectedMtime);

            Instant ctime = archive.creationTime(idx);
            assertTrue(Duration.between(expectedCtime, ctime).abs().toNanos() < 200,
                    "Creation time should match within 200ns, got " + ctime + " expected " + expectedCtime);
        } finally {
            archive.release();
        }
    }

    /**
     * Create a minimal ZIP archive with a single STORED entry having the given timestamp.
     * Uses {@link ZipEntry#setTime(long)} to set only DOS time fields (no extra fields).
     *
     * @param name the entry name
     * @param content the entry content
     * @param mtime the modification time to set
     * @return the ZIP file bytes
     */
    private static byte[] createZipWithTimestamp(String name, String content, Instant mtime) throws Exception {
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

    private static final Instant NTFS_EPOCH = Instant.from(ZonedDateTime.of(1601, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));

    /**
     * Convert an {@link Instant} to an NTFS FILETIME value (100-nanosecond intervals since 1601-01-01 UTC).
     *
     * @param instant the instant to convert
     * @return the NTFS FILETIME value
     */
    private static long toNtfsFileTime(Instant instant) {
        Duration d = Duration.between(NTFS_EPOCH, instant);
        return d.getSeconds() * 10_000_000L + d.getNano() / 100;
    }

    /**
     * Create a ZIP with a single STORED entry containing an NTFS extra field (0x000a)
     * in the central directory entry. The NTFS extra field contains mtime, atime, and ctime
     * as 100-nanosecond intervals since 1601-01-01 UTC.
     *
     * @param name the entry name
     * @param content the entry content
     * @param mtime the modification time
     * @param ctime the creation time
     * @return the ZIP file bytes
     */
    private static byte[] createZipWithNtfsTimestamps(String name, String content, Instant mtime, Instant ctime)
            throws Exception {
        // Build the NTFS extra field data:
        // 4 bytes reserved + subtag(2) + sublen(2) + mtime(8) + atime(8) + ctime(8) = 32 bytes
        byte[] ntfsExtra = new byte[32];
        ByteBuffer buf = ByteBuffer.wrap(ntfsExtra).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(0); // reserved
        buf.putShort((short) 0x0001); // subtag: timestamps
        buf.putShort((short) 24); // subtag data length: 3 * 8 bytes
        buf.putLong(toNtfsFileTime(mtime)); // mtime
        buf.putLong(toNtfsFileTime(mtime)); // atime (same as mtime for this test)
        buf.putLong(toNtfsFileTime(ctime)); // ctime

        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);

        // Build the ZIP manually: local file header + data + central directory entry + EOCD
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        CRC32 crc32 = new CRC32();
        crc32.update(data);
        int crc = (int) crc32.getValue();

        // Local file header (no extra field here, only in CDE)
        int lfhOff = baos.size();
        writeLfh(baos, nameBytes, data, crc);

        // Central directory entry with NTFS extra field
        int cdOff = baos.size();
        writeCde(baos, nameBytes, data, crc, ntfsExtra, lfhOff);

        // EOCD
        int cdSize = baos.size() - cdOff;
        writeEocd(baos, 1, cdSize, cdOff);

        return baos.toByteArray();
    }

    /**
     * Write a local file header for a STORED entry.
     */
    private static void writeLfh(ByteArrayOutputStream baos, byte[] name, byte[] data, int crc) {
        ByteBuffer buf = ByteBuffer.allocate(30 + name.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(0x04034b50); // signature
        buf.putShort((short) 20); // version needed
        buf.putShort((short) 0); // GP bits
        buf.putShort((short) 0); // method: STORED
        buf.putShort((short) 0); // mod time
        buf.putShort((short) 0); // mod date
        buf.putInt(crc);
        buf.putInt(data.length); // compressed size
        buf.putInt(data.length); // uncompressed size
        buf.putShort((short) name.length);
        buf.putShort((short) 0); // extra field length
        buf.put(name);
        baos.write(buf.array(), 0, buf.position());
        baos.write(data, 0, data.length);
    }

    /**
     * Write a central directory entry with an NTFS extra field.
     */
    private static void writeCde(ByteArrayOutputStream baos, byte[] name, byte[] data, int crc,
            byte[] extra, int lfhOffset) {
        // extra field: tag(2) + size(2) + data
        int extraFieldTotalLen = 4 + extra.length;
        ByteBuffer buf = ByteBuffer.allocate(46 + name.length + extraFieldTotalLen).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(0x02014b50); // signature
        buf.putShort((short) 20); // version made by
        buf.putShort((short) 20); // version needed
        buf.putShort((short) 0); // GP bits
        buf.putShort((short) 0); // method: STORED
        buf.putShort((short) 0); // mod time
        buf.putShort((short) 0); // mod date
        buf.putInt(crc);
        buf.putInt(data.length); // compressed size
        buf.putInt(data.length); // uncompressed size
        buf.putShort((short) name.length);
        buf.putShort((short) extraFieldTotalLen); // extra field length
        buf.putShort((short) 0); // comment length
        buf.putShort((short) 0); // disk number start
        buf.putShort((short) 0); // internal attributes
        buf.putInt(0); // external attributes
        buf.putInt(lfhOffset); // local header offset
        buf.put(name);
        // extra field: NTFS tag + data size + data
        buf.putShort((short) 0x000a); // NTFS tag
        buf.putShort((short) extra.length); // data size
        buf.put(extra);
        baos.write(buf.array(), 0, buf.position());
    }

    /**
     * Write an end-of-central-directory record.
     */
    private static void writeEocd(ByteArrayOutputStream baos, int entryCount, int cdSize, int cdOffset) {
        ByteBuffer buf = ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(0x06054b50); // signature
        buf.putShort((short) 0); // disk number
        buf.putShort((short) 0); // CD start disk
        buf.putShort((short) entryCount); // entries this disk
        buf.putShort((short) entryCount); // entries total
        buf.putInt(cdSize);
        buf.putInt(cdOffset);
        buf.putShort((short) 0); // comment length
        baos.write(buf.array(), 0, buf.position());
    }
}
