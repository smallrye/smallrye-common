package io.smallrye.common.io.archive;

import static io.smallrye.common.io.archive.Constants.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.zip.Inflater;

import io.smallrye.common.annotation.Experimental;
import io.smallrye.common.search.BinarySearch;

/**
 * A ZIP archive.
 */
@Experimental("Incubating archive API")
public final class Archive {
    private final ArchiveData data;
    private final Index index;

    Archive(final ArchiveData data, final Index index) {
        this.data = data;
        this.index = index;
    }

    /**
     * Open a ZIP archive from a byte array.
     *
     * @param bytes the byte array containing the archive data (must not be {@code null})
     * @return the opened archive (not {@code null})
     * @throws IllegalArgumentException if the byte array does not contain a valid ZIP archive
     */
    public static Archive open(final byte[] bytes) {
        return of(new BufferArchiveData(ByteBuffer.wrap(bytes)), 0, bytes.length);
    }

    /**
     * Open a ZIP archive from a byte buffer.
     * The archive is read from the buffer's current position to its limit.
     *
     * @param buffer the byte buffer containing the archive data (must not be {@code null})
     * @return the opened archive (not {@code null})
     * @throws IllegalArgumentException if the buffer does not contain a valid ZIP archive
     */
    public static Archive open(final ByteBuffer buffer) {
        return of(new BufferArchiveData(buffer), 0, buffer.remaining());
    }

