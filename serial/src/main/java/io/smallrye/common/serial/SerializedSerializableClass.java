package io.smallrye.common.serial;

import java.io.ObjectStreamField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.serial.impl.SerialObjectStreamField;

/**
 * The serialized representation of a class that implements {@link java.io.Serializable}
 * (but not {@link java.io.Externalizable} and not a {@code record}).
 * <p>
 * In addition to the stream field layout inherited from {@link SerializedFieldedClass},
 * serializable class descriptors carry a reference to the nearest serializable superclass
 * in the serialization hierarchy.
 */
public final class SerializedSerializableClass extends SerializedFieldedClass {

    private final SerializedSerializableClass superClass;

    /**
     * Construct a new instance.
     *
     * @param name the class name (must not be {@code null})
     * @param classLoader the serialized class loader (must not be {@code null} but may be {@link SerializedNull#INSTANCE})
     * @param superClass the nearest serializable superclass descriptor, or {@code null} if none
     * @param fieldIndex the field index map (must not be {@code null})
     * @param primitiveBufferSize the size of the primitive buffer for this class
     * @param objectBufferSize the size of the object buffer for this class
     * @param uid the serial version UID
     */
    // called via MethodHandle from ClassSerializer
    SerializedSerializableClass(final String name, final Serialized classLoader, final SerializedSerializableClass superClass,
            final Map<String, ObjectStreamField> fieldIndex, final int primitiveBufferSize, final int objectBufferSize,
            final long uid) {
        super(name, classLoader, uid, fieldIndex, primitiveBufferSize, objectBufferSize);
        this.superClass = superClass;
    }

    /**
     * {@return the nearest serializable superclass descriptor in the serialization hierarchy,
     * or {@code null} if there is none}
     */
    public SerializedSerializableClass superClass() {
        return superClass;
    }

    /**
     * A builder for constructing {@link SerializedSerializableClass} instances from wire protocol data.
     */
    public static final class Builder {
        private Builder() {
        }

        private String name;
        private Serialized classLoader;
        private SerializedSerializableClass superClass;
        private List<SerialObjectStreamField> fields = List.of();
        private long uid;

        /**
         * Set the class name.
         *
         * @param name the class name (must not be {@code null})
         * @return this builder
         */
        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the class loader.
         *
         * @param classLoader the serialized class loader (must not be {@code null})
         * @return this builder
         */
        public Builder classLoader(final Serialized classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        /**
         * Set the superclass in the serialization hierarchy.
         *
         * @param superClass the serialized superclass, or {@code null} if none
         * @return this builder
         */
        public Builder superClass(final SerializedSerializableClass superClass) {
            this.superClass = superClass;
            return this;
        }

        /**
         * Set the serial version UID.
         *
         * @param uid the UID value
         * @return this builder
         */
        public Builder uid(final long uid) {
            this.uid = uid;
            return this;
        }

        /**
         * Add a serializable stream field.
         *
         * @param name the field name (must not be {@code null})
         * @param type the field type (must not be {@code null})
         * @return this builder
         */
        public Builder addField(final String name, final Class<?> type) {
            SerialObjectStreamField newField = new SerialObjectStreamField(name, type);
            switch (fields.size()) {
                case 0 -> fields = List.of(newField);
                case 1 -> fields = List.of(fields.get(0), newField);
                case 2 -> {
                    SerialObjectStreamField f0 = fields.get(0);
                    SerialObjectStreamField f1 = fields.get(1);
                    fields = new ArrayList<>(8);
                    fields.add(f0);
                    fields.add(f1);
                    fields.add(newField);
                }
                default -> fields.add(newField);
            }
            return this;
        }

        /**
         * Build and return the configured {@link SerializedSerializableClass}.
         *
         * @return the new instance (not {@code null})
         */
        public SerializedSerializableClass build() {
            String name = Assert.checkNotNullParam("name", this.name);
            Serialized classLoader = Assert.checkNotNullParam("classLoader", this.classLoader);
            SerialObjectStreamField[] array = fields.toArray(SerialObjectStreamField[]::new);
            Arrays.sort(array, Comparator.comparing(ObjectStreamField::getName));
            int po = 0, oo = 0;
            for (SerialObjectStreamField osf : array) {
                if (osf.isPrimitive()) {
                    osf.setOffset(po);
                    po += switch (osf.getTypeCode()) {
                        case 'B', 'Z' -> 1;
                        case 'C', 'S' -> 2;
                        case 'I', 'F' -> 4;
                        case 'J', 'D' -> 8;
                        default -> throw Assert.impossibleSwitchCase(osf.getTypeCode());
                    };
                } else {
                    osf.setOffset(oo++);
                }
            }
            final Map<String, ObjectStreamField> index = Stream.of(array).collect(Collectors.toMap(
                    ObjectStreamField::getName, Function.identity()));

            return new SerializedSerializableClass(name, classLoader, superClass, index, po, oo, uid);
        }
    }
}
