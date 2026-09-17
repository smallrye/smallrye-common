import io.smallrye.common.annotation.NativeAccess;

/**
 * Utilities for managing network addresses and information.
 */
@NativeAccess
module io.smallrye.common.net {
    requires io.smallrye.common.constraint;
    requires io.smallrye.common.os;
    requires static io.smallrye.common.annotation;

    requires io.smallrye.ffm;

    requires org.jboss.logging;
    requires static org.jboss.logging.annotations;

    exports io.smallrye.common.net;
}
