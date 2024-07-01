package io.smallrye.common.resource;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

import io.smallrye.common.constraint.Assert;

/**
 * An in-memory resource.
 */
public final class MemoryResource extends Resource {
    // todo: switch to MemorySegment
    private final ByteBuffer data;
    private final Instant modifiedTime = Instant.now();
    private URL url;

    /**
     * Construct a new instance.
     * The given buffer contents are not copied.
     * Accessing the resource will not affect the buffer's position or limit.
     * Modifying the buffer's position or limit will not affect the contents of the resource.
     *
     * @param buffer the byte buffer containing the resource data (must not be {@code null})
     * @param pathName the resource path name (must not be {@code null})
     */
    public MemoryResource(final String pathName, final ByteBuffer data) {
        super(pathName);
        this.data = Assert.checkNotNullParam("data", data).asReadOnlyBuffer();
    }

    /**
     * Construct a new instance for a byte array.
     * The byte array is not copied.
     *
     * @param bytes the byte array (must not be {@code null})
     * @param pathName the resource path name (must not be {@code null})
     */
    public MemoryResource(final String pathName, final byte[] data) {
        this(pathName, ByteBuffer.wrap(data));
    }

    // todo: MemorySegment ctor

    public URL url() {
        URL url = this.url;
        if (url == null) {
            try {
                this.url = url = new URL("memory", null, -1, pathName(), new ResourceURLStreamHandler(this));
            } catch (MalformedURLException e) {
                throw new UncheckedIOException("Unexpected URL problem", e);
            }
        }
        return url;
    }

    public MemoryInputStream openStream() {
        return new MemoryInputStream(data);
    }

    public ByteBuffer asBuffer() {
        return data.duplicate();
    }

    public long copyTo(final Path destination) throws IOException {
        try (SeekableByteChannel ch = Files.newByteChannel(destination, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            return copyTo(ch);
        }
    }

    public long copyTo(final WritableByteChannel channel) throws IOException {
        if (channel instanceof SelectableChannel sc && !sc.isBlocking()) {
            return super.copyTo(channel);
        }
        long cnt = 0;
        ByteBuffer buf = data.duplicate();
        while (buf.hasRemaining()) {
            long res = channel.write(buf);
            cnt += res;
        }
        return cnt;
    }

    public long copyTo(final OutputStream destination) throws IOException {
        if (destination instanceof FileOutputStream fos) {
            return copyTo(fos.getChannel());
        } else {
            return super.copyTo(destination);
        }
    }

    public Instant modifiedTime() {
        return modifiedTime;
    }

    public long size() {
        return data.remaining();
    }
}
