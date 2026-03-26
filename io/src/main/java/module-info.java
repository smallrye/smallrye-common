/**
 * Utilities for file I/O and JAR file management.
 */
module io.smallrye.common.io {
    requires io.smallrye.common.constraint;

    requires org.jboss.logging;

    requires static org.jboss.logging.annotations;

    exports io.smallrye.common.io;
    exports io.smallrye.common.io.jar;
}