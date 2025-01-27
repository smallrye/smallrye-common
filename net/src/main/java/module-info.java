/**
 * Utilities for managing network addresses and information.
 */
module io.smallrye.common.net {
    requires io.smallrye.common.constraint;
    requires org.jboss.logging;
    requires static org.graalvm.nativeimage;
    requires static org.graalvm.word;
    requires static org.jboss.logging.annotations;

    exports io.smallrye.common.net;
}