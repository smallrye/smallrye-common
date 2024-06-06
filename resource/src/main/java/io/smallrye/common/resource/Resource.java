package io.smallrye.common.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.ProtectionDomain;
import java.time.Instant;
import java.util.function.Function;

import io.smallrye.common.constraint.Assert;

/**
 * A handle to a loadable resource, which usually will come from a JAR or the filesystem.
 */
public abstract class Resource {

    private final String path;

    /**
     * Construct a new instance.
     *
     * @param path the resource path (must not be {@code null})
     */
    protected Resource(final String path) {
        this.path = ResourceUtils.canonicalizeRelativePath(Assert.checkNotNullParam("path", path));
    }

    /**
     * {@return the resource's relative path}
     * The returned path is relative (that is, it does not start with {@code /})
     * and canonical (that is, contains no sequences of more than one consecutive {@code /}, contains
     * no {@code .} or {@code ..} segments, and does not end with a {@code /}).
     */
    public final String pathName() {
        return path;
    }

    /**
     * {@return the resource URL (not <code>null</code>)}
     * If the resource location information cannot be converted to a URL, an exception may be thrown.
     */
    public abstract URL url();

    /**
     * Open an input stream to read this resource.
     *
     * @return the input stream (not {@code null})
     * @throws IOException if the input stream could not be opened or the resource is a directory
     */
    public abstract InputStream openStream() throws IOException;

    /**
     * Perform the given action on the input stream of this resource.
     *
     * @param function an action to perform (must not be {@code null})
     * @return the result of the action function
     * @param <R> the type of the function result
     * @throws IOException if the stream could not be opened, or the resource is a directory, or the
     *         action throws an instance of {@link UncheckedIOException}
     */
    public <R> R readStream(Function<InputStream, R> function) throws IOException {
        try (InputStream is = openStream()) {
            return function.apply(is);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Open a directory stream to read the contents of this directory.
     * Not every resource implementation supports directory access.
     *
     * @return the directory stream (not {@code null})
     * @throws IOException if the directory could not be opened or the resource is not a directory
     */
    public DirectoryStream<Resource> openDirectoryStream() throws IOException {
        throw new IOException("Not a directory");
    }

    /**
     * {@return true if this resource represents a directory, or false otherwise}
     * Not every resource implementation supports directory access.
     */
    public boolean isDirectory() {
        return false;
    }

    /**
     * {@return the bytes of this resource, as a read-only byte buffer}
     * The buffer is suitable for passing to {@link ClassLoader#defineClass(String, ByteBuffer, ProtectionDomain)}.
     * The default implementation reads all of the resource bytes from the stream returned by {@link #openStream()}.
     * Other implementations might return a buffer for data already contained in memory, or might return a memory-mapped
     * buffer if the resource is very large.
     * The buffer might or might not be cached on the resource.
     * Because of this, care should be taken to avoid calling this method repeatedly for a single resource.
     *
     * @implSpec Implementers must ensure that the returned buffer is read-only.
     *
     * @throws IOException if the content could not be read
     * @throws OutOfMemoryError if the size of the resource is greater than the maximum allowed size of a buffer
     */
    public ByteBuffer asBuffer() throws IOException {
        try (InputStream is = openStream()) {
            return ByteBuffer.wrap(is.readAllBytes()).asReadOnlyBuffer();
        }
    }

    /**
     * Copy the bytes of this resource to the given destination.
     * The copy may fail before all of the bytes have been transferred;
     * in this case the content and state of the destination are undefined.
     * <p>
     * The path is opened as if with the following options:
     * <ul>
     * <li>{@link StandardOpenOption#CREATE}</li>
     * <li>{@link StandardOpenOption#TRUNCATE_EXISTING}</li>
     * <li>{@link StandardOpenOption#WRITE}</li>
     * </ul>
     *
     * @param destination the destination path (must not be {@code null})
     * @return the number of bytes copied
     * @throws IOException if the copy fails
     */
    public long copyTo(Path destination) throws IOException {
        try (InputStream is = openStream()) {
            return Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Copy the bytes of this resource to the given destination.
     * The copy may fail before all of the bytes have been transferred;
     * in this case the content and state of the destination are undefined.
     * The destination stream is not closed.
     *
     * @param destination the destination stream (must not be {@code null})
     * @return the number of bytes copied
     * @throws IOException if the copy fails
     */
    public long copyTo(OutputStream destination) throws IOException {
        try (InputStream is = openStream()) {
            return is.transferTo(destination);
        }
    }

    /**
     * Copy the bytes of this resource to the given destination.
     * The copy may fail before all of the bytes have been transferred;
     * in this case the content and state of the destination are undefined.
     * The destination channel is not closed.
     *
     * @param destination the destination channel (must not be {@code null} and must not be non-blocking)
     * @return the number of bytes copied
     * @throws IOException if the copy fails
     */
    public long copyTo(WritableByteChannel channel) throws IOException {
        if (channel instanceof SelectableChannel sc && !sc.isBlocking()) {
            throw new IllegalArgumentException("Channel must not be non-blocking");
        }
        ByteBuffer buf = asBuffer();
        long c = 0;
        while (buf.hasRemaining()) {
            c += channel.write(buf);
        }
        return c;
    }

    /**
     * {@return the resource content as a string}
     *
     * @param charset the character set to use for decoding (must not be {@code null})
     * @throws IOException if the content could not be read
     */
    public String asString(Charset charset) throws IOException {
        return charset.newDecoder().decode(asBuffer()).toString();
    }

    /**
     * {@return the modification time of the resource, or <code>null</code> if the time is unknown}
     */
    public Instant modifiedTime() {
        return null;
    }

    /**
     * {@return the size of the resource, or <code>-1</code> if the size is not known}
     */
    public abstract long size();
}
