package io.smallrye.common.io.jar;

import java.util.jar.JarEntry;

public class JarEntries {
    /**
     * Returns the real name of this {@link JarEntry}. On Java 8, it returns the {@link JarEntry#getName()}
     * On Java 10+, a getRealName() method was added
     */
    public static String getRealName(JarEntry jarEntry) {
        return jarEntry.getName();
    }
}
