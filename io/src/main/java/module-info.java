/**
 * Utilities for file I/O and JAR file management.
 */
module io.smallrye.common.io {
    requires static io.smallrye.common.annotation;

    requires io.smallrye.common.constraint;
    requires io.smallrye.common.os;
    requires io.smallrye.common.search;

    requires org.jboss.logging;

    requires static org.jboss.logging.annotations;

    exports io.smallrye.common.io;
    exports io.smallrye.common.io.archive;
    exports io.smallrye.common.io.jar;
}
