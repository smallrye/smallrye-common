package io.smallrye.common.vertx;

import java.util.Optional;

import io.smallrye.common.constraint.Assert;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Utilities to access Vert.x Context locals.
 */
public class ContextLocals {

    private ContextLocals() {
        // Avoid direct instantiation
    }

    private static Context ensureDuplicatedContext() {
        Context current = Vertx.currentContext();
        if (current == null || !VertxContext.isDuplicatedContext(current)) {
            throw new UnsupportedOperationException("Access to Context Locals are forbidden from a 'root' context  as " +
                    "it can leak data between unrelated processing. Make sure the method runs on a 'duplicated' (local)" +
                    " Context");
        }
        return current;
    }

    /**
     * Gets the value from the context local associated with the given key.
     *
     * @param key the key, must not be {@code null}
     * @param <T> the expected type of the associated value
     * @return an optional containing the associated value if any, empty otherwise.
     */
    public static <T> Optional<T> get(String key) {
        Context current = ensureDuplicatedContext();
        return Optional.ofNullable(current.getLocal(Assert.checkNotNullParam("key", key)));
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
        Context current = ensureDuplicatedContext();
        T local = current.getLocal(Assert.checkNotNullParam("key", key));
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
        current.putLocal(
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
        return current.removeLocal(Assert.checkNotNullParam("key", key));
    }

}
