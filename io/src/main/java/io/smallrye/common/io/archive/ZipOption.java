package io.smallrye.common.io.archive;

import java.nio.file.OpenOption;

import io.smallrye.common.annotation.Experimental;

/**
 * Options that control the behavior of archive creation.
 * These options may be specified at the builder level (as defaults) and/or per entry (as overrides).
 * <p>
 * Compression options ({@link #STORED}, {@link #DEFLATED}) are mutually exclusive.
 * If neither is specified, the default is {@link #DEFLATED}.
 * A compression option given to an entry overrides the builder-level default.
 *
 * @see ArchiveBuilder#open(java.nio.file.Path, java.util.Collection, java.nio.file.attribute.FileAttribute[])
 * @see ArchiveBuilder#addEntry(String, java.util.Collection, java.nio.file.attribute.FileAttribute[])
 */
@Experimental("Incubating archive API")
public enum ZipOption implements OpenOption {
    /**
     * Store the entry without compression.
     * The compressed and uncompressed sizes will be equal.
     */
    STORED,
    /**
     * Compress the entry using the DEFLATE algorithm.
     */
    DEFLATED,
    /**
     * Write ZIP64 extended information in local file headers and central directory entries.
     * This must be used if any single entry may exceed 4 GB in compressed or uncompressed size.
     * <p>
     * When this option is not specified, entries are written in standard ZIP format and an exception
     * is thrown if any entry's size overflows the 32-bit size fields.
     * The central directory and end-of-central-directory records will still use ZIP64 format
     * when necessary (e.g., when the entry count exceeds 65534 or the central directory offset
     * exceeds 4 GB), regardless of this option.
     */
    ZIP64,
    ;
}
