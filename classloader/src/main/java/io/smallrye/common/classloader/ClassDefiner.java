package io.smallrye.common.classloader;

import java.lang.invoke.MethodHandles;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class ClassDefiner {
    public static Class<?> defineClass(MethodHandles.Lookup lookup, Class<?> parent, String className, byte[] classBytes) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(DefineClassPermission.getInstance());
        }

        return AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
            @Override
            public Class<?> run() {
                try {
                    MethodHandles.Lookup privateLookupIn = MethodHandles.privateLookupIn(parent, lookup);
                    return privateLookupIn.defineClass(classBytes);
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessError(e.getMessage());
                }
            }
        });
    }
}
