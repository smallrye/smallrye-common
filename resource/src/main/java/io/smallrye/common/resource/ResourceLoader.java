package io.smallrye.common.resource;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Manifest;

/**
 * A loader which can find resources by their path.
 */
public interface ResourceLoader extends Closeable {

    /**
     * A resource loader containing no resources.
     */
    ResourceLoader EMPTY = path -> null;

    /**
     * Find a resource from this loader.
     *
     * @param path the resource path (must not be {@code null})
     * @return the loaded resource, or {@code null} if no resource is found at the given path
     * @throws IOException if the resource could not be loaded
     */
    Resource findResource(String path) throws IOException;

    /**
     * {@return the base URL for this loader}
     */
    default URL baseUrl() {
        throw new UnsupportedOperationException("Base URL is not supported by this resource loader");
    }

    /**
     * Get a child resource loader for the given child path.
     * This method always returns a resource loader, even if the path does not exist.
     * Closing the child loader does not close the enclosing loader.
     *
     * @param path the relative sub-path (must not be {@code null})
     * @return the resource loader (not {@code null}, may be empty)
     */
    default ResourceLoader getChildLoader(String path) {
        String subPath = ResourceUtils.canonicalizeRelativePath(path);
        if (subPath.isEmpty()) {
            return this;
        }
        return p -> findResource(subPath + '/' + p);
    }

    /**
     * Get the manifest for this resource loader, if any.
     * The default implementation constructs a new instance every time,
     * so the caller should avoid repeated invocation of this method, caching as needed.
     *
     * @return the manifest, or {@code null} if no manifest was found
     * @throws IOException if the manifest resource could not be loaded
     */
    default Manifest manifest() throws IOException {
        Resource resource = findResource("META-INF/MANIFEST.MF");
        if (resource == null) {
            return null;
        }
        Manifest manifest = new Manifest();
        try (InputStream is = resource.openStream()) {
            manifest.read(is);
        }
        return manifest;
    }

    /**
     * Hint that this resource loader is unlikely to be used in the near future.
     */
    default void release() {
    }

    /**
     * Release any system resources or allocations associated with this resource loader.
     */
    default void close() {
    }
}
