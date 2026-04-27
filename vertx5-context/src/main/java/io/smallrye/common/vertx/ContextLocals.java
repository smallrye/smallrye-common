package io.smallrye.common.vertx;

import java.util.Optional;

import io.smallrye.common.constraint.Assert;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.internal.ContextInternal;

/**
 * Utilities to access Vert.x Context locals of {@link VertxContext#DATA_MAP_LOCAL}.
 *
 * This facilitates the transition from Vert.x 4 by keeping a context as a {@link java.util.concurrent.ConcurrentHashMap}.
 * It is advised to gradually embrace the new Vert.x 5 {@link io.vertx.core.spi.context.storage.ContextLocal} API.
 */
public class ContextLocals {

    private ContextLocals() {
        // Avoid direct instantiation
    }

    private static ContextInternal ensureDuplicatedContext(Context context) {
        if (context == null || !VertxContext.isDuplicatedContext(context)) {
            throw new UnsupportedOperationException("Access to Context Locals are forbidden from a 'root' context  as " +
                    "it can leak data between unrelated processing. Make sure the method runs on a 'duplicated' (local)" +
                    " Context");
        }
        return (ContextInternal) context;
    }

    /**
     * Gets the value from the context local associated with the given key, using the given context.
     *
     * @param context the context, must be a duplicated context and not {@code null}
     * @param key the key, must not be {@code null}
     * @param <T> the expected type of the associated value
     * @return an optional containing the associated value if any, empty otherwise.
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> get(Context context, String key) {
        Context duplicatedContext = ensureDuplicatedContext(context);
        var map = VertxContext.localContextData(duplicatedContext);
        return Optional.ofNullable((T) map.get(Assert.checkNotNullParam("key", key)));
    }

    /**
     * Gets the value from the context local associated with the given key.
     *
     * @param key the key, must not be {@code null}
     * @param <T> the expected type of the associated value
     * @return an optional containing the associated value if any, empty otherwise.
     */
    public static <T> Optional<T> get(String key) {
        return get(Vertx.currentContext(), key);
    }

    /**
     * Gets the value from the context local associated with the given key, using the given context.
     * If there is no associated value, it returns the given default.
     *
     * @param context the context, must be a duplicated context and not {@code null}
     * @param key the key, must not be {@code null}
     * @param def the default value returned if there is no associated value with the given key.
     *        Can be {@code null}
     * @param <T> the expected type of the associated value
     * @return the associated value if any, the given default otherwise.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Context context, String key, T def) {
        Context duplicatedContext = ensureDuplicatedContext(context);
        var map = VertxContext.localContextData(duplicatedContext);
        T local = (T) map.get(Assert.checkNotNullParam("key", key));
        if (local == null) {
            return def;
        }
        return local;
    }

    /**
     * Gets the value from the context local associated with the given key.
     * If there is no associated value, it returns the given default.
     *
     * @param key the key, must not be {@code null}
     * @param def the default value returned if there is no associated value with the given key.
     *        Can be {@code null}
     * @param <T> the expected type of the associated value
     * @return the associated value if any, the given default otherwise.
     */
    public static <T> T get(String key, T def) {
        return get(Vertx.currentContext(), key, def);
    }

    /**
     * Stores the given key/value in the context local of the given context.
     * This method overwrites the existing value if any.
     *
     * @param context the context, must be a duplicated context and not {@code null}
     * @param key the key, must not be {@code null}
     * @param value the value, must not be {@code null}
     * @param <T> the expected type of the associated value
     */
    public static <T> void put(Context context, String key, T value) {
        Context duplicatedContext = ensureDuplicatedContext(context);
        var map = VertxContext.localContextData(duplicatedContext);
        map.put(
                Assert.checkNotNullParam("key", key),
                Assert.checkNotNullParam("value", value));
    }

    /**
     * Stores the given key/value in the context local.
     * This method overwrite the existing value if any.
     *
     * @param key the key, must not be {@code null}
     * @param value the value, must not be {@code null}
     * @param <T> the expected type of the associated value
     */
    public static <T> void put(String key, T value) {
        put(Vertx.currentContext(), key, value);
    }

    /**
     * Removes the value associated with the given key from the context locals of the given context.
     *
     * @param context the context, must be a duplicated context and not {@code null}
     * @param key the key, must not be {@code null}
     * @return {@code true} if there were a value associated with the given key. {@code false} otherwise.
     */
    public static boolean remove(Context context, String key) {
        Context duplicatedContext = ensureDuplicatedContext(context);
        var map = VertxContext.localContextData(duplicatedContext);
        return map.remove(Assert.checkNotNullParam("key", key)) != null;
    }

    /**
     * Removes the value associated with the given key from the context locals.
     *
     * @param key the key, must not be {@code null}
     * @return {@code true} if there were a value associated with the given key. {@code false} otherwise.
     */
    public static boolean remove(String key) {
        return remove(Vertx.currentContext(), key);
    }

