package io.smallrye.common.serial.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import io.smallrye.common.constraint.Assert;
import sun.misc.Unsafe;

final class JDK24Specific {
    // IMPORTANT NOTE: Do not be alarmed at the complexity/inefficiency/use of Unsafe here.
    // The Java24+ version delegates directly to the JDK without doing all of this stuff.
    // Also, maybe don't look at the JDK implementation either :-I

    private static final Unsafe unsafe;
    private static final long ObjectStreamField_field_offset;
    private static final MethodHandle readObjectHandle;
    private static final MethodHandle writeObjectHandle;

    static {
        Field unsafeField;
        try {
            unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        } catch (NoSuchFieldException e) {
            throw Util.asError(e);
        }
        unsafeField.setAccessible(true);
        try {
            unsafe = (Unsafe) unsafeField.get(null);
        } catch (IllegalAccessException e) {
            throw Util.asError(e);
        }
        Field fieldField;
        try {
            fieldField = ObjectStreamField.class.getDeclaredField("field");
        } catch (NoSuchFieldException e) {
            throw Util.asError(e);
        }
        ObjectStreamField_field_offset = unsafe.objectFieldOffset(fieldField);
        try {
            readObjectHandle = MethodHandles.lookup().findStatic(
                    JDK24Specific.class,
                    "defaultReadObject",
                    MethodType.methodType(void.class, List.class, Object.class, ObjectInputStream.class));
            writeObjectHandle = MethodHandles.lookup().findStatic(
                    JDK24Specific.class,
                    "defaultWriteObject",
                    MethodType.methodType(void.class, List.class, Object.class, ObjectOutputStream.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw Util.asError(e);
        }
    }

    private JDK24Specific() {
    }

    static MethodHandle defaultWriteObjectForSerialization(final Class<?> type) {
        ObjectStreamClass osc = ObjectStreamClass.lookup(type);
        if (osc == null) {
            return null;
        }
        ObjectStreamField[] fields = osc.getFields();
        List<OutputStep> steps = new ArrayList<>(fields.length);
        for (ObjectStreamField field : fields) {
            // see if it has a real field
            Field f = (Field) unsafe.getObject(field, ObjectStreamField_field_offset);
            if (f != null) {
                // add a step
                steps.add(outputStep(f));
            }
        }
        return writeObjectHandle.bindTo(List.copyOf(steps));
    }

    static MethodHandle defaultReadObjectForSerialization(final Class<?> type) {
        ObjectStreamClass osc = ObjectStreamClass.lookup(type);
        if (osc == null) {
            return null;
        }
        ObjectStreamField[] fields = osc.getFields();
        List<InputStep> steps = new ArrayList<>(fields.length);
        for (ObjectStreamField field : fields) {
            // see if it has a real field
            Field f = (Field) unsafe.getObject(field, ObjectStreamField_field_offset);
            if (f != null) {
                // add a step
                steps.add(inputStep(f));
            }
        }
        return readObjectHandle.bindTo(List.copyOf(steps));
    }

    private static void defaultWriteObject(List<OutputStep> steps, Object obj, ObjectOutputStream oos) throws IOException {
        ObjectOutputStream.PutField pf = oos.putFields();
        for (OutputStep step : steps) {
            step.accept(obj, pf);
        }
        oos.writeFields();
    }

    private static void defaultReadObject(List<InputStep> steps, Object obj, ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField gf = ois.readFields();
        for (InputStep step : steps) {
            step.accept(obj, gf);
        }
    }

    static OutputStep outputStep(Field field) {
        Class<?> type = field.getType();
        String name = field.getName();
        long offset = unsafe.objectFieldOffset(field);
        if (type.isPrimitive()) {
            char c = type.descriptorString().charAt(0);
            return switch (c) {
                case 'B' -> (obj, pf) -> pf.put(name, unsafe.getByte(obj, offset));
                case 'C' -> (obj, pf) -> pf.put(name, unsafe.getChar(obj, offset));
                case 'D' -> (obj, pf) -> pf.put(name, unsafe.getDouble(obj, offset));
                case 'F' -> (obj, pf) -> pf.put(name, unsafe.getFloat(obj, offset));
                case 'I' -> (obj, pf) -> pf.put(name, unsafe.getInt(obj, offset));
                case 'J' -> (obj, pf) -> pf.put(name, unsafe.getLong(obj, offset));
                case 'S' -> (obj, pf) -> pf.put(name, unsafe.getShort(obj, offset));
                case 'Z' -> (obj, pf) -> pf.put(name, unsafe.getBoolean(obj, offset));
                default -> throw Assert.impossibleSwitchCase(c);
            };
        } else {
            return (obj, pf) -> pf.put(name, type.cast(unsafe.getObject(obj, offset)));
        }
    }

    interface OutputStep {
        void accept(Object obj, ObjectOutputStream.PutField pf) throws IOException;
    }

    static InputStep inputStep(Field field) {
        Class<?> type = field.getType();
        String name = field.getName();
        long offset = unsafe.objectFieldOffset(field);
        if (type.isPrimitive()) {
            char c = type.descriptorString().charAt(0);
            return switch (c) {
                case 'B' -> (obj, gf) -> unsafe.putByte(obj, offset, gf.get(name, (byte) 0));
                case 'C' -> (obj, gf) -> unsafe.putChar(obj, offset, gf.get(name, '\0'));
                case 'D' -> (obj, gf) -> unsafe.putDouble(obj, offset, gf.get(name, 0d));
                case 'F' -> (obj, gf) -> unsafe.putFloat(obj, offset, gf.get(name, 0f));
                case 'I' -> (obj, gf) -> unsafe.putInt(obj, offset, gf.get(name, 0));
                case 'J' -> (obj, gf) -> unsafe.putLong(obj, offset, gf.get(name, 0L));
                case 'S' -> (obj, gf) -> unsafe.putShort(obj, offset, gf.get(name, (short) 0));
                case 'Z' -> (obj, gf) -> unsafe.putBoolean(obj, offset, gf.get(name, false));
                default -> throw Assert.impossibleSwitchCase(c);
            };
        } else {
            return (obj, gf) -> unsafe.putObject(obj, offset, type.cast(gf.get(name, null)));
        }
    }

    interface InputStep {
        void accept(Object object, ObjectInputStream.GetField gf) throws IOException, ClassNotFoundException;
    }
}
