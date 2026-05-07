package io.smallrye.common.serial;

import java.io.IOException;
import java.io.NotSerializableException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.serial.impl.ClassLocal;
import io.smallrye.common.serial.impl.providers.ArrayDeserializer;
import io.smallrye.common.serial.impl.providers.ArraySerializer;
import io.smallrye.common.serial.impl.providers.BuiltInClassLoaderDeserializer;
import io.smallrye.common.serial.impl.providers.BuiltInClassLoaderSerializer;
import io.smallrye.common.serial.impl.providers.ClassDeserializer;
import io.smallrye.common.serial.impl.providers.ClassSerializer;
import io.smallrye.common.serial.impl.providers.EnumDeserializer;
import io.smallrye.common.serial.impl.providers.EnumSerializer;
import io.smallrye.common.serial.impl.providers.ExternalizableDeserializer;
import io.smallrye.common.serial.impl.providers.ExternalizableSerializer;
import io.smallrye.common.serial.impl.providers.ProxyDeserializer;
import io.smallrye.common.serial.impl.providers.ProxySerializer;
import io.smallrye.common.serial.impl.providers.ReadResolveDeserializer;
import io.smallrye.common.serial.impl.providers.RecordDeserializer;
import io.smallrye.common.serial.impl.providers.RecordSerializer;
import io.smallrye.common.serial.impl.providers.SerializableDeserializer;
import io.smallrye.common.serial.impl.providers.SerializableSerializer;
import io.smallrye.common.serial.impl.providers.StringDeserializer;
import io.smallrye.common.serial.impl.providers.StringSerializer;
import io.smallrye.common.serial.impl.providers.WriteReplaceSerializer;
import io.smallrye.common.serial.spi.ObjectDeserializer;
import io.smallrye.common.serial.spi.ObjectSerializer;
import io.smallrye.common.serial.spi.Prioritized;

/**
 * The main context for serialization and deserialization operations.
 * <p>
 * A serial context maintains identity maps for both directions (object→serialized and serialized→object)
 * to correctly handle circular references. Providers are tried in priority order (highest first) using
 * a chain-of-responsibility pattern.
 * <p>
 * Use {@link #builder()} to create and configure a new instance.
 */
public final class SerialContext implements Serializer, Deserializer {
    private final IdentityHashMap<Object, Serialized> serializedObjects = new IdentityHashMap<>();
    private final IdentityHashMap<Serialized, Object> deserializedObjects = new IdentityHashMap<>();
    private final IdentityHashMap<ClassLocal<?>, Map<Class<?>, Object>> classLocalCache = new IdentityHashMap<>();
    private final List<ObjectSerializer> serializers;
    private final List<ObjectDeserializer> deserializers;

    private SerialContext(final List<ObjectSerializer> serializers, final List<ObjectDeserializer> deserializers) {
        this.serializers = serializers;
        this.deserializers = deserializers;
    }

    /**
     * {@return a new builder for configuring a serial context}
     */
    public static Builder builder() {
        return new Builder();
    }

    public Object deserialize(final Serialized serialized) throws IOException, ClassNotFoundException {
        synchronized (deserializedObjects) {
            Assert.checkNotNullParam("serialized", serialized);
            if (serialized instanceof SerializedNull) {
                return null;
            }
            if (deserializedObjects.containsKey(serialized)) {
                return deserializedObjects.get(serialized);
            }
            Object obj = new DeserializerContextImpl(serialized).next();
            deserializedObjects.put(serialized, obj);
            return obj;
        }
    }

    public boolean hasDeserialized(final Serialized serialized) {
        synchronized (deserializedObjects) {
            return deserializedObjects.containsKey(serialized);
        }
    }

    public Serialized serialize(final Object object) throws IOException {
        synchronized (serializedObjects) {
            if (object == null) {
                return SerializedNull.INSTANCE;
            }
            Serialized serialized = serializedObjects.get(object);
            if (serialized != null) {
                return serialized;
            }
            serialized = new SerializerContextImpl(object).next();
            serializedObjects.put(object, serialized);
            return serialized;
        }
    }

    public boolean hasSerialized(final Object object) {
        synchronized (serializedObjects) {
            return object == null || serializedObjects.containsKey(object);
        }
    }

    /**
     * Compute and cache a per-class value for the lifetime of this context.
     *
     * @param local the class local key (must not be {@code null})
     * @param type the class to compute data for (must not be {@code null})
     * @param <T> the type of the cached value
     * @return the computed or cached value
     */
    @SuppressWarnings("unchecked")
    <T> T classLocal(ClassLocal<T> local, Class<?> type) {
        return (T) classLocalCache
                .computeIfAbsent(local, k -> new HashMap<>())
                .computeIfAbsent(type, local.compute());
    }

    /**
     * The deserialization context implementation, providing chain-of-responsibility
     * delegation and identity map management for a single deserialization operation.
     */
    public final class DeserializerContextImpl implements ObjectDeserializer.Context {
        private final Serialized serialized;
        /**
         * The index of the deserializer that will be called when {@code next()} is called.
         */
        private int current;

        private DeserializerContextImpl(final Serialized serialized) {
            this.serialized = serialized;
        }

        public void preSetObject(final Object obj) {
            deserializedObjects.put(serialized, obj);
        }

        public Object next() throws IOException, ClassNotFoundException {
            if (current == deserializers.size()) {
                throw new NotSerializableException("No deserializer available for " + serialized.getClass().getName());
            }
            try {
                Object instance = deserializers.get(current++).deserialize(this, serialized);
                preSetObject(instance);
                return instance;
            } finally {
                current--;
            }
        }