    /**
     * Gets the parent context of the given context.
     *
     * @param context the context, must be a duplicated context and not {@code null}
     * @return the parent context if any, {@code null} otherwise.
     */
    public static Context getParentContext(Context context) {
        var current = ensureDuplicatedContext(context);
        return current.getLocal(VertxContext.PARENT_CONTEXT_LOCAL);
    }

    /**
     * Gets the parent context of the current context.
     *
     * @return the parent context if any, {@code null} otherwise.
     */
    public static Context getParentContext() {
        return getParentContext(Vertx.currentContext());
    }

    /**
     * Puts the given key/value in the parent context of the given context.
     * <p>
     * If the parent context cannot be found, it throws an {@link IllegalStateException}.
     * If the parent context is a root context, it throws an {@link IllegalStateException}.
     * If the parent context is a duplicated context, the value is put in the parent context locals.
     * <p>
     * Values put in the parent context can only be written once, subsequent writes will be ignored.
     * However, all the children of the parent context will have access to the value using
     * {@link #getFromParent(Context, String)}.
     *
     * @param context the context, must be a duplicated context and not {@code null}
     * @param key the key, must not be {@code null}
     * @param value the value, must not be {@code null}
     * @param <T> the expected type of the associated value
     * @return {@code true} if the value was put in the parent context, {@code false} otherwise.
     */
    public static <T> boolean putInParent(Context context, String key, T value) {
        var duplicatedContext = ensureDuplicatedContext(context);
        var k = Assert.checkNotNullParam("key", key);
        var v = Assert.checkNotNullParam("value", value);

        var parent = getParentContext(duplicatedContext);
        if (parent == null) {
            throw new IllegalStateException("Parent context is not set");
        }
        if (VertxContext.isDuplicatedContext(parent)) {
            var map = VertxContext.localContextData(parent);
            return map.putIfAbsent(k, v) == null;
        } else {
            throw new IllegalStateException("The parent context is a root context");
        }
    }

    /**
     * Puts the given key/value in the parent context.
     * <p>
     * If the parent context cannot be found, it throws an {@link IllegalStateException}.
     * If the parent context is a root context, it throws an {@link IllegalStateException}.
     * If the parent context is a duplicated context, the value is put in the parent context locals.
     * <p>
     * Values put in the parent context can only be written once, subsequent writes will be ignored.
     * However, all the children of the parent context will have access to the value using {@link #getFromParent(String)}.
     *
     * @param key the key, must not be {@code null}
     * @param value the value, must not be {@code null}
     * @param <T> the expected type of the associated value
     * @return {@code true} if the value was put in the parent context, {@code false} otherwise.
     */
    public static <T> boolean putInParent(String key, T value) {
        return putInParent(Vertx.currentContext(), key, value);
    }

    /**
     * Gets the value associated with the given key from the parent of the given context.
     * <p>
     * If the parent context is not set, it returns an empty optional.
     *
     * @param context the context, must be a duplicated context and not {@code null}
     * @param key the key, must not be {@code null}
     * @param <T> the expected type of the associated value
     * @return an optional containing the associated value if any, empty otherwise.
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getFromParent(Context context, String key) {
        var k = Assert.checkNotNullParam("key", key);
        var parent = getParentContext(context);
        if (parent == null) {
            return Optional.empty();
        }
        var map = VertxContext.localContextData(parent);
        return Optional.ofNullable((T) map.get(k));
    }

    /**
     * Gets the value associated with the given key from the parent context.
     * <p>
     * If the parent context is not set, it returns an empty optional.
     *
     * @param key the key, must not be {@code null}
     * @param <T> the expected type of the associated value
     * @return an optional containing the associated value if any, empty otherwise.
     */
    public static <T> Optional<T> getFromParent(String key) {
        return getFromParent(Vertx.currentContext(), key);
    }

    /**
     * Gets the value associated with the given key from the parent of the given context.
     * <p>
     * If the parent context is not set, it returns the given default.
     * If there is a parent context, but, there is no associated value with the given key, it returns the given default.
     *
     * @param context the context, must be a duplicated context and not {@code null}
     * @param key the key, must not be {@code null}
     * @param def the default value returned if there is no associated value with the given key.
     * @param <T> the expected type of the associated value
     * @return the associated value if any, the given default otherwise.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFromParent(Context context, String key, T def) {
        var k = Assert.checkNotNullParam("key", key);
        var parent = getParentContext(context);
        if (parent == null) {
            return def;
        }
        var map = VertxContext.localContextData(parent);
        T local = (T) map.get(k);
        if (local == null) {
            return def;
        }
        return local;
    }

    /**
     * Gets the value associated with the given key from the parent context.
     * <p>
     * If the parent context is not set, it returns the given default.
     * If there is a parent context, but, there is no associated value with the given key, it returns the given default.
     *
     * @param key the key, must not be {@code null}
     * @param def the default value returned if there is no associated value with the given key.
     * @param <T> the expected type of the associated value
     * @return the associated value if any, the given default otherwise.
     */
    public static <T> T getFromParent(String key, T def) {
        return getFromParent(Vertx.currentContext(), key, def);
    }
}
