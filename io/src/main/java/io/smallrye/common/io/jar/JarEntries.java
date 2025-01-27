package io.smallrye.common.io.jar;

import java.util.jar.JarEntry;

/**
 * Utility class for handling JAR entries.
 */
public class JarEntries {
    private JarEntries() {
    }

    /**
     * {@return the real name of this {@link JarEntry}}.
     * On Java 8, it returns the {@link JarEntry#getName()}
     * On Java 10+, a {@code getRealName()} method was added.
     *
     * @param jarEntry the entry (must not be {@code null})
     */
    public static String getRealName(JarEntry jarEntry) {
        return jarEntry.getRealName();
    }
}
