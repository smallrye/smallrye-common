package io.smallrye.common.resource;

import java.io.IOException;
import java.net.URL;

import io.smallrye.common.constraint.Assert;

/**
 * A resource loader for a URL base.
 */
public final class URLResourceLoader implements ResourceLoader {
    private final URL base;

    /**
     * Construct a new instance.
     * <em>Note:</em> to open a JAR file, use {@link JarFileResourceLoader} instead.
     * To access files on the file system, use {@link PathResourceLoader} instead.
     *
     * @param base the URL base (must not be {@code null})
     */
    public URLResourceLoader(final URL base) {
        this.base = Assert.checkNotNullParam("base", base);
    }

    public Resource findResource(final String path) throws IOException {
        String canon = ResourceUtils.canonicalizeRelativePath(path);
        return new URLResource(canon, new URL(base, canon));
    }
}
