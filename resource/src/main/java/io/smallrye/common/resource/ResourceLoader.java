package io.smallrye.common.resource;

import java.io.Closeable;
import java.io.IOException;

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
