package io.smallrye.common.serial;

import io.smallrye.common.constraint.Assert;

/**
 * The serialized representation of an array class.
 * Array class descriptors carry a reference to the component type's class descriptor,
 * in addition to the JVM array class name and class loader inherited from {@link SerializedClass}.
 */
public final class SerializedArrayClass extends SerializedClass {

    private final SerializedClass componentType;

    /**
     * Construct a new instance.
     *
     * @param name the JVM array class name (e.g. {@code "[Ljava.lang.String;"}) (must not be {@code null})
     * @param classLoader the serialized class loader (must not be {@code null} but may be {@link SerializedNull#INSTANCE})
     * @param componentType the class descriptor of the array's component type (must not be {@code null})
     */
    public SerializedArrayClass(final String name, final Serialized classLoader, final SerializedClass componentType) {
        super(name, classLoader);
        this.componentType = Assert.checkNotNullParam("componentType", componentType);
    }

    /**
     * {@return the class descriptor of the array's component type (not {@code null})}
     * For multi-dimensional arrays, this is itself a {@code SerializedArrayClass}.
     */
    public SerializedClass componentType() {
        return componentType;
    }

    /**
     * {@return the number of array dimensions}
     * For example, {@code String[]} has 1 dimension, {@code int[][]} has 2 dimensions.
     */
    public int dimensions() {
        int dims = 1;
        SerializedClass ct = componentType;
        while (ct instanceof SerializedArrayClass arr) {
            dims++;
            ct = arr.componentType;
        }
        return dims;
    }

    /**
     * {@return the innermost non-array component type (not {@code null})}
     * For example, for {@code String[][]}, this returns the descriptor for {@code String}.
     */
    public SerializedClass leafComponentType() {
        SerializedClass ct = componentType;
        while (ct instanceof SerializedArrayClass arr) {
            ct = arr.componentType;
        }
        return ct;
    }
}
