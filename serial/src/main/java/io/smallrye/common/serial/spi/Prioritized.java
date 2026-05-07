package io.smallrye.common.serial.spi;

/**
 * A prioritized serializer/deserializer.
 * <p>
 * Serializers and deserializers are executed in order from the highest priority to the lowest priority.
 * If two have the same priority, an unspecified (but consistent) ordering is performed involving the class name
 * and other information.
 */
public sealed interface Prioritized permits ObjectDeserializer, ObjectSerializer {
    /**
     * The priority of {@code readResolve}/{@code writeReplace} operations.
     */
    int PRIORITY_REPLACE = 13_000;

    /**
     * The priority of {@code Class} serialization.
     */
    int PRIORITY_CLASS = 12_000;

    /**
     * The priority of basic type serialization, such as {@code String} and {@code Enum}.
     */
    int PRIORITY_BASIC = 11_000;

    /**
     * The priority of array serialization.
     */
    int PRIORITY_ARRAY = 10_000;

    /**
     * The priority of standard {@code Externalizable} serialization.
     */
    int PRIORITY_EXTERNALIZABLE = 9_000;

    /**
     * The priority of standard {@code Serializable} serialization.
     */
    int PRIORITY_SERIALIZABLE = 8_000;

    /**
     * The default priority of user-provided serializers and deserializers.
     */
    int PRIORITY_USER = 0;

    /**
     * {@return the priority}
     * By default, {@link #PRIORITY_USER} is returned.
     */
    default int priority() {
        return PRIORITY_USER;
    }
}
