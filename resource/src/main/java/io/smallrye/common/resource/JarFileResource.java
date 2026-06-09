package io.smallrye.common.resource;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * A resource representing an entry in a JAR file.
 */
public abstract class JarFileResource extends Resource {
    final URL base;
    private URL url;

    JarFileResource(final String path, final URL base) {
        super(path);
        this.base = base;
    }

    public URL url() {
        URL url = this.url;
        if (url == null) {
            try {
                // todo: Java 20+: URL.of(new URI("jar", null, base.toURI().toASCIIString() + "!/" + pathName()),
                //                        new ResourceURLStreamHandler(this));
                url = this.url = new URL(null,
                        new URI("jar", base.toURI().toASCIIString() + "!/" + pathName(), null).toASCIIString(),
                        streamHandler());
            } catch (MalformedURLException | URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return url;
    }
}
