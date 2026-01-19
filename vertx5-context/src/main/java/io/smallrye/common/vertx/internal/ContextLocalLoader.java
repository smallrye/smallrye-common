package io.smallrye.common.vertx.internal;

import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.internal.VertxBootstrap;
import io.vertx.core.spi.VertxServiceProvider;

/**
 * Service provider that ensures {@link VertxContext} class is loaded before Vert.x initialization.
 * <p>
 * This is necessary because {@link VertxContext} contains {@code ContextLocal} field registrations
 * that must be initialized before any Vert.x instance is created.
 * </p>
 * <p>
 * The Vert.x ServiceLoader mechanism loads this provider during Vert.x initialization,
 * which triggers the loading of {@link VertxContext} and its static {@code ContextLocal} fields.
 * </p>
 */
public class ContextLocalLoader implements VertxServiceProvider {

    static {
        // Trigger class loading to register ContextLocal fields before Vertx initialization
        // This ensures the static ContextLocal fields in VertxContext are initialized
        // at the right time in the Vertx lifecycle
        @SuppressWarnings("unused")
        Class<?> vertxContextClass = VertxContext.class;
    }

    @Override
    public void init(VertxBootstrap bootstrap) {
        // No-op - we just need the class to be loaded via the static initializer
    }

    /**
     * Ensures {@link VertxContext} is loaded before the first {@link io.vertx.core.Vertx} context is ready.
     */
    public ContextLocalLoader() {
        // Nothing
    }
}
