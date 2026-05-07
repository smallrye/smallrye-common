package io.smallrye.common.serial.impl.providers;

import static java.lang.invoke.MethodHandles.lookup;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.serial.Serialized;
import io.smallrye.common.serial.SerializedArrayClass;
import io.smallrye.common.serial.SerializedClass;
import io.smallrye.common.serial.SerializedEnumClass;
import io.smallrye.common.serial.SerializedExternalizableClass;
import io.smallrye.common.serial.SerializedNonSerializableClass;
import io.smallrye.common.serial.SerializedRecordClass;
import io.smallrye.common.serial.SerializedSerializableClass;
import io.smallrye.common.serial.impl.SerialObjectStreamField;
import io.smallrye.common.serial.impl.Util;
import io.smallrye.common.serial.spi.ObjectSerializer;

/**
 * Serializer that handles {@link Class} objects by producing the appropriate
 * {@link SerializedClass} subtype based on the class's serialization characteristics.
 */
public final class ClassSerializer implements ObjectSerializer {
    /**
     * Construct a new instance.
     */
    public ClassSerializer() {
    }

    public Serialized serialize(final Context ctxt, final Object object) throws IOException {
        if (object instanceof Class<?> clazz) {
            String name = clazz.getName();
            Serialized classLoader = ctxt.serialize(clazz.getClassLoader());

            if (clazz.isArray()) {
                SerializedClass componentType = (SerializedClass) ctxt.serialize(clazz.getComponentType());
                return new SerializedArrayClass(name, classLoader, componentType);
            }

            ObjectStreamClass osc = ObjectStreamClass.lookup(clazz);
            if (osc == null) {
                return new SerializedNonSerializableClass(name, classLoader);
            }

            long uid = osc.getSerialVersionUID();

            if (clazz.isEnum()) {
                return new SerializedEnumClass(name, classLoader, uid);
            }

            if (Externalizable.class.isAssignableFrom(clazz)) {
                return new SerializedExternalizableClass(name, classLoader, uid);
            }

            if (clazz.isRecord()) {
                // the JDK does not assign offsets to ObjectStreamField objects for record classes,
                // so we compute our own sequential layout
                Map<String, ObjectStreamField> fieldIndex = computeRecordFieldIndex(osc);
                int[] bufferSizes = computeBufferSizes(fieldIndex.values());
                return new SerializedRecordClass(name, classLoader, uid, fieldIndex, bufferSizes[0], bufferSizes[1]);
            }

            // fielded class (regular serializable) — compute field layout
            Map<String, ObjectStreamField> fieldIndex = computeFieldIndex(osc);
            int[] bufferSizes = computeBufferSizes(fieldIndex.values());

            // regular Serializable — walk superclass chain
            SerializedSerializableClass superClass = null;
            Class<?> sup = clazz.getSuperclass();
            if (sup != null && Serializable.class.isAssignableFrom(sup)) {
                superClass = (SerializedSerializableClass) ctxt.serialize(sup);
            }
            try {
                return (SerializedSerializableClass) newSerializedSerializableClass.invokeExact(
                        name, classLoader, superClass, fieldIndex, bufferSizes[0], bufferSizes[1], uid);
            } catch (Error | RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw Util.sneak(e);
            }
        } else {
            return ctxt.next();
        }
    }

    /**
     * Compute the field name → ObjectStreamField index for the given class descriptor.
     */
    private static Map<String, ObjectStreamField> computeFieldIndex(ObjectStreamClass osc) {
        ObjectStreamField[] fields = osc.getFields();
        return Stream.of(fields)
                .collect(Collectors.toMap(ObjectStreamField::getName, Function.identity()));
    }

    /**
     * Compute the field index for a record class, assigning sequential offsets.
     * The JDK does not assign offsets to {@link ObjectStreamField} objects for record classes,
     * so we create {@link SerialObjectStreamField} copies with explicitly computed offsets.
     *
     * @param osc the object stream class descriptor for a record class
     * @return a map of field name to {@link ObjectStreamField} with valid offsets
     */
    private static Map<String, ObjectStreamField> computeRecordFieldIndex(ObjectStreamClass osc) {
        ObjectStreamField[] fields = osc.getFields();
        Map<String, ObjectStreamField> result = new LinkedHashMap<>(fields.length);
        int primOffset = 0, objOffset = 0;
        for (ObjectStreamField field : fields) {
            SerialObjectStreamField copy = new SerialObjectStreamField(field.getName(), field.getType());
            if (field.isPrimitive()) {
                copy.setOffset(primOffset);
                primOffset += primFieldSize(field.getTypeCode());
            } else {
                copy.setOffset(objOffset++);
            }
            result.put(field.getName(), copy);
        }
        return result;
    }

    /**
     * Compute the primitive and object buffer sizes from a collection of fields with valid offsets.
     *
     * @param fields the fields to compute sizes from
     * @return a two-element array: {@code [primitiveBufferSize, objectBufferSize]}
     */
    private static int[] computeBufferSizes(Collection<ObjectStreamField> fields) {
        int po = 0, oo = 0;
        for (ObjectStreamField field : fields) {
            if (field.isPrimitive()) {
                int end = primFieldSize(field.getTypeCode()) + field.getOffset();
                if (end > po) {
                    po = end;
                }
            } else {
                int end = field.getOffset() + 1;
                if (end > oo) {
                    oo = end;
                }
            }
        }
        return new int[] { po, oo };
    }

    /**
     * {@return the byte size of a primitive field given its type code}
     *
     * @param typeCode the type code character
     */
    private static int primFieldSize(char typeCode) {
        return switch (typeCode) {
            case 'B', 'Z' -> 1;
            case 'C', 'S' -> 2;
            case 'I', 'F' -> 4;
            case 'J', 'D' -> 8;
            default -> throw Assert.impossibleSwitchCase(typeCode);
        };
    }

    public int priority() {
        return PRIORITY_CLASS;
    }

    private static final MethodHandle newSerializedSerializableClass;

    static {
        try {
            newSerializedSerializableClass = MethodHandles.privateLookupIn(SerializedSerializableClass.class, lookup())
                    .findConstructor(SerializedSerializableClass.class,
                            MethodType.methodType(void.class, String.class, Serialized.class,
                                    SerializedSerializableClass.class, Map.class, int.class, int.class, long.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw Util.asError(e);
        }
    }
}
