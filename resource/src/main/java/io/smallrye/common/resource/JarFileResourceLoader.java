package io.smallrye.common.resource;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A resource loader which corresponds to a JAR file.
 */
public final class JarFileResourceLoader implements ResourceLoader {
    private final URL base;
    private final JarFile jarFile;
    private Path tempFile;

    /**
     * Construct a new instance.
     *
     * @param jarPath the path of the JAR file (must not be {@code null})
     * @throws IOException if opening the JAR file fails for some reason
     */
    public JarFileResourceLoader(final Path jarPath) throws IOException {
        this.base = jarPath.toUri().toURL();
        jarFile = new JarFile(jarPath.toFile());
    }

    /**
     * Construct a new instance from a JAR file contained within a resource.
     *
     * @param resource the resource of the JAR file (must not be {@code null})
     * @throws IOException if opening the JAR file fails for some reason
     */
    public JarFileResourceLoader(final Resource resource) throws IOException {
        // todo: this will be replaced with a version which opens the file in-place from a buffer
        base = resource.url();
        JarFile jf = null;
        if (resource instanceof PathResource pr) {
            try {
                // avoid using a temp file, if possible
                jf = new JarFile(pr.path().toFile(), true, JarFile.OPEN_READ, JarFile.runtimeVersion());
            } catch (UnsupportedOperationException ignored) {
            }
        }
        if (jf == null) {
            tempFile = Files.createTempFile("srcr-tmp-", ".jar");
            try {
                resource.copyTo(tempFile);
                jf = new JarFile(tempFile.toFile(), true, JarFile.OPEN_READ, JarFile.runtimeVersion());
            } catch (Throwable t) {
                try {
                    Files.delete(tempFile);
                } catch (Throwable t2) {
                    t.addSuppressed(t2);
                }
                throw t;
            }
        }
        jarFile = jf;
    }

    public Resource findResource(final String path) {
        String canonPath = ResourceUtils.canonicalizeRelativePath(path);
        JarEntry jarEntry = jarFile.getJarEntry(canonPath);
        if (jarEntry != null) {
            return new JarFileResource(base, jarFile, jarEntry);
        } else {
            jarEntry = jarFile.getJarEntry(canonPath + "/");
            if (jarEntry != null) {
                return new JarFileResource(base, jarFile, jarEntry);
            } else {
                return null;
            }
        }
    }

    public URL baseUrl() {
        return base;
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
