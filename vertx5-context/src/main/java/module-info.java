/**
 * Utility classes related to Vert.x context manipulation.
 */
module io.smallrye.common.vertx {
    requires io.smallrye.common.constraint;
    requires io.vertx.core;

    exports io.smallrye.common.vertx;

    provides io.vertx.core.spi.VertxServiceProvider
            with io.smallrye.common.vertx.internal.ContextLocalLoader;
}
