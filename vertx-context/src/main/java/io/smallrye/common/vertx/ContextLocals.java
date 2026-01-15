package io.smallrye.common.vertx;

import java.util.Optional;

import io.smallrye.common.constraint.Assert;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.internal.ContextInternal;

/**
 * Utilities to access Vert.x Context locals.
 */
public class ContextLocals {

    private ContextLocals() {
        // Avoid direct instantiation
    }

    private static ContextInternal ensureDuplicatedContext() {
        Context current = Vertx.currentContext();
        if (current == null || !VertxContext.isDuplicatedContext(current)) {
            throw new UnsupportedOperationException("Access to Context Locals are forbidden from a 'root' context  as " +
                    "it can leak data between unrelated processing. Make sure the method runs on a 'duplicated' (local)" +
                    " Context");
        }
        return (ContextInternal) current;
    }

    /**
     * Gets the value from the context local associated with the given key.
     *
     * @param key the key, must not be {@code null}
     * @param <T> the expected type of the associated value
     * @return an optional containing the associated value if any, empty otherwise.
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> get(String key) {
        Context current = ensureDuplicatedContext();
        var map = VertxContext.localContextData(current);
        return Optional.ofNullable((T) map.get(Assert.checkNotNullParam("key", key)));
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
    @SuppressWarnings("unchecked")
    public static <T> T get(String key, T def) {
        Context current = ensureDuplicatedContext();
        var map = VertxContext.localContextData(current);
        T local = (T) map.get(Assert.checkNotNullParam("key", key));
        if (local == null) {
            return def;
        }
        return local;
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
        Context current = ensureDuplicatedContext();
        var map = VertxContext.localContextData(current);
        map.put(
                Assert.checkNotNullParam("key", key),
                Assert.checkNotNullParam("value", value));
    }

    /**
     * Removes the value associated with the given key from the context locals.
     *
     * @param key the key, must not be {@code null}
     * @return {@code true} if there were a value associated with the given key. {@code false} otherwise.
     */
    public static boolean remove(String key) {
        Context current = ensureDuplicatedContext();
        var map = VertxContext.localContextData(current);
        return map.remove(Assert.checkNotNullParam("key", key)) != null;
    }

    /**
     * Gets the parent context of the current context.
     *
     * @return the parent context if any, {@code null} otherwise.
     */
    public static Context getParentContext() {
        var current = ensureDuplicatedContext();
        return current.getLocal(VertxContext.PARENT_CONTEXT_LOCAL);
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
        ensureDuplicatedContext();
        var k = Assert.checkNotNullParam("key", key);
        var v = Assert.checkNotNullParam("value", value);

        var parent = getParentContext();
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
     * Gets the value associated with the given key from the parent context.
     * <p>
     * If the parent context is not set, it returns an empty optional.
     *
     * @param key the key, must not be {@code null}
     * @param <T> the expected type of the associated value
     * @return an optional containing the associated value if any, empty otherwise.
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getFromParent(String key) {
        var k = Assert.checkNotNullParam("key", key);
        var parent = getParentContext();
        if (parent == null) {
            return Optional.empty();
        }
        var map = VertxContext.localContextData(parent);
        return Optional.ofNullable((T) map.get(k));
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
    @SuppressWarnings("unchecked")
    public static <T> T getFromParent(String key, T def) {
        var k = Assert.checkNotNullParam("key", key);
        var parent = getParentContext();
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
}
