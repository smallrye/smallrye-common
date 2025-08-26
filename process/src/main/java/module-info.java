/**
 * SmallRye Common: Process.
 * Process management utilities.
 */
module io.smallrye.common.process {
    exports io.smallrye.common.process;

    requires static java.logging;
    requires io.smallrye.common.constraint;
    requires io.smallrye.common.function;
    requires io.smallrye.common.os;
    requires static org.jboss.logging.annotations;
    requires org.jboss.logging;
}
