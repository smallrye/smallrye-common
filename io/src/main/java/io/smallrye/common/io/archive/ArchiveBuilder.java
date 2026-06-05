package io.smallrye.common.io.archive;

import static io.smallrye.common.io.archive.Constants.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import io.smallrye.common.annotation.Experimental;
import io.smallrye.common.constraint.Assert;
import io.smallrye.common.io.BufferedFile;
import io.smallrye.common.io.Files2;

/**
 * A builder for creating ZIP/JAR archive files.
 * <p>
 * The builder writes entries sequentially to the archive.
 * Each file entry returns an {@link OutputStream} that must be closed before another entry can be added.
 * Closing the builder writes the central directory and end-of-central-directory records to complete the archive.
 * <p>
 * The archive is written using a {@link BufferedFile}, which allows random-access patching of CRC-32
 * and size fields in local file headers after entry data has been written.
 * All file names are encoded as UTF-8 (general purpose bit 11 is always set).
 *
 * <h2>Usage example</h2>
 *
 * <pre>{@code
 * try (ArchiveBuilder builder = ArchiveBuilder.open(path)) {
 *     builder.addDirectory("META-INF/");
 *     try (OutputStream os = builder.addEntry("META-INF/MANIFEST.MF", Set.of(ZipOption.STORED))) {
 *         os.write(manifestBytes);
 *     }
 *     try (OutputStream os = builder.addEntry("com/example/Main.class")) {
 *         os.write(classBytes);
 *     }
 * }
 * }</pre>
 *
 * <h2>Compression</h2>
 * The compression method can be set at the builder level (as a default for all entries) and/or
 * per entry. Per-entry options override the builder default. If no compression option is specified
 * at either level, the default is {@link ZipOption#DEFLATED}.
 *
 * <h2>ZIP64 support</h2>
 * By default, entries are written in standard ZIP format. If any single entry may exceed 4 GB,
 * pass {@link ZipOption#ZIP64} to the {@link #open(Path, Collection, FileAttribute[])} factory method
 * (as a builder-level default) or to
 * {@link #addEntry(String, Collection, FileAttribute[])} (for individual entries)
 * to reserve ZIP64 extended information in the local file header.
 * The central directory and end-of-central-directory records will use ZIP64 format when necessary,
 * regardless of this option.
 *
 * <h2>Thread safety</h2>
 * This class is not thread-safe.
 *
 * @see Archive
 * @see ZipOption
 */
@Experimental("Incubating archive API")
public final class ArchiveBuilder implements Closeable {

    private static final Instant NTFS_EPOCH = Instant.from(ZonedDateTime.of(1601, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC));

    private final BufferedFile file;
    private final int defaultMethod;
    private final boolean defaultZip64;
    private final List<CdEntry> entries = new ArrayList<>();
    private Deflater deflater;
    private CRC32 crc;
    private Closeable activeEntry;
    private boolean closed;

    private ArchiveBuilder(BufferedFile file, int defaultMethod, boolean defaultZip64) {
        this.file = file;
        this.defaultMethod = defaultMethod;
        this.defaultZip64 = defaultZip64;
    }

    /**
     * Open a new archive builder for writing to the given path.
     * The file is created if it does not exist.
     * If the file already exists, its previous contents are overwritten.
     * This is equivalent to calling {@link #open(Path, Collection, FileAttribute...)} with an empty options set.
     *
     * @param path the path of the archive file to create (must not be {@code null})
     * @return a new archive builder (not {@code null})
     * @throws IOException if an I/O error occurs
     */
    public static ArchiveBuilder open(Path path) throws IOException {
        return open(path, Set.of());
    }

    /**
     * Open a new archive builder for writing to the given path.
     * The file is created if it does not exist.
     * If the file already exists, its previous contents are overwritten.
     * The optional file attributes are applied to the created file on the filesystem.
     * This is equivalent to calling {@link #open(Path, Collection, FileAttribute...)} with an empty options set.
     *
     * @param path the path of the archive file to create (must not be {@code null})
     * @param attrs optional file attributes to set on the created file
     * @return a new archive builder (not {@code null})
     * @throws IOException if an I/O error occurs
     */
    public static ArchiveBuilder open(Path path, FileAttribute<?>... attrs) throws IOException {
        return open(path, Set.of(), attrs);
    }

    /**
     * Open a new archive builder for writing to the given path, with the given options.
     * See {@link #open(Path, Collection, FileAttribute...)} for details on how options are handled.
     *
     * @param path the path of the archive file to create (must not be {@code null})
     * @param options the open options (must not be {@code null}); may contain {@link ZipOption} values
     *        and/or {@link StandardOpenOption} values
     * @return a new archive builder (not {@code null})
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if an unsupported option is specified
     */
    public static ArchiveBuilder open(Path path, OpenOption... options) throws IOException {
        return open(path, Set.of(options));
    }