    /**
     * Open a ZIP archive from a filesystem file.
     * The file is mapped into memory, so normally this method should only be used
     * for long-lived instances.
     *
     * @param path the path of the archive file to open (must not be {@code null})
     * @return the opened archive (not {@code null})
     * @throws IOException if the file cannot be opened
     * @throws IllegalArgumentException if the file does not contain a valid ZIP archive
     */
    public static Archive of(final Path path) throws IOException {
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
            return of(new MappedArchiveData(ch), 0, ch.size());
        }
    }

    static Archive of(final ArchiveData data, long start, long end) {
        // find EOCD
        long eocd = data.findRecord(SIG_EOCD, Math.max(start, end - 4096), end);
        if (eocd == -1) {
            throw new IllegalArgumentException("Invalid archive (no EOCD record)");
        }
        // look for a zip64 EOCDL
        long eocdl = data.findRecord(SIG_EOCDL_ZIP64, Math.max(start, eocd - 128), eocd);
        long zip64eocd = eocdl == -1 ? -1 : data.zip64eocdlEocdRelativeOffset(eocdl);
        long cd = data.eocdDirectoryRelativeOffset(eocd);
        if (cd == 0xffffffffL && zip64eocd != -1) {
            cd = data.zip64eocdDirectoryRelativeOffset(zip64eocd);
        }
        Index index = Index.of(data, eocd, zip64eocd, cd);
        return new Archive(data, index);
    }

    /**
     * Find an entry with the given name.
     *
     * @param name the name of the entry (must not be {@code null})
     * @return the index of the entry, or a negative value indicating the point where the name would be found ({@code -n - 1})
     */
    public long findEntry(String name) {
        return findEntry(name, 0, entryCount());
    }

    /**
     * Find an entry with the given name.
     *
     * @param name the name of the entry (must not be {@code null})
     * @param from the first entry to search (inclusive)
     * @param to the last entry to search (exclusive)
     * @return the index of the entry, or a negative value indicating the point where the name would be found ({@code -n - 1})
     */
    public long findEntry(String name, long from, long to) {
        if (from >= to) {
            return -from - 1;
        }
        long res = BinarySearch.longRange().inCollection().find(name, from, to, (n, idx) -> compareEntryNameTo(idx, n) >= 0);
        if (res < to) {
            // possibly found
            if (entryNameEquals(res, name)) {
                return res;
            }
        }
        // not found; here is where it would go
        return -res - 1;
    }

    /**
     * {@return the number of entries in the archive}
     */
    public long entryCount() {
        return index.entries();
    }

    /**
     * {@return the name of the entry corresponding to the given index.}
     *
     * @param idx the index of the entry
     */
    public String entryName(long idx) {
        long cde = index.cdeOffset(idx);
        int len = data.cdeFileNameLength(cde);
        if (data.cdeIsUtf8(cde)) {
            return data.utf8ToString(cde + CDE_END, len);
        } else {
            return data.cp437ToString(cde + CDE_END, len);
        }
    }

    /**
     * Determine whether the name of the entry at the given index is equal to the given name.
     * The comparison is performed directly against the encoded entry name without allocating a string.
     *
     * @param idx the index of the entry
     * @param name the name to compare against (must not be {@code null})
     * @return {@code true} if the entry name equals the given name, or {@code false} otherwise
     */
    public boolean entryNameEquals(long idx, String name) {
        return compareEntryNameTo(idx, name) == 0;
    }

    /**
     * Determine whether the name of the entry at the given index starts with the given prefix.
     * The comparison is performed directly against the encoded entry name without allocating a string.
     *
     * @param idx the index of the entry
     * @param prefix the prefix to test against (must not be {@code null})
     * @return {@code true} if the entry name starts with the given prefix, or {@code false} otherwise
     */
    public boolean entryNameStartsWith(long idx, String prefix) {
        long cde = index.cdeOffset(idx);
        int len = data.cdeFileNameLength(cde);
        if (data.cdeIsUtf8(cde)) {
            return data.utf8StartsWithString(cde + CDE_END, len, prefix);
        } else {
            return data.cp437StartsWithString(cde + CDE_END, len, prefix);
        }
    }

    /**
     * Compare the name of the entry at the given index to the given name.
     * The comparison is performed directly against the encoded entry name without allocating a string.
     *
     * @param idx the index of the entry
     * @param name the name to compare against (must not be {@code null})
     * @return a negative value, zero, or a positive value if the entry name is less than, equal to,
     *         or greater than the given name, respectively
     */
    public int compareEntryNameTo(long idx, String name) {
        long cde = index.cdeOffset(idx);
        int len = data.cdeFileNameLength(cde);
        if (data.cdeIsUtf8(cde)) {
            return data.compareUtf8ToString(cde + CDE_END, len, name, 0);
        } else {
            return data.compareCp437ToString(cde + CDE_END, len, name);
        }
    }

    /**
     * Compare the name of the entry at the given index to the given name, starting at the given offset
     * within the entry name.
     * The comparison is performed directly against the encoded entry name without allocating a string.
     *
     * @param idx the index of the entry
     * @param name the name to compare against (must not be {@code null})
     * @param offs the character offset within the entry name at which to begin the comparison
     * @return a negative value, zero, or a positive value if the entry name (from the given offset) is less than,
     *         equal to, or greater than the given name, respectively
     */
    public int compareEntryNameTo(long idx, String name, int offs) {
        long cde = index.cdeOffset(idx);
        int len = data.cdeFileNameLength(cde);
        if (data.cdeIsUtf8(cde)) {
            // compute character offset
            return data.compareUtf8ToString(cde + CDE_END, len, name, offs);
        } else {
            return data.compareCp437ToString(cde + CDE_END + offs, len, name);
        }
    }

    /**
     * Open a nested archive stored at the given index.
     *
     * @param idx the entry index
     * @return the archive (not {@code null})
     * @throws IllegalArgumentException if the zip entry is not {@code STORED} or if the entry
     *         does not appear to be a zip file
     */
    public Archive storedArchive(long idx) {
        long cde = index.cdeOffset(idx);
        if (data.cdeMethod(cde) != METHOD_STORED) {
            throw new IllegalArgumentException("Only STORED archive members can be mapped");
        }
        // find zip64 info, if any
        long zip64 = data.cdeZip64(cde);
        // find the start of the data
        long lfh = data.cdeLocalHeaderRelativeOffset(cde, zip64);
        long start = lfh + data.lfhEntrySize(lfh);
        long size = data.cdeCompressedSize(cde, zip64);
        return Archive.of(data, start, start + size);
    }

    /**
     * Map the uncompressed data of a {@code STORED} entry directly to a byte buffer.
     * Since the data is stored without compression, this provides a zero-copy view of the entry content.
     *
     * @param idx the entry index
     * @return the byte buffer containing the entry data (not {@code null})
     * @throws IllegalArgumentException if the entry is not stored with the {@code STORED} method
     */
    public ByteBuffer mapStoredToBuffer(long idx) {
        long cde = index.cdeOffset(idx);
        if (data.cdeMethod(cde) != METHOD_STORED) {
            throw new IllegalArgumentException("Only STORED archive members can be mapped");
        }
        // find zip64 info, if any
        long zip64 = data.cdeZip64(cde);
        // find the start of the data
        long lfh = data.cdeLocalHeaderRelativeOffset(cde, zip64);
        long start = lfh + data.lfhEntrySize(lfh);
        long size = data.cdeCompressedSize(cde, zip64);
        return data.buffer(start, (int) size);
    }

    /**
     * Open the entry at the given index as an input stream.
     * The returned stream will decompress the data if necessary.
     * Both the {@code STORED} and {@code DEFLATE} compression methods are supported.
     *
     * @param idx the entry index
     * @return the input stream for reading the entry data (not {@code null})
     * @throws UnsupportedOperationException if the entry uses an unsupported compression method
     */
    public InputStream openEntry(long idx) {
        long cde = index.cdeOffset(idx);
        // find zip64 info, if any
        long zip64 = data.cdeZip64(cde);
        // find the start of the compressed data
        long lfh = data.cdeLocalHeaderRelativeOffset(cde, zip64);
        long start = lfh + data.lfhEntrySize(lfh);
        long size = data.cdeCompressedSize(cde, zip64);
        long uncSize = data.cdeUncompressedSize(cde, zip64);
        // find the compression method
        int method = data.cdeMethod(cde);
        return switch (method) {
            case METHOD_STORED -> new ArchiveDataInputStream(data, start, size);
            case METHOD_DEFLATE -> new BufferedInputStream(new ArchiveInflaterInputStream(data, new Inflater(true), start, size, uncSize));
            default -> throw new UnsupportedOperationException("Compression method not supported: " + method);
        };
    }

    /**
     * Determine whether the entry at the given index is stored without compression.
     *
     * @param idx the entry index
     * @return {@code true} if the entry uses the {@code STORED} compression method, or {@code false} otherwise
     */
    public boolean isStored(long idx) {
        long cde = index.cdeOffset(idx);
        return data.cdeMethod(cde) == METHOD_STORED;
    }

    /**
     * Attempt to release some or all of the resources associated with this archive.
     * After calling this method, accessing the archive (other than to close it) may incur some unspecified performance penalty.
     */
    public void release() {
        data.release();
    }

    /**
     * {@return the compressed size in bytes of the entry at the given index}
     *
     * @param idx the entry index
     */
    public long compressedSize(final long idx) {
        return data.cdeCompressedSize(index.cdeOffset(idx));
    }

    /**
     * {@return the uncompressed size in bytes of the entry at the given index}
     *
     * @param idx the entry index
     */
    public long uncompressedSize(final long idx) {
        return data.cdeUncompressedSize(index.cdeOffset(idx));
    }

    private static final Instant NTFS_EPOCH = Instant.from(ZonedDateTime.of(1601, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));

    /**
     * Convert an NTFS FILETIME value (100-nanosecond intervals since 1601-01-01 UTC) to an {@link Instant}.
     *
     * @param fileTime the FILETIME value
     * @return the corresponding instant
     */
    private static Instant ntfsFileTimeToInstant(long fileTime) {
        return NTFS_EPOCH.plusSeconds(fileTime / 10_000_000L).plusNanos(fileTime % 10_000_000L * 100L);
    }

    /**
     * {@return the creation time of the entry at the given index}
     * The time is determined by checking for NTFS and Unix extended attributes in the entry's extra field data.
     * If no extended attributes are present, the modification date and time from the central directory entry
     * are used as a fallback, interpreted in the system default time zone.
     *
     * @param idx the entry index
     */
    public Instant creationTime(final long idx) {
        // first check for extended attributes
        long cde = index.cdeOffset(idx);
        int fnLen = data.cdeFileNameLength(cde);
        int efl = data.cdeExtraFieldLength(cde);
        long off = cde + CDE_END + fnLen;
        long endOff = off + efl;
        while (off < endOff) {
            int sig = data.u16le(off);
            int len = data.u16le(off + 2);
            switch (sig) {
                case EX_NTFS -> {
                    // skip 4 reserved bytes, then iterate NTFS subtags
                    long subOff = off + 8;
                    long subEnd = off + 4 + len;
                    while (subOff < subEnd) {
                        int subSig = data.u16le(subOff);
                        int subLen = data.u16le(subOff + 2);
                        if (subSig == 0x0001) {
                            // got our times: ctime is at subtag data offset 16
                            return ntfsFileTimeToInstant(data.s64le(subOff + 20));
                        }
                        subOff += subLen + 4;
                    }
                }
                case EX_UNIX -> {
                    return Instant.ofEpochSecond(data.u32le(off + 8));
                }
            }
            off += len + 4;
        }
        // no match; use the info found in the CDE, such as it is
        int date = data.cdeModDate(cde);
        LocalDate ld = LocalDate.of(1980 + (date >> 9), date >> 5 & 0xf, date & 0x1f);
        int time = data.cdeModTime(cde);
        LocalTime lt = LocalTime.of(time >> 11, time >> 5 & 0x3f, (time & 0x1f) << 1);
        return LocalDateTime.of(ld, lt).atZone(ZoneId.systemDefault()).toInstant();

    }

    /**
     * {@return the last modification time of the entry at the given index}
     * The time is determined by checking for NTFS and Unix extended attributes in the entry's extra field data.
     * If no extended attributes are present, the modification date and time from the central directory entry
     * are used as a fallback, interpreted in the system default time zone.
     *
     * @param idx the entry index
     */
    public Instant modifiedTime(final long idx) {
        // first check for extended attributes
        long cde = index.cdeOffset(idx);
        int fnLen = data.cdeFileNameLength(cde);
        int efl = data.cdeExtraFieldLength(cde);
        long off = cde + CDE_END + fnLen;
        long endOff = off + efl;
        while (off < endOff) {
            int sig = data.u16le(off);
            int len = data.u16le(off + 2);
            switch (sig) {
                case EX_NTFS -> {
                    // skip 4 reserved bytes, then iterate NTFS subtags
                    long subOff = off + 8;
                    long subEnd = off + 4 + len;
                    while (subOff < subEnd) {
                        int subSig = data.u16le(subOff);
                        int subLen = data.u16le(subOff + 2);
                        if (subSig == 0x0001) {
                            // got our times: mtime is at subtag data offset 0
                            return ntfsFileTimeToInstant(data.s64le(subOff + 4));
                        }
                        subOff += subLen + 4;
                    }
                }
                case EX_UNIX -> {
                    return Instant.ofEpochSecond(data.u32le(off + 8));
                }
            }
            off += len + 4;
        }
        // no match; use the info found in the CDE, such as it is
        int date = data.cdeModDate(cde);
        LocalDate ld = LocalDate.of(1980 + (date >> 9), date >> 5 & 0xf, date & 0x1f);
        int time = data.cdeModTime(cde);
        LocalTime lt = LocalTime.of(time >> 11, time >> 5 & 0x3f, (time & 0x1f) << 1);
        return LocalDateTime.of(ld, lt).atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * Determine whether the entry at the given index represents a directory.
     * An entry is considered a directory if its name ends with {@code '/'}.
     *
     * @param idx the entry index
     * @return {@code true} if the entry is a directory, or {@code false} otherwise
     */
    public boolean isDirectory(final long idx) {
        long cde = index.cdeOffset(idx);
        int len = data.cdeFileNameLength(cde);
        int last = data.u8(cde + CDE_END + len - 1);
        return last == '/';
    }
}
