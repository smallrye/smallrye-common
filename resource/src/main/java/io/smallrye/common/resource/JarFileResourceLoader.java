package io.smallrye.common.resource;

import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.jar.Manifest;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.io.archive.Archive;

/**
 * A resource loader which corresponds to a JAR file.
 */
public final class JarFileResourceLoader implements ResourceLoader {
    private final URL base;
    private final AbstractImpl impl;

    /**
     * Construct a new instance.
     *
     * @param jarPath the path of the JAR file (must not be {@code null})
     * @throws IOException if opening the JAR file fails for some reason
     */
    public JarFileResourceLoader(final Path jarPath) throws IOException {
        base = jarPath.toUri().toURL();
        impl = new ArchiveImpl(jarPath);
    }

    /**
     * Construct a new instance from a JAR file contained within a resource.
     *
     * @param resource the resource of the JAR file (must not be {@code null})
     * @throws IOException if opening the JAR file fails for some reason
     */
    public JarFileResourceLoader(final Resource resource) throws IOException {
        base = resource.url();
        impl = new ArchiveImpl(resource);
    }

    public Resource findResource(final String path) throws IOException {
        return impl.findResource(path);
    }

    public URL baseUrl() {
        return base;
    }

    public ResourceLoader getChildLoader(final String path) {
        return impl.getChildLoader(path);
    }

    public Manifest manifest() throws IOException {
        return impl.manifest();
    }

    public void release() {
        impl.release();
    }

    public void close() {
        impl.close();
    }

    private abstract class AbstractImpl implements ResourceLoader {
        public URL baseUrl() {
            return JarFileResourceLoader.this.baseUrl();
        }
    }

    private final class ArchiveImpl extends AbstractImpl {
        private final Archive archive;

        private ArchiveImpl(final Path jarPath) throws IOException {
            try (FileChannel fc = FileChannel.open(jarPath, StandardOpenOption.READ)) {
                try {
                    archive = Archive.open(fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size()));
                } catch (Throwable t) {
                    try {
                        fc.close();
                    } catch (Throwable t2) {
                        t.addSuppressed(t2);
                    }
                    throw t;
                }
            }
        }

        private ArchiveImpl(final Resource resource) throws IOException {
            if (resource.isMappable()) {
                archive = Archive.open(resource.mapAsBuffer());
            } else {
                archive = Archive.open(resource.asBuffer());
            }
        }

        public Resource findResource(String path) {
            path = ResourceUtils.canonicalizeRelativePath(Assert.checkNotNullParam("path", path));
            if (path.isEmpty()) {
                // -(-1) - 1 == 0
                return new ArchiveJarFileResource(path, archive, baseUrl(), -1);
            }
            long idx = archive.findEntry(path);
            if (idx >= 0) {
                return new ArchiveJarFileResource(path, archive, baseUrl(), idx);
            } else {
                String dirName = path + "/";
                idx = archive.findEntry(dirName);
                if (idx >= 0) {
                    return new ArchiveJarFileResource(path, archive, baseUrl(), idx);
                } else {
                    long insertionPoint = -idx - 1;
                    if (insertionPoint < archive.entryCount() && archive.entryNameStartsWith(insertionPoint, dirName)) {
                        return new ArchiveJarFileResource(path, archive, baseUrl(), idx);
                    }
                }
            }
            return null;
        }

        public void release() {
            archive.release();
        }

        public void close() {
            release();
        }
    }
}