    /**
     * Open a new archive builder for writing to the given path, with the given options.
     * The file is always opened for both reading and writing.
     * By default, the file is created if it does not exist, and its previous contents (if any) are overwritten.
     * <p>
     * The following {@link StandardOpenOption} values are accepted:
     * <ul>
     * <li>{@link StandardOpenOption#CREATE_NEW CREATE_NEW} &mdash; create the file atomically;
     * an exception is thrown if the file already exists</li>
     * <li>{@link StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} &mdash; truncate the file
     * to zero length when opened (this is the default behavior when {@code CREATE_NEW} is not given)</li>
     * <li>{@link StandardOpenOption#CREATE CREATE}, {@link StandardOpenOption#READ READ},
     * {@link StandardOpenOption#WRITE WRITE} &mdash; accepted silently (these behaviors are always implied)</li>
     * </ul>
     * {@link ZipOption} values may also be specified to set builder-level defaults.
     * The optional file attributes are applied to the created file on the filesystem.
     *
     * @param path the path of the archive file to create (must not be {@code null})
     * @param options the open options (must not be {@code null}); may contain {@link ZipOption} values
     *        and/or {@link StandardOpenOption} values
     * @param attrs optional file attributes to set on the created file
     * @return a new archive builder (not {@code null})
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if an unsupported option is specified
     */
    public static ArchiveBuilder open(Path path, Collection<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException {
        Assert.checkNotNullParam("path", path);
        Assert.checkNotNullParam("options", options);
        boolean createNew = false;
        boolean truncate = false;
        for (OpenOption opt : options) {
            if (opt instanceof ZipOption zo) {
                switch (zo) {
                    case STORED, DEFLATED, ZIP64 -> {
                    }
                }
            } else if (opt instanceof StandardOpenOption soo) {
                switch (soo) {
                    case READ, WRITE, CREATE -> {
                        // always implied
                    }
                    case CREATE_NEW -> createNew = true;
                    case TRUNCATE_EXISTING -> truncate = true;
                    default -> throw new UnsupportedOperationException("Unsupported option: " + opt);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported option: " + opt);
            }
        }
        EnumSet<StandardOpenOption> fileOptions = EnumSet.of(StandardOpenOption.READ, StandardOpenOption.WRITE,
                StandardOpenOption.CREATE);
        if (createNew) {
            fileOptions.add(StandardOpenOption.CREATE_NEW);
        } else {
            // default: truncate existing content so stale data does not trail the new archive
            fileOptions.add(StandardOpenOption.TRUNCATE_EXISTING);
        }
        if (truncate) {
            fileOptions.add(StandardOpenOption.TRUNCATE_EXISTING);
        }
        BufferedFile bf = Files2.openBuffered(path, fileOptions, List.of(attrs));
        return openTrusted(bf, options);
    }

    /**
     * Open a new archive builder for writing to an existing {@link BufferedFile}.
     * The file must be open for both reading and writing.
     * When the builder is {@linkplain #close() closed}, the central directory is written
     * but the {@code BufferedFile} itself is <em>not</em> closed.
     * The {@code BufferedFile} may not be used until the builder is closed, because the builder
     * opens a nested file on it.
     * <p>
     * {@link ZipOption} values may be specified to set builder-level defaults.
     * {@link StandardOpenOption#CREATE CREATE}, {@link StandardOpenOption#CREATE_NEW CREATE_NEW},
     * {@link StandardOpenOption#READ READ}, {@link StandardOpenOption#WRITE WRITE}, and
     * {@link StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} are accepted but ignored,
     * since the file is already open.
     *
     * @param file the buffered file to write the archive to (must not be {@code null})
     * @param options the options (must not be {@code null}); may contain {@link ZipOption} values
     * @return a new archive builder (not {@code null})
     * @throws UnsupportedOperationException if an unsupported option is specified
     */
    public static ArchiveBuilder open(BufferedFile file, OpenOption... options) throws IOException {
        return open(file, Set.of(options));
    }

    /**
     * Open a new archive builder for writing to an existing {@link BufferedFile}.
     * The file must be open for both reading and writing.
     * When the builder is {@linkplain #close() closed}, the central directory is written
     * but the {@code BufferedFile} itself is <em>not</em> closed.
     * <p>
     * {@link ZipOption} values may be specified to set builder-level defaults.
     * {@link StandardOpenOption#CREATE CREATE}, {@link StandardOpenOption#CREATE_NEW CREATE_NEW},
     * {@link StandardOpenOption#READ READ}, {@link StandardOpenOption#WRITE WRITE}, and
     * {@link StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} are accepted but ignored,
     * since the file is already open.
     *
     * @param file the buffered file to write the archive to (must not be {@code null})
     * @param options the options (must not be {@code null}); may contain {@link ZipOption} values
     * @return a new archive builder (not {@code null})
     * @throws UnsupportedOperationException if an unsupported option is specified
     */
    public static ArchiveBuilder open(BufferedFile file, Collection<? extends OpenOption> options) throws IOException {
        return openTrusted(Assert.checkNotNullParam("file", file).openNested(),
                Assert.checkNotNullParam("options", options));
    }

    private static ArchiveBuilder openTrusted(final BufferedFile file, final Collection<? extends OpenOption> options) {
        int method = -1;
        boolean zip64 = false;
        for (OpenOption opt : options) {
            if (opt instanceof ZipOption zo) {
                switch (zo) {
                    case STORED -> method = setMethod(method, METHOD_STORED);
                    case DEFLATED -> method = setMethod(method, METHOD_DEFLATE);
                    case ZIP64 -> zip64 = true;
                }
            } else if (opt instanceof StandardOpenOption soo) {
                switch (soo) {
                    case READ, WRITE, CREATE, CREATE_NEW, TRUNCATE_EXISTING -> {
                        // ignored; file is already open
                    }
                    default -> throw new UnsupportedOperationException("Unsupported option: " + opt);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported option: " + opt);
            }
        }
        return new ArchiveBuilder(file, method == -1 ? METHOD_DEFLATE : method, zip64);
    }

    /**
     * Add a directory entry to the archive.
     * A trailing {@code '/'} is appended to the name if not already present.
     * Directory entries are always stored with the {@link ZipOption#STORED STORED} method,
     * zero CRC-32, and zero size.
     *
     * @param name the directory name (must not be {@code null})
     * @param attrs optional file attributes for the entry (e.g., POSIX permissions, timestamps)
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if an entry output stream is still open, or this builder is closed
     */
    public void addDirectory(String name, FileAttribute<?>... attrs) throws IOException {
        Assert.checkNotNullParam("name", name);
        checkOpen();
        checkNoActiveEntry();
        if (!name.endsWith("/")) {
            name = name + "/";
        }

        CdEntry entry = new CdEntry();
        entry.fileName = name.getBytes(StandardCharsets.UTF_8);
        entry.method = METHOD_STORED;
        parseEntryAttributes(entry, attrs);
        entry.externalAttributes |= S_IFDIR << 16;
        entry.localHeaderOffset = file.filePosition();

        writeLfh(entry, false);

        entries.add(entry);
    }

    /**
     * Add a file entry to the archive using the builder's default options and no file attributes.
     * The returned output stream must be closed before another entry can be added or the archive can be closed.
     *
     * @param name the entry name (must not be {@code null})
     * @return an output stream to which the entry data should be written (not {@code null})
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if an entry output stream is still open, or this builder is closed
     */
    public OutputStream addEntry(String name) throws IOException {
        return addEntry(name, Set.of());
    }

    /**
     * Add a file entry to the archive with the given options and no file attributes.
     * Options given here override the builder-level defaults for this entry.
     * The returned output stream must be closed before another entry can be added or the archive can be closed.
     *
     * @param name the entry name (must not be {@code null})
     * @param options the entry options; may contain {@link ZipOption} values
     * @return an output stream to which the entry data should be written (not {@code null})
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if an entry output stream is still open, or this builder is closed
     * @throws IllegalArgumentException if conflicting compression options are specified
     * @throws UnsupportedOperationException if an unsupported option is specified
     */
    public OutputStream addEntry(String name, OpenOption... options) throws IOException {
        return addEntry(name, List.of(options));
    }

    /**
     * Add a file entry to the archive with the given options.
     * Options given here override the builder-level defaults for this entry.
     * The returned output stream must be closed before another entry can be added or the archive can be closed.
     *
     * @param name the entry name (must not be {@code null})
     * @param attrs optional file attributes for the entry (e.g., POSIX permissions, timestamps)
     * @return an output stream to which the entry data should be written (not {@code null})
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if an entry output stream is still open, or this builder is closed
     * @throws IllegalArgumentException if conflicting compression options are specified
     * @throws UnsupportedOperationException if an unsupported option is specified
     */
    public OutputStream addEntry(String name, FileAttribute<?>... attrs) throws IOException {
        return addEntry(name, Set.of(), attrs);
    }

    /**
     * Add a file entry to the archive with the given options.
     * Options given here override the builder-level defaults for this entry.
     * The returned output stream must be closed before another entry can be added or the archive can be closed.
     * <p>
     * {@link StandardOpenOption#CREATE CREATE}, {@link StandardOpenOption#CREATE_NEW CREATE_NEW},
     * {@link StandardOpenOption#READ READ}, {@link StandardOpenOption#WRITE WRITE}, and
     * {@link StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} are accepted but ignored,
     * since entries are always created within the archive.
     *
     * @param name the entry name (must not be {@code null})
     * @param options the entry options (must not be {@code null}); may contain {@link ZipOption} values
     * @param attrs optional file attributes for the entry (e.g., POSIX permissions, timestamps)
     * @return an output stream to which the entry data should be written (not {@code null})
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if an entry output stream is still open, or this builder is closed
     * @throws IllegalArgumentException if conflicting compression options are specified
     * @throws UnsupportedOperationException if an unsupported option is specified
     */
    public OutputStream addEntry(String name, Collection<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("options", options);
        checkOpen();
        checkNoActiveEntry();

        // resolve per-entry options against builder defaults
        int method = -1;
        boolean zip64 = false;
        for (OpenOption opt : options) {
            if (opt instanceof ZipOption zo) {
                switch (zo) {
                    case STORED -> method = setMethod(method, METHOD_STORED);
                    case DEFLATED -> method = setMethod(method, METHOD_DEFLATE);
                    case ZIP64 -> zip64 = true;
                }
            } else if (opt instanceof StandardOpenOption soo) {
                switch (soo) {
                    case CREATE, CREATE_NEW, READ, WRITE, TRUNCATE_EXISTING -> {
                        // ignored; entries are always created
                    }
                    default -> throw new UnsupportedOperationException("Unsupported option: " + opt);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported option: " + opt);
            }
        }
        if (method == -1) {
            method = defaultMethod;
        }
        if (!zip64) {
            zip64 = defaultZip64;
        }

        CdEntry entry = new CdEntry();
        entry.fileName = name.getBytes(StandardCharsets.UTF_8);
        entry.method = method;
        entry.zip64 = zip64;
        parseEntryAttributes(entry, attrs);
        entry.externalAttributes |= S_IFREG << 16;
        entry.localHeaderOffset = file.filePosition();

        writeLfh(entry, zip64);

        entries.add(entry);

        EntryOutputStream eos = new EntryOutputStream(entry);
        activeEntry = eos;
        return eos;
    }

    /**
     * Add a file entry to the archive that is backed by a nested {@link BufferedFile} rather than
     * an output stream, using the builder's default options and no file attributes.
     * The returned file allows random-access writing of the entry data.
     * The entry is always stored with the {@link ZipOption#STORED STORED} method because
     * random-access writing is incompatible with compression.
     * The returned file must be {@linkplain BufferedFile#close() closed} before another entry
     * can be added or the archive can be closed.
     * When closed, the CRC-32 is computed from the written data and the local file header
     * is patched with the final CRC and size values.
     *
     * @param name the entry name (must not be {@code null})
     * @return the nested file for writing entry data (not {@code null})
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if an entry is still being written, or this builder is closed
     */
    public BufferedFile addBufferedEntry(String name) throws IOException {
        return addBufferedEntry(name, Set.of());
    }

    /**
     * Add a file entry to the archive that is backed by a nested {@link BufferedFile} rather than
     * an output stream.
     * The returned file allows random-access writing of the entry data.
     * The entry is always stored with the {@link ZipOption#STORED STORED} method because
     * random-access writing is incompatible with compression.
     * The returned file must be {@linkplain BufferedFile#close() closed} before another entry
     * can be added or the archive can be closed.
     * When closed, the CRC-32 is computed from the written data and the local file header
     * is patched with the final CRC and size values.
     *
     * @param name the entry name (must not be {@code null})
     * @param options the entry options; may contain {@link ZipOption#ZIP64}
     * @return the nested file for writing entry data (not {@code null})
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if an entry is still being written, or this builder is closed
     * @throws IllegalArgumentException if {@link ZipOption#DEFLATED} is specified
     * @throws UnsupportedOperationException if an unsupported option is specified
     */
    public BufferedFile addBufferedEntry(String name, OpenOption... options) throws IOException {
        return addBufferedEntry(name, List.of(options));
    }

    /**
     * Add a file entry to the archive that is backed by a nested {@link BufferedFile} rather than
     * an output stream.
     * The returned file allows random-access writing of the entry data.
     * The entry is always stored with the {@link ZipOption#STORED STORED} method because
     * random-access writing is incompatible with compression.
     * The returned file must be {@linkplain BufferedFile#close() closed} before another entry
     * can be added or the archive can be closed.
     * When closed, the CRC-32 is computed from the written data and the local file header
     * is patched with the final CRC and size values.
     *
     * @param name the entry name (must not be {@code null})
     * @param attrs optional file attributes for the entry (e.g., POSIX permissions, timestamps)
     * @return the nested file for writing entry data (not {@code null})
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if an entry is still being written, or this builder is closed
     */
    public BufferedFile addBufferedEntry(String name, FileAttribute<?>... attrs) throws IOException {
        return addBufferedEntry(name, Set.of(), attrs);
    }

    /**
     * Add a file entry to the archive that is backed by a nested {@link BufferedFile} rather than
     * an output stream.
     * The returned file allows random-access writing of the entry data.
     * The entry is always stored with the {@link ZipOption#STORED STORED} method because
     * random-access writing is incompatible with compression.
     * The returned file must be {@linkplain BufferedFile#close() closed} before another entry
     * can be added or the archive can be closed.
     * When closed, the CRC-32 is computed from the written data and the local file header
     * is patched with the final CRC and size values.
     * <p>
     * {@link StandardOpenOption#CREATE CREATE}, {@link StandardOpenOption#CREATE_NEW CREATE_NEW},
     * {@link StandardOpenOption#READ READ}, {@link StandardOpenOption#WRITE WRITE}, and
     * {@link StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} are accepted but ignored,
     * since entries are always created within the archive.
     *
     * @param name the entry name (must not be {@code null})
     * @param options the entry options (must not be {@code null}); may contain {@link ZipOption#ZIP64}
     * @param attrs optional file attributes for the entry (e.g., POSIX permissions, timestamps)
     * @return the nested file for writing entry data (not {@code null})
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if an entry is still being written, or this builder is closed
     * @throws IllegalArgumentException if {@link ZipOption#DEFLATED} is specified
     * @throws UnsupportedOperationException if an unsupported option is specified
     */
    public BufferedFile addBufferedEntry(String name, Collection<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("options", options);
        checkOpen();
        checkNoActiveEntry();

        boolean zip64 = false;
        for (OpenOption opt : options) {
            if (opt instanceof ZipOption zo) {
                switch (zo) {
                    case STORED -> {
                    }
                    case DEFLATED -> throw new IllegalArgumentException(
                            "Buffered entries require STORED method; DEFLATED is incompatible with random-access writing");
                    case ZIP64 -> zip64 = true;
                }
            } else if (opt instanceof StandardOpenOption soo) {
                switch (soo) {
                    case CREATE, CREATE_NEW, READ, WRITE, TRUNCATE_EXISTING -> {
                        // ignored; entries are always created
                    }
                    default -> throw new UnsupportedOperationException("Unsupported option: " + opt);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported option: " + opt);
            }
        }
        if (!zip64) {
            zip64 = defaultZip64;
        }

        CdEntry entry = new CdEntry();
        entry.fileName = name.getBytes(StandardCharsets.UTF_8);
        entry.method = METHOD_STORED;
        entry.zip64 = zip64;
        parseEntryAttributes(entry, attrs);
        entry.externalAttributes |= S_IFREG << 16;
        entry.localHeaderOffset = file.filePosition();

        writeLfh(entry, zip64);

        entries.add(entry);

        BufferedFile nested = file.openNested(() -> {
            // compute entry data boundaries from the LFH layout
            long dataStart = entry.localHeaderOffset + LH_END + entry.fileName.length
                    + lfhExtraFieldLength(entry.zip64);
            long size = file.filePosition() - dataStart;

            // check for overflow when ZIP64 was not reserved
            if (!entry.zip64 && size > 0xFFFF_FFFEL) {
                throw new IOException("Entry size exceeds 4 GB; use ZipOption.ZIP64");
            }

            // compute CRC by reading back the written data from the parent
            CRC32 c = crc();
            byte[] buf = new byte[8192];
            long pos = dataStart;
            long remaining = size;
            while (remaining > 0) {
                int n = (int) Math.min(buf.length, remaining);
                file.read(pos, buf, 0, n);
                c.update(buf, 0, n);
                pos += n;
                remaining -= n;
            }
            int crcValue = (int) c.getValue();
            c.reset();

            // patch local file header: CRC-32, compressed size, uncompressed size
            long lfhOffset = entry.localHeaderOffset;
            file.writeIntLE(lfhOffset + LH_CRC_32, crcValue);
            if (entry.zip64) {
                long extraStart = lfhOffset + LH_END + entry.fileName.length;
                file.writeLongLE(extraStart + 4, size);
                file.writeLongLE(extraStart + 12, size);
            } else {
                file.writeIntLE(lfhOffset + LH_COMPRESSED_SIZE, (int) size);
                file.writeIntLE(lfhOffset + LH_UNCOMPRESSED_SIZE, (int) size);
            }

            // record final values on the central directory entry
            entry.crc32 = crcValue;
            entry.compressedSize = size;
            entry.uncompressedSize = size;

            // release active entry
            activeEntry = null;
        });
        activeEntry = nested;
        return nested;
    }

    /**
     * Add a nested archive as a STORED entry in this archive.
     * This is a convenience method that combines {@link #addBufferedEntry(String)} and
     * {@link #open(BufferedFile, OpenOption...)} into a single call.
     * The returned archive builder writes to a nested region of the outer archive file.
     * When the returned builder is {@linkplain #close() closed}, its central directory is written,
     * then the enclosing entry's CRC-32 and size are computed and patched into the outer
     * local file header.
     * <p>
     * The nested builder must be closed before another entry can be added to the outer builder
     * or the outer builder can be closed.
     *
     * <h4>Usage example</h4>
     *
     * <pre>{@code
     * try (ArchiveBuilder outer = ArchiveBuilder.open(path)) {
     *     try (ArchiveBuilder inner = outer.addArchive("lib/nested.jar")) {
     *         try (OutputStream os = inner.addEntry("hello.txt")) {
     *             os.write(data);
     *         }
     *     }
     * }
     * }</pre>
     *
     * @param name the entry name for the nested archive (must not be {@code null})
     * @return a new archive builder for the nested archive (not {@code null})
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if an entry is still being written, or this builder is closed
     */
    public ArchiveBuilder addArchive(String name) throws IOException {
        return addArchive(name, Set.of());
    }

    /**
     * Add a nested archive as a STORED entry in this archive with the given options.
     * This is a convenience method that combines {@link #addBufferedEntry(String, OpenOption...)} and
     * {@link #open(BufferedFile, OpenOption...)} into a single call.
     * The returned archive builder writes to a nested region of the outer archive file.
     * When the returned builder is {@linkplain #close() closed}, its central directory is written,
     * then the enclosing entry's CRC-32 and size are computed and patched into the outer
     * local file header.
     * <p>
     * The nested builder must be closed before another entry can be added to the outer builder
     * or the outer builder can be closed.
     * <p>
     * {@link ZipOption#ZIP64} applies to the outer entry (reserving ZIP64 space in the outer local
     * file header). The inner builder inherits the same options as its defaults.
     *
     * @param name the entry name for the nested archive (must not be {@code null})
     * @param options the options; may contain {@link ZipOption} values
     * @return a new archive builder for the nested archive (not {@code null})
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if an entry is still being written, or this builder is closed
     * @throws UnsupportedOperationException if an unsupported option is specified
     */
    public ArchiveBuilder addArchive(String name, OpenOption... options) throws IOException {
        return addArchive(name, List.of(options));
    }

    /**
     * Add a nested archive as a STORED entry in this archive with the given file attributes.
     * This is a convenience method that combines
     * {@link #addBufferedEntry(String, FileAttribute[])} and
     * {@link #open(BufferedFile, OpenOption...)} into a single call.
     * The returned archive builder writes to a nested region of the outer archive file.
     * When the returned builder is {@linkplain #close() closed}, its central directory is written,
     * then the enclosing entry's CRC-32 and size are computed and patched into the outer
     * local file header.
     * <p>
     * The nested builder must be closed before another entry can be added to the outer builder
     * or the outer builder can be closed.
     *
     * @param name the entry name for the nested archive (must not be {@code null})
     * @param attrs optional file attributes for the outer entry (e.g., POSIX permissions, timestamps)
     * @return a new archive builder for the nested archive (not {@code null})
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if an entry is still being written, or this builder is closed
     */
    public ArchiveBuilder addArchive(String name, FileAttribute<?>... attrs) throws IOException {
        return addArchive(name, Set.of(), attrs);
    }

    /**
     * Add a nested archive as a STORED entry in this archive with the given options and
     * file attributes.
     * This is a convenience method that combines
     * {@link #addBufferedEntry(String, Collection, FileAttribute[])} and
     * {@link #open(BufferedFile, OpenOption...)} into a single call.
     * The returned archive builder writes to a nested region of the outer archive file.
     * When the returned builder is {@linkplain #close() closed}, its central directory is written,
     * then the enclosing entry's CRC-32 and size are computed and patched into the outer
     * local file header.
     * <p>
     * The nested builder must be closed before another entry can be added to the outer builder
     * or the outer builder can be closed.
     * <p>
     * {@link ZipOption#ZIP64} applies to the outer entry (reserving ZIP64 space in the outer local
     * file header). The inner builder inherits the same options as its defaults.
     * {@link ZipOption#DEFLATED} is permitted here (unlike {@code addBufferedEntry}) and sets
     * the inner builder's default compression method; the outer entry is always STORED.
     * <p>
     * {@link StandardOpenOption#CREATE CREATE}, {@link StandardOpenOption#CREATE_NEW CREATE_NEW},
     * {@link StandardOpenOption#READ READ}, {@link StandardOpenOption#WRITE WRITE}, and
     * {@link StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} are accepted but ignored,
     * since entries are always created within the archive.
     *
     * @param name the entry name for the nested archive (must not be {@code null})
     * @param options the options (must not be {@code null}); may contain {@link ZipOption} values
     * @param attrs optional file attributes for the outer entry (e.g., POSIX permissions, timestamps)
     * @return a new archive builder for the nested archive (not {@code null})
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if an entry is still being written, or this builder is closed
     * @throws UnsupportedOperationException if an unsupported option is specified
     */
    public ArchiveBuilder addArchive(String name, Collection<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("options", options);

        // parse options: ZIP64 applies to outer entry; STORED/DEFLATED set the inner default
        int innerMethod = -1;
        boolean zip64 = false;
        for (OpenOption opt : options) {
            if (opt instanceof ZipOption zo) {
                switch (zo) {
                    case STORED -> innerMethod = setMethod(innerMethod, METHOD_STORED);
                    case DEFLATED -> innerMethod = setMethod(innerMethod, METHOD_DEFLATE);
                    case ZIP64 -> zip64 = true;
                }
            } else if (opt instanceof StandardOpenOption soo) {
                switch (soo) {
                    case CREATE, CREATE_NEW, READ, WRITE, TRUNCATE_EXISTING -> {
                        // ignored; entries are always created
                    }
                    default -> throw new UnsupportedOperationException("Unsupported option: " + opt);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported option: " + opt);
            }
        }

        // the outer entry needs only ZIP64 (method is always STORED for buffered entries)
        Collection<OpenOption> outerOptions = zip64 ? Set.of(ZipOption.ZIP64) : Set.of();
        BufferedFile nested = addBufferedEntry(name, outerOptions, attrs);

        // create the inner builder; ownsFile = true so closing it closes the nested file,
        // which triggers the close action for CRC/LFH patching on the outer entry
        return new ArchiveBuilder(nested, innerMethod == -1 ? METHOD_DEFLATE : innerMethod, zip64);
    }

    /**
     * Close this archive builder, writing the central directory and end-of-central-directory records.
     *
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if an entry is still being written
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        if (activeEntry != null) {
            throw new IllegalStateException("An entry is still being written");
        }
        closed = true;
        try {
            writeCentralDirectory();
        } finally {
            file.close();
            if (deflater != null) {
                deflater.end();
            }
        }
    }

    // ── Private helpers ─────────────────────────────────────────────────

    /**
     * Checks that this builder has not been closed.
     */
    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("ArchiveBuilder is closed");
        }
    }

    /**
     * Set the compression method, throwing if a conflicting method was already set.
     *
     * @param current the current method ({@code -1} if not yet set)
     * @param requested the requested method
     * @return the requested method
     * @throws IllegalArgumentException if a different method was already set
     */
    private static int setMethod(int current, int requested) {
        if (current != -1 && current != requested) {
            throw new IllegalArgumentException("Conflicting compression options: STORED and DEFLATED");
        }
        return requested;
    }

    /**
     * Checks that no entry (stream or buffered) is currently being written.
     */
    private void checkNoActiveEntry() {
        if (activeEntry != null) {
            throw new IllegalStateException("An entry is still being written");
        }
    }

    /**
     * {@return the lazily allocated Deflater instance}
     */
    private Deflater deflater() {
        Deflater d = deflater;
        if (d == null) {
            // raw deflate (no zlib header/trailer) matches ZIP spec
            d = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
            deflater = d;
        }
        return d;
    }

    /**
     * {@return the lazily allocated CRC32 instance}
     */
    private CRC32 crc() {
        CRC32 c = crc;
        if (c == null) {
            c = new CRC32();
            crc = c;
        }
        return c;
    }

    // ── POSIX permission bit constants ──────────────────────────────────

    @SuppressWarnings("OctalInteger")
    private static final int S_IFREG = 0100000;
    @SuppressWarnings("OctalInteger")
    private static final int S_IFDIR = 0040000;
    @SuppressWarnings("OctalInteger")
    private static final int S_IRUSR = 0400;
    @SuppressWarnings("OctalInteger")
    private static final int S_IWUSR = 0200;
    @SuppressWarnings("OctalInteger")
    private static final int S_IXUSR = 0100;
    @SuppressWarnings("OctalInteger")
    private static final int S_IRGRP = 040;
    @SuppressWarnings("OctalInteger")
    private static final int S_IWGRP = 020;
    @SuppressWarnings("OctalInteger")
    private static final int S_IXGRP = 010;
    @SuppressWarnings("OctalInteger")
    private static final int S_IROTH = 04;
    @SuppressWarnings("OctalInteger")
    private static final int S_IWOTH = 02;
    @SuppressWarnings("OctalInteger")
    private static final int S_IXOTH = 01;

    // ── FileAttribute parsing ───────────────────────────────────────────

    /**
     * Parse the given file attributes and populate the entry's DOS time, NTFS timestamps,
     * and external attributes fields.
     * If no modification time is specified, the current time is used for the DOS fields.
     *
     * @param entry the entry to populate
     * @param attrs the file attributes to parse
     */
    @SuppressWarnings("unchecked")
    private static void parseEntryAttributes(CdEntry entry, FileAttribute<?>[] attrs) {
        int unixMode = 0;
        boolean hasPerms = false;
        FileTime lastModifiedTime = null;
        FileTime creationTime = null;
        FileTime lastAccessTime = null;

        for (FileAttribute<?> attr : attrs) {
            switch (attr.name()) {
                case "posix:permissions" -> {
                    Set<PosixFilePermission> perms = (Set<PosixFilePermission>) attr.value();
                    unixMode = posixPermissionsToMode(perms);
                    hasPerms = true;
                }
                case "basic:lastModifiedTime" -> lastModifiedTime = (FileTime) attr.value();
                case "basic:creationTime" -> creationTime = (FileTime) attr.value();
                case "basic:lastAccessTime" -> lastAccessTime = (FileTime) attr.value();
                default -> throw new UnsupportedOperationException("Unsupported file attribute: " + attr.name());
            }
        }

        // compute DOS date/time from modification time or "now"
        Instant modTime = lastModifiedTime != null ? lastModifiedTime.toInstant() : Instant.now();
        LocalDateTime ldt = LocalDateTime.ofInstant(modTime, ZoneId.systemDefault());
        entry.dosDate = ldt.getYear() - 1980 << 9 | ldt.getMonthValue() << 5 | ldt.getDayOfMonth();
        entry.dosTime = ldt.getHour() << 11 | ldt.getMinute() << 5 | ldt.getSecond() / 2;

        // convert timestamps to NTFS FILETIMEs, defaulting missing ones to the modification time
        long modFt = instantToNtfsFileTime(modTime);
        entry.ntfsMtime = lastModifiedTime != null ? instantToNtfsFileTime(lastModifiedTime.toInstant()) : modFt;
        entry.ntfsAtime = lastAccessTime != null ? instantToNtfsFileTime(lastAccessTime.toInstant()) : modFt;
        entry.ntfsCtime = creationTime != null ? instantToNtfsFileTime(creationTime.toInstant()) : modFt;

        if (hasPerms) {
            entry.externalAttributes = unixMode << 16;
        }
    }

    /**
     * Convert a set of POSIX file permissions to a UNIX mode integer.
     *
     * @param perms the permissions set
     * @return the UNIX mode bits
     */
    private static int posixPermissionsToMode(Set<PosixFilePermission> perms) {
        int mode = 0;
        for (PosixFilePermission perm : perms) {
            mode |= switch (perm) {
                case OWNER_READ -> S_IRUSR;
                case OWNER_WRITE -> S_IWUSR;
                case OWNER_EXECUTE -> S_IXUSR;
                case GROUP_READ -> S_IRGRP;
                case GROUP_WRITE -> S_IWGRP;
                case GROUP_EXECUTE -> S_IXGRP;
                case OTHERS_READ -> S_IROTH;
                case OTHERS_WRITE -> S_IWOTH;
                case OTHERS_EXECUTE -> S_IXOTH;
            };
        }
        return mode;
    }

    // ── Extra field writing ────────────────────────────────────────────

    /**
     * Compute the total extra field length for a local file header.
     *
     * @param zip64 whether ZIP64 extended information is included
     * @return the total extra field length in bytes
     */
    private static int lfhExtraFieldLength(boolean zip64) {
        return NTFS_EXTRA_TOTAL_SIZE + (zip64 ? ZIP64_LFH_EXTRA_TOTAL_SIZE : 0);
    }

    /**
     * Compute the total extra field length for a central directory entry.
     *
     * @param zip64 whether ZIP64 extended information is included
     * @return the total extra field length in bytes
     */
    private static int cdeExtraFieldLength(boolean zip64) {
        return NTFS_EXTRA_TOTAL_SIZE + (zip64 ? ZIP64_CDE_EXTRA_TOTAL_SIZE : 0);
    }

    /**
     * Write the LFH extra field data directly to the file.
     * Includes ZIP64 placeholder data and/or NTFS timestamp data as appropriate.
     *
     * @param entry the entry
     * @param zip64 whether to include ZIP64 extended information (with placeholder sizes)
     * @throws IOException if an I/O error occurs
     */
    private void writeLfhExtraField(CdEntry entry, boolean zip64) throws IOException {
        if (zip64) {
            file.writeShortLE(EX_ZIP64);
            file.writeShortLE(ZIP64_LFH_EXTRA_DATA_SIZE);
            file.writeLongLE(0L); // uncompressed size placeholder
            file.writeLongLE(0L); // compressed size placeholder
        }
        writeNtfsExtraField(entry.ntfsMtime, entry.ntfsAtime, entry.ntfsCtime);
    }

    /**
     * Write the CDE extra field data directly to the file.
     * Includes ZIP64 data with actual sizes/offset and NTFS timestamp data.
     *
     * @param entry the entry
     * @param zip64 whether to include ZIP64 extended information
     * @throws IOException if an I/O error occurs
     */
    private void writeCdeExtraField(CdEntry entry, boolean zip64) throws IOException {
        if (zip64) {
            file.writeShortLE(EX_ZIP64);
            file.writeShortLE(ZIP64_CDE_EXTRA_DATA_SIZE);
            file.writeLongLE(entry.uncompressedSize);
            file.writeLongLE(entry.compressedSize);
            file.writeLongLE(entry.localHeaderOffset);
        }
        writeNtfsExtraField(entry.ntfsMtime, entry.ntfsAtime, entry.ntfsCtime);
    }

    /**
     * Write an NTFS extra field (ID 0x000a) with timestamp data directly to the file.
     *
     * @param mtime the modification time as an NTFS FILETIME
     * @param atime the access time as an NTFS FILETIME
     * @param ctime the creation time as an NTFS FILETIME
     * @throws IOException if an I/O error occurs
     */
    private void writeNtfsExtraField(long mtime, long atime, long ctime) throws IOException {
        file.writeShortLE(EX_NTFS);
        file.writeShortLE(NTFS_EXTRA_DATA_SIZE);
        file.writeIntLE(0); // reserved
        file.writeShortLE(NTFS_SUBTAG_TIME);
        file.writeShortLE(NTFS_SUBTAG_TIME_DATA_SIZE);
        file.writeLongLE(mtime);
        file.writeLongLE(atime);
        file.writeLongLE(ctime);
    }

    // ── NTFS FILETIME conversion ────────────────────────────────────────

    /**
     * Convert an {@link Instant} to an NTFS FILETIME value (100-nanosecond intervals since 1601-01-01 UTC).
     *
     * @param instant the instant to convert
     * @return the NTFS FILETIME value
     */
    private static long instantToNtfsFileTime(Instant instant) {
        long epochSeconds = instant.getEpochSecond() - NTFS_EPOCH.getEpochSecond();
        long nanoAdjust = instant.getNano() - NTFS_EPOCH.getNano();
        return epochSeconds * 10_000_000L + nanoAdjust / 100L;
    }

    // ── Local file header writing ───────────────────────────────────────

    /**
     * Write a local file header at the current position.
     * The CRC-32 and size fields are written as placeholders (zero, or 0xFFFFFFFF for ZIP64)
     * and patched later by {@link EntryOutputStream#close()}.
     *
     * @param entry the entry whose metadata to write
     * @param zip64 whether to write ZIP64 sentinel values and include a ZIP64 extra field
     * @throws IOException if an I/O error occurs
     */
    private void writeLfh(CdEntry entry, boolean zip64) throws IOException {
        file.writeIntLE(SIG_LH);
        file.writeShortLE(zip64 ? VERSION_NEEDED_ZIP64 : VERSION_NEEDED_DEFAULT);
        file.writeShortLE(GP_UTF_8);
        file.writeShortLE(entry.method);
        file.writeShortLE(entry.dosTime);
        file.writeShortLE(entry.dosDate);
        file.writeIntLE(0); // CRC-32 placeholder
        if (zip64) {
            file.writeIntLE(0xFFFF_FFFF); // compressed size sentinel
            file.writeIntLE(0xFFFF_FFFF); // uncompressed size sentinel
        } else {
            file.writeIntLE(0); // compressed size placeholder
            file.writeIntLE(0); // uncompressed size placeholder
        }
        file.writeShortLE(entry.fileName.length);
        file.writeShortLE(lfhExtraFieldLength(zip64));
        file.write(entry.fileName);
        writeLfhExtraField(entry, zip64);
    }

    // ── Central directory writing ───────────────────────────────────────

    /**
     * Write the central directory, ZIP64 end records (if needed), and end-of-central-directory record.
     *
     * @throws IOException if an I/O error occurs
     */
    private void writeCentralDirectory() throws IOException {
        long cdOffset = file.filePosition();

        boolean needsZip64Eocd = false;
        for (CdEntry entry : entries) {
            boolean entryNeedsZip64 = entry.zip64
                    || entry.compressedSize > 0xFFFF_FFFEL
                    || entry.uncompressedSize > 0xFFFF_FFFEL
                    || entry.localHeaderOffset > 0xFFFF_FFFEL;
            if (entryNeedsZip64) {
                needsZip64Eocd = true;
            }
            writeCde(entry, entryNeedsZip64);
        }

        long cdSize = file.filePosition() - cdOffset;

        if (entries.size() > 0xFFFE || cdOffset > 0xFFFF_FFFEL || cdSize > 0xFFFF_FFFEL) {
            needsZip64Eocd = true;
        }

        if (needsZip64Eocd) {
            writeZip64Eocd(cdOffset, cdSize);
        }
        writeEocd(cdOffset, cdSize, needsZip64Eocd);

        file.flush();
    }

    /**
     * Write a single central directory entry.
     *
     * @param entry the entry record
     * @param zip64 whether to use sentinel values in the standard fields and include ZIP64 extra data
     * @throws IOException if an I/O error occurs
     */
    private void writeCde(CdEntry entry, boolean zip64) throws IOException {
        int versionNeeded = zip64 ? VERSION_NEEDED_ZIP64 : VERSION_NEEDED_DEFAULT;
        file.writeIntLE(SIG_CDE);
        file.writeShortLE((MADE_BY_UNIX << 8) | versionNeeded);
        file.writeShortLE(versionNeeded);
        file.writeShortLE(GP_UTF_8);
        file.writeShortLE(entry.method);
        file.writeShortLE(entry.dosTime);
        file.writeShortLE(entry.dosDate);
        file.writeIntLE(entry.crc32);
        if (zip64) {
            file.writeIntLE(0xFFFF_FFFF); // compressed size sentinel
            file.writeIntLE(0xFFFF_FFFF); // uncompressed size sentinel
        } else {
            file.writeIntLE((int) entry.compressedSize);
            file.writeIntLE((int) entry.uncompressedSize);
        }
        file.writeShortLE(entry.fileName.length);
        file.writeShortLE(cdeExtraFieldLength(zip64));
        file.writeShortLE(0); // comment length
        file.writeShortLE(0); // disk number start
        file.writeShortLE(0); // internal attributes
        file.writeIntLE(entry.externalAttributes);
        if (zip64) {
            file.writeIntLE(0xFFFF_FFFF); // local header offset sentinel
        } else {
            file.writeIntLE((int) entry.localHeaderOffset);
        }
        file.write(entry.fileName);
        writeCdeExtraField(entry, zip64);
    }

    /**
     * Write the ZIP64 end-of-central-directory record and ZIP64 end-of-central-directory locator.
     *
     * @param cdOffset the offset of the central directory
     * @param cdSize the size of the central directory
     * @throws IOException if an I/O error occurs
     */
    private void writeZip64Eocd(long cdOffset, long cdSize) throws IOException {
        long zip64EocdOffset = file.filePosition();

        // ZIP64 end of central directory record
        file.writeIntLE(SIG_EOCD_ZIP64);
        file.writeLongLE(EOCD_ZIP64_END - 12L); // size of remaining record (exclude signature + size fields)
        file.writeShortLE((MADE_BY_UNIX << 8) | VERSION_NEEDED_ZIP64);
        file.writeShortLE(VERSION_NEEDED_ZIP64);
        file.writeIntLE(0); // disk number
        file.writeIntLE(0); // CD first disk number
        file.writeLongLE(entries.size()); // entries on this disk
        file.writeLongLE(entries.size()); // total entries
        file.writeLongLE(cdSize);
        file.writeLongLE(cdOffset);

        // ZIP64 end of central directory locator
        file.writeIntLE(SIG_EOCDL_ZIP64);
        file.writeIntLE(0); // ZIP64 EOCD disk number
        file.writeLongLE(zip64EocdOffset);
        file.writeIntLE(1); // total disks
    }

    /**
     * Write the standard end-of-central-directory record.
     *
     * @param cdOffset the offset of the central directory
     * @param cdSize the size of the central directory
     * @param zip64 whether to use sentinel values in the standard fields
     * @throws IOException if an I/O error occurs
     */
    private void writeEocd(long cdOffset, long cdSize, boolean zip64) throws IOException {
        file.writeIntLE(SIG_EOCD);
        file.writeShortLE(0); // disk number
        file.writeShortLE(0); // CD first disk number
        if (zip64 || entries.size() > 0xFFFE) {
            file.writeShortLE(0xFFFF);
            file.writeShortLE(0xFFFF);
        } else {
            file.writeShortLE(entries.size());
            file.writeShortLE(entries.size());
        }
        if (zip64 || cdSize > 0xFFFF_FFFEL) {
            file.writeIntLE(0xFFFF_FFFF);
        } else {
            file.writeIntLE((int) cdSize);
        }
        if (zip64 || cdOffset > 0xFFFF_FFFEL) {
            file.writeIntLE(0xFFFF_FFFF);
        } else {
            file.writeIntLE((int) cdOffset);
        }
        file.writeShortLE(0); // comment length
    }

    // ── Central directory entry record ──────────────────────────────────

    /**
     * A mutable record holding the information needed to write a central directory entry.
     * Also serves as the target for parsed {@link FileAttribute} values.
     */
    private static final class CdEntry {
        long localHeaderOffset;
        byte[] fileName;
        int method;
        boolean zip64;
        int crc32;
        long compressedSize;
        long uncompressedSize;
        int dosTime;
        int dosDate;
        int externalAttributes;
        long ntfsMtime;
        long ntfsAtime;
        long ntfsCtime;
    }

    // ── Entry output stream ─────────────────────────────────────────────

    /**
     * An output stream that writes entry data to the archive and patches the local file header
     * with the final CRC-32 and size values when closed.
     * <p>
     * For STORED entries, data is written directly to the underlying file.
     * For DEFLATED entries, input is accumulated in a buffer and flushed through the deflater
     * when full, avoiding per-call deflater overhead.
     */
    private final class EntryOutputStream extends OutputStream {
        private static final int BUFFER_SIZE = 8192;

        private final CdEntry entry;
        private long bytesWritten;
        private long compressedBytesWritten;
        private boolean streamClosed;
        // input accumulation buffer for DEFLATE (lazily allocated)
        private byte[] inputBuffer;
        private int inputPos;
        // output buffer for draining the deflater (lazily allocated, separate from input)
        private byte[] outputBuffer;

        EntryOutputStream(CdEntry entry) {
            this.entry = entry;
        }

        @Override
        public void write(int b) throws IOException {
            if (streamClosed) {
                throw new IOException("Stream is closed");
            }
            if (entry.method == METHOD_STORED) {
                crc().update(b);
                file.writeByte(b);
                bytesWritten++;
                compressedBytesWritten++;
            } else {
                byte[] buf = inputBuffer();
                buf[inputPos++] = (byte) b;
                if (inputPos == buf.length) {
                    flushInput();
                }
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (streamClosed) {
                throw new IOException("Stream is closed");
            }
            if (len == 0) {
                return;
            }
            if (entry.method == METHOD_STORED) {
                crc().update(b, off, len);
                file.write(b, off, len);
                bytesWritten += len;
                compressedBytesWritten += len;
            } else {
                byte[] buf = inputBuffer();
                while (len > 0) {
                    int space = buf.length - inputPos;
                    int cnt = Math.min(len, space);
                    System.arraycopy(b, off, buf, inputPos, cnt);
                    inputPos += cnt;
                    off += cnt;
                    len -= cnt;
                    if (inputPos == buf.length) {
                        flushInput();
                    }
                }
            }
        }

        /**
         * {@return the lazily allocated input buffer}
         */
        private byte[] inputBuffer() {
            byte[] buf = inputBuffer;
            if (buf == null) {
                buf = new byte[BUFFER_SIZE];
                inputBuffer = buf;
            }
            return buf;
        }

        /**
         * {@return the lazily allocated deflater output buffer}
         */
        private byte[] outputBuffer() {
            byte[] buf = outputBuffer;
            if (buf == null) {
                buf = new byte[BUFFER_SIZE];
                outputBuffer = buf;
            }
            return buf;
        }

        /**
         * Flush accumulated input through the deflater and write compressed output to the file.
         * Updates the CRC and byte counters.
         *
         * @throws IOException if an I/O error occurs
         */
        private void flushInput() throws IOException {
            if (inputPos == 0) {
                return;
            }
            CRC32 c = crc();
            c.update(inputBuffer, 0, inputPos);
            bytesWritten += inputPos;
            Deflater d = deflater();
            d.setInput(inputBuffer, 0, inputPos);
            inputPos = 0;
            drainDeflater(d);
        }

        /**
         * Drain all available output from the deflater to the file.
         *
         * @param d the deflater
         * @throws IOException if an I/O error occurs
         */
        private void drainDeflater(Deflater d) throws IOException {
            byte[] out = outputBuffer();
            while (!d.needsInput()) {
                // fill `out` from the deflater
                int n = d.deflate(out, 0, out.length);
                if (n > 0) {
                    // write the deflated bytes
                    file.write(out, 0, n);
                    compressedBytesWritten += n;
                }
            }
        }

        @Override
        public void close() throws IOException {
            if (streamClosed) {
                return;
            }
            streamClosed = true;

            if (entry.method == METHOD_DEFLATE) {
                // flush any remaining buffered input, then finish the deflater
                flushInput();
                Deflater d = deflater();
                d.finish();
                byte[] out = outputBuffer();
                while (!d.finished()) {
                    // fill `out` from the deflater
                    int n = d.deflate(out, 0, out.length);
                    if (n > 0) {
                        // write the deflated bytes
                        file.write(out, 0, n);
                        compressedBytesWritten += n;
                    }
                }
                d.reset();
            }

            // check for overflow when ZIP64 was not reserved for this entry
            if (!entry.zip64) {
                if (bytesWritten > 0xFFFF_FFFEL || compressedBytesWritten > 0xFFFF_FFFEL) {
                    throw new IOException(
                            "Entry size exceeds 4 GB; use ZipOption.ZIP64 to write entries larger than 4 GB");
                }
            }

            long endPos = file.filePosition();
            int crcValue = (int) crc().getValue();
            crc().reset();
            long lfhOffset = entry.localHeaderOffset;

            // patch local file header: CRC-32, compressed size, uncompressed size
            file.writeIntLE(lfhOffset + LH_CRC_32, crcValue);
            if (entry.zip64) {
                // standard size fields remain 0xFFFFFFFF; patch ZIP64 extra field
                // ZIP64 is always the first extra field when forceZip64
                long extraStart = lfhOffset + LH_END + entry.fileName.length;
                file.writeLongLE(extraStart + 4, bytesWritten); // uncompressed
                file.writeLongLE(extraStart + 12, compressedBytesWritten); // compressed
            } else {
                file.writeIntLE(lfhOffset + LH_COMPRESSED_SIZE, (int) compressedBytesWritten);
                file.writeIntLE(lfhOffset + LH_UNCOMPRESSED_SIZE, (int) bytesWritten);
            }

            // seek back to end
            file.seek(endPos);

            // record final values
            entry.crc32 = crcValue;
            entry.compressedSize = compressedBytesWritten;
            entry.uncompressedSize = bytesWritten;

            // release active entry
            activeEntry = null;
        }
    }
}
