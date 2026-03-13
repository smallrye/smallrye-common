import io.smallrye.common.annotation.NativeAccess;

/**
 * Utilities for managing network addresses and information.
 */
@NativeAccess
module io.smallrye.common.net {
    requires io.smallrye.common.constraint;
    requires org.jboss.logging;
    requires static org.graalvm.nativeimage;
    requires static org.graalvm.word;
    requires static org.jboss.logging.annotations;
    requires io.smallrye.ffm;
    requires io.smallrye.common.os;
    requires static io.smallrye.common.annotation;

    exports io.smallrye.common.net;
}
