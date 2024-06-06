package io.smallrye.common.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import io.smallrye.common.constraint.Assert;

/**
 * A resource corresponding to a {@link Path}.
 */
public final class PathResource extends Resource {
    /**
     * Only allow cached mapping when the resource is very large.
     */
    private static final long MAP_THRESHOLD = 128 << 20; // 128MiB
    private final Path path;
    private URL url;
    private MappedByteBuffer mapped;

    /**
     * Construct a new instance.
     *
     * @param pathName the relative path name (must not be {@code null})
     * @param path the path (must not be {@code null})
     */
    public PathResource(final String pathName, final Path path) {
        super(pathName);
        this.path = Assert.checkNotNullParam("path", path);
    }

    /**
     * {@return the path of this resource}
     */
    public Path path() {
        return path;
    }

    public URL url() {
        URL url = this.url;
        if (url == null) {
            try {
                url = this.url = path.toUri().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Unexpected URL problem", e);
            }
        }
        return url;
    }

    public DirectoryStream<Resource> openDirectoryStream() throws IOException {
        return new MappedDirectoryStream<>(
                Files.newDirectoryStream(path),
                p -> new PathResource(pathName() + '/' + p.getFileName().toString(), p));
    }

    public boolean isDirectory() {
        return Files.isDirectory(path);
    }

    public InputStream openStream() throws IOException {
        return Files.newInputStream(path);
    }

    public ByteBuffer asBuffer() throws IOException {
        MappedByteBuffer mapped = this.mapped;
        if (mapped == null) {
            long size = size();
            if (size >= MAP_THRESHOLD) {
                // map the (large) file into memory
                synchronized (this) {
                    mapped = this.mapped;
                    if (mapped == null) {
                        try (FileChannel fc = FileChannel.open(path(), StandardOpenOption.READ)) {
                            if (fc.size() > Integer.MAX_VALUE) {
                                throw new OutOfMemoryError("Resource is too large to load into a buffer");
                            }
                            mapped = this.mapped = fc.map(FileChannel.MapMode.READ_ONLY, 0, size);
                        }
                    }
                }
            }
        }
        if (mapped == null) {
            // just read the bytes
            return ByteBuffer.wrap(Files.readAllBytes(path));
        } else {
            return mapped.duplicate();
        }
    }

    public String asString(final Charset charset) throws IOException {
        return Files.readString(path(), charset);
    }

    public Instant modifiedTime() {
        try {
            FileTime fileTime = Files.getLastModifiedTime(path());
            return fileTime.toMillis() == 0 ? null : fileTime.toInstant();
        } catch (IOException e) {
            return null;
        }
    }

    public long size() {
        MappedByteBuffer mapped = this.mapped;
        if (mapped != null) {
            return mapped.capacity();
        }
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1;
        }
    }
}
