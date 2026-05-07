package io.smallrye.common.serial.impl;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.lang.invoke.MethodHandle;

import sun.reflect.ReflectionFactory;

public final class WriteUtil {

    private static final ReflectionFactory rf = ReflectionFactory.getReflectionFactory();

    static final ClassValue<MethodHandle> writeReplaces = new ClassValue<MethodHandle>() {
        protected MethodHandle computeValue(final Class<?> type) {
            return rf.writeReplaceForSerialization(type);
        }
    };
    static final ClassValue<MethodHandle> writeObjects = new ClassValue<MethodHandle>() {
        protected MethodHandle computeValue(final Class<?> type) {
            return rf.writeObjectForSerialization(type);
        }
    };
    static final ClassValue<MethodHandle> defaultWriteObjects = new ClassValue<MethodHandle>() {
        protected MethodHandle computeValue(final Class<?> type) {
            // todo: inline on JDK 22+
            return JDK24Specific.defaultWriteObjectForSerialization(type);
        }
    };

    private WriteUtil() {
    }

    public static boolean hasWriteObject(Class<?> type) {
        return writeObjects.get(type) != null;
    }

    public static void writeObject(Class<?> type, Object serializable, ObjectOutputStream oos) throws IOException {
        MethodHandle mh = writeObjects.get(type);
        if (mh == null) {
            throw new IllegalArgumentException("No writeObject method found on " + type);
        }
        try {
            mh.invoke(serializable, oos);
        } catch (IOException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw Util.sneak(e);
        }
    }

    public static void defaultWriteObject(Class<?> type, Object serializable, ObjectOutputStream oos) throws IOException {
        MethodHandle mh = defaultWriteObjects.get(type);
        if (mh == null) {
            throw new IllegalArgumentException("No defaultWriteObject method available for " + type);
        }
        try {
            mh.invoke(serializable, oos);
        } catch (IOException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw Util.sneak(e);
        }
    }

    public static boolean hasWriteReplace(Class<?> type) {
        return writeReplaces.get(type) != null;
    }

    public static Object writeReplace(Object object) throws ObjectStreamException {
        if (object == null) {
            return null;
        }
        MethodHandle wr = writeReplaces.get(object.getClass());
        if (wr == null) {
            throw new IllegalArgumentException("No writeReplace method found on " + object.getClass());
        }
        try {
            return wr.invoke(object);
        } catch (RuntimeException | Error | ObjectStreamException e) {
            throw e;
        } catch (Throwable e) {
            throw Util.sneak(e);
        }
    }
}