        public Object deserialize(final Serialized serialized) throws IOException, ClassNotFoundException {
            return SerialContext.this.deserialize(serialized);
        }

        public boolean hasDeserialized(final Serialized serialized) {
            return SerialContext.this.hasDeserialized(serialized);
        }
    }

    /**
     * The serialization context implementation, providing chain-of-responsibility
     * delegation and identity map management for a single serialization operation.
     */
    public final class SerializerContextImpl implements ObjectSerializer.Context {
        private final Object deserialized;
        /**
         * The index of the serializer that will be called when {@code next()} is called.
         */
        private int current;

        private SerializerContextImpl(final Object deserialized) {
            this.deserialized = deserialized;
        }

        public void preSetSerialized(final Serialized serialized) {
            serializedObjects.put(deserialized, serialized);
        }

        public Serialized next() throws IOException {
            if (current == serializers.size()) {
                throw new NotSerializableException(deserialized.getClass().getName());
            }
            try {
                Serialized serialized = serializers.get(current++).serialize(this, deserialized);
                preSetSerialized(serialized);
                return serialized;
            } finally {
                current--;
            }
        }

        public Serialized serialize(final Object object) throws IOException {
            return SerialContext.this.serialize(object);
        }

        public boolean hasSerialized(final Object object) {
            return SerialContext.this.hasSerialized(object);
        }

        /**
         * Compute and cache a per-class value for the lifetime of the enclosing context.
         *
         * @param local the class local key (must not be {@code null})
         * @param type the class to compute data for (must not be {@code null})
         * @param <T> the type of the cached value
         * @return the computed or cached value
         */
        <T> T classLocal(ClassLocal<T> local, Class<?> type) {
            return SerialContext.this.classLocal(local, type);
        }
    }

    /**
     * A builder for configuring and creating {@link SerialContext} instances.
     */
    public static final class Builder {
        private final List<ObjectSerializer> serializers = new ArrayList<>();
        private final List<ObjectDeserializer> deserializers = new ArrayList<>();

        private Builder() {
        }

        /**
         * Add the default built-in providers for standard Java serialization support.
         * This includes providers for {@code writeReplace}/{@code readResolve},
         * {@link Class}, built-in class loaders, enums, strings, dynamic proxies,
         * arrays, {@link java.io.Externalizable}, and {@link java.io.Serializable}.
         *
         * @return this builder
         */
        public Builder addDefaultProviders() {
            serializers.add(new WriteReplaceSerializer());
            serializers.add(new ClassSerializer());
            serializers.add(new BuiltInClassLoaderSerializer());
            serializers.add(new EnumSerializer());
            serializers.add(new StringSerializer());
            serializers.add(new ProxySerializer());
            serializers.add(new RecordSerializer());
            serializers.add(new ArraySerializer());
            serializers.add(new ExternalizableSerializer());
            serializers.add(new SerializableSerializer());

            deserializers.add(new ReadResolveDeserializer());
            deserializers.add(new ClassDeserializer());
            deserializers.add(new BuiltInClassLoaderDeserializer());
            deserializers.add(new EnumDeserializer());
            deserializers.add(new StringDeserializer());
            deserializers.add(new ProxyDeserializer());
            deserializers.add(new RecordDeserializer());
            deserializers.add(new ArrayDeserializer());
            deserializers.add(new ExternalizableDeserializer());
            deserializers.add(new SerializableDeserializer());
            return this;
        }

        /**
         * Discover and add providers from the given class loader using {@link ServiceLoader}.
         *
         * @param classLoader the class loader to search for providers (must not be {@code null})
         * @return this builder
         */
        public Builder addProvidersFrom(final ClassLoader classLoader) {
            Assert.checkNotNullParam("classLoader", classLoader);
            ServiceLoader.load(ObjectSerializer.class, classLoader).forEach(serializers::add);
            ServiceLoader.load(ObjectDeserializer.class, classLoader).forEach(deserializers::add);
            return this;
        }

        /**
         * Add a specific serializer instance.
         *
         * @param serializer the serializer to add (must not be {@code null})
         * @return this builder
         */
        public Builder addSerializer(final ObjectSerializer serializer) {
            serializers.add(Assert.checkNotNullParam("serializer", serializer));
            return this;
        }

        /**
         * Add a specific deserializer instance.
         *
         * @param deserializer the deserializer to add (must not be {@code null})
         * @return this builder
         */
        public Builder addDeserializer(final ObjectDeserializer deserializer) {
            deserializers.add(Assert.checkNotNullParam("deserializer", deserializer));
            return this;
        }

        /**
         * Build and return the configured {@link SerialContext}.
         * Providers are sorted by {@linkplain Prioritized#priority() priority} in descending order
         * (highest priority first).
         *
         * @return the new serial context (not {@code null})
         */
        public SerialContext build() {
            List<ObjectSerializer> sortedSerializers = new ArrayList<>(serializers);
            sortedSerializers.sort(Comparator.comparingInt(Prioritized::priority).reversed());
            List<ObjectDeserializer> sortedDeserializers = new ArrayList<>(deserializers);
            sortedDeserializers.sort(Comparator.comparingInt(Prioritized::priority).reversed());
            return new SerialContext(List.copyOf(sortedSerializers), List.copyOf(sortedDeserializers));
        }
    }
}
