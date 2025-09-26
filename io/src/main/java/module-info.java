/**
 * Utilities for managing JAR files.
 */
module io.smallrye.common.io {
    requires io.smallrye.common.constraint;

    requires org.jboss.logging;

    requires static org.jboss.logging.annotations;

    exports io.smallrye.common.io.jar;
}