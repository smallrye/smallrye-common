package io.smallrye.common.serial.impl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;

import sun.reflect.ReflectionFactory;

public final class ReadUtil {
    // Constructor.newInstance() must be used here rather than unreflecting to a MethodHandle, because the
    // Constructor returned by ReflectionFactory has a declaring class of the non-serializable superclass
    // (not the target class), so unreflectConstructor would create instances of the wrong class.
    static final ClassValue<Constructor<?>> serNewInstances = new ClassValue<>() {
        protected Constructor<?> computeValue(final Class<?> type) {
            return rf.newConstructorForSerialization(type);
        }
    };
    static final ClassValue<Constructor<?>> extNewInstances = new ClassValue<>() {
        protected Constructor<?> computeValue(final Class<?> type) {
            return rf.newConstructorForExternalization(type);
        }
    };
    static final ClassValue<MethodHandle> readObjects = new ClassValue<MethodHandle>() {
        protected MethodHandle computeValue(final Class<?> type) {
            return rf.readObjectForSerialization(type);
        }
    };
    static final ClassValue<MethodHandle> readObjectNoDatas = new ClassValue<MethodHandle>() {
        protected MethodHandle computeValue(final Class<?> type) {
            return rf.readObjectNoDataForSerialization(type);
        }
    };
    static final ClassValue<MethodHandle> defaultReadObjects = new ClassValue<MethodHandle>() {
        protected MethodHandle computeValue(final Class<?> type) {
            // todo: inline on JDK 22+
            return JDK24Specific.defaultReadObjectForSerialization(type);
        }
    };
    static final ClassValue<MethodHandle> readResolves = new ClassValue<MethodHandle>() {
        protected MethodHandle computeValue(final Class<?> type) {
            return rf.readResolveForSerialization(type);
        }
    };
    private static final ReflectionFactory rf = ReflectionFactory.getReflectionFactory();

    private ReadUtil() {
    }

    public static boolean hasReadObject(Class<?> type) {
        return readObjects.get(type) != null;
    }

    public static void readObject(Class<?> type, Object serializable, ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
        MethodHandle mh = readObjects.get(type);
        if (mh == null) {
            throw new IllegalArgumentException("No readObject method found on " + type);
        }
        try {
            mh.invoke(serializable, ois);
        } catch (IOException | ClassNotFoundException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw Util.sneak(e);
        }
    }

    public static boolean hasReadObjectNoData(Class<?> type) {
        return readObjectNoDatas.get(type) != null;
    }

    public static void readObjectNoData(Class<?> type, Object serializable) throws ObjectStreamException {
        MethodHandle mh = readObjectNoDatas.get(type);
        if (mh == null) {
            throw new IllegalArgumentException("No readObject method found on " + type);
        }
        try {
            mh.invoke(serializable);
        } catch (ObjectStreamException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw Util.sneak(e);
        }
    }

    public static void defaultReadObject(Class<?> type, Object serializable, ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
        MethodHandle mh = defaultReadObjects.get(type);
        if (mh == null) {
            throw new IllegalArgumentException("No defaultReadObject method available for " + type);
        }
        try {
            mh.invoke(serializable, ois);
        } catch (IOException | ClassNotFoundException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw Util.sneak(e);
        }
    }

    public static <T> T newSerializableInstance(final Class<T> clazz) {
        Constructor<?> ctor = serNewInstances.get(clazz);
        if (ctor == null) {
            throw new IllegalArgumentException("No valid constructor found on serializable " + clazz);
        }
        try {
            return clazz.cast(ctor.newInstance());
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw Util.sneak(e);
        }
    }

    public static Externalizable newExternalizableInstance(final Class<? extends Externalizable> clazz) {
        Constructor<?> ctor = extNewInstances.get(clazz);
        if (ctor == null) {
            throw new IllegalArgumentException("No valid constructor found on Externalizable " + clazz);
        }
        try {
            return (Externalizable) ctor.newInstance();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw Util.sneak(e);
        }
    }

    public static boolean hasReadResolve(Class<?> clazz) {
        return readResolves.get(clazz) != null;
    }

    public static Object readResolve(Object object) throws ObjectStreamException {
        if (object == null) {
            return null;
        }
        MethodHandle rr = readResolves.get(object.getClass());
        if (rr == null) {
            throw new IllegalArgumentException("No readResolve method found on " + object.getClass());
        }
        try {
            return rr.invoke(object);
        } catch (RuntimeException | Error | ObjectStreamException e) {
            throw e;
        } catch (Throwable e) {
            throw Util.sneak(e);
        }
    }
}
