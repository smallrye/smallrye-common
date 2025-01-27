/**
 * Reference classes which support strong references and attachments.
 */
module io.smallrye.common.ref {
    requires io.smallrye.common.constraint;
    requires static org.graalvm.nativeimage;

    exports io.smallrye.common.ref;
}