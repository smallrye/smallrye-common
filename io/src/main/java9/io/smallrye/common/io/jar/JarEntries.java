package io.smallrye.common.io.jar;

import java.lang.reflect.Method;
import java.util.jar.JarEntry;

public class JarEntries {

    private static final Method REAL_NAME_METHOD;

    static {
        Method method;
        try {
            method = Class.forName("java.util.jar.JarFile$JarFileEntry").getDeclaredMethod("realName");
            method.setAccessible(true);
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            method = null;
        }
        REAL_NAME_METHOD = method;
    }

    /**
     * Returns the real name of this {@link JarEntry}. On Java 8, it returns the {@link JarEntry#getName()}
     * On Java 10+, a getRealName() method was added
     */
    public static String getRealName(JarEntry jarEntry) {
        if (REAL_NAME_METHOD != null) {
            try {
                return REAL_NAME_METHOD.invoke(jarEntry).toString();
            } catch (Exception e) {
                // This should never happen
            }
        }
        // As a safe net, fallback to the original value
        return jarEntry.getName();
    }
}
