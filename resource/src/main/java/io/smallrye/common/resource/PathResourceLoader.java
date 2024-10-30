package io.smallrye.common.resource;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class PathResourceLoader implements ResourceLoader {
    private final Path base;
    private Closeable c;

    /**
     * Construct a new instance.
     * <em>Note:</em> to open a JAR file, use {@link JarFileResourceLoader} instead.
     *
     * @param base the base path (must not be {@code null})
     */
    public PathResourceLoader(final Path base) {
        this.c = null;
        this.base = Assert.checkNotNullParam("base", base);
    }

    /**
     * Construct a new instance from a filesystem's root.
     * The filesystem is closed when this resource loader is closed.
     * <em>Note:</em> to open a JAR file, use {@link JarFileResourceLoader} instead.
     *
     * @param fs the filesystem (must not be {@code null})
     */
    public PathResourceLoader(final FileSystem fs) {
        this.c = Assert.checkNotNullParam("fs", fs);
        this.base = fs.getPath("/");
    }

    public Resource findResource(final String path) {
        String canon = ResourceUtils.canonicalizeRelativePath(path);
        Path filePath = base.resolve(canon);
        if (!Files.exists(filePath)) {
            return null;
        }
        return new PathResource(canon, filePath);
    }

    public URL baseUrl() {
        try {
            return base.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new UnsupportedOperationException(
                    "Base URL is not supported for this loader because the path does not have a valid URL", e);
        }
    }

    public void close() {
        Closeable c = this.c;
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
            this.c = null;
        }
    }
}
