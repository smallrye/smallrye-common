package io.smallrye.common.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.util.Objects;

import io.smallrye.common.constraint.Assert;

/**
 * A resource backed by a connection to a URL.
 */
public final class URLResource extends Resource {
    private final URLConnection connection;

    /**
     * Construct a new instance for a URL.
     *
     * @param pathName the resource path name (must not be {@code null})
     * @param url the URL (must not be {@code null})
     * @throws IOException if the connection could not be opened
     */
    public URLResource(final String pathName, final URL url) throws IOException {
        this(pathName, url.openConnection());
    }

    /**
     * Construct a new instance.
     *
     * @param pathName the resource path name (must not be {@code null})
     * @param connection the URL connection (must not be {@code null})
     */
    public URLResource(final String pathName, final URLConnection connection) {
        super(pathName);
        if (connection instanceof JarURLConnection j) {
            j.setUseCaches(false);
        }
        this.connection = Assert.checkNotNullParam("connection", connection);
    }

    /**
     * Construct a new instance for a JAR URL connection.
     * The JAR entry name is used as the resource name.
     *
     * @param jarConnection the JAR URL connection (must not be {@code null})
     */
    public URLResource(final JarURLConnection jarConnection) {
        this(Objects.requireNonNullElse(jarConnection.getEntryName(), ""), jarConnection);
    }

    public URL url() {
        return connection.getURL();
    }

    public InputStream openStream() throws IOException {
        return connection.getInputStream();
    }

    public Instant modifiedTime() {
        long lastModified = connection.getLastModified();
        return lastModified == 0 ? null : Instant.ofEpochMilli(lastModified);
    }

    public long size() {
        return connection.getContentLengthLong();
    }
}
