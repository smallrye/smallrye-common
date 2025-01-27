/**
 * Constraint checking methods and annotations for SmallRye projects and their consumers.
 */
module io.smallrye.common.constraint {
    requires org.jboss.logging;
    requires static org.jboss.logging.annotations;

    exports io.smallrye.common.constraint;
}