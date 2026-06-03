package io.smallrye.common.resource;

import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.io.archive.Archive;

/**
 * A resource loader which corresponds to a JAR file.
 */
public final class JarFileResourceLoader implements ResourceLoader {
    private static final boolean USE_CLASSIC_IMPL = false;
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
        if (USE_CLASSIC_IMPL) {
            impl = new ClassicImpl(jarPath);
        } else {
            impl = new ArchiveImpl(jarPath);
        }
    }

    /**
     * Construct a new instance from a JAR file contained within a resource.
     *
     * @param resource the resource of the JAR file (must not be {@code null})
     * @throws IOException if opening the JAR file fails for some reason
     */
    public JarFileResourceLoader(final Resource resource) throws IOException {
        base = resource.url();
        if (USE_CLASSIC_IMPL) {
            impl = new ClassicImpl(resource);
        } else {
            impl = new ArchiveImpl(resource);
        }
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

    private final class ClassicImpl extends AbstractImpl {
        private final JarFile jarFile;
        private Path tempFile;

        /**
         * Construct a new instance.
         *
         * @param jarPath the path of the JAR file (must not be {@code null})
         * @throws IOException if opening the JAR file fails for some reason
         */
        public ClassicImpl(final Path jarPath) throws IOException {
            jarFile = new JarFile(jarPath.toFile(), true, JarFile.OPEN_READ, JarFile.runtimeVersion());
        }

        /**
         * Construct a new instance from a JAR file contained within a resource.
         *
         * @param resource the resource of the JAR file (must not be {@code null})
         * @throws IOException if opening the JAR file fails for some reason
         */
        public ClassicImpl(final Resource resource) throws IOException {
            if (resource instanceof PathResource pr && pr.hasFile()) {
                // avoid using a temp file, if possible
                jarFile = new JarFile(pr.file(), true, JarFile.OPEN_READ, JarFile.runtimeVersion());
            } else {
                tempFile = Files.createTempFile("srcr-tmp-", ".jar");
                try {
                    resource.copyTo(tempFile);
                    jarFile = new JarFile(tempFile.toFile(), true, JarFile.OPEN_READ, JarFile.runtimeVersion());
                } catch (Throwable t) {
                    try {
                        Files.delete(tempFile);
                    } catch (Throwable t2) {
                        t.addSuppressed(t2);
                    }
                    throw t;
                }
            }
        }

        public Resource findResource(final String path) {
            String canonPath = ResourceUtils.canonicalizeRelativePath(path);
            if (canonPath.isEmpty()) {
                // root directory
                JarEntry entry = new JarEntry("/");
                entry.setSize(0);
                entry.setCompressedSize(0);
                return new ClassicJarFileResource(baseUrl(), jarFile, entry);
            }
            JarEntry jarEntry = jarFile.getJarEntry(canonPath);
            if (jarEntry != null) {
                return new ClassicJarFileResource(baseUrl(), jarFile, jarEntry);
            } else {
                jarEntry = jarFile.getJarEntry(canonPath + "/");
                if (jarEntry != null) {
                    return new ClassicJarFileResource(baseUrl(), jarFile, jarEntry);
                } else {
                    // search for a directory with the given name (todo: may be slow)
                    String dirName = canonPath + "/";
                    boolean found = jarFile.versionedStream().map(JarEntry::getName)
                            .map(ResourceUtils::canonicalizeRelativePath)
                            .anyMatch(n -> n.startsWith(dirName));
                    if (found) {
                        JarEntry entry = new JarEntry(dirName);
                        entry.setSize(0);
                        entry.setCompressedSize(0);
                        return new ClassicJarFileResource(base, jarFile, entry);
                    }
                    return null;
                }
            }
        }

        public Manifest manifest() throws IOException {
            return jarFile.getManifest();
        }

        public void close() {
            try {
                jarFile.close();
            } catch (IOException ignored) {
            }
            if (tempFile != null) {
                try {
                    Files.delete(tempFile);
                } catch (IOException ignored) {
                } finally {
                    tempFile = null;
                }
            }
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
