package io.smallrye.common.classloader;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

import sun.misc.Unsafe;

public class ClassDefiner {
    private static final Unsafe unsafe;
    private static final Method defineClass;

    static {
        unsafe = AccessController.doPrivileged(new PrivilegedAction<Unsafe>() {
            public Unsafe run() {
                try {
                    final Field field = Unsafe.class.getDeclaredField("theUnsafe");
                    field.setAccessible(true);
                    return (Unsafe) field.get(null);
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessError(e.getMessage());
                } catch (NoSuchFieldException e) {
                    throw new NoSuchFieldError(e.getMessage());
                }
            }
        });

        defineClass = AccessController.doPrivileged(new PrivilegedAction<Method>() {
            @Override
            public Method run() {
                try {
                    return Unsafe.class.getMethod("defineClass", String.class, byte[].class, int.class, int.class,
                            ClassLoader.class, ProtectionDomain.class);
                } catch (NoSuchMethodException e) {
                    throw new NoSuchMethodError(e.getMessage());
                }
            }
        });
    }

    public static Class<?> defineClass(MethodHandles.Lookup lookup, Class<?> parent, String className, byte[] classBytes) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(DefineClassPermission.getInstance());
        }

        // The className for Unsafe.defineClass must match the one in classBytes, so we can try to verify the package
        // with the parent
        String parentPkg = parent.getPackage().getName();
        String classPkg = className.substring(0, className.lastIndexOf('.'));

        if (!parentPkg.equals(classPkg)) {
            throw new IllegalArgumentException("Class not in same package as lookup class");
        }

        try {
            return (Class<?>) defineClass.invoke(unsafe, className, classBytes, 0, classBytes.length, parent.getClassLoader(),
                    null);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }
}
