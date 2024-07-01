package io.smallrye.common.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.time.Instant;

import io.smallrye.common.constraint.Assert;

/**
 * A URL connection backed by a resource.
 */
public final class ResourceURLConnection extends URLConnection {
    private final Resource resource;

    ResourceURLConnection(final URL url, final Resource resource) {
        super(Assert.checkNotNullParam("url", url));
        this.resource = Assert.checkNotNullParam("resource", resource);
    }

    /**
     * {@return the resource associated with this connection (not <code>null</code>)}
     */
    public Resource resource() {
        return resource;
    }

    public void connect() {
    }

    public long getContentLengthLong() {
        return resource.size();
    }

    public String getContentType() {
        return "application/octet-stream";
    }

    public long getLastModified() {
        Instant instant = resource.modifiedTime();
        return instant == null ? 0 : instant.toEpochMilli();
    }

    public Object getContent(final Class<?>... classes) throws IOException {
        for (Class<?> clazz : classes) {
            if (clazz == ByteBuffer.class) {
                return resource.asBuffer();
            } else if (clazz == byte[].class) {
                return getInputStream().readAllBytes();
            } else if (clazz == Resource.class) {
                return resource;
            }
        }
        return null;
    }

    public Object getContent() throws IOException {
        return getContent(byte[].class);
    }

    public InputStream getInputStream() throws IOException {
        return resource.openStream();
    }
}
