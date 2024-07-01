package io.smallrye.common.resource;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * A simple resource-based URL stream handler.
 */
final class ResourceURLStreamHandler extends URLStreamHandler {
    private final Resource resource;

    ResourceURLStreamHandler(final Resource resource) {
        this.resource = resource;
    }

    protected URLConnection openConnection(final URL u) {
        return new ResourceURLConnection(u, resource);
    }
}
