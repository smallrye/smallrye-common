package io.smallrye.common.io.jar;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/**
 * Java 9+ variant of a JDK-specific class for working with {@code JarFile}s.
 */
public class JarFiles {
    private JarFiles() {
    }

    /**
     * {@return an equivalent of {@code new JarFile(name)}}
     * On Java 8, that's exactly what is returned.
     * On Java 9+, an equivalent that is multi-release-enabled is returned.
     *
     * @param name the file name (must not be {@code null})
     * @throws IOException if the operation fails
     */
    public static JarFile create(String name) throws IOException {
        return new JarFile(new File(name), true, ZipFile.OPEN_READ, JarFile.runtimeVersion());
    }

    /**
     * {@return an equivalent of {@code new JarFile(name, verify)}}
     * On Java 8, that's exactly what is returned.
     * On Java 9+, an equivalent that is multi-release-enabled is returned.
     *
     * @param name the file name (must not be {@code null})
     * @param verify {@code true} to verify the JAR signatures
     * @throws IOException if the operation fails
     */
    public static JarFile create(String name, boolean verify) throws IOException {
        return new JarFile(new File(name), verify, ZipFile.OPEN_READ, JarFile.runtimeVersion());
    }

    /**
     * {@return an equivalent of {@code new JarFile(file)}}
     * On Java 8, that's exactly what is returned.
     * On Java 9+, an equivalent that is multi-release-enabled is returned.
     *
     * @param file the file (must not be {@code null})
     * @throws IOException if the operation fails
     */
    public static JarFile create(File file) throws IOException {
        return new JarFile(file, true, ZipFile.OPEN_READ, JarFile.runtimeVersion());
    }

    /**
     * {@return an equivalent of {@code new JarFile(file, verify)}}
     * On Java 8, that's exactly what is returned.
     * On Java 9+, an equivalent that is multi-release-enabled is returned.
     *
     * @param file the file (must not be {@code null})
     * @param verify {@code true} to verify the JAR signatures
     * @throws IOException if the operation fails
     */
    public static JarFile create(File file, boolean verify) throws IOException {
        return new JarFile(file, verify, ZipFile.OPEN_READ, JarFile.runtimeVersion());
    }

    /**
     * {@return {@code true} if this {@link JarFile} is a multi-release jar}
     * On Java 8 this is done by browsing the manifest.
     * On Java 9+, there is a {@code isMultiRelease} method.
     *
     * @param jarFile the JAR file (must not be {@code null})
     */
    public static boolean isMultiRelease(JarFile jarFile) {
        return jarFile.isMultiRelease();
    }
}
