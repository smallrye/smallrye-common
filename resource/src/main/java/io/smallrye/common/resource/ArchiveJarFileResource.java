package io.smallrye.common.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.DirectoryStream;
import java.time.Instant;
import java.util.Iterator;
import java.util.NoSuchElementException;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.io.archive.Archive;

final class ArchiveJarFileResource extends JarFileResource {
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocateDirect(0);

    private final Archive archive;
    private final long index;

    ArchiveJarFileResource(final String path, final Archive archive, final URL base, final long index) {
        super(path, base);
        this.archive = archive;
        this.index = index;
    }

    public ByteBuffer asBuffer() {
        if (isDirectory() || index < 0) {
            return EMPTY_BUFFER;
        } else if (isMappable()) {
            return archive.mapStoredToBuffer(index);
        } else {
            long size = size();
            if (size > Integer.MAX_VALUE - 16) {
                throw new IllegalArgumentException("Resource is too large");
            }
            byte[] bytes = new byte[(int) size];
            int cnt;
            try (InputStream is = archive.openEntry(index)) {
                cnt = is.read(bytes);
            } catch (IOException e) {
                // should be impossible unless there is a compression error
                throw new IllegalStateException(e);
            }
            return ByteBuffer.wrap(bytes, 0, cnt);
        }
    }

    public boolean isMappable() {
        return !isDirectory() && archive.isStored(index);
    }

    public ByteBuffer mapAsBuffer(final long offset, final int length) {
        if (!isMappable()) {
            throw new IllegalArgumentException("Resource is not mappable");
        }
        Assert.checkMinimumParameter("offset", 0, offset);
        Assert.checkMinimumParameter("length", 0, length);
        ByteBuffer data = archive.mapStoredToBuffer(index);
        int lim = data.limit();
        Assert.checkMaximumParameter("offset", offset, lim);
        Assert.checkMaximumParameter("length", length, lim - offset);
        return data.slice((int) offset, length);
    }

    public boolean isDirectory() {
        return index < 0 || archive.isDirectory(index);
    }

    public DirectoryStream<Resource> openDirectoryStream() throws IOException {
        if (!isDirectory()) {
            return super.openDirectoryStream();
        }
        return new DirectoryStream<Resource>() {

            public static final char AFTER = (char) ('/' + 1);

            public Iterator<Resource> iterator() {
                long idx = index;
                // exclude the directory itself (if it actually exists)
                final long start = idx >= 0 ? idx + 1 : -idx - 1;
                idx = pathName().isEmpty() ? archive.entryCount() : archive.findEntry(pathName() + AFTER);
                // this is the entry after the last possible file entry
                final long end = idx < 0 ? -idx - 1 : idx;
                return new Iterator<Resource>() {
                    long next = start;

                    public boolean hasNext() {
                        return next < end;
                    }

                    public Resource next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        long idx = next;
                        // find the entry that comes next; a directory might have many entries so skip them all at once
                        String entry = archive.entryName(idx);
                        int sl = entry.indexOf('/', pathName().length() + 1);
                        if (sl != -1) {
                            entry = entry.substring(0, sl);
                        }
                        String nextEntry = entry + AFTER;
                        long entryAfter = archive.findEntry(nextEntry, idx + 1, end);
                        next = entryAfter < 0 ? -entryAfter - 1 : entryAfter;
                        return new ArchiveJarFileResource(entry, archive, base, idx);
                    }
                };
            }

            public void close() {
            }
        };
    }

    public long copyTo(final WritableByteChannel channel) throws IOException {
        ByteBuffer buf = asBuffer();
        long t = 0;
        while (buf.hasRemaining()) {
            t += channel.write(buf);
        }
        return t;
    }

    public InputStream openStream() {
        return isDirectory() ? InputStream.nullInputStream() : archive.openEntry(index);
    }

    public long size() {
        return index < 0 ? 0 : archive.uncompressedSize(index);
    }

    public Instant modifiedTime() {
        return index < 0 ? null : archive.modifiedTime(index);
    }
}
